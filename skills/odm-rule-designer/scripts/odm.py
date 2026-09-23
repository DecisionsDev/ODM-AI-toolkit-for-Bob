#!/usr/bin/env python3
"""ODM Decision Service helper: scaffold boilerplate, add rules, check consistency, build.

Stdlib only. Subcommands:
  init   Generate the XOM + rule project skeleton (all XML boilerplate, fresh UUIDs, build .properties).
  rule   Wrap a BAL body into a .brl file (new UUID, creates the .rulepackage if missing).
  check  Verify UUID uniqueness, cross-file UUID links, file naming, ruleflow packages, and common BAL mistakes.
  xom    Compile the XOM with --release 17 and package <xom>-1.0.0.jar.
  build  Run rules-compiler.jar and print only the relevant lines (errors, BUILD result, RuleApp path).

Run `odm.py <subcommand> -h` for arguments.
"""
import argparse
import glob
import os
import re
import shutil
import subprocess
import sys
import urllib.parse
import urllib.request
import uuid as uuidlib
from collections import Counter

SKILL_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_JAR = os.path.join(SKILL_DIR, "tools", "rules-compiler.jar")
JACKSON_VERSION = "2.15.2"
JACKSON = [("core", "jackson-core"), ("core", "jackson-databind"), ("core", "jackson-annotations")]

XML = '<?xml version="1.0" encoding="UTF-8"?>\n'
NS_XMI = 'xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"'
NS_BASE = 'xmlns:ilog.rules.studio.model.base="http://ilog.rules.studio/model/base.ecore"'
NS_DS = 'xmlns:com.ibm.rules.studio.model.decisionservice="http://com.ibm.rules.studio/model/decisionservice.ecore"'


def new_uuid():
    return str(uuidlib.uuid4())


def write(path, content, force=False):
    if os.path.exists(path) and not force:
        print(f"skip (exists): {os.path.relpath(path)}")
        return
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"wrote: {os.path.relpath(path)}")


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


# --------------------------------------------------------------------------- templates

def eclipse_project(name, builder, natures, refs=()):
    projects = "".join(f"\n        <project>{r}</project>" for r in refs)
    nat = "".join(f"\n        <nature>{n}</nature>" for n in natures)
    return (f"{XML}<projectDescription>\n    <name>{name}</name>\n    <comment></comment>\n"
            f"    <projects>{projects}\n    </projects>\n    <buildSpec>\n        <buildCommand>\n"
            f"            <name>{builder}</name>\n            <arguments>\n            </arguments>\n"
            f"        </buildCommand>\n    </buildSpec>\n    <natures>{nat}\n    </natures>\n</projectDescription>\n")


def xom_classpath():
    libs = "".join(f'\n    <classpathentry kind="lib" path="lib/{a}-{JACKSON_VERSION}.jar"/>' for _, a in JACKSON)
    return (f'{XML}<classpath>\n    <classpathentry kind="src" path="src"/>\n'
            f'    <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>{libs}\n'
            f'    <classpathentry kind="output" path="bin"/>\n</classpath>\n')


def ruleproject(name, xom, base, u):
    folders = [("ilog.rules.studio.model.base:SourceFolder", "rules"),
               ("ilog.rules.studio.model.bom:BOMFolder", "bom"),
               ("ilog.rules.studio.model.rule:TemplateFolder", "templates"),
               ("ilog.rules.studio.model.query:QueryFolder", "queries"),
               ("com.ibm.rules.studio.model.decisionservice:OperationFolder", "deployment"),
               ("ilog.rules.studio.model.base:ResourceFolder", "resources")]
    mf = "".join(f'  <modelFolders xsi:type="{t}">\n    <name>{n}</name>\n  </modelFolders>\n' for t, n in folders)
    ns = " ".join(f'xmlns:ilog.rules.studio.model.{m}="http://ilog.rules.studio/model/{m}.ecore"'
                  for m in ("base", "bom", "query", "rule", "xom"))
    return (f'{XML}<ilog.rules.studio.model.base:RuleProject {NS_XMI} '
            f'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" {NS_DS} {ns} '
            f'buildMode="DecisionEngine" isADecisionService="true" migrationFlag="3" MigratedToOperationId="{u["dop"]}">\n'
            f'  <name>{name}</name>\n  <uuid>{u["project"]}</uuid>\n  <outputLocation>output</outputLocation>\n'
            f'  <categories>any</categories>\n'
            f'  <paths xsi:type="ilog.rules.studio.model.xom:XOMPath" pathID="XOM">\n'
            f'    <entries xsi:type="ilog.rules.studio.model.xom:LibraryXOMPathEntry" name="org.eclipse.jdt.launching.JRE_CONTAINER" url="file:org.eclipse.jdt.launching.JRE_CONTAINER" kind="LIBRARY"/>\n'
            f'    <entries xsi:type="ilog.rules.studio.model.xom:SystemXOMPathEntry" name="{xom}" url="platform:/{xom}" kind="JAVA_PROJECT"/>\n'
            f'  </paths>\n'
            f'  <paths xsi:type="ilog.rules.studio.model.bom:BOMPath" pathID="BOM">\n'
            f'    <entries xsi:type="ilog.rules.studio.model.bom:BOMEntry" name="{base}" url="platform:/{name}/bom/{base}.bom" origin="xom:/{name}/{xom}"/>\n'
            f'  </paths>\n{mf}</ilog.rules.studio.model.base:RuleProject>\n')


def rulepackage(name, u, doc=""):
    return (f'{XML}<ilog.rules.studio.model.base:RulePackage {NS_XMI} {NS_BASE}>\n  <name>{name}</name>\n'
            f'  <uuid>{u}</uuid>\n  <documentation><![CDATA[{doc or "Rules for " + name}]]></documentation>\n'
            f'</ilog.rules.studio.model.base:RulePackage>\n')


def varset(name, vars_, u):
    v = "".join(f'  <variables name="{n}" type="{t}" initialValue="" verbalization="the {n}"/>\n' for n, t, _ in vars_)
    return (f'{XML}<ilog.rules.studio.model.base:VariableSet {NS_XMI} {NS_BASE}>\n  <name>{name}</name>\n'
            f'  <uuid>{u}</uuid>\n  <documentation><![CDATA[Variables for {name}]]></documentation>\n{v}'
            f'</ilog.rules.studio.model.base:VariableSet>\n')


def ruleflow(name, pkgs, java_pkg, locale, u):
    tasks, nodes, trans, labels = ['      <StartTask Identifier="task_0"/>'], [], [], []
    for i, (p, mode) in enumerate(pkgs, 1):
        tasks.append(f'      <RuleTask ExecutionMode="{mode}" Identifier="task_{i}" Ordering="Default">\n'
                     f'        <RuleList>\n          <Package Name="{p}"/>\n        </RuleList>\n      </RuleTask>')
        labels.append(f'      <Data Name="node_{i}#label">{p}</Data>')
    last = len(pkgs) + 1
    tasks.append(f'      <StopTask Identifier="task_{last}"/>')
    for i in range(last + 1):
        nodes.append(f'      <TaskNode Identifier="node_{i}" Task="task_{i}"/>')
        if i < last:
            trans.append(f'      <Transition Identifier="transition_{i}" Source="node_{i}" Target="node_{i + 1}"/>')
    j = "\n".join
    return (f'{XML}<ilog.rules.studio.model.ruleflow:RuleFlow {NS_XMI} '
            f'xmlns:ilog.rules.studio.model.ruleflow="http://ilog.rules.studio/model/ruleflow.ecore">\n'
            f'  <name>{name}</name>\n  <uuid>{u}</uuid>\n  <locale>{locale}</locale>\n  <categories>any</categories>\n'
            f'  <rfModel>\n<Ruleflow xmlns="http://schemas.ilog.com/Rules/7.0/Ruleflow">\n  <Body>\n'
            f'    <TaskList>\n{j(tasks)}\n    </TaskList>\n    <NodeList>\n{j(nodes)}\n    </NodeList>\n'
            f'    <TransitionList>\n{j(trans)}\n    </TransitionList>\n  </Body>\n  <Resources>\n'
            f'    <ResourceSet Locale="{locale}">\n{j(labels)}\n    </ResourceSet>\n  </Resources>\n'
            f'  <Properties>\n    <imports><![CDATA[use {java_pkg};\n]]></imports>\n  </Properties>\n</Ruleflow>\n'
            f'  </rfModel>\n</ilog.rules.studio.model.ruleflow:RuleFlow>\n')


def dop(name, vars_, rfl, u):
    op, vs = f"{name}Operation", f"{name}Parameters"
    refs = "".join(f'  <referencedVariables variableName="{n}" variableSetName="{vs}" direction="{d}">\n'
                   f'    <variableSet href="../rules/{vs}.var#{u["var"]}"/>\n  </referencedVariables>\n'
                   for n, _, d in vars_)
    return (f'{XML}<com.ibm.rules.studio.model.decisionservice:Operation {NS_XMI} '
            f'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" {NS_DS} '
            f'xmlns:ilog.rules.studio.model.query.extractor="http://ilog.rules.studio/model/query/extractor.ecore" '
            f'rulesetName="{name}Ruleset" usingRuleflow="true" ruleflowName="{rfl}" targetRuleProjectName="{name}">\n'
            f'  <name>{op}</name>\n  <uuid>{u["dop"]}</uuid>\n{refs}'
            f'  <ruleflow href="../rules/{rfl}.rfl#{u["rfl"]}"/>\n'
            f'  <extractor xsi:type="ilog.rules.studio.model.query.extractor:QueryExtractor" name="{op}_extractor" validator="Default Validator"/>\n'
            f'  <targetRuleProject href="../../{urllib.parse.quote(name)}#{u["project"]}"/>\n'
            f'</com.ibm.rules.studio.model.decisionservice:Operation>\n')


def dep(name, version, u):
    op = f"{name}Operation"
    policies = [
        ('Increment minor version numbers', ' ruleset="INCREMENT_MINOR" default="true"',
         'Updates the minor version for each ruleset. Makes the new version available but retains previous versions.'),
        ('Use the base version numbers', '',
         'Uses the numbers provided in the deployment configuration. Replaces the latest version of each ruleset with this release. Used for hot fixes or development.'),
        ('The user can define the version numbers', ' ruleset="MANUAL"',
         'Allows you to enter your own version numbers. Used for hot fixes or updates to an earlier release.')]
    pol = "".join(f'  <versionPolicies label="{l}"{a} recurrent="true">\n    <description><![CDATA[{d}]]></description>\n'
                  f'  </versionPolicies>\n' for l, a, d in policies)
    return (f'{XML}<com.ibm.rules.studio.model.decisionservice:Deployment {NS_XMI} {NS_DS} '
            f'ruleAppName="{name}" managingXom="true">\n  <name>{name}</name>\n  <uuid>{u["dep"]}</uuid>\n'
            f'  <operations operationName="{op}">\n    <operation href="{op}.dop#{u["dop"]}"/>\n'
            f'    <properties key="ruleset.version">\n      <value><![CDATA[{version}]]></value>\n    </properties>\n'
            f'  </operations>\n{pol}</com.ibm.rules.studio.model.decisionservice:Deployment>\n')


def b2xa(u):
    return (f'{XML}<b2x:translation xmlns:b2x="http://schemas.ilog.com/JRules/1.3/Translation" '
            f'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" '
            f'xsi:schemaLocation="http://schemas.ilog.com/JRules/1.3/Translation ilog/rules/schemas/1_3/b2x.xsd">\n'
            f'    <id>{u}</id>\n    <lang>ARL</lang>\n</b2x:translation>\n')


def brl(name, body, locale, u):
    return (f'{XML}<ilog.rules.studio.model.brl:ActionRule {NS_XMI} '
            f'xmlns:ilog.rules.studio.model.brl="http://ilog.rules.studio/model/brl.ecore">\n'
            f'  <name>{name}</name>\n  <uuid>{u}</uuid>\n  <locale>{locale}</locale>\n'
            f'  <definition><![CDATA[{body.strip()}]]></definition>\n</ilog.rules.studio.model.brl:ActionRule>\n')


# --------------------------------------------------------------------------- init

def cmd_init(a):
    name = a.name
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9]*", name):
        sys.exit(f"--name must be camelCase with no spaces/underscores, got '{name}'")
    root = os.path.abspath(a.dir)
    xom_dir, rp = os.path.join(root, a.xom), os.path.join(root, name)
    vars_ = []
    for spec in a.var:
        parts = spec.split(":")
        vname, vtype = parts[0], parts[1]
        direction = parts[2] if len(parts) > 2 else "IN_OUT"
        if "." not in vtype:
            vtype = f"{a.package}.{vtype}"
        vars_.append((vname, vtype, direction))
    pkgs = []
    for spec in a.packages.split(","):
        p, _, mode = spec.partition(":")
        pkgs.append((p.strip(), mode or "Fastpath"))
    u = {k: new_uuid() for k in ("project", "dop", "dep", "var", "rfl", "bom", "b2xa", "voc")}
    f = a.force

    # XOM project
    write(os.path.join(xom_dir, ".project"),
          eclipse_project(a.xom, "org.eclipse.jdt.core.javabuilder", ["org.eclipse.jdt.core.javanature"]), f)
    write(os.path.join(xom_dir, ".classpath"), xom_classpath(), f)
    os.makedirs(os.path.join(xom_dir, "src", *a.package.split(".")), exist_ok=True)
    lib = os.path.join(xom_dir, "lib")
    os.makedirs(lib, exist_ok=True)
    if a.fetch_jackson:
        for grp, art in JACKSON:
            dest = os.path.join(lib, f"{art}-{JACKSON_VERSION}.jar")
            if not os.path.exists(dest):
                url = (f"https://repo1.maven.org/maven2/com/fasterxml/jackson/{grp}/{art}/"
                       f"{JACKSON_VERSION}/{art}-{JACKSON_VERSION}.jar")
                urllib.request.urlretrieve(url, dest)
                print(f"fetched: {dest}")

    # Rule project
    write(os.path.join(rp, ".project"),
          eclipse_project(name, "ilog.rules.studio.model.ruleBuilder",
                          ["ilog.rules.studio.model.decisionProject", "ilog.rules.studio.model.operationProject",
                           "ilog.rules.studio.model.ruleNature"], [a.xom]), f)
    write(os.path.join(rp, ".ruleproject"), ruleproject(name, a.xom, a.base, u), f)
    write(os.path.join(rp, ".gitignore"), "output/\nreports/\n.syncEntries\n", f)
    for d in ("templates", "queries", "resources"):
        os.makedirs(os.path.join(rp, d), exist_ok=True)
    write(os.path.join(rp, "bom", f"{a.base}.bom"),
          f'property loadGetterSetterAsProperties "true"\nproperty origin "xom:/{name}/{a.xom}"\n'
          f'property uuid "{u["bom"]}"\npackage {a.package};\n\n// TODO: class declarations\n', f)
    write(os.path.join(rp, "bom", f"{a.base}.b2xa"), b2xa(u["b2xa"]), f)
    write(os.path.join(rp, "bom", f"{a.base}_{a.locale}.voc"),
          f"# Vocabulary Properties\nuuid = {u['voc']}\n\n# TODO: concept labels and phrases\n", f)
    write(os.path.join(rp, "rules", f"{name}Parameters.var"), varset(f"{name}Parameters", vars_, u["var"]), f)
    for p, _ in pkgs:
        write(os.path.join(rp, "rules", p, ".rulepackage"), rulepackage(p, new_uuid()), f)
    write(os.path.join(rp, "rules", f"{a.ruleflow}.rfl"), ruleflow(a.ruleflow, pkgs, a.package, a.locale, u["rfl"]), f)
    write(os.path.join(rp, "deployment", f"{name}Operation.dop"), dop(name, vars_, a.ruleflow, u), f)
    write(os.path.join(rp, "deployment", f"{name}.dep"), dep(name, a.version, u), f)

    # Build properties (paths relative to the properties file's directory, which is the build cwd)
    props = os.path.abspath(a.props or os.path.join(root, f"{name}.properties"))
    pdir = os.path.dirname(props)
    rel = lambda p: os.path.relpath(p, pdir)
    write(props, f"project = {rel(rp)}\noutput = {rel(os.path.join(rp, 'output'))}\ndep = {name}\n"
                 f"xom-classpath = {rel(os.path.join(xom_dir, a.xom + '-1.0.0.jar'))}\nruleapp-name = {name}\n", f)
    print(f"\nNext: write XOM classes in {os.path.join(xom_dir, 'src')}, fill the .bom/.voc, add rules with "
          f"`odm.py rule`, then `odm.py xom {os.path.relpath(xom_dir)}`, `odm.py check {rp}` and `odm.py build {props}`.")


# --------------------------------------------------------------------------- rule

def cmd_rule(a):
    rp = os.path.abspath(a.project)
    body = read(a.file) if a.file else sys.stdin.read()
    if not body.strip():
        sys.exit("empty BAL body (pass --file or pipe it on stdin)")
    pdir = os.path.join(rp, "rules", a.package)
    if not os.path.exists(os.path.join(pdir, ".rulepackage")):
        write(os.path.join(pdir, ".rulepackage"), rulepackage(a.package, new_uuid()))
        print(f"note: new package '{a.package}' — add it to the ruleflow if it isn't there")
    fname = re.sub(r"[^A-Za-z0-9_-]+", "-", a.name.strip()).strip("-").lower()
    write(os.path.join(pdir, f"{fname}.brl"), brl(a.name, body, a.locale, new_uuid()), a.force)
    for msg in lint_bal(body):
        print(f"lint: {msg}")


# --------------------------------------------------------------------------- check

RESERVED_LABEL_TOKENS = {"elapsed", "km", "distance", "travel", "speed", "minutes", "velocity", "span", "geolocation",
                         "increase", "decrease", "by", "points", "notifications", "location"}
BAL_PATTERNS = [
    (r"(>=|<=|==|!=|\s>\s|\s<\s)", "use 'is at least/at most/more than/less than' or 'is'/'is not', not symbols"),
    (r"\bis\s+(not\s+)?equal\s+to\b", "'is equal to' fails ('January' expected) — use plain 'is' / 'is not'"),
    (r"\bis\s+in\s*\{", "use 'is one of { ... }', not 'is in { ... }'"),
    (r"\bis\s+not\s+defined\b", "'is not defined' is not BAL — use 'is not null'"),
    (r"\bnot\s*\(", "negate with 'it is not true that ...', not 'not (...)'"),
]


def lint_bal(body):
    out = []
    body = re.sub(r'"[^"\n]*"', '""', body)  # ignore operators inside string literals
    for pat, msg in BAL_PATTERNS:
        if re.search(pat, body, re.I):
            out.append(msg)
    m = re.search(r"\bthen\b(.*?)(\belse\b|$)", body, re.S | re.I)
    if m:
        stmts = [s.strip() for s in m.group(1).strip().splitlines() if s.strip()]
        if stmts and not stmts[-1].endswith(";"):
            out.append("last 'then' statement must end with ';'")
    return out


def cmd_check(a):
    rp = os.path.abspath(a.project)
    errs, warns = [], []
    files = [p for p in glob.glob(os.path.join(rp, "**", "*"), recursive=True) if os.path.isfile(p)]
    files += glob.glob(os.path.join(rp, "**", ".rulepackage"), recursive=True)
    files += [os.path.join(rp, ".ruleproject")]
    files = sorted(set(p for p in files if os.path.exists(p) and "/output/" not in p))

    ids = []
    for p in files:
        ext = os.path.splitext(p)[1] or os.path.basename(p)
        if ext not in (".brl", ".var", ".rfl", ".dop", ".dep", ".ruleproject", ".rulepackage", ".dta",
                       ".bom", ".b2xa", ".voc"):
            continue
        t = read(p)
        found = (re.findall(r"<uuid>([^<]+)</uuid>", t) or re.findall(r'property uuid "([^"]+)"', t)
                 or re.findall(r"<id>([^<]+)</id>", t) or re.findall(r"^uuid\s*=\s*(\S+)", t, re.M))
        ids += [(i.strip(), os.path.relpath(p, rp)) for i in found[:1]]
        if ext in (".brl", ".dta") and a.lint:
            d = re.search(r"<definition><!\[CDATA\[(.*?)\]\]></definition>", t, re.S)
            if d and ext == ".brl":
                warns += [f"{os.path.relpath(p, rp)}: {m}" for m in lint_bal(d.group(1))]
            if ext == ".dta":
                for v in re.findall(r"<Param><!\[CDATA\[([A-Za-z_][A-Za-z0-9_ ]*)\]\]></Param>", t):
                    if v not in ("true", "false"):
                        warns.append(f"{os.path.relpath(p, rp)}: string Param '{v}' must be quoted: \"{v}\"")
    for i, n in Counter(i for i, _ in ids).items():
        if n > 1:
            errs.append(f"duplicate UUID {i} in: " + ", ".join(f for u, f in ids if u == i))
    by_id = {i: f for i, f in ids}

    rpf = os.path.join(rp, ".ruleproject")
    if not os.path.exists(rpf):
        sys.exit(f"no .ruleproject in {rp}")
    rpt = read(rpf)
    pname = re.search(r"<name>([^<]+)</name>", rpt).group(1)
    puuid = re.search(r"<uuid>([^<]+)</uuid>", rpt).group(1)
    if " " in pname:
        warns.append(f"project name '{pname}' contains spaces — use camelCase")
    if 'buildMode="DecisionEngine"' not in rpt:
        errs.append('.ruleproject: buildMode must be "DecisionEngine"')
    mig = re.search(r'MigratedToOperationId="([^"]*)"', rpt)

    dops = glob.glob(os.path.join(rp, "deployment", "*.dop"))
    dop_ids = {}
    for p in dops:
        t, rel = read(p), os.path.relpath(p, rp)
        du = re.search(r"<uuid>([^<]+)</uuid>", t).group(1)
        dop_ids[os.path.basename(p)] = (du, re.search(r"<name>([^<]+)</name>", t).group(1))
        tr = re.search(r'<targetRuleProject href="[^#]*#([^"]+)"', t)
        if not tr or tr.group(1) != puuid:
            errs.append(f"{rel}: targetRuleProject UUID != .ruleproject uuid {puuid}")
        trn = re.search(r'targetRuleProjectName="([^"]*)"', t)
        if not trn or trn.group(1) != pname:
            errs.append(f"{rel}: targetRuleProjectName != '{pname}'")
        for href in re.findall(r'href="\.\./rules/([^"#]+)#([^"]+)"', t):
            f, u = urllib.parse.unquote(href[0]), href[1]
            if by_id.get(u) != os.path.join("rules", f):
                errs.append(f"{rel}: href rules/{f}#{u} does not match that file's UUID")
        rfn = re.search(r'ruleflowName="([^"]*)"', t)
        if rfn and not os.path.exists(os.path.join(rp, "rules", rfn.group(1) + ".rfl")):
            errs.append(f"{rel}: ruleflowName '{rfn.group(1)}' has no rules/{rfn.group(1)}.rfl")
    if mig and mig.group(1) not in [d for d, _ in dop_ids.values()]:
        errs.append(".ruleproject: MigratedToOperationId does not match any .dop uuid")

    for p in glob.glob(os.path.join(rp, "deployment", "*.dep")):
        t, rel = read(p), os.path.relpath(p, rp)
        if not re.search(r'ruleAppName="[^"]+"', t):
            errs.append(f"{rel}: missing/empty ruleAppName (-> 'A RuleApp name cannot be empty')")
        if "<targets" in t:
            warns.append(f"{rel}: remove <targets> (no target server in generated .dep)")
        for block in re.findall(r"<operations\b.*?</operations>", t, re.S):
            on = re.search(r'operationName="([^"]*)"', block).group(1)
            h = re.search(r'href="([^"#]+)#([^"]+)"', block)
            h = h and (urllib.parse.unquote(h.group(1)), h.group(2))
            if 'key="ruleset.version"' not in block:
                errs.append(f"{rel}: operation {on} lacks ruleset.version property")
            if h:
                d = dop_ids.get(h[0])
                if not d or d[0] != h[1]:
                    errs.append(f"{rel}: operation href {h[0]}#{h[1]} != .dop uuid")
                elif d[1] != on:
                    errs.append(f"{rel}: operationName '{on}' != .dop name '{d[1]}'")

    bom = [os.path.splitext(os.path.basename(p))[0] for p in glob.glob(os.path.join(rp, "bom", "*.bom"))]
    b2x = [os.path.splitext(os.path.basename(p))[0] for p in glob.glob(os.path.join(rp, "bom", "*.b2xa"))]
    vocs = [os.path.basename(p) for p in glob.glob(os.path.join(rp, "bom", "*.voc"))]
    for b in bom:
        if b not in b2x:
            errs.append(f"bom/{b}.b2xa missing (must share the .bom base name)")
        if not any(re.fullmatch(re.escape(b) + r"_[a-z]{2}_[A-Z]{2}\.voc", v) for v in vocs):
            errs.append(f"bom/{b}_<locale>.voc missing (must share the .bom base name + locale suffix)")
        bt = read(os.path.join(rp, "bom", b + ".bom"))
        if bt.lstrip().startswith("<"):
            errs.append(f"bom/{b}.bom is XML — must be text BRL format")
        for m in re.findall(r"public\s+(?:readonly\s+)?boolean\s+(is[A-Z]\w*)\s*[;\s]", bt):
            warns.append(f"bom/{b}.bom: boolean '{m}' — BOM name is the setter name without 'set' (drop 'is')")
        for m in re.findall(r"\s(operator|function|rule|package|import)\s*;", bt):
            errs.append(f"bom/{b}.bom: reserved keyword '{m}' used as a property name")
    for v in vocs:
        for line in read(os.path.join(rp, "bom", v)).splitlines():
            if re.match(r"[^#=]*\(.*\)#phrase\.", line):  # method phrases only; property labels tolerate these
                label = line.split("=", 1)[-1]
                label = re.sub(r"\{[^}]*\}", " ", label)
                bad = sorted(RESERVED_LABEL_TOKENS & set(re.findall(r"[a-z/]+", label.lower())))
                if bad:
                    warns.append(f"bom/{v}: reserved token(s) {bad} in method phrase: {line.strip()[:100]}")

    for p in glob.glob(os.path.join(rp, "rules", "*.rfl")):
        for pkg in re.findall(r'<Package Name="([^"]+)"', read(p)):
            if not os.path.isdir(os.path.join(rp, "rules", pkg)):
                errs.append(f"{os.path.relpath(p, rp)}: package '{pkg}' has no rules/{pkg}/ directory")

    for w in warns:
        print(f"WARN  {w}")
    for e in errs:
        print(f"ERROR {e}")
    print(f"check: {len(errs)} error(s), {len(warns)} warning(s), {len(ids)} UUIDs")
    sys.exit(1 if errs else 0)


# --------------------------------------------------------------------------- xom

def cmd_xom(a):
    xd = os.path.abspath(a.xom_dir)
    name = os.path.basename(xd)
    srcs = glob.glob(os.path.join(xd, "src", "**", "*.java"), recursive=True)
    if not srcs:
        sys.exit(f"no .java files under {xd}/src")
    java = find_java()
    bindir = os.path.dirname(java)
    javac = os.path.join(bindir, "javac") if os.path.exists(os.path.join(bindir, "javac")) else "javac"
    jar = os.path.join(bindir, "jar") if os.path.exists(os.path.join(bindir, "jar")) else "jar"
    shutil.rmtree(os.path.join(xd, "bin"), ignore_errors=True)
    # --release 17: the ODM runtime rejects newer bytecode (UnsupportedClassVersionError, "bad major version")
    r = subprocess.run([javac, "--release", a.release, "-cp", os.path.join(xd, "lib", "*"),
                        "-d", os.path.join(xd, "bin")] + srcs, capture_output=True, text=True)
    if r.returncode:
        sys.exit(r.stdout + r.stderr)
    out = os.path.join(xd, f"{name}-1.0.0.jar")
    subprocess.run([jar, "cf", out, "-C", os.path.join(xd, "bin"), "."], check=True)
    print(f"compiled {len(srcs)} class file(s) -> {os.path.relpath(out)}")


# --------------------------------------------------------------------------- build

def find_java():
    if os.environ.get("ODM_JAVA"):
        return os.environ["ODM_JAVA"]
    if sys.platform == "darwin" and os.path.exists("/usr/libexec/java_home"):
        for v in ("21", "17"):
            r = subprocess.run(["/usr/libexec/java_home", "-v", v], capture_output=True, text=True)
            if r.returncode == 0:
                return os.path.join(r.stdout.strip(), "bin", "java")
    if os.environ.get("JAVA_HOME"):
        return os.path.join(os.environ["JAVA_HOME"], "bin", "java")
    return shutil.which("java") or "java"


def cmd_build(a):
    props = os.path.abspath(a.config)
    jar = os.path.abspath(a.jar)
    if not os.path.exists(jar):
        sys.exit(f"compiler not found: {jar} (set --jar, or use the build_ruleset MCP tool)")
    java = find_java()
    r = subprocess.run([java, "-jar", jar, "-config", os.path.basename(props)], cwd=os.path.dirname(props),
                       capture_output=True, text=True)
    lines = (r.stdout + r.stderr).splitlines()
    if a.full:
        print("\n".join(lines))
    else:
        keep = [l for l in lines if re.search(r"ERROR|Error|BUILD|RuleApp|Exception|aborted|WARN", l)]
        print("\n".join(keep[: a.max_lines]))
        if len(keep) > a.max_lines:
            print(f"... {len(keep) - a.max_lines} more relevant lines (use --full)")
    print(f"exit code: {r.returncode} ({'BUILD SUCCESS' if r.returncode == 0 else 'BUILD FAILURE'}) java={java}")
    sys.exit(r.returncode)


# --------------------------------------------------------------------------- main

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = ap.add_subparsers(dest="cmd", required=True)

    i = sp.add_parser("init", help="scaffold XOM + rule project")
    i.add_argument("--name", required=True, help="rule project name, camelCase (e.g. LoanApproval)")
    i.add_argument("--xom", required=True, help="XOM project name (e.g. loan-approval-xom)")
    i.add_argument("--base", required=True, help="base name shared by .bom/.b2xa/.voc (e.g. loan-approval)")
    i.add_argument("--package", required=True, help="Java package of the XOM (e.g. com.example.loan)")
    i.add_argument("--var", action="append", required=True,
                   help="ruleset parameter name:Type[:IN|OUT|IN_OUT] (repeatable; default IN_OUT)")
    i.add_argument("--packages", required=True,
                   help="ordered rule packages with mode, e.g. validation:Fastpath,scoring:RetePlus")
    i.add_argument("--ruleflow", default="main-ruleflow")
    i.add_argument("--locale", default="en_US")
    i.add_argument("--version", default="1.0", help="ruleset.version in the .dep")
    i.add_argument("--dir", default=".", help="parent directory for both projects")
    i.add_argument("--props", help="build .properties path (default <dir>/<name>.properties)")
    i.add_argument("--fetch-jackson", action="store_true", help=f"download Jackson {JACKSON_VERSION} jars into lib/")
    i.add_argument("--force", action="store_true", help="overwrite existing files")
    i.set_defaults(func=cmd_init)

    r = sp.add_parser("rule", help="create a .brl from a BAL body")
    r.add_argument("project", help="rule project directory")
    r.add_argument("package", help="rule package (directory under rules/)")
    r.add_argument("name", help="rule name (file name is derived from it)")
    r.add_argument("--file", help="file containing the BAL body (default: stdin)")
    r.add_argument("--locale", default="en_US")
    r.add_argument("--force", action="store_true")
    r.set_defaults(func=cmd_rule)

    c = sp.add_parser("check", help="consistency + lint checks")
    c.add_argument("project", help="rule project directory")
    c.add_argument("--no-lint", dest="lint", action="store_false", help="skip BAL/decision-table lint")
    c.set_defaults(func=cmd_check)

    x = sp.add_parser("xom", help="compile XOM (--release 17) and package its jar")
    x.add_argument("xom_dir", help="XOM project directory")
    x.add_argument("--release", default="17")
    x.set_defaults(func=cmd_xom)

    b = sp.add_parser("build", help="run rules-compiler.jar with filtered output")
    b.add_argument("config", help="build .properties file")
    b.add_argument("--jar", default=DEFAULT_JAR)
    b.add_argument("--max-lines", type=int, default=40)
    b.add_argument("--full", action="store_true", help="print the full compiler log")
    b.set_defaults(func=cmd_build)

    a = ap.parse_args()
    a.func(a)


if __name__ == "__main__":
    main()
