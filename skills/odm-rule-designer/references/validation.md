# Validation — Building the Ruleset

Building the ruleset is the only real proof the project is correct. It is performed by IBM's **Build Command CLI** (`rules-compiler.jar`), which parses BAL, resolves the BOM↔XOM mapping, and produces a RuleApp JAR.

```
java -jar rules-compiler.jar -config <ProjectName>.properties
```

- Exit code `0` → BUILD SUCCESS: `[INFO] RuleApp "[Name].jar" saved in "[path]/output"`
- Exit code `2` → BUILD FAILURE: `[ERROR] Error: [message] ([package].[rule-name])`

## Path A (preferred): the bundled compiler

This skill ships `tools/rules-compiler.jar` locally — no download or Docker step needed, use it directly.

**Step 1 — create `buildcommand/samples/config-files/[ProjectName].properties`** in the workspace (paths relative to that directory):
```
project = ../../../[ProjectName]
output = ../../../[ProjectName]/output
dep = [ProjectName]
xom-classpath = ../../../[xom-project-name]/[xom-project-name]-1.0.0.jar
ruleapp-name = ProjectName
```

**Step 2 — run with JDK 17+** (IBM Semeru OpenJ9 21 preferred, then Semeru 17, then standard OpenJDK 21 — never JDK 8 or 11):
```bash
/usr/libexec/java_home -V 2>&1 | grep -i semeru   # locate a Semeru JDK, macOS
cd buildcommand/samples/config-files && \
  <java-home>/bin/java -jar <path-to-skill>/tools/rules-compiler.jar -config [ProjectName].properties 2>&1
```

## Path B: the `build_ruleset` MCP tool

If an MCP tool named `build_ruleset` (or similar) is connected, it's equally valid — the ODM toolchain runs server-side and you get a structured result back instead of parsing CLI output.

Input:
```json
{
  "projectPath": "/abs/path/to/Rule Project",
  "xomJarPath": "/abs/path/to/xom-1.0.0.jar",
  "rulesetName": "ProjectNameRuleset",
  "decisionOperation": "ProjectNameOperation"   // optional
}
```
Output: `{ success, exitCode, rulesetArchivePath, errors: [{rule, message}], rawLog }`. On failure, act on `errors` directly — fix one at a time, then call the tool again.

## Path C: neither available

State this plainly. Compile the XOM with `javac` to catch Java errors, then tell the user the rules were not compiled and must be built with the bundled `tools/rules-compiler.jar` or a connected `build_ruleset` MCP tool. Do not claim the project is validated.

## Interpreting failures and the fix-rebuild cycle

| Error message | Likely cause |
|---|---|
| `The word 'X' is expected in place of 'Y'` | Wrong BAL keyword / phrase mismatch |
| `The word 'X' is not required` | Extra tokens ODM doesn't recognize |
| `The word 'X' is missing` | Missing token (often reserved-keyword conflict) |
| `Invalid type 'X', it is not assignable from type 'Y'` | Type mismatch |
| `Cannot find attribute 'X' in execution class` | BOM property name mismatch with XOM (see `bom-format.md` booleans) |
| `Ambiguous sentence` | Two vocabulary phrases share an opening token sequence |
| `A RuleApp name cannot be empty` | Missing `ruleAppName` on `.dep` root element (see `ruleflow-deployment.md`) |

Cycle: read the failing rule → read the vocabulary → apply the fix (see `vocabulary-and-bal.md` and `bom-format.md`) → if XOM changed, recompile Java and rebuild the XOM JAR first → rebuild. Fix the first error first; later ones are often cascading.

**CRITICAL — the compiler aborts at the first broken rule package and validates nothing after it.** Confirmed empirically: packages appear to be checked in alphabetical order by package name (not ruleflow execution order), and compilation stops completely at the first package containing an error — `[ERROR] Compilation ... aborted`. A clean build report for everything else is **not proof the rest of the project is valid** — it may simply never have been reached. Do not assume "no errors shown for package X" means package X is correct; re-run the build after every fix and keep going until the reported failing package changes or the build reaches `BUILD SUCCESS`. If a package name happens to sort early alphabetically (e.g. `audit-logging`), it can mask real errors in every other package for many rebuild cycles.

## UUID generation and uniqueness

Every ODM artifact file needs its own UUID. Never reuse or copy-paste one from a template.

**Generate in batch before creating files** (one file needs one UUID: `.ruleproject`, each `.rulepackage`, each `.brl`, `.var`, `.rfl`, `.dop`, `.dep`, `.bom`, `.b2xa`, optionally `.voc`):
```bash
# macOS/Linux
for i in {1..15}; do uuidgen | tr '[:upper:]' '[:lower:]'; done

# cross-platform
python3 -c "import uuid; [print(str(uuid.uuid4())) for _ in range(15)]"
```
Assign them to a list before writing files, so you don't reach for the same UUID twice. Duplicates cause "Cannot load operation" errors and can corrupt the project in Rule Designer.

**Verify uniqueness after a successful build:**
```bash
{ grep -r "<uuid>" "[ProjectName]" --include="*.brl" --include="*.var" --include="*.rfl" \
    --include="*.dop" --include="*.dep" --include="*.ruleproject" --include="*.rulepackage" \
    | sed 's/.*<uuid>\(.*\)<\/uuid>.*/\1/'; \
  grep -E "property uuid" "[ProjectName]/bom/"*.bom | sed 's/.*"\([a-f0-9\-]*\)".*/\1/'; \
  grep "<id>" "[ProjectName]/bom/"*.b2xa | sed 's/.*<id>\(.*\)<\/id>.*/\1/'; \
  grep "<id>" "[ProjectName]/bom/"*.voc  | sed 's/.*<id>\(.*\)<\/id>.*/\1/'; } | sort | uniq -d
```
Empty output = no duplicates (good). For any duplicate, regenerate with `uuidgen`, replace in the affected files, re-check, and rebuild.
