/*
 * ODM Decision Service helper: scaffold boilerplate, add rules, check consistency, build.
 *
 * JDK only, no dependencies. Subcommands:
 *   init   Generate the XOM + rule project skeleton (all XML boilerplate, fresh UUIDs, build .properties).
 *   rule   Wrap a BAL body into a .brl file (new UUID, creates the .rulepackage if missing).
 *   check  Verify UUID uniqueness, cross-file UUID links, file naming, ruleflow packages, and common BAL mistakes.
 *   deps   Find which rules read what other rules write, check the ruleflow order, suggest one.
 *   ruleflow  Rewrite the ruleflow as a sequence of rule tasks (keeps its name and UUID).
 *   xom    Compile the XOM with --release 17 and package <xom>-1.0.0.jar (in-process javac).
 *   jdk    Show the ODM release of rules-compiler.jar and the JDK it must run on.
 *   build  Run the ODM rules compiler on the JDK its ODM release requires and print only the relevant lines.
 *   uuid   Print a fresh UUID (for hand-made .dta files).
 *
 * Run:    java -jar odm.jar <subcommand> -h        (any JDK 17+; or, without the jar: java Odm.java <subcommand> ...)
 * Build:  mvn -q package   (in scripts/; writes odm.jar next to this file. Without Maven:
 *         javac --release 17 -d out Odm.java && jar --create --file odm.jar --main-class Odm -C out .)
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class Odm {

    static final String VERSION = "1.0.0";
    static final String JACKSON_VERSION = "2.15.2";
    static final String[][] JACKSON = {{"core", "jackson-core"}, {"core", "jackson-databind"}, {"core", "jackson-annotations"}};

    static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    static final String NS_XMI = "xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\"";
    static final String NS_BASE = "xmlns:ilog.rules.studio.model.base=\"http://ilog.rules.studio/model/base.ecore\"";
    static final String NS_DS = "xmlns:com.ibm.rules.studio.model.decisionservice=\"http://com.ibm.rules.studio/model/decisionservice.ecore\"";

    /** Exit with a message on stderr, like Python's sys.exit("..."). */
    static final class Fail extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final int code;
        Fail(String msg) { this(msg, 1); }
        Fail(String msg, int code) { super(msg); this.code = code; }
    }

    public static void main(String[] argv) {
        int rc;
        try {
            rc = run(argv);
        } catch (Fail f) {
            if (f.getMessage() != null && !f.getMessage().isEmpty()) System.err.println(f.getMessage());
            rc = f.code;
        } catch (IOException e) {
            System.err.println("error: " + e);
            rc = 1;
        }
        System.out.flush();
        System.exit(rc);
    }

    static int run(String[] argv) throws IOException {
        if (argv.length == 0 || argv[0].equals("-h") || argv[0].equals("--help")) {
            System.out.println(USAGE);
            return argv.length == 0 ? 2 : 0;
        }
        if (argv[0].equals("--version")) {
            System.out.println("odm " + VERSION);
            return 0;
        }
        String cmd = argv[0];
        String[] rest = Arrays.copyOfRange(argv, 1, argv.length);
        switch (cmd) {
            case "init": return cmdInit(Args.parse(INIT, rest));
            case "rule": return cmdRule(Args.parse(RULE, rest));
            case "check": return cmdCheck(Args.parse(CHECK, rest));
            case "deps": return cmdDeps(Args.parse(DEPS, rest));
            case "ruleflow": return cmdRuleflow(Args.parse(RULEFLOW, rest));
            case "xom": return cmdXom(Args.parse(XOM, rest));
            case "jdk": return cmdJdk(Args.parse(JDK, rest));
            case "build": return cmdBuild(Args.parse(BUILD, rest));
            case "uuid": System.out.println(newUuid()); return 0;
            default: throw new Fail("unknown subcommand '" + cmd + "'\n\n" + USAGE, 2);
        }
    }

    // ------------------------------------------------------------------------- helpers

    static String newUuid() {
        return UUID.randomUUID().toString();
    }

    static String rel(Path p) {
        return relTo(p, Paths.get("").toAbsolutePath());
    }

    static String relTo(Path p, Path base) {
        try {
            return base.relativize(p.toAbsolutePath().normalize()).toString();
        } catch (IllegalArgumentException e) { // different roots (Windows drives)
            return p.toString();
        }
    }

    /** Path of p inside project root rp, always with '/' so it compares with href values. */
    static String relSlash(Path p, Path rp) {
        return rp.relativize(p).toString().replace(File.separatorChar, '/');
    }

    static void write(Path path, String content, boolean force) throws IOException {
        if (Files.exists(path) && !force) {
            System.out.println("skip (exists): " + rel(path));
            return;
        }
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
        System.out.println("wrote: " + rel(path));
    }

    static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    static String group(Pattern p, String s, int g) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(g) : null;
    }

    static List<String> findAll(Pattern p, String s) {
        List<String> out = new ArrayList<>();
        Matcher m = p.matcher(s);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    /** Percent-encoding equivalent to Python's urllib.parse.quote (safe chars: letters, digits, _.-~/). */
    static String quote(String s) {
        StringBuilder b = new StringBuilder();
        for (byte x : s.getBytes(StandardCharsets.UTF_8)) {
            int c = x & 0xff;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || "_.-~/".indexOf(c) >= 0) {
                b.append((char) c);
            } else {
                b.append('%').append(String.format("%02X", c));
            }
        }
        return b.toString();
    }

    /** Percent-decoding equivalent to Python's urllib.parse.unquote ('+' is left alone). */
    static String unquote(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] in = s.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < in.length; i++) {
            if (in[i] == '%' && i + 2 < in.length) {
                int hi = Character.digit(in[i + 1], 16), lo = Character.digit(in[i + 2], 16);
                if (hi >= 0 && lo >= 0) {
                    out.write(hi * 16 + lo);
                    i += 2;
                    continue;
                }
            }
            out.write(in[i]);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    static String repeat(List<String> items, java.util.function.Function<String, String> f) {
        return items.stream().map(f).collect(Collectors.joining());
    }

    // ------------------------------------------------------------------------- templates

    static String eclipseProject(String name, String builder, List<String> natures, List<String> refs) {
        String projects = repeat(refs, r -> "\n        <project>" + r + "</project>");
        String nat = repeat(natures, n -> "\n        <nature>" + n + "</nature>");
        return XML + "<projectDescription>\n    <name>" + name + "</name>\n    <comment></comment>\n"
                + "    <projects>" + projects + "\n    </projects>\n    <buildSpec>\n        <buildCommand>\n"
                + "            <name>" + builder + "</name>\n            <arguments>\n            </arguments>\n"
                + "        </buildCommand>\n    </buildSpec>\n    <natures>" + nat + "\n    </natures>\n</projectDescription>\n";
    }

    static String xomClasspath() {
        StringBuilder libs = new StringBuilder();
        for (String[] j : JACKSON) {
            libs.append("\n    <classpathentry kind=\"lib\" path=\"lib/").append(j[1]).append('-').append(JACKSON_VERSION).append(".jar\"/>");
        }
        return XML + "<classpath>\n    <classpathentry kind=\"src\" path=\"src\"/>\n"
                + "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>" + libs + "\n"
                + "    <classpathentry kind=\"output\" path=\"bin\"/>\n</classpath>\n";
    }

    static String ruleproject(String name, String xom, String base, Map<String, String> u) {
        String[][] folders = {
                {"ilog.rules.studio.model.base:SourceFolder", "rules"},
                {"ilog.rules.studio.model.bom:BOMFolder", "bom"},
                {"ilog.rules.studio.model.rule:TemplateFolder", "templates"},
                {"ilog.rules.studio.model.query:QueryFolder", "queries"},
                {"com.ibm.rules.studio.model.decisionservice:OperationFolder", "deployment"},
                {"ilog.rules.studio.model.base:ResourceFolder", "resources"}};
        StringBuilder mf = new StringBuilder();
        for (String[] f : folders) {
            mf.append("  <modelFolders xsi:type=\"").append(f[0]).append("\">\n    <name>").append(f[1]).append("</name>\n  </modelFolders>\n");
        }
        String ns = Stream.of("base", "bom", "query", "rule", "xom")
                .map(m -> "xmlns:ilog.rules.studio.model." + m + "=\"http://ilog.rules.studio/model/" + m + ".ecore\"")
                .collect(Collectors.joining(" "));
        return XML + "<ilog.rules.studio.model.base:RuleProject " + NS_XMI + " "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + NS_DS + " " + ns + " "
                + "buildMode=\"DecisionEngine\" isADecisionService=\"true\" migrationFlag=\"3\" MigratedToOperationId=\"" + u.get("dop") + "\">\n"
                + "  <name>" + name + "</name>\n  <uuid>" + u.get("project") + "</uuid>\n  <outputLocation>output</outputLocation>\n"
                + "  <categories>any</categories>\n"
                + "  <paths xsi:type=\"ilog.rules.studio.model.xom:XOMPath\" pathID=\"XOM\">\n"
                + "    <entries xsi:type=\"ilog.rules.studio.model.xom:LibraryXOMPathEntry\" name=\"org.eclipse.jdt.launching.JRE_CONTAINER\" url=\"file:org.eclipse.jdt.launching.JRE_CONTAINER\" kind=\"LIBRARY\"/>\n"
                + "    <entries xsi:type=\"ilog.rules.studio.model.xom:SystemXOMPathEntry\" name=\"" + xom + "\" url=\"platform:/" + xom + "\" kind=\"JAVA_PROJECT\"/>\n"
                + "  </paths>\n"
                + "  <paths xsi:type=\"ilog.rules.studio.model.bom:BOMPath\" pathID=\"BOM\">\n"
                + "    <entries xsi:type=\"ilog.rules.studio.model.bom:BOMEntry\" name=\"" + base + "\" url=\"platform:/" + name + "/bom/" + base + ".bom\" origin=\"xom:/" + name + "/" + xom + "\"/>\n"
                + "  </paths>\n" + mf + "</ilog.rules.studio.model.base:RuleProject>\n";
    }

    static String rulepackage(String name, String u) {
        return XML + "<ilog.rules.studio.model.base:RulePackage " + NS_XMI + " " + NS_BASE + ">\n  <name>" + name + "</name>\n"
                + "  <uuid>" + u + "</uuid>\n  <documentation><![CDATA[Rules for " + name + "]]></documentation>\n"
                + "</ilog.rules.studio.model.base:RulePackage>\n";
    }

    record Var(String name, String type, String direction) {}

    record Pkg(String name, String mode) {}

    static final Set<String> MODES = Set.of("Fastpath", "RetePlus");

    /** "validation:Fastpath,scoring+pricing:RetePlus" -> tasks; mode defaults to Fastpath. */
    static List<Pkg> parsePkgs(String specs) {
        List<Pkg> pkgs = new ArrayList<>();
        for (String spec : specs.split(",", -1)) {
            int i = spec.indexOf(':');
            String p = (i < 0 ? spec : spec.substring(0, i)).strip(), mode = i < 0 ? "Fastpath" : spec.substring(i + 1).strip();
            if (p.isEmpty()) throw new Fail("empty package name in --packages '" + specs + "'");
            if (!MODES.contains(mode)) throw new Fail("unknown execution mode '" + mode + "' (use " + String.join(", ", new TreeSet<>(MODES)) + ")");
            pkgs.add(new Pkg(p, mode));
        }
        return pkgs;
    }

    static String varset(String name, List<Var> vars, String u) {
        StringBuilder v = new StringBuilder();
        for (Var x : vars) {
            v.append("  <variables name=\"").append(x.name).append("\" type=\"").append(x.type)
                    .append("\" initialValue=\"\" verbalization=\"the ").append(x.name).append("\"/>\n");
        }
        return XML + "<ilog.rules.studio.model.base:VariableSet " + NS_XMI + " " + NS_BASE + ">\n  <name>" + name + "</name>\n"
                + "  <uuid>" + u + "</uuid>\n  <documentation><![CDATA[Variables for " + name + "]]></documentation>\n" + v
                + "</ilog.rules.studio.model.base:VariableSet>\n";
    }

    /** Linear ruleflow: one RuleTask per Pkg, in order. A Pkg name "a+b" puts packages a and b in the same task. */
    static String ruleflow(String name, List<Pkg> pkgs, String imports, String locale, String u) {
        List<String> tasks = new ArrayList<>(), nodes = new ArrayList<>(), trans = new ArrayList<>(), labels = new ArrayList<>();
        tasks.add("      <StartTask Identifier=\"task_0\"/>");
        for (int i = 1; i <= pkgs.size(); i++) {
            Pkg p = pkgs.get(i - 1);
            tasks.add("      <RuleTask ExecutionMode=\"" + p.mode + "\" Identifier=\"task_" + i + "\" Ordering=\"Default\">\n"
                    + "        <RuleList>\n" + Stream.of(p.name.split("\\+")).map(n -> "          <Package Name=\"" + n + "\"/>\n")
                            .collect(Collectors.joining()) + "        </RuleList>\n      </RuleTask>");
            labels.add("      <Data Name=\"node_" + i + "#label\">" + p.name + "</Data>");
        }
        int last = pkgs.size() + 1;
        tasks.add("      <StopTask Identifier=\"task_" + last + "\"/>");
        for (int i = 0; i <= last; i++) {
            nodes.add("      <TaskNode Identifier=\"node_" + i + "\" Task=\"task_" + i + "\"/>");
            if (i < last) trans.add("      <Transition Identifier=\"transition_" + i + "\" Source=\"node_" + i + "\" Target=\"node_" + (i + 1) + "\"/>");
        }
        return XML + "<ilog.rules.studio.model.ruleflow:RuleFlow " + NS_XMI + " "
                + "xmlns:ilog.rules.studio.model.ruleflow=\"http://ilog.rules.studio/model/ruleflow.ecore\">\n"
                + "  <name>" + name + "</name>\n  <uuid>" + u + "</uuid>\n  <locale>" + locale + "</locale>\n  <categories>any</categories>\n"
                + "  <rfModel>\n<Ruleflow xmlns=\"http://schemas.ilog.com/Rules/7.0/Ruleflow\">\n  <Body>\n"
                + "    <TaskList>\n" + String.join("\n", tasks) + "\n    </TaskList>\n    <NodeList>\n" + String.join("\n", nodes) + "\n    </NodeList>\n"
                + "    <TransitionList>\n" + String.join("\n", trans) + "\n    </TransitionList>\n  </Body>\n  <Resources>\n"
                + "    <ResourceSet Locale=\"" + locale + "\">\n" + String.join("\n", labels) + "\n    </ResourceSet>\n  </Resources>\n"
                + "  <Properties>\n    <imports><![CDATA[" + imports + "]]></imports>\n  </Properties>\n</Ruleflow>\n"
                + "  </rfModel>\n</ilog.rules.studio.model.ruleflow:RuleFlow>\n";
    }

    static String dop(String name, List<Var> vars, String rfl, Map<String, String> u) {
        String op = name + "Operation", vs = name + "Parameters";
        StringBuilder refs = new StringBuilder();
        for (Var x : vars) {
            refs.append("  <referencedVariables variableName=\"").append(x.name).append("\" variableSetName=\"").append(vs)
                    .append("\" direction=\"").append(x.direction).append("\">\n")
                    .append("    <variableSet href=\"../rules/").append(vs).append(".var#").append(u.get("var")).append("\"/>\n  </referencedVariables>\n");
        }
        return XML + "<com.ibm.rules.studio.model.decisionservice:Operation " + NS_XMI + " "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + NS_DS + " "
                + "xmlns:ilog.rules.studio.model.query.extractor=\"http://ilog.rules.studio/model/query/extractor.ecore\" "
                + "rulesetName=\"" + name + "Ruleset\" usingRuleflow=\"true\" ruleflowName=\"" + rfl + "\" targetRuleProjectName=\"" + name + "\">\n"
                + "  <name>" + op + "</name>\n  <uuid>" + u.get("dop") + "</uuid>\n" + refs
                + "  <ruleflow href=\"../rules/" + rfl + ".rfl#" + u.get("rfl") + "\"/>\n"
                + "  <extractor xsi:type=\"ilog.rules.studio.model.query.extractor:QueryExtractor\" name=\"" + op + "_extractor\" validator=\"Default Validator\"/>\n"
                + "  <targetRuleProject href=\"../../" + quote(name) + "#" + u.get("project") + "\"/>\n"
                + "</com.ibm.rules.studio.model.decisionservice:Operation>\n";
    }

    static String dep(String name, String version, Map<String, String> u) {
        String op = name + "Operation";
        String[][] policies = {
                {"Increment minor version numbers", " ruleset=\"INCREMENT_MINOR\" default=\"true\"",
                        "Updates the minor version for each ruleset. Makes the new version available but retains previous versions."},
                {"Use the base version numbers", "",
                        "Uses the numbers provided in the deployment configuration. Replaces the latest version of each ruleset with this release. Used for hot fixes or development."},
                {"The user can define the version numbers", " ruleset=\"MANUAL\"",
                        "Allows you to enter your own version numbers. Used for hot fixes or updates to an earlier release."}};
        StringBuilder pol = new StringBuilder();
        for (String[] p : policies) {
            pol.append("  <versionPolicies label=\"").append(p[0]).append('"').append(p[1]).append(" recurrent=\"true\">\n")
                    .append("    <description><![CDATA[").append(p[2]).append("]]></description>\n  </versionPolicies>\n");
        }
        return XML + "<com.ibm.rules.studio.model.decisionservice:Deployment " + NS_XMI + " " + NS_DS + " "
                + "ruleAppName=\"" + name + "\" managingXom=\"true\">\n  <name>" + name + "</name>\n  <uuid>" + u.get("dep") + "</uuid>\n"
                + "  <operations operationName=\"" + op + "\">\n    <operation href=\"" + op + ".dop#" + u.get("dop") + "\"/>\n"
                + "    <properties key=\"ruleset.version\">\n      <value><![CDATA[" + version + "]]></value>\n    </properties>\n"
                + "  </operations>\n" + pol + "</com.ibm.rules.studio.model.decisionservice:Deployment>\n";
    }

    static String b2xa(String u) {
        return XML + "<b2x:translation xmlns:b2x=\"http://schemas.ilog.com/JRules/1.3/Translation\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://schemas.ilog.com/JRules/1.3/Translation ilog/rules/schemas/1_3/b2x.xsd\">\n"
                + "    <id>" + u + "</id>\n    <lang>ARL</lang>\n</b2x:translation>\n";
    }

    /** Maven build of the XOM, same layout and jar as `odm xom`: src/ -> bin/, <xom>-1.0.0.jar next to the pom. */
    static String xomPom(String xom, String pkg) {
        StringBuilder deps = new StringBuilder();
        for (String[] j : JACKSON) {
            deps.append("        <dependency>\n            <groupId>com.fasterxml.jackson.").append(j[0]).append("</groupId>\n")
                    .append("            <artifactId>").append(j[1]).append("</artifactId>\n")
                    .append("            <version>${jackson.version}</version>\n        </dependency>\n");
        }
        return XML + "<!--\n  Optional Maven build of the XOM jar " + xom + "-1.0.0.jar, for users who prefer Maven.\n"
                + "  `odm xom` builds the same jar with only a JDK; nothing requires this file.\n"
                + "  Usage: mvn -q package      (Java 17 bytecode: loads on the JDK of every ODM 9.x release)\n-->\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n\n"
                + "    <groupId>" + pkg + "</groupId>\n    <artifactId>" + xom + "</artifactId>\n    <version>1.0.0</version>\n"
                + "    <packaging>jar</packaging>\n\n"
                + "    <properties>\n        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n"
                + "        <maven.compiler.release>17</maven.compiler.release>\n"
                + "        <jackson.version>" + JACKSON_VERSION + "</jackson.version>\n"
                + "        <project.build.outputTimestamp>2026-01-01T00:00:00Z</project.build.outputTimestamp>\n    </properties>\n\n"
                + "    <dependencies>\n" + deps + "    </dependencies>\n\n"
                + "    <build>\n        <sourceDirectory>src</sourceDirectory>\n        <outputDirectory>bin</outputDirectory>\n"
                + "        <plugins>\n            <plugin>\n                <groupId>org.apache.maven.plugins</groupId>\n"
                + "                <artifactId>maven-compiler-plugin</artifactId>\n                <version>3.13.0</version>\n"
                + "            </plugin>\n            <plugin>\n                <groupId>org.apache.maven.plugins</groupId>\n"
                + "                <artifactId>maven-jar-plugin</artifactId>\n                <version>3.4.2</version>\n"
                + "                <configuration>\n                    <outputDirectory>${project.basedir}</outputDirectory>\n"
                + "                    <archive>\n                        <addMavenDescriptor>false</addMavenDescriptor>\n"
                + "                    </archive>\n                </configuration>\n            </plugin>\n"
                + "        </plugins>\n    </build>\n</project>\n";
    }

    static String brl(String name, String body, String locale, String u) {
        return XML + "<ilog.rules.studio.model.brl:ActionRule " + NS_XMI + " "
                + "xmlns:ilog.rules.studio.model.brl=\"http://ilog.rules.studio/model/brl.ecore\">\n"
                + "  <name>" + name + "</name>\n  <uuid>" + u + "</uuid>\n  <locale>" + locale + "</locale>\n"
                + "  <definition><![CDATA[" + body.strip() + "]]></definition>\n</ilog.rules.studio.model.brl:ActionRule>\n";
    }

    // ------------------------------------------------------------------------- init

    static int cmdInit(Args a) throws IOException {
        String name = a.get("name");
        if (!name.matches("[A-Za-z][A-Za-z0-9]*")) {
            throw new Fail("--name must be camelCase with no spaces/underscores, got '" + name + "'");
        }
        String xom = a.get("xom"), base = a.get("base"), pkg = a.get("package"), locale = a.get("locale");
        boolean f = a.flag("force");
        Path root = Paths.get(a.get("dir")).toAbsolutePath().normalize();
        Path xomDir = root.resolve(xom), rp = root.resolve(name);

        List<Var> vars = new ArrayList<>();
        for (String spec : a.all("var")) {
            String[] parts = spec.split(":");
            if (parts.length < 2) throw new Fail("--var must be name:Type[:IN|OUT|IN_OUT], got '" + spec + "'");
            String vtype = parts[1].contains(".") ? parts[1] : pkg + "." + parts[1];
            vars.add(new Var(parts[0], vtype, parts.length > 2 ? parts[2] : "IN_OUT"));
        }
        List<Pkg> pkgs = parsePkgs(a.get("packages"));
        Map<String, String> u = new HashMap<>();
        for (String k : List.of("project", "dop", "dep", "var", "rfl", "bom", "b2xa", "voc")) u.put(k, newUuid());

        // XOM project
        write(xomDir.resolve(".project"),
                eclipseProject(xom, "org.eclipse.jdt.core.javabuilder", List.of("org.eclipse.jdt.core.javanature"), List.of()), f);
        write(xomDir.resolve(".classpath"), xomClasspath(), f);
        write(xomDir.resolve("pom.xml"), xomPom(xom, pkg), f);
        Files.createDirectories(xomDir.resolve("src").resolve(pkg.replace('.', File.separatorChar)));
        Path lib = xomDir.resolve("lib");
        Files.createDirectories(lib);
        if (a.flag("fetch-jackson")) {
            for (String[] j : JACKSON) {
                Path dest = lib.resolve(j[1] + "-" + JACKSON_VERSION + ".jar");
                if (!Files.exists(dest)) {
                    String url = "https://repo1.maven.org/maven2/com/fasterxml/jackson/" + j[0] + "/" + j[1] + "/"
                            + JACKSON_VERSION + "/" + j[1] + "-" + JACKSON_VERSION + ".jar";
                    download(url, dest);
                    System.out.println("fetched: " + dest);
                }
            }
        }

        // Rule project
        write(rp.resolve(".project"),
                eclipseProject(name, "ilog.rules.studio.model.ruleBuilder",
                        List.of("ilog.rules.studio.model.decisionProject", "ilog.rules.studio.model.operationProject",
                                "ilog.rules.studio.model.ruleNature"), List.of(xom)), f);
        write(rp.resolve(".ruleproject"), ruleproject(name, xom, base, u), f);
        write(rp.resolve(".gitignore"), "output/\nreports/\n.syncEntries\n", f);
        for (String d : List.of("templates", "queries", "resources")) Files.createDirectories(rp.resolve(d));
        write(rp.resolve("bom").resolve(base + ".bom"),
                "property loadGetterSetterAsProperties \"true\"\nproperty origin \"xom:/" + name + "/" + xom + "\"\n"
                        + "property uuid \"" + u.get("bom") + "\"\npackage " + pkg + ";\n\n// TODO: class declarations\n", f);
        write(rp.resolve("bom").resolve(base + ".b2xa"), b2xa(u.get("b2xa")), f);
        write(rp.resolve("bom").resolve(base + "_" + locale + ".voc"),
                "# Vocabulary Properties\nuuid = " + u.get("voc") + "\n\n# TODO: concept labels and phrases\n", f);
        write(rp.resolve("rules").resolve(name + "Parameters.var"), varset(name + "Parameters", vars, u.get("var")), f);
        for (Pkg p : pkgs) write(rp.resolve("rules").resolve(p.name).resolve(".rulepackage"), rulepackage(p.name, newUuid()), f);
        String rfl = a.get("ruleflow");
        write(rp.resolve("rules").resolve(rfl + ".rfl"), ruleflow(rfl, pkgs, "use " + pkg + ";\n", locale, u.get("rfl")), f);
        write(rp.resolve("deployment").resolve(name + "Operation.dop"), dop(name, vars, rfl, u), f);
        write(rp.resolve("deployment").resolve(name + ".dep"), dep(name, a.get("version"), u), f);

        // Build properties (paths relative to the properties file's directory; the compiler resolves them from there)
        Path props = (a.get("props") != null ? Paths.get(a.get("props")) : root.resolve(name + ".properties")).toAbsolutePath().normalize();
        Path pdir = props.getParent();
        write(props, "project = " + relTo(rp, pdir) + "\noutput = " + relTo(rp.resolve("output"), pdir) + "\ndep = " + name + "\n"
                + "xom-classpath = " + relTo(xomDir.resolve(xom + "-1.0.0.jar"), pdir) + "\nruleapp-name = " + name + "\n", f);
        System.out.println("\nNext: write XOM classes in " + xomDir.resolve("src") + ", fill the .bom/.voc, add rules with "
                + "`odm rule`, then `odm xom " + rel(xomDir) + "`, `odm check " + rp + "` and `odm build " + props + "`.");
        return 0;
    }

    static void download(String url, Path dest) throws IOException {
        Path tmp = dest.resolveSibling(dest.getFileName() + ".part");
        try (InputStream in = URI.create(url).toURL().openStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    // ------------------------------------------------------------------------- rule

    static int cmdRule(Args a) throws IOException {
        Path rp = Paths.get(a.pos(0)).toAbsolutePath().normalize();
        String pkg = a.pos(1), name = a.pos(2);
        String body = a.get("file") != null ? read(Paths.get(a.get("file"))) : new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) throw new Fail("empty BAL body (pass --file or pipe it on stdin)");
        Path pdir = rp.resolve("rules").resolve(pkg);
        if (!Files.exists(pdir.resolve(".rulepackage"))) {
            write(pdir.resolve(".rulepackage"), rulepackage(pkg, newUuid()), false);
            System.out.println("note: new package '" + pkg + "' — add it to the ruleflow if it isn't there");
        }
        String fname = name.strip().replaceAll("[^A-Za-z0-9_-]+", "-").replaceAll("^-+|-+$", "").toLowerCase(Locale.ROOT);
        write(pdir.resolve(fname + ".brl"), brl(name, body, a.get("locale"), newUuid()), a.flag("force"));
        for (String msg : lintBal(body)) System.out.println("lint: " + msg);
        return 0;
    }

    // ------------------------------------------------------------------------- check

    static final Set<String> RESERVED_LABEL_TOKENS = Set.of("elapsed", "km", "distance", "travel", "speed", "minutes", "velocity",
            "span", "geolocation", "increase", "decrease", "by", "points", "notifications", "location");
    static final Object[][] BAL_PATTERNS = {
            {Pattern.compile("(>=|<=|==|!=|\\s>\\s|\\s<\\s)", Pattern.CASE_INSENSITIVE),
                    "use 'is at least/at most/more than/less than' or 'is'/'is not', not symbols"},
            {Pattern.compile("\\bis\\s+(not\\s+)?equal\\s+to\\b", Pattern.CASE_INSENSITIVE),
                    "'is equal to' fails ('January' expected) — use plain 'is' / 'is not'"},
            {Pattern.compile("\\bis\\s+in\\s*\\{", Pattern.CASE_INSENSITIVE), "use 'is one of { ... }', not 'is in { ... }'"},
            {Pattern.compile("\\bis\\s+not\\s+defined\\b", Pattern.CASE_INSENSITIVE), "'is not defined' is not BAL — use 'is not null'"},
            {Pattern.compile("\\bnot\\s*\\(", Pattern.CASE_INSENSITIVE), "negate with 'it is not true that ...', not 'not (...)'"},
    };
    static final Pattern STRING_LITERAL = Pattern.compile("\"[^\"\\n]*\"");
    static final Pattern THEN_BLOCK = Pattern.compile("\\bthen\\b(.*?)(\\belse\\b|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    static List<String> lintBal(String body) {
        List<String> out = new ArrayList<>();
        body = STRING_LITERAL.matcher(body).replaceAll("\"\""); // ignore operators inside string literals
        for (Object[] p : BAL_PATTERNS) {
            if (((Pattern) p[0]).matcher(body).find()) out.add((String) p[1]);
        }
        Matcher m = THEN_BLOCK.matcher(body);
        if (m.find()) {
            List<String> stmts = m.group(1).strip().lines().map(String::strip).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            if (!stmts.isEmpty() && !stmts.get(stmts.size() - 1).endsWith(";")) out.add("last 'then' statement must end with ';'");
        }
        return out;
    }

    static final Set<String> UUID_EXTS = Set.of(".brl", ".var", ".rfl", ".dop", ".dep", ".ruleproject", ".rulepackage", ".dta",
            ".bom", ".b2xa", ".voc");
    static final Pattern[] UUID_PATTERNS = {
            Pattern.compile("<uuid>([^<]+)</uuid>"), Pattern.compile("property uuid \"([^\"]+)\""),
            Pattern.compile("<id>([^<]+)</id>"), Pattern.compile("^uuid\\s*=\\s*(\\S+)", Pattern.MULTILINE)};
    static final Pattern NAME = Pattern.compile("<name>([^<]+)</name>");
    static final Pattern UUID_TAG = Pattern.compile("<uuid>([^<]+)</uuid>");
    static final Pattern DEFINITION = Pattern.compile("<definition><!\\[CDATA\\[(.*?)\\]\\]></definition>", Pattern.DOTALL);
    static final Pattern DTA_PARAM = Pattern.compile("<Param><!\\[CDATA\\[([A-Za-z_][A-Za-z0-9_ ]*)\\]\\]></Param>");

    static String ext(Path p) {
        String n = p.getFileName().toString();
        int i = n.lastIndexOf('.');
        return i <= 0 ? n : n.substring(i); // ".rulepackage" -> ".rulepackage", like os.path.splitext fallback
    }

    static List<Path> glob(Path dir, String suffix) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(suffix)
                    && !p.getFileName().toString().startsWith(".")).sorted().collect(Collectors.toList());
        }
    }

    static String stem(Path p) {
        String n = p.getFileName().toString();
        int i = n.lastIndexOf('.');
        return i <= 0 ? n : n.substring(0, i);
    }

    static int cmdCheck(Args a) throws IOException {
        Path rp = Paths.get(a.pos(0)).toAbsolutePath().normalize();
        boolean lint = !a.flag("no-lint");
        List<String> errs = new ArrayList<>(), warns = new ArrayList<>();

        Path rpf = rp.resolve(".ruleproject");
        if (!Files.exists(rpf)) throw new Fail("no .ruleproject in " + rp);

        // Project files, skipping hidden files/dirs (except .rulepackage/.ruleproject) and the output/ folder
        List<Path> files;
        try (Stream<Path> s = Files.walk(rp)) {
            files = s.filter(Files::isRegularFile).filter(p -> {
                Path r = rp.relativize(p);
                for (int i = 0; i < r.getNameCount(); i++) {
                    String seg = r.getName(i).toString();
                    boolean isFile = i == r.getNameCount() - 1;
                    if (seg.equals("output") && !isFile) return false;
                    if (seg.startsWith(".") && !(isFile && (seg.equals(".rulepackage") || seg.equals(".ruleproject")))) return false;
                }
                return true;
            }).sorted(Comparator.comparing(Path::toString)).collect(Collectors.toList());
        }

        List<String[]> ids = new ArrayList<>(); // {uuid, relative file}
        for (Path p : files) {
            String e = ext(p);
            if (!UUID_EXTS.contains(e)) continue;
            String t = read(p), r = relSlash(p, rp);
            for (Pattern up : UUID_PATTERNS) {
                String found = group(up, t, 1);
                if (found != null) {
                    ids.add(new String[]{found.strip(), r});
                    break;
                }
            }
            if (lint && e.equals(".brl")) {
                String d = group(DEFINITION, t, 1);
                if (d != null) for (String m : lintBal(d)) warns.add(r + ": " + m);
            }
            if (lint && e.equals(".dta")) {
                for (String v : findAll(DTA_PARAM, t)) {
                    if (!v.equals("true") && !v.equals("false")) warns.add(r + ": string Param '" + v + "' must be quoted: \"" + v + "\"");
                }
            }
        }
        Map<String, List<String>> byUuid = new LinkedHashMap<>();
        for (String[] i : ids) byUuid.computeIfAbsent(i[0], k -> new ArrayList<>()).add(i[1]);
        byUuid.forEach((i, fs) -> {
            if (fs.size() > 1) errs.add("duplicate UUID " + i + " in: " + String.join(", ", fs));
        });
        Map<String, String> byId = new HashMap<>();
        for (String[] i : ids) byId.put(i[0], i[1]);

        String rpt = read(rpf);
        String pname = group(NAME, rpt, 1), puuid = group(UUID_TAG, rpt, 1);
        if (pname == null || puuid == null) throw new Fail(".ruleproject has no <name> or <uuid>");
        if (pname.contains(" ")) warns.add("project name '" + pname + "' contains spaces — use camelCase");
        if (!rpt.contains("buildMode=\"DecisionEngine\"")) errs.add(".ruleproject: buildMode must be \"DecisionEngine\"");
        String mig = group(Pattern.compile("MigratedToOperationId=\"([^\"]*)\""), rpt, 1);

        Map<String, String[]> dopIds = new HashMap<>(); // file name -> {uuid, name}
        for (Path p : glob(rp.resolve("deployment"), ".dop")) {
            String t = read(p), r = relSlash(p, rp);
            dopIds.put(p.getFileName().toString(), new String[]{group(UUID_TAG, t, 1), group(NAME, t, 1)});
            String tr = group(Pattern.compile("<targetRuleProject href=\"[^#]*#([^\"]+)\""), t, 1);
            if (tr == null || !tr.equals(puuid)) errs.add(r + ": targetRuleProject UUID != .ruleproject uuid " + puuid);
            String trn = group(Pattern.compile("targetRuleProjectName=\"([^\"]*)\""), t, 1);
            if (trn == null || !trn.equals(pname)) errs.add(r + ": targetRuleProjectName != '" + pname + "'");
            Matcher hm = Pattern.compile("href=\"\\.\\./rules/([^\"#]+)#([^\"]+)\"").matcher(t);
            while (hm.find()) {
                String f = unquote(hm.group(1)), hu = hm.group(2);
                if (!("rules/" + f).equals(byId.get(hu))) errs.add(r + ": href rules/" + f + "#" + hu + " does not match that file's UUID");
            }
            String rfn = group(Pattern.compile("ruleflowName=\"([^\"]*)\""), t, 1);
            if (rfn != null && !Files.exists(rp.resolve("rules").resolve(rfn + ".rfl"))) {
                errs.add(r + ": ruleflowName '" + rfn + "' has no rules/" + rfn + ".rfl");
            }
        }
        if (mig != null && dopIds.values().stream().noneMatch(d -> mig.equals(d[0]))) {
            errs.add(".ruleproject: MigratedToOperationId does not match any .dop uuid");
        }

        Pattern opBlock = Pattern.compile("<operations\\b.*?</operations>", Pattern.DOTALL);
        for (Path p : glob(rp.resolve("deployment"), ".dep")) {
            String t = read(p), r = relSlash(p, rp);
            if (!Pattern.compile("ruleAppName=\"[^\"]+\"").matcher(t).find()) {
                errs.add(r + ": missing/empty ruleAppName (-> 'A RuleApp name cannot be empty')");
            }
            if (t.contains("<targets")) warns.add(r + ": remove <targets> (no target server in generated .dep)");
            Matcher bm = opBlock.matcher(t);
            while (bm.find()) {
                String block = bm.group();
                String on = group(Pattern.compile("operationName=\"([^\"]*)\""), block, 1);
                Matcher h = Pattern.compile("href=\"([^\"#]+)#([^\"]+)\"").matcher(block);
                if (!block.contains("key=\"ruleset.version\"")) errs.add(r + ": operation " + on + " lacks ruleset.version property");
                if (h.find()) {
                    String hf = unquote(h.group(1)), hu = h.group(2);
                    String[] d = dopIds.get(hf);
                    if (d == null || !hu.equals(d[0])) errs.add(r + ": operation href " + hf + "#" + hu + " != .dop uuid");
                    else if (!d[1].equals(on)) errs.add(r + ": operationName '" + on + "' != .dop name '" + d[1] + "'");
                }
            }
        }

        Path bomDir = rp.resolve("bom");
        List<String> b2x = glob(bomDir, ".b2xa").stream().map(Odm::stem).collect(Collectors.toList());
        List<String> vocs = glob(bomDir, ".voc").stream().map(p -> p.getFileName().toString()).collect(Collectors.toList());
        Pattern boolIs = Pattern.compile("public\\s+(?:readonly\\s+)?boolean\\s+(is[A-Z]\\w*)\\s*[;\\s]");
        Pattern reservedProp = Pattern.compile("\\s(operator|function|rule|package|import)\\s*;");
        for (Path bp : glob(bomDir, ".bom")) {
            String b = stem(bp);
            if (!b2x.contains(b)) errs.add("bom/" + b + ".b2xa missing (must share the .bom base name)");
            Pattern vocName = Pattern.compile(Pattern.quote(b) + "_[a-z]{2}_[A-Z]{2}\\.voc");
            if (vocs.stream().noneMatch(v -> vocName.matcher(v).matches())) {
                errs.add("bom/" + b + "_<locale>.voc missing (must share the .bom base name + locale suffix)");
            }
            String bt = read(bp);
            if (bt.stripLeading().startsWith("<")) errs.add("bom/" + b + ".bom is XML — must be text BRL format");
            for (String m : findAll(boolIs, bt)) {
                warns.add("bom/" + b + ".bom: boolean '" + m + "' — BOM name is the setter name without 'set' (drop 'is')");
            }
            for (String m : findAll(reservedProp, bt)) errs.add("bom/" + b + ".bom: reserved keyword '" + m + "' used as a property name");
        }
        Pattern methodPhrase = Pattern.compile("[^#=]*\\(.*\\)#phrase\\.");
        Pattern placeholder = Pattern.compile("\\{[^}]*\\}"), word = Pattern.compile("[a-z/]+");
        for (String v : vocs) {
            for (String line : read(bomDir.resolve(v)).split("\\R")) {
                if (!methodPhrase.matcher(line).lookingAt()) continue; // method phrases only; property labels tolerate these
                String label = line.substring(line.indexOf('=') + 1);
                label = placeholder.matcher(label).replaceAll(" ");
                Set<String> bad = new TreeSet<>(findAllWords(word, label.toLowerCase(Locale.ROOT)));
                bad.retainAll(RESERVED_LABEL_TOKENS);
                if (!bad.isEmpty()) {
                    String s = line.strip();
                    warns.add("bom/" + v + ": reserved token(s) " + pyList(bad) + " in method phrase: " + s.substring(0, Math.min(100, s.length())));
                }
            }
        }

        for (Path p : glob(rp.resolve("rules"), ".rfl")) {
            for (String pkg : findAll(Pattern.compile("<Package Name=\"([^\"]+)\""), read(p))) {
                if (!Files.isDirectory(rp.resolve("rules").resolve(pkg))) {
                    errs.add(relSlash(p, rp) + ": package '" + pkg + "' has no rules/" + pkg + "/ directory");
                }
            }
        }

        for (String w : warns) System.out.println("WARN  " + w);
        for (String e : errs) System.out.println("ERROR " + e);
        System.out.println("check: " + errs.size() + " error(s), " + warns.size() + " warning(s), " + ids.size() + " UUIDs");
        return errs.isEmpty() ? 0 : 1;
    }

    static List<String> findAllWords(Pattern p, String s) {
        List<String> out = new ArrayList<>();
        Matcher m = p.matcher(s);
        while (m.find()) out.add(m.group());
        return out;
    }

    static String pyList(Set<String> items) {
        return items.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
    }

    // ------------------------------------------------------------------------- deps

    /** A vocabulary phrase of a BOM member, as the literal chunks that must appear in order in BAL text. */
    record Phrase(String cls, String member, boolean method, int arity, boolean action, List<Pattern> chunks, int weight) {}

    /** Data items ("Class.field") a rule reads in its conditions, reads in its actions, and writes. */
    static final class RuleInfo {
        final String id, pkg;
        final Set<String> condReads = new TreeSet<>(), actReads = new TreeSet<>(), writes = new TreeSet<>(), sets = new TreeSet<>();
        RuleInfo(String id, String pkg) { this.id = id; this.pkg = pkg; }
        Set<String> reads() {
            Set<String> r = new TreeSet<>(condReads);
            r.addAll(actReads);
            return r;
        }
    }

    record Effects(Set<String> reads, Set<String> writes) {}

    /** A XOM class, parsed just enough to know which of its fields each method reads and writes. */
    static final class JClass {
        final String name;
        final Set<String> fields = new TreeSet<>();
        final List<String[]> methods = new ArrayList<>(); // {name, arity, body}
        JClass(String name) { this.name = name; }
    }

    record Task(List<String> pkgs, String mode) {}

    static final Pattern VOC_PHRASE = Pattern.compile("^([\\w.$]+?)\\.(\\w+)(\\(([^)]*)\\))?#phrase\\.(navigation|action)\\s*=\\s*(.+)$");
    static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]*)\\}");
    static final Pattern J_FIELD = Pattern.compile(
            "(?m)^\\s*(?:(?:private|protected|public)\\s+)(?:(?:final|transient|volatile)\\s+)*([\\w<>\\[\\],.? ]+?)\\s+(\\w+)\\s*(?:=[^;]*)?;");
    static final Pattern J_METHOD = Pattern.compile(
            "(?:(?:public|protected|private|static|final|synchronized)\\s+)+([\\w<>\\[\\],.? ]+?)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:throws\\s+[\\w.,\\s]+)?\\{");
    static final Pattern J_ASSIGN = Pattern.compile("(?:\\bthis\\.)?\\b(\\w+)\\s*=(?!=)");
    static final Pattern J_UPDATE = Pattern.compile("(?:\\bthis\\.)?\\b(\\w+)\\s*(?:[-+*/%&|^]=|\\+\\+|--)|(?:\\+\\+|--)\\s*(?:this\\.)?(\\w+)");
    static final Pattern J_MUTATE = Pattern.compile(
            "(?:\\bthis\\.)?\\b(\\w+)\\.(?:add|addAll|remove|removeAll|removeIf|clear|put|putAll|set|offer|push)\\s*\\(");
    static final Pattern J_CALL = Pattern.compile("(?<![.\\w])(?:this\\.)?(\\w+)\\s*\\(");
    static final Pattern J_IDENT = Pattern.compile("\\b(\\w+)\\b");

    static Pattern chunk(String text) {
        return Pattern.compile("(?<![\\w'])" + Arrays.stream(text.strip().split("\\s+")).map(Pattern::quote).collect(Collectors.joining("\\s+"))
                + "(?![\\w'])", Pattern.CASE_INSENSITIVE);
    }

    static List<Phrase> vocPhrases(Path rp) throws IOException {
        List<Phrase> out = new ArrayList<>();
        for (Path voc : glob(rp.resolve("bom"), ".voc")) {
            for (String line : read(voc).split("\\R")) {
                Matcher m = VOC_PHRASE.matcher(line.strip());
                if (!m.matches()) continue;
                String cls = m.group(1).substring(m.group(1).lastIndexOf('.') + 1);
                boolean method = m.group(3) != null, action = m.group(5).equals("action");
                int arity = !method || m.group(4).isBlank() ? 0 : m.group(4).split(",").length;
                String tpl = m.group(6).strip();
                // In a navigation phrase, {label} is the member's own words ({this} and {0} are arguments); in an action it is the value.
                if (!action) tpl = PLACEHOLDER.matcher(tpl).replaceAll(r -> r.group(1).equals("this") || r.group(1).matches("\\d+")
                        ? "{" + r.group(1) + "}" : Matcher.quoteReplacement(r.group(1)));
                List<Pattern> chunks = new ArrayList<>();
                int weight = 0;
                for (String c : PLACEHOLDER.split(tpl)) {
                    if (c.isBlank()) continue;
                    chunks.add(chunk(c));
                    weight += c.strip().length();
                }
                if (!chunks.isEmpty()) out.add(new Phrase(cls, m.group(2), method, arity, action, chunks, weight));
            }
        }
        out.sort(Comparator.comparingInt((Phrase p) -> -p.weight));
        return out;
    }

    /** Spans of the phrase's chunks, in order, from position 0; null if it does not occur. */
    static List<int[]> matchPhrase(Phrase p, CharSequence s) {
        List<int[]> spans = new ArrayList<>();
        int pos = 0;
        for (Pattern c : p.chunks) {
            Matcher m = c.matcher(s);
            if (!m.find(pos)) return null;
            spans.add(new int[] {m.start(), m.end()});
            pos = m.end();
        }
        return spans;
    }

    static void mask(StringBuilder s, List<int[]> spans) {
        for (int[] sp : spans) for (int i = sp[0]; i < sp[1]; i++) s.setCharAt(i, ' ');
    }

    /** XOM source directory of a rule project: --xom, else the XOM project named in .ruleproject, next to the rule project. */
    static Path xomSrc(Path rp, String override) throws IOException {
        List<Path> cands = new ArrayList<>();
        if (override != null) cands.add(Paths.get(override).toAbsolutePath().normalize());
        if (Files.exists(rp.resolve(".ruleproject"))) {
            String t = read(rp.resolve(".ruleproject"));
            for (String n : findAll(Pattern.compile("<entries[^>]*XOMPathEntry\"[^>]*\\bname=\"([^\"]+)\""), t)) cands.add(rp.resolveSibling(n));
            for (String n : findAll(Pattern.compile("origin=\"xom:/[^/\"]+/([^\"]+)\""), t)) cands.add(rp.resolveSibling(n));
        }
        for (Path c : cands) {
            if (Files.isDirectory(c.resolve("src"))) return c.resolve("src");
            if (override != null && Files.isDirectory(c)) return c;
        }
        return null;
    }

    static Map<String, JClass> loadXom(Path src) throws IOException {
        Map<String, JClass> out = new HashMap<>();
        if (src == null) return out;
        List<Path> files;
        try (Stream<Path> s = Files.walk(src)) {
            files = s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        for (Path f : files) {
            String t = read(f).replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ")
                    .replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"").replaceAll("'(?:\\\\.|[^'\\\\])'", "' '");
            JClass c = new JClass(stem(f));
            Matcher m = J_FIELD.matcher(t);
            while (m.find()) if (!m.group(1).matches(".*\\b(static|return|new)\\b.*")) c.fields.add(m.group(2));
            m = J_METHOD.matcher(t);
            while (m.find()) {
                if (m.group(1).matches(".*\\b(new|return|else)\\b.*")) continue;
                int open = m.end() - 1, depth = 0, i = open;
                for (; i < t.length(); i++) {
                    if (t.charAt(i) == '{') depth++;
                    else if (t.charAt(i) == '}' && --depth == 0) break;
                }
                String params = m.group(3);
                while (params.matches("(?s).*<[^<>]*>.*")) params = params.replaceAll("<[^<>]*>", "");
                c.methods.add(new String[] {m.group(2), String.valueOf(params.isBlank() ? 0 : params.split(",").length),
                        t.substring(open + 1, Math.min(i, t.length()))});
            }
            out.put(c.name, c);
        }
        return out;
    }

    /** Fields of c a method reads and writes, following calls to c's own methods. arity < 0 matches any overload. */
    static Effects effects(JClass c, String name, int arity, Set<String> stack) {
        Set<String> reads = new TreeSet<>(), writes = new TreeSet<>();
        boolean found = false;
        for (String[] md : c.methods) {
            if (!md[0].equals(name) || (arity >= 0 && Integer.parseInt(md[1]) != arity)) continue;
            found = true;
            String body = md[2];
            Matcher m = J_UPDATE.matcher(body);
            while (m.find()) {
                String f = m.group(1) != null ? m.group(1) : m.group(2);
                if (c.fields.contains(f)) { writes.add(f); reads.add(f); }
            }
            m = J_MUTATE.matcher(body);
            while (m.find()) if (c.fields.contains(m.group(1))) writes.add(m.group(1));
            StringBuilder rest = new StringBuilder(body);
            m = J_ASSIGN.matcher(body);
            while (m.find()) {
                if (!c.fields.contains(m.group(1))) continue;
                writes.add(m.group(1));
                for (int i = m.start(); i < m.end(); i++) rest.setCharAt(i, ' ');
            }
            m = J_IDENT.matcher(rest);
            while (m.find()) if (c.fields.contains(m.group(1)) && !J_MUTATE.matcher(rest.substring(m.start())).lookingAt()) reads.add(m.group(1));
            m = J_CALL.matcher(body);
            while (m.find()) {
                String callee = m.group(1);
                if (callee.equals(name) || stack.contains(callee)) continue;
                stack.add(callee);
                Effects e = effects(c, callee, -1, stack);
                if (e != null) { reads.addAll(e.reads); writes.addAll(e.writes); }
            }
        }
        return found ? new Effects(reads, writes) : null;
    }

    static Set<String> qualify(String cls, Set<String> fields) {
        return fields.stream().map(f -> cls + "." + f).collect(Collectors.toCollection(TreeSet::new));
    }

    /** Items a navigation phrase reads: the field, or what a computed getter / method reads in the XOM. */
    static Set<String> readItems(Phrase p, Map<String, JClass> xom) {
        JClass c = xom.get(p.cls);
        String fallback = p.cls + "." + p.member + (p.method ? "()" : "");
        if (c == null || (!p.method && c.fields.contains(p.member))) return Set.of(fallback);
        String cap = Character.toUpperCase(p.member.charAt(0)) + p.member.substring(1);
        Effects e = p.method ? effects(c, p.member, p.arity, new TreeSet<>()) : effects(c, "get" + cap, 0, new TreeSet<>());
        if (e == null && !p.method) e = effects(c, "is" + cap, 0, new TreeSet<>());
        return e == null || e.reads.isEmpty() ? Set.of(fallback) : qualify(p.cls, e.reads);
    }

    /** Items an action phrase writes: the attribute, or the fields the XOM method changes. */
    static Set<String> writeItems(Phrase p, Map<String, JClass> xom) {
        if (!p.method) return Set.of(p.cls + "." + p.member);
        JClass c = xom.get(p.cls);
        Effects e = c == null ? null : effects(c, p.member, p.arity, new TreeSet<>());
        return e == null || e.writes.isEmpty() ? Set.of(p.cls + "." + p.member + "()") : qualify(p.cls, e.writes);
    }

    /** Navigation reads in s (longest phrases first, each match masked so shorter phrases can't reuse its words). */
    static List<Map.Entry<Integer, Set<String>>> scanReads(StringBuilder s, List<Phrase> phrases, Map<String, JClass> xom) {
        List<Map.Entry<Integer, Set<String>>> out = new ArrayList<>();
        for (Phrase p : phrases) {
            if (p.action) continue;
            List<int[]> sp;
            while ((sp = matchPhrase(p, s)) != null) {
                out.add(Map.entry(sp.get(0)[0], readItems(p, xom)));
                mask(s, sp);
            }
        }
        return out;
    }

    static String clean(String bal) {
        return bal.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"").replaceAll("'[^'\\n]*'", "'v'");
    }

    static void analyze(RuleInfo r, String cond, String act, List<Phrase> phrases, Map<String, JClass> xom) {
        for (Map.Entry<Integer, Set<String>> e : scanReads(new StringBuilder(clean(cond)), phrases, xom)) r.condReads.addAll(e.getValue());
        for (String stmt : clean(act).split(";")) {
            StringBuilder s = new StringBuilder(stmt);
            boolean acted = false;
            for (Phrase p : phrases) {
                if (!p.action) continue;
                List<int[]> sp = matchPhrase(p, s);
                if (sp == null) continue;
                Set<String> w = writeItems(p, xom);
                r.writes.addAll(w);
                if (!p.method) r.sets.addAll(w);
                mask(s, sp);
                acted = true;
                break;
            }
            List<Map.Entry<Integer, Set<String>>> reads = scanReads(s, phrases, xom);
            // Built-in BAL actions on a member with no action phrase: `set <target> to ...`, `add/remove ... to/from <target>`.
            Matcher verb = Pattern.compile("^\\s*(set|add|remove)\\b", Pattern.CASE_INSENSITIVE).matcher(stmt);
            Map.Entry<Integer, Set<String>> target = null;
            if (!acted && verb.find() && !reads.isEmpty()) {
                int after = 0;
                if (!verb.group(1).equalsIgnoreCase("set")) {
                    Matcher to = Pattern.compile("\\b(to|from)\\b", Pattern.CASE_INSENSITIVE).matcher(stmt);
                    while (to.find()) after = to.end();
                }
                for (Map.Entry<Integer, Set<String>> e : reads) {
                    if (e.getKey() >= after && (target == null || e.getKey() < target.getKey())) target = e;
                }
                if (target != null) {
                    r.writes.addAll(target.getValue());
                    if (verb.group(1).equalsIgnoreCase("set")) r.sets.addAll(target.getValue());
                }
            }
            for (Map.Entry<Integer, Set<String>> e : reads) if (e != target) r.actReads.addAll(e.getValue());
        }
    }

    static String cdataText(String xml, String section) {
        String sec = group(Pattern.compile("(?s)<" + section + ">(.*?)</" + section + ">"), xml, 1);
        if (sec == null) return "";
        return String.join(" ;\n", findAll(Pattern.compile("(?s)<!\\[CDATA\\[(.*?)\\]\\]>"), sec)).replaceAll("<[^>]*>", " x ");
    }

    static List<RuleInfo> loadRules(Path rp, List<Phrase> phrases, Map<String, JClass> xom) throws IOException {
        Path rules = rp.resolve("rules");
        List<Path> files;
        try (Stream<Path> s = Files.walk(rules)) {
            files = s.filter(p -> p.toString().endsWith(".brl") || p.toString().endsWith(".dta")).sorted().collect(Collectors.toList());
        }
        List<RuleInfo> out = new ArrayList<>();
        for (Path f : files) {
            String pkg = relSlash(f.getParent(), rules).replace('/', '.');
            RuleInfo r = new RuleInfo(pkg + "/" + stem(f), pkg);
            String t = read(f);
            if (f.toString().endsWith(".brl")) {
                String body = group(Pattern.compile("(?s)<definition><!\\[CDATA\\[(.*?)\\]\\]></definition>"), t, 1);
                if (body == null) continue;
                Matcher then = Pattern.compile("\\bthen\\b", Pattern.CASE_INSENSITIVE).matcher(body);
                boolean has = then.find();
                analyze(r, has ? body.substring(0, then.start()) : body, has ? body.substring(then.end()) : "", phrases, xom);
            } else {
                analyze(r, cdataText(t, "Preconditions") + " ;\n" + cdataText(t, "ConditionDefinitions"), cdataText(t, "ActionDefinitions"), phrases, xom);
            }
            out.add(r);
        }
        return out;
    }

    static Path findRuleflow(Path rp, String name) throws IOException {
        Path rules = rp.resolve("rules");
        if (name != null) return rules.resolve(name.endsWith(".rfl") ? name : name + ".rfl");
        for (Path dop : glob(rp.resolve("deployment"), ".dop")) {
            String n = group(Pattern.compile("ruleflowName=\"([^\"]*)\""), read(dop), 1);
            if (n != null && Files.exists(rules.resolve(n + ".rfl"))) return rules.resolve(n + ".rfl");
        }
        List<Path> all = glob(rules, ".rfl");
        return all.size() == 1 ? all.get(0) : null;
    }

    static List<Task> flowTasks(String rfl) {
        List<Task> out = new ArrayList<>();
        Matcher m = Pattern.compile("(?s)<RuleTask([^>]*)>(.*?)</RuleTask>").matcher(rfl);
        while (m.find()) {
            String mode = group(Pattern.compile("ExecutionMode=\"(\\w+)\""), m.group(1), 1);
            out.add(new Task(findAll(Pattern.compile("<Package Name=\"([^\"]+)\""), m.group(2)), mode == null ? "RetePlus" : mode));
        }
        return out;
    }

    static String items(Set<String> s) {
        List<String> l = new ArrayList<>(s);
        return l.size() <= 3 ? String.join(", ", l) : String.join(", ", l.subList(0, 3)) + " (+" + (l.size() - 3) + ")";
    }

    /** Strongly connected components (Tarjan), each sorted by name. */
    static List<List<String>> sccs(List<String> nodes, Map<String, Set<String>> g) {
        Map<String, Integer> idx = new HashMap<>(), low = new HashMap<>();
        List<String> stack = new ArrayList<>();
        Set<String> on = new java.util.HashSet<>();
        List<List<String>> out = new ArrayList<>();
        int[] counter = {0};
        java.util.function.Consumer<String>[] visit = new java.util.function.Consumer[1];
        visit[0] = v -> {
            idx.put(v, counter[0]);
            low.put(v, counter[0]++);
            stack.add(v);
            on.add(v);
            for (String w : g.getOrDefault(v, Set.of())) {
                if (!idx.containsKey(w)) { visit[0].accept(w); low.put(v, Math.min(low.get(v), low.get(w))); }
                else if (on.contains(w)) low.put(v, Math.min(low.get(v), idx.get(w)));
            }
            if (low.get(v).equals(idx.get(v))) {
                List<String> comp = new ArrayList<>();
                String w;
                do { w = stack.remove(stack.size() - 1); on.remove(w); comp.add(w); } while (!w.equals(v));
                java.util.Collections.sort(comp);
                out.add(comp);
            }
        };
        for (String v : nodes) if (!idx.containsKey(v)) visit[0].accept(v);
        return out;
    }

    static int cmdDeps(Args a) throws IOException {
        Path rp = Paths.get(a.pos(0)).toAbsolutePath().normalize();
        if (!Files.isDirectory(rp.resolve("rules"))) throw new Fail(rp + " is not a rule project (no rules/ directory)");
        List<Phrase> phrases = vocPhrases(rp);
        Path src = xomSrc(rp, a.get("xom"));
        Map<String, JClass> xom = loadXom(src);
        List<RuleInfo> rules = loadRules(rp, phrases, xom);
        if (rules.isEmpty()) throw new Fail("no .brl or .dta rules under " + rp.resolve("rules"));
        Path rflPath = findRuleflow(rp, a.get("ruleflow"));
        List<Task> flow = rflPath != null && Files.exists(rflPath) ? flowTasks(read(rflPath)) : List.of();
        Map<String, Integer> taskOf = new HashMap<>();
        for (int i = 0; i < flow.size(); i++) for (String p : flow.get(i).pkgs) taskOf.putIfAbsent(p, i);

        System.out.println("rules: " + rules.size() + ", vocabulary phrases: " + phrases.size() + ", XOM: "
                + (src != null ? rel(src) + " (" + xom.size() + " classes)" : "not found (method effects unknown; pass --xom)"));
        System.out.println("ruleflow: " + (flow.isEmpty() ? "none" : stem(rflPath) + ": " + flow.stream()
                .map(t -> String.join("+", t.pkgs) + ":" + t.mode).collect(Collectors.joining(" -> "))));

        List<String> warns = new ArrayList<>(), errs = new ArrayList<>();
        for (RuleInfo r : rules) {
            if (r.reads().isEmpty() && r.writes.isEmpty()) warns.add(r.id + ": no vocabulary phrase recognized; its dependencies are unknown");
        }
        if (a.flag("verbose")) {
            System.out.println("\nreads / writes per rule:");
            for (RuleInfo r : rules) {
                System.out.println("  " + r.id);
                if (!r.condReads.isEmpty()) System.out.println("      if:     " + String.join(", ", r.condReads));
                if (!r.actReads.isEmpty()) System.out.println("      uses:   " + String.join(", ", r.actReads));
                if (!r.writes.isEmpty()) System.out.println("      writes: " + String.join(", ", r.writes));
            }
        }

        // Rule graph: A -> B when B reads something A writes (B must run after A). A value B's own package also writes is
        // shared state (a status moving PENDING -> APPROVE -> REVIEW, a guard like "if X is null, set X"): the ruleflow
        // order decides it, so it is reported as an overwrite, not as a dependency.
        Map<String, RuleInfo> byId = new LinkedHashMap<>();
        for (RuleInfo r : rules) byId.put(r.id, r);
        Map<String, Set<String>> pkgWrites = new HashMap<>(), settersOf = new java.util.TreeMap<>();
        for (RuleInfo r : rules) {
            pkgWrites.computeIfAbsent(r.pkg, k -> new TreeSet<>()).addAll(r.writes);
            for (String w : r.sets) settersOf.computeIfAbsent(w, k -> new TreeSet<>()).add(r.pkg);
        }
        Map<String, Map<String, Set<String>>> edges = new LinkedHashMap<>();
        for (RuleInfo x : rules) {
            for (RuleInfo y : rules) {
                if (x == y) continue;
                Set<String> on = new TreeSet<>(x.writes);
                on.retainAll(y.reads());
                on.removeAll(x.pkg.equals(y.pkg) ? y.writes : pkgWrites.get(y.pkg));
                if (!on.isEmpty()) edges.computeIfAbsent(x.id, k -> new LinkedHashMap<>()).put(y.id, on);
            }
        }

        List<String> pkgs = rules.stream().map(r -> r.pkg).distinct()
                .sorted(Comparator.comparingInt((String p) -> taskOf.getOrDefault(p, Integer.MAX_VALUE)).thenComparing(p -> p))
                .collect(Collectors.toList());
        Map<String, Set<String>> pg = new HashMap<>(), pOn = new LinkedHashMap<>();
        Map<String, List<String>> intra = new LinkedHashMap<>();
        edges.forEach((x, ys) -> ys.forEach((y, on) -> {
            String px = byId.get(x).pkg, py = byId.get(y).pkg;
            if (px.equals(py)) {
                intra.computeIfAbsent(px, k -> new ArrayList<>()).add(short_(x) + " -> " + short_(y) + "  [" + items(on) + "]");
            } else {
                pg.computeIfAbsent(px, k -> new TreeSet<>()).add(py);
                pOn.computeIfAbsent(px + " -> " + py, k -> new TreeSet<>()).addAll(on);
            }
        }));

        System.out.println("\npackage dependencies (writer -> reader):");
        if (pOn.isEmpty()) System.out.println("  none");
        pOn.forEach((k, on) -> System.out.println("  " + k + "  [" + items(on) + "]"));
        if (!intra.isEmpty()) {
            System.out.println("\ndependencies inside a package:");
            intra.forEach((p, l) -> { System.out.println("  " + p + ":"); l.forEach(e -> System.out.println("    " + e)); });
        }
        List<String> shared = new ArrayList<>();
        settersOf.forEach((w, ps) -> { if (ps.size() > 1) shared.add(w + " (" + String.join(", ", ps) + ")"); });
        if (!shared.isEmpty()) {
            System.out.println("\nset by several packages (the last task to set it wins):");
            shared.forEach(x -> System.out.println("  " + x));
        }

        // Check the current ruleflow against the graph.
        if (!flow.isEmpty()) {
            for (String p : pkgs) if (!taskOf.containsKey(p)) errs.add("package '" + p + "' has rules but is not in the ruleflow: they never run");
            Map<String, Set<String>> late = new LinkedHashMap<>(), lateBy = new LinkedHashMap<>();
            edges.forEach((x, ys) -> ys.forEach((y, on) -> {
                Integer tx = taskOf.get(byId.get(x).pkg), ty = taskOf.get(byId.get(y).pkg);
                if (tx == null || ty == null || tx <= ty) return;
                String k = y + " (task " + (ty + 1) + ")";
                late.computeIfAbsent(k, z -> new TreeSet<>()).addAll(on);
                lateBy.computeIfAbsent(k, z -> new TreeSet<>()).add(x + " (task " + (tx + 1) + ")");
            }));
            late.forEach((k, on) -> errs.add(k + " reads " + items(on) + " before it is written by " + String.join(", ", lateBy.get(k))));
            for (int i = 0; i < flow.size(); i++) {
                Task t = flow.get(i);
                List<String> own = t.pkgs.stream().filter(intra::containsKey).collect(Collectors.toList());
                if (!own.isEmpty() && !t.mode.equals("RetePlus")) {
                    warns.add("task " + (i + 1) + " (" + String.join("+", t.pkgs) + ", " + t.mode + "): rules read what other rules of the same "
                            + "task write (see 'dependencies inside a package'); a " + t.mode + " task does not guarantee they see the change");
                }
                Map<String, Set<String>> setters = new java.util.TreeMap<>();
                for (RuleInfo r : rules) {
                    if (!t.pkgs.contains(r.pkg)) continue;
                    for (String x : r.sets) setters.computeIfAbsent(x, k -> new TreeSet<>()).add(short_(r.id));
                }
                final int n = i + 1;
                setters.forEach((x, rs) -> {
                    if (rs.size() > 1) warns.add("task " + n + ": " + x + " is set by " + rs.size() + " rules (" + items(rs)
                            + "); if several fire, the result depends on rule order unless their conditions exclude each other");
                });
            }
        }
        System.out.println();
        for (String w : warns) System.out.println("WARN  " + w);
        for (String e : errs) System.out.println("ERROR " + e);

        // Suggested ruleflow: packages in dependency order; mutually dependent packages share one task.
        List<List<String>> comps = sccs(pkgs, pg);
        Map<String, Integer> compOf = new HashMap<>();
        for (int i = 0; i < comps.size(); i++) for (String p : comps.get(i)) compOf.put(p, i);
        Map<Integer, Set<Integer>> cg = new HashMap<>();
        int[] indeg = new int[comps.size()];
        pg.forEach((p, qs) -> qs.forEach(q -> {
            int cp = compOf.get(p), cq = compOf.get(q);
            if (cp != cq && cg.computeIfAbsent(cp, k -> new java.util.HashSet<>()).add(cq)) indeg[cq]++;
        }));
        Comparator<Integer> prio = Comparator.comparingInt((Integer c) -> comps.get(c).stream()
                .mapToInt(p -> taskOf.getOrDefault(p, Integer.MAX_VALUE)).min().orElse(Integer.MAX_VALUE)).thenComparing(c -> comps.get(c).get(0));
        java.util.PriorityQueue<Integer> ready = new java.util.PriorityQueue<>(prio);
        for (int i = 0; i < comps.size(); i++) if (indeg[i] == 0) ready.add(i);
        List<String> spec = new ArrayList<>(), notes = new ArrayList<>();
        while (!ready.isEmpty()) {
            int c = ready.poll();
            List<String> comp = new ArrayList<>(comps.get(c));
            comp.sort(Comparator.comparingInt((String p) -> taskOf.getOrDefault(p, Integer.MAX_VALUE)).thenComparing(p -> p));
            boolean inner = comp.size() > 1 || comp.stream().anyMatch(intra::containsKey);
            String mode = inner ? "RetePlus" : comp.stream().filter(taskOf::containsKey).map(p -> flow.get(taskOf.get(p)).mode).findFirst().orElse("Fastpath");
            spec.add(String.join("+", comp) + ":" + mode);
            if (comp.size() > 1) {
                notes.add(String.join(" and ", comp) + " depend on each other; they share one RetePlus task. To keep them separate, "
                        + "move the rules behind the backward edges above so the dependency runs one way.");
            } else if (inner) {
                notes.add(comp.get(0) + " has rules depending on each other; it runs as RetePlus. To make the order explicit instead, "
                        + "split it: " + layers(comp.get(0), rules, edges));
            }
            for (int d : cg.getOrDefault(c, Set.of())) if (--indeg[d] == 0) ready.add(d);
        }
        String current = flow.stream().map(t -> String.join("+", t.pkgs) + ":" + t.mode).collect(Collectors.joining(","));
        String suggested = String.join(",", spec);
        System.out.println("\nsuggested ruleflow: " + String.join(" -> ", spec));
        for (String n : notes) System.out.println("  note: " + n);
        if (suggested.equals(current)) System.out.println("the current ruleflow already follows the dependencies.");
        else System.out.println("apply with:\n  odm ruleflow " + rel(rp) + " --packages " + suggested);
        return errs.isEmpty() ? 0 : 1;
    }

    static String short_(String id) {
        return id.substring(id.indexOf('/') + 1);
    }

    /** Rules of one package grouped by depth in its internal dependency graph. */
    static String layers(String pkg, List<RuleInfo> rules, Map<String, Map<String, Set<String>>> edges) {
        List<String> ids = rules.stream().filter(r -> r.pkg.equals(pkg)).map(r -> r.id).collect(Collectors.toList());
        Map<String, Integer> indeg = new HashMap<>(), depth = new HashMap<>();
        for (String id : ids) indeg.put(id, 0);
        for (String x : ids) for (String y : edges.getOrDefault(x, Map.of()).keySet()) if (indeg.containsKey(y)) indeg.merge(y, 1, Integer::sum);
        List<String> queue = ids.stream().filter(id -> indeg.get(id) == 0).collect(Collectors.toList());
        for (String id : queue) depth.put(id, 0);
        for (int i = 0; i < queue.size(); i++) {
            String x = queue.get(i);
            for (String y : edges.getOrDefault(x, Map.of()).keySet()) {
                if (!indeg.containsKey(y)) continue;
                depth.merge(y, depth.get(x) + 1, Math::max);
                if (indeg.merge(y, -1, Integer::sum) == 0) queue.add(y);
            }
        }
        if (queue.size() < ids.size()) return "not possible, its rules form a cycle (a rule chain rewrites what it reads)";
        Map<Integer, List<String>> byDepth = new java.util.TreeMap<>();
        depth.forEach((id, d) -> byDepth.computeIfAbsent(d, k -> new ArrayList<>()).add(id.substring(id.indexOf('/') + 1)));
        List<String> parts = new ArrayList<>();
        byDepth.forEach((d, l) -> { java.util.Collections.sort(l); parts.add("[" + String.join(", ", l) + "]"); });
        return String.join(" -> ", parts);
    }

    // ------------------------------------------------------------------------- ruleflow

    /** Why a ruleflow is more than start -> rule tasks -> stop in a line (branches, guards, subflows); null if it is linear. */
    static String nonLinear(String rfl) {
        for (String task : findAll(Pattern.compile("<(\\w+Task)\\b"), rfl)) {
            if (!Set.of("StartTask", "RuleTask", "StopTask").contains(task)) return "has a " + task;
        }
        for (String node : findAll(Pattern.compile("<(\\w+Node)\\b"), rfl)) if (!node.equals("TaskNode")) return "has a " + node;
        if (Pattern.compile("<Transition\\b[^>]*[^/]>").matcher(rfl).find()) return "has conditional transitions";
        List<String> sources = findAll(Pattern.compile("<Transition\\b[^>]*\\bSource=\"([^\"]+)\""), rfl);
        if (sources.size() != new TreeSet<>(sources).size()) return "branches";
        for (String body : findAll(Pattern.compile("(?s)<RuleTask\\b[^>]*>(.*?)</RuleTask>"), rfl)) {
            if (Pattern.compile("<(?!/?RuleList\\b|Package\\b)\\w+").matcher(body).find()) return "has task-level settings (actions, selects or rule lists)";
        }
        return null;
    }

    static int cmdRuleflow(Args a) throws IOException {
        Path rp = Paths.get(a.pos(0)).toAbsolutePath().normalize();
        Path rfl = findRuleflow(rp, a.get("name"));
        if (rfl == null || !Files.exists(rfl)) {
            throw new Fail("no ruleflow to update in " + rp.resolve("rules") + (a.get("name") == null ? " (pass --name)" : ""));
        }
        String t = read(rfl);
        String name = group(Pattern.compile("<name>([^<]*)</name>"), t, 1), uuid = group(Pattern.compile("<uuid>([^<]*)</uuid>"), t, 1);
        String locale = group(Pattern.compile("<locale>([^<]*)</locale>"), t, 1);
        String imports = group(Pattern.compile("(?s)<imports><!\\[CDATA\\[(.*?)\\]\\]></imports>"), t, 1);
        if (uuid == null) throw new Fail(rel(rfl) + " has no <uuid>");
        String why = nonLinear(t);
        if (why != null && !a.flag("force")) {
            throw new Fail(rel(rfl) + " is not a plain sequence of rule tasks (" + why + "); rewriting it would lose that. "
                    + "Edit it in Rule Designer, or pass --force to replace it anyway.");
        }
        List<Pkg> pkgs = parsePkgs(a.get("packages"));
        Set<String> listed = new TreeSet<>();
        for (Pkg p : pkgs) {
            for (String n : p.name.split("\\+")) {
                if (!listed.add(n)) throw new Fail("package '" + n + "' is listed twice");
                if (!Files.isDirectory(rp.resolve("rules").resolve(n))) throw new Fail("package '" + n + "' has no rules/" + n + "/ directory");
            }
        }
        List<String> dropped;
        try (Stream<Path> s = Files.list(rp.resolve("rules"))) {
            dropped = s.filter(Files::isDirectory).filter(d -> !listed.contains(d.getFileName().toString())).filter(d -> {
                try (Stream<Path> w = Files.walk(d)) {
                    return w.anyMatch(f -> f.toString().endsWith(".brl") || f.toString().endsWith(".dta"));
                } catch (IOException e) {
                    return true;
                }
            }).map(d -> d.getFileName().toString()).sorted().collect(Collectors.toList());
        }
        if (!dropped.isEmpty()) {
            String msg = "package(s) with rules not in --packages, so they would never run: " + String.join(", ", dropped);
            if (!a.flag("force")) throw new Fail(msg + " (pass --force if that is intended)");
            System.out.println("WARN  " + msg);
        }
        write(rfl, ruleflow(name != null ? name : stem(rfl), pkgs, imports != null ? imports : "", locale != null ? locale : "en_US", uuid), true);
        System.out.println("ruleflow " + stem(rfl) + ": " + pkgs.stream().map(p -> p.name + ":" + p.mode).collect(Collectors.joining(" -> ")));
        return 0;
    }

    // ------------------------------------------------------------------------- xom

    static int cmdXom(Args a) throws IOException {
        Path xd = Paths.get(a.pos(0)).toAbsolutePath().normalize();
        String name = xd.getFileName().toString();
        List<String> srcs;
        try (Stream<Path> s = Files.isDirectory(xd.resolve("src")) ? Files.walk(xd.resolve("src")) : Stream.empty()) {
            srcs = s.filter(p -> p.toString().endsWith(".java")).map(Path::toString).sorted().collect(Collectors.toList());
        }
        if (srcs.isEmpty()) throw new Fail("no .java files under " + xd.resolve("src"));
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) throw new Fail("no Java compiler in this runtime (" + System.getProperty("java.home") + ") — run odm with a JDK, not a JRE");

        // The XOM bytecode must load on the compiler's JDK (and on RES, which may be older): refuse a --release
        // above the JDK of the ODM release when the compiler jar is available.
        int release = Integer.parseInt(a.get("release"));
        Path jar = a.get("jar") != null ? Paths.get(a.get("jar")).toAbsolutePath() : defaultCompilerJar();
        if (jar != null && Files.exists(jar)) {
            OdmRelease r = odmRelease(jar);
            if (release > r.jdk()) {
                throw new Fail("--release " + release + " is newer than Java " + r.jdk() + " required by ODM " + r.version()
                        + ": the XOM would not load (bad major version). Use --release 17.");
            }
        }

        Path bin = xd.resolve("bin");
        deleteTree(bin);
        String cp = glob(xd.resolve("lib"), ".jar").stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
        List<String> args = new ArrayList<>(List.of("--release", a.get("release"), "-d", bin.toString()));
        if (!cp.isEmpty()) args.addAll(List.of("-cp", cp));
        args.addAll(srcs);
        // --release 17 by default: the lowest JDK of any ODM 9.x release, so the jar also loads on an older RES
        ByteArrayOutputStream diag = new ByteArrayOutputStream();
        int rc = javac.run(null, diag, diag, args.toArray(new String[0]));
        if (rc != 0) throw new Fail(diag.toString(StandardCharsets.UTF_8));

        Path out = xd.resolve(name + "-1.0.0.jar");
        writeJar(bin, out);
        System.out.println("compiled " + srcs.size() + " source file(s) -> " + rel(out));
        return 0;
    }

    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) Files.delete(p);
        }
    }

    static void writeJar(Path bin, Path out) throws IOException {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(new Attributes.Name("Created-By"), "odm " + VERSION);
        List<Path> entries;
        try (Stream<Path> s = Files.walk(bin)) {
            entries = s.filter(p -> !p.equals(bin)).sorted().collect(Collectors.toList());
        }
        try (OutputStream fo = Files.newOutputStream(out); JarOutputStream jar = new JarOutputStream(fo, mf)) {
            for (Path p : entries) {
                String n = bin.relativize(p).toString().replace(File.separatorChar, '/');
                if (Files.isDirectory(p)) {
                    jar.putNextEntry(new JarEntry(n + "/"));
                } else {
                    jar.putNextEntry(new JarEntry(n));
                    Files.copy(p, jar);
                }
                jar.closeEntry();
            }
        }
    }

    // ------------------------------------------------------------------------- build

    static Path skillDir() {
        try {
            URL loc = Odm.class.getProtectionDomain().getCodeSource().getLocation();
            Path p = Paths.get(loc.toURI()); // scripts/odm.jar, or scripts/Odm.java in source-launch mode
            return p.toAbsolutePath().getParent().getParent();
        } catch (Exception | Error e) {
            return null;
        }
    }

    static Path defaultCompilerJar() {
        String env = System.getenv("ODM_RULES_COMPILER");
        if (env != null && !env.isEmpty()) return Paths.get(env);
        Path sd = skillDir();
        return sd == null ? null : sd.resolve("tools").resolve("rules-compiler.jar");
    }

    static final Pattern RELEVANT = Pattern.compile("ERROR|Error|BUILD|RuleApp|Exception|aborted|WARN");

    /** ODM release (major.minor) -> the JDK its rules compiler must run on. Only 9.x is supported. */
    static final Map<String, Integer> ODM_JDK = Map.of("9.0", 17, "9.5", 21, "9.6", 21, "9.7", 25);

    /** ODM version of a rules-compiler.jar and the JDK it requires, read from its manifest (no ODM class is loaded). */
    record OdmRelease(String version, int jdk) {}

    static OdmRelease odmRelease(Path jar) throws IOException {
        Attributes at;
        try (JarFile jf = new JarFile(jar.toFile())) {
            Manifest mf = jf.getManifest();
            if (mf == null) throw new Fail("no MANIFEST.MF in " + jar + ": cannot tell which ODM release it is");
            at = mf.getMainAttributes();
        }
        String ver = at.getValue("Implementation-Version");
        if (ver == null) ver = at.getValue("Specification-Version");
        if (ver == null) throw new Fail("no Implementation-Version/Specification-Version in the manifest of " + jar);
        Matcher m = Pattern.compile("^(\\d+)\\.(\\d+)").matcher(ver);
        if (!m.find()) throw new Fail("cannot parse ODM version '" + ver + "' from " + jar);
        String mm = m.group(1) + "." + m.group(2);
        Integer jdk = ODM_JDK.get(mm);
        if (jdk != null) return new OdmRelease(ver, jdk);
        if (!m.group(1).equals("9")) {
            throw new Fail("ODM " + ver + " (" + jar + ") is not supported: odm only handles ODM 9.x ("
                    + String.join(", ", new TreeSet<>(ODM_JDK.keySet())) + ")");
        }
        // A 9.x release newer than the table: trust the JDK it was built with.
        Integer built = javaFeature(at.getValue("Build-Jdk"));
        if (built == null) throw new Fail("ODM " + ver + " is not in the ODM/JDK table and its manifest has no Build-Jdk");
        System.err.println("WARN  ODM " + ver + " is not in the ODM/JDK table; using its Build-Jdk (" + built + ")");
        return new OdmRelease(ver, built);
    }

    /** Feature release of a Java version string: "21.0.10" -> 21, "1.8.0_402" -> 8. */
    static Integer javaFeature(String v) {
        if (v == null) return null;
        Matcher m = Pattern.compile("^\\s*(?:1\\.)?(\\d+)").matcher(v);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    /** Feature release of a JDK home, from its release file; null if it is not a JDK with bin/java. */
    static Integer jdkFeature(Path home) {
        if (home == null || !Files.isRegularFile(javaBin(home))) return null;
        try {
            for (String l : Files.readAllLines(home.resolve("release"), StandardCharsets.UTF_8)) {
                if (l.startsWith("JAVA_VERSION=")) return javaFeature(l.substring(13).replace("\"", ""));
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    static Path javaBin(Path home) {
        return home.resolve("bin").resolve(System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
    }

    static Path envPath(String name) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? null : Paths.get(v);
    }

    /**
     * Find a JDK whose feature release is exactly {@code want}. Order: $ODM_JAVA_HOME, /usr/libexec/java_home (macOS),
     * SDKMAN, $JAVA_HOME. A mismatched $ODM_JAVA_HOME is an error, not skipped: it is an explicit choice.
     */
    static Path findJdk(int want, String odmVersion) throws IOException {
        List<String> tried = new ArrayList<>();
        Path odmHome = envPath("ODM_JAVA_HOME");
        if (odmHome != null) {
            Integer f = jdkFeature(odmHome);
            if (f != null && f == want) return odmHome;
            throw new Fail("ODM_JAVA_HOME=" + odmHome + " is " + (f == null ? "not a JDK" : "Java " + f)
                    + ", but ODM " + odmVersion + " requires Java " + want + ". Point ODM_JAVA_HOME to a JDK " + want + " or unset it.");
        }
        tried.add("$ODM_JAVA_HOME (unset)");

        Path javaHome = Paths.get("/usr/libexec/java_home");
        if (Files.isExecutable(javaHome)) {
            try {
                Process p = new ProcessBuilder(javaHome.toString(), "-v", String.valueOf(want)).redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                if (p.waitFor() == 0 && !out.isEmpty()) {
                    Path h = Paths.get(out.lines().reduce((x, y) -> y).orElse(out));
                    Integer f = jdkFeature(h);
                    if (f != null && f == want) return h;
                    tried.add("/usr/libexec/java_home -v " + want + " -> " + h + " (Java " + f + ")");
                } else {
                    tried.add("/usr/libexec/java_home -v " + want + " (none)");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Fail("interrupted while running /usr/libexec/java_home");
            }
        }

        Path sdk = envPath("SDKMAN_DIR");
        if (sdk == null) sdk = Paths.get(System.getProperty("user.home"), ".sdkman");
        Path cands = sdk.resolve("candidates").resolve("java");
        if (Files.isDirectory(cands)) {
            try (Stream<Path> s = Files.list(cands)) {
                List<Path> hits = s.filter(p -> !p.getFileName().toString().equals("current"))
                        .filter(p -> { Integer f = jdkFeature(p); return f != null && f == want; })
                        .sorted(Comparator.reverseOrder()).collect(Collectors.toList());
                if (!hits.isEmpty()) return hits.get(0);
            }
            tried.add("SDKMAN " + cands + " (no Java " + want + ")");
        } else {
            tried.add("SDKMAN (not installed)");
        }

        Path jh = envPath("JAVA_HOME");
        if (jh != null) {
            Integer f = jdkFeature(jh);
            if (f != null && f == want) return jh;
            tried.add("$JAVA_HOME=" + jh + " (" + (f == null ? "not a JDK" : "Java " + f) + ")");
        } else {
            tried.add("$JAVA_HOME (unset)");
        }
        throw new Fail("ODM " + odmVersion + " requires Java " + want + " (exact), but none was found. Tried:\n  - "
                + String.join("\n  - ", tried)
                + "\nInstall a JDK " + want + " or set ODM_JAVA_HOME to one.");
    }

    static Path compilerJar(Args a) {
        Path jar = a.get("jar") != null ? Paths.get(a.get("jar")).toAbsolutePath() : defaultCompilerJar();
        if (jar == null || !Files.exists(jar)) {
            throw new Fail("compiler not found: " + jar + " (set --jar or ODM_RULES_COMPILER, or use the build_ruleset MCP tool)");
        }
        return jar;
    }

    static int cmdJdk(Args a) throws IOException {
        Path jar = compilerJar(a);
        OdmRelease r = odmRelease(jar);
        System.out.println("rules compiler: " + jar);
        System.out.println("ODM version:    " + r.version());
        System.out.println("required Java:  " + r.jdk());
        Path home = findJdk(r.jdk(), r.version());
        System.out.println("JDK:            " + home);
        return 0;
    }

    static int cmdBuild(Args a) throws IOException {
        Path props = Paths.get(a.pos(0)).toAbsolutePath().normalize();
        if (!Files.isRegularFile(props)) throw new Fail("config not found: " + props);
        Path jar = compilerJar(a);
        // The compiler must run on the exact JDK of its ODM release (e.g. a 9.6 compiler on Java 25 fails in the
        // B2X mapping), so it runs in its own process with that JDK instead of in this JVM.
        OdmRelease r = odmRelease(jar);
        Path home = findJdk(r.jdk(), r.version());
        int maxLines = Integer.parseInt(a.get("max-lines"));

        // The compiler resolves the .properties paths against the file's own directory.
        Process p = new ProcessBuilder(javaBin(home).toString(), "-jar", jar.toString(), "-config", props.toString())
                .directory(props.getParent().toFile()).redirectErrorStream(true).start();
        p.getOutputStream().close();
        String log = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int rc;
        try {
            rc = p.waitFor();
        } catch (InterruptedException e) {
            p.destroy();
            Thread.currentThread().interrupt();
            throw new Fail("interrupted while waiting for the rules compiler");
        }

        List<String> lines = log.lines().collect(Collectors.toList());
        if (a.flag("full")) {
            lines.forEach(System.out::println);
        } else {
            List<String> keep = lines.stream().filter(l -> RELEVANT.matcher(l).find()).collect(Collectors.toList());
            keep.stream().limit(maxLines).forEach(System.out::println);
            if (keep.size() > maxLines) System.out.println("... " + (keep.size() - maxLines) + " more relevant lines (use --full)");
        }
        System.out.println("exit code: " + rc + " (" + (rc == 0 ? "BUILD SUCCESS" : "BUILD FAILURE") + ") ODM " + r.version()
                + ", java=" + home + " (Java " + r.jdk() + ")");
        return rc;
    }

    // ------------------------------------------------------------------------- arguments

    /** Option spec: name, takes a value, repeatable, required, default, help. */
    record Opt(String name, boolean value, boolean repeat, boolean required, String def, String help) {
        static Opt val(String n, String def, String help) { return new Opt(n, true, false, false, def, help); }
        static Opt req(String n, String help) { return new Opt(n, true, false, true, null, help); }
        static Opt flag(String n, String help) { return new Opt(n, false, false, false, null, help); }
    }

    record Cmd(String name, String help, List<String> positionals, List<Opt> opts) {}

    static final Cmd INIT = new Cmd("init", "scaffold XOM + rule project", List.of(), List.of(
            Opt.req("name", "rule project name, camelCase (e.g. LoanApproval)"),
            Opt.req("xom", "XOM project name (e.g. loan-approval-xom)"),
            Opt.req("base", "base name shared by .bom/.b2xa/.voc (e.g. loan-approval)"),
            Opt.req("package", "Java package of the XOM (e.g. com.example.loan)"),
            new Opt("var", true, true, true, null, "ruleset parameter name:Type[:IN|OUT|IN_OUT] (repeatable; default IN_OUT)"),
            Opt.req("packages", "ordered rule packages with mode, e.g. validation:Fastpath,scoring:RetePlus"),
            Opt.val("ruleflow", "main-ruleflow", "ruleflow name"),
            Opt.val("locale", "en_US", "vocabulary locale"),
            Opt.val("version", "1.0", "ruleset.version in the .dep"),
            Opt.val("dir", ".", "parent directory for both projects"),
            Opt.val("props", null, "build .properties path (default <dir>/<name>.properties)"),
            Opt.flag("fetch-jackson", "download Jackson " + JACKSON_VERSION + " jars into lib/"),
            Opt.flag("force", "overwrite existing files")));
    static final Cmd RULE = new Cmd("rule", "create a .brl from a BAL body (stdin or --file)",
            List.of("project", "package", "name"), List.of(
            Opt.val("file", null, "file containing the BAL body (default: stdin)"),
            Opt.val("locale", "en_US", "rule locale"),
            Opt.flag("force", "overwrite an existing rule")));
    static final Cmd CHECK = new Cmd("check", "consistency + lint checks", List.of("project"), List.of(
            Opt.flag("no-lint", "skip BAL/decision-table lint")));
    static final Cmd DEPS = new Cmd("deps", "rule dependencies (who writes what others read) vs the ruleflow order",
            List.of("project"), List.of(
            Opt.val("xom", null, "XOM project or source dir (default: the XOM named in .ruleproject)"),
            Opt.val("ruleflow", null, "ruleflow name (default: the one the .dop uses)"),
            Opt.flag("verbose", "also print what each rule reads and writes")));
    static final Cmd RULEFLOW = new Cmd("ruleflow", "rewrite the ruleflow as ordered rule tasks (keeps name + UUID)",
            List.of("project"), List.of(
            Opt.req("packages", "ordered tasks with mode, e.g. validation:Fastpath,scoring+pricing:RetePlus ('+' = same task)"),
            Opt.val("name", null, "ruleflow name (default: the one the .dop uses)"),
            Opt.flag("force", "replace a non-linear ruleflow, or leave out packages that have rules")));
    static final String JAR_HELP = "rules-compiler.jar (default: $ODM_RULES_COMPILER or <skill>/tools/rules-compiler.jar)";
    static final Cmd XOM = new Cmd("xom", "compile XOM (--release 17) and package its jar", List.of("xom_dir"), List.of(
            Opt.val("release", "17", "javac --release level (must not exceed the JDK of the ODM release)"),
            Opt.val("jar", null, JAR_HELP)));
    static final Cmd JDK = new Cmd("jdk", "show the ODM release and the JDK the rules compiler needs", List.of(), List.of(
            Opt.val("jar", null, JAR_HELP)));
    static final Cmd BUILD = new Cmd("build", "run the rules compiler on the JDK of its ODM release", List.of("config"), List.of(
            Opt.val("jar", null, JAR_HELP),
            Opt.val("max-lines", "40", "max relevant lines to print"),
            Opt.flag("full", "print the full compiler log")));

    static final String USAGE = "usage: odm <init|rule|check|deps|ruleflow|xom|jdk|build|uuid> [options]   (odm <subcommand> -h for details)\n\n"
            + "ODM Decision Service helper " + VERSION + ": scaffold boilerplate, add rules, check consistency, build.\n"
            + Stream.of(INIT, RULE, CHECK, DEPS, RULEFLOW, XOM, JDK, BUILD).map(c -> String.format("  %-8s %s", c.name, c.help)).collect(Collectors.joining("\n"))
            + "\n  uuid     print a fresh UUID";

    static final class Args {
        final Map<String, List<String>> vals = new HashMap<>();
        final List<String> pos = new ArrayList<>();
        final Cmd cmd;

        Args(Cmd cmd) { this.cmd = cmd; }

        String get(String n) {
            List<String> v = vals.get(n);
            return v == null || v.isEmpty() ? null : v.get(v.size() - 1);
        }

        List<String> all(String n) { return vals.getOrDefault(n, List.of()); }

        boolean flag(String n) { return vals.containsKey(n); }

        String pos(int i) { return pos.get(i); }

        static Args parse(Cmd cmd, String[] argv) {
            Args a = new Args(cmd);
            Map<String, Opt> byName = new LinkedHashMap<>();
            for (Opt o : cmd.opts) byName.put(o.name, o);
            for (int i = 0; i < argv.length; i++) {
                String s = argv[i];
                if (s.equals("-h") || s.equals("--help")) {
                    System.out.println(help(cmd));
                    throw new Fail("", 0);
                }
                if (s.startsWith("--") && s.length() > 2) {
                    String n = s.substring(2), v = null;
                    int eq = n.indexOf('=');
                    if (eq >= 0) { v = n.substring(eq + 1); n = n.substring(0, eq); }
                    Opt o = byName.get(n);
                    if (o == null) throw new Fail("odm " + cmd.name + ": unknown option --" + n + "\n\n" + help(cmd), 2);
                    if (o.value) {
                        if (v == null) {
                            if (i + 1 >= argv.length) throw new Fail("odm " + cmd.name + ": --" + n + " needs a value", 2);
                            v = argv[++i];
                        }
                        List<String> list = a.vals.computeIfAbsent(n, k -> new ArrayList<>());
                        if (!o.repeat) list.clear();
                        list.add(v);
                    } else {
                        if (v != null) throw new Fail("odm " + cmd.name + ": --" + n + " takes no value", 2);
                        a.vals.put(n, List.of());
                    }
                } else {
                    a.pos.add(s);
                }
            }
            List<String> missing = new ArrayList<>();
            for (Opt o : cmd.opts) {
                if (o.required && !a.vals.containsKey(o.name)) missing.add("--" + o.name);
                if (o.def != null && !a.vals.containsKey(o.name)) a.vals.put(o.name, List.of(o.def));
            }
            if (a.pos.size() < cmd.positionals.size()) {
                missing.addAll(cmd.positionals.subList(a.pos.size(), cmd.positionals.size()));
            } else if (a.pos.size() > cmd.positionals.size()) {
                throw new Fail("odm " + cmd.name + ": unexpected argument(s) " + a.pos.subList(cmd.positionals.size(), a.pos.size())
                        + "\n\n" + help(cmd), 2);
            }
            if (!missing.isEmpty()) {
                throw new Fail("odm " + cmd.name + ": missing required argument(s): " + String.join(", ", missing) + "\n\n" + help(cmd), 2);
            }
            return a;
        }

        static String help(Cmd cmd) {
            StringBuilder b = new StringBuilder("usage: odm " + cmd.name);
            for (String p : cmd.positionals) b.append(' ').append(p);
            b.append(" [options]\n\n").append(cmd.help).append("\n\noptions:\n");
            for (Opt o : cmd.opts) {
                String left = "--" + o.name + (o.value ? " <" + o.name.toUpperCase(Locale.ROOT).replace('-', '_') + ">" : "");
                String extra = (o.required ? " (required)" : "") + (o.def != null ? " (default: " + o.def + ")" : "");
                b.append(String.format("  %-26s %s%s%n", left, o.help, extra));
            }
            return b.toString().stripTrailing();
        }
    }
}
