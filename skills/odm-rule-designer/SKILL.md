---
name: odm-rule-designer
description: >
  Build and fix IBM ODM (Operational Decision Manager) Decision Services — XOM, BOM, vocabulary,
  BAL rules, decision tables, ruleflows, deployment — and compile them locally; export a rule
  project and deploy it to Decision Center; build a RuleApp archive and deploy it to the Rule
  Execution Server (RES console); analyse dependencies between rules and design the ruleflow from
  them; review a rule project against ODM design best practices. Use for anything about IBM ODM,
  business rules, BOM/XOM/BAL, .ruleproject, rules-compiler, ruleset build errors, Decision Center
  import/deploy, RuleApp deployment to RES, rule order or a rule that never fires, or turning
  business requirements into executable rules — in any language (e.g. French "règles métier",
  "dépendances entre les règles", "ruleflow", "ordre d'exécution des règles").
license: Apache-2.0
allowed-tools: Bash(java -jar ${CLAUDE_SKILL_DIR}/scripts/odm.jar *) Bash(java ${CLAUDE_SKILL_DIR}/scripts/Odm.java *)
metadata:
  version: 1.1.0
  display_name: IBM ODM Rule Designer
  short_description: Build IBM ODM Decision Services — XOM/BOM/BAL authoring, ruleflow design, dependency analysis, Decision Center deploy, and RES RuleApp deployment.
  example_prompts: >-
    Create an ODM decision service for loan approval from scratch |
    Add a new BAL rule to check if the transaction country is sanctioned |
    Analyse rule dependencies and generate the ruleflow |
    Deploy my rule project to Decision Center |
    Build a RuleApp and deploy it to the Rule Execution Server |
    My rule never fires — help me debug it |
    Review my ODM project against design best practices |
    Génère un service de décision ODM pour valider une demande de crédit
---

# IBM ODM Rule Designer

Layers propagate upward only: **XOM** (Java POJOs) → **BOM** (text BRL + vocabulary) → **Rules** (BAL). The vocabulary is an API contract; the BOM does not auto-sync with the XOM.

**`<skill>`** below means the directory that contains this `SKILL.md`. Replace it with that absolute path, exactly as your agent reported it (don't resolve symlinks or make it relative), and start each command with `java -jar <skill>/scripts/odm.jar`: agents that pre-approve `allowed-tools` then run it without a permission prompt.

**One plain command per call.** Write the full path in each command. Don't store the path in a shell variable (`ODM=…; java -jar $ODM`), don't prefix it with `cd … &&` or `VAR=… ;`, and don't chain several `odm` calls with `;` or `&&`. Each of these forms makes the command miss the pre-approval, so the user gets a permission prompt.

`<skill>/scripts/odm.jar` (`odm` below; source in `scripts/Odm.java`) generates all XML boilerplate with consistent UUIDs and cross-links. **Never hand-write** `.project`, `.classpath`, the XOM `pom.xml`, `.ruleproject`, `.rulepackage`, `.var`, `.rfl`, `.dop`, `.dep`, `.b2xa`, `.brl` wrappers, or the build `.properties`. You author only the Java classes, the `.bom` body, the `.voc` body, and BAL rule bodies.

Answer in the user's language. Keep ODM identifiers, BAL, and commands as they are.

**Commands work in any shell** (bash, zsh, PowerShell, cmd): they are plain `java -jar …` calls. Quote paths that contain spaces. To pass multi-line text (a BAL body), write it to a file and use `--file`, never a shell heredoc.

## JDK

Run `odm` with any **JDK 17+** (not a JRE: `xom` needs `javac`). If `odm.jar` is missing, `java <skill>/scripts/Odm.java <subcommand> …` runs the source directly.

The rules compiler must run on the exact JDK of its ODM release (read from the `rules-compiler.jar` manifest). Only ODM 9.x is supported:

| ODM | Rules compiler JDK | XOM `--release` |
|---|---|---|
| 9.0.x | 17 | 17 |
| 9.5.x, 9.6.x | 21 | 17 |
| 9.7.x | 25 | 17 |

`build` starts the compiler on that JDK, found in this order: `$ODM_JAVA_HOME`, `/usr/libexec/java_home` (macOS), SDKMAN, `$JAVA_HOME`. A JDK with the wrong version is refused. If the required JDK is missing, tell the user to install it (IBM Semeru recommended) or set `ODM_JAVA_HOME`. Never work around it with another JDK.

## Workflow

0. **Rules compiler — first, before any scaffolding.** From the project root (the `<parent>` you'll give `odm init`):
   ```
   java -jar <skill>/scripts/odm.jar jdk
   ```
   - It prints `rules compiler: <path>`, the ODM release and the JDK → go to step 1.
   - `no rules compiler found` → install it with the command it prints, without asking for permission, but **tell the user first** that it may pull the ODM Docker image (about 1 GB, no server started):
     ```
     java -jar <skill>/scripts/odm.jar compiler --dir <project-root>
     ```
     If the user named an ODM version, add `--tag 9.0`, `--tag 9.5` or `--tag 9.6`; otherwise the default is `latest`. **Never search the file system for the jar** (`find`, `mdfind`, `locate`, `dir /s`). Ask the user only if `odm compiler` fails (no `ODM_HOME` and no working Docker/Podman). Details and manual fallbacks: `references/prerequisites.md`.
   - Continue without a compiler only if the user explicitly says so, and then say at the end that the rules were not validated.
1. **Names.** camelCase rule project (`LoanApproval`), kebab XOM project (`loan-approval-xom`), kebab base name (`loan-approval`), locale `en_US` unless told otherwise. Kebab-case `verb-object` rule names (`check-age`), unique across packages.
2. **Scaffold:**
   ```
   java -jar <skill>/scripts/odm.jar init --dir <parent> --name LoanApproval --xom loan-approval-xom --base loan-approval --package com.example.loan --var request:LoanRequest:IN --var decision:LoanDecision:OUT --packages validation,scoring,decision --fetch-jackson
   ```
   `--var name:Type[:IN|OUT|IN_OUT]` is repeatable (default `IN_OUT`, verbalized `the <name>`). Use one input and one output object, or a single `IN_OUT` object. OUT variables get `initialValue="new <Type>()"`, so the XOM class needs a public no-arg constructor. Packages are ruleflow tasks, in order; every task uses **Fastpath** (any other mode is refused). Never use RetePlus: when rules chain inside a package, split it into sequential packages.
3. **XOM** — read `references/xom.md`, write classes under `src/`, then `odm xom <xom-dir>`. It compiles with `--release 17` and packages the jar, and it fetches the Jackson jars if they're missing. Never use `mvn` for the XOM.
4. **BOM + vocabulary** — append to the generated `bom/<base>.bom` and `bom/<base>_<locale>.voc`. Read `references/bom-format.md` and `references/vocabulary-and-bal.md`.
5. **Rules** — write each BAL body to a file, then one call per rule:
   ```
   java -jar <skill>/scripts/odm.jar rule <RuleProjectDir> validation check-amount --file check-amount.bal
   ```
   (Piping the body on stdin also works.) Each rule has one action phrase, `and` conditions only, and no `else`, `print` or priority. **Before writing rules, look for lookups:** when the requirement maps the values of one or more inputs to an output value (a limit, rate, score or category per tier, region, band, …), write **one decision table, even for 2–4 rows**, instead of one rule per value. Also use a decision table for 5+ rules with the same condition/action shape. See `references/decision-tables.md`.
6. **Check, then build — mandatory.** The task is not done until `odm build` prints `BUILD SUCCESS`. Never stop after `check`. Repeat until it succeeds:
   ```
   java -jar <skill>/scripts/odm.jar check <RuleProjectDir>
   java -jar <skill>/scripts/odm.jar build <parent>/<Name>.properties
   ```
   `check` validates UUIDs, links and file names, checks every BOM attribute against the XOM getters and every OUT variable's initial value, lints BAL, and reports design issues as `WARN … best practice:`. On a new project, fix the warnings too. `build` prints only error and result lines (`--full` for the whole log), always embeds the XOM in the RuleApp, refuses a XOM jar older than its sources, and on success prints the RuleApp path and its ruleset paths. If the compiler is missing but a `build_ruleset` MCP tool is connected, use that tool. If neither is available, say so and don't claim the rules are validated.
7. **Deploy to Decision Center** — only when the user asks, after `BUILD SUCCESS`. Follow `references/decision-center-deployment.md` (`odm export`, then the ODM Management MCP Server).
8. **Deploy a RuleApp to the RES console** — only when the user asks, after `BUILD SUCCESS`. Follow `references/decision-server-console-deployment.md` (the `ruleapp:` path printed by `build`, then the ODM Management MCP Server).

For both deployments: confirm with the user before changing a shared server. If the MCP server or its tools aren't available, give the archive path and the manual steps, and don't claim anything was deployed.

## Rule dependencies → ruleflow

To find which rules depend on which, why a rule never fires, or what the ruleflow should be, read `references/ruleflow-design.md`, then:
```
java -jar <skill>/scripts/odm.jar deps <RuleProjectDir>
java -jar <skill>/scripts/odm.jar ruleflow <RuleProjectDir> --packages a,b,c
```
`deps` reports who writes what others read, checked against the current ruleflow. `ruleflow` rewrites the `.rfl` and keeps its name and UUID. Also run `deps` after adding rules to a package that another task reads from. `ERROR … reads X before it is written` means the rule sees only the input value.

## Design best practices (readability for business users)

Read `references/best-practices.md` before you design or restructure a project. In short:

- **Layout:** rules only in leaf packages, never in the `rules/` root. No empty packages. Each package holds cohesive rules. One naming convention, no duplicate rule names.
- **Rules:** one action phrase per rule (use a virtual BOM method when actions always go together). `and` only: split an `or` into several rules. No `else`, `print` or priorities. As few functions as possible.
- **BOM:** B2X bodies have at most 5 statements; put more logic in the XOM.
- **Decision tables:** at most 500 rows, no sparse tables.
- **Ruleflow:** tasks reference packages; Fastpath only; at most 10 tasks and a cyclomatic complexity of 5, but no trivial subflows; no exit criteria, firing limits, custom ordering, task actions or dynamic filters.
- **Parameters:** one input and one output object, and as few variables as possible.

## Rules that break builds

`odm check` reports the first four. More detail is in `references/vocabulary-and-bal.md`.

- **Every BOM attribute needs a XOM getter** `getX()` or `isX()` (or a B2X body). Naming follows the getter without `get`/`is`: `isPregnant()` → `public boolean pregnant;`, never `isPregnant`. A plain `hasX()` is not a getter: rename it `isHasX()` → BOM `hasX`. Computed getters with no setter → `public readonly <type> x property "factory.ignore" "true";` plus `@JsonIgnore` in Java.
- **OUT variables need an object:** `initialValue="new <pkg>.<Type>()"` in the `.var` (`odm init` writes it).
- **BAL operators:** `is more than`, `is less than`, `is at least`, `is at most`; equality is plain `is` / `is not`. Never `is equal to`, `is in`, `is not defined`, `not (…)`, or symbols (`>`, `>=`, `==`).
- **Reserved label words** in method phrases (`points`, `by`, `distance`, …): `odm check` prints the full list.
- **Declare every XOM property the rules use in the BOM.** The compiler never reads the XOM to find properties.
- Negation: `it is not true that <condition>`. To compare two navigated values, bind one in `definitions` (`set 'v' to <expr> ;`) and write `it is not true that X is 'v'`.
- Every statement in `then` ends with ` ;`.
- Null-check each intermediate object of a navigation chain in its own `and` line.
- Collections: never `is empty`. Expose a computed boolean instead.
- Reserved property names: `operator`, `function`, `rule`, `package`, `import`.
- Phrases must not share an opening token sequence (`Ambiguous sentence`).
- The compiler stops at the **first failing package** (alphabetical order). A clean report for other packages proves nothing.

## Error → fix

| Compiler message | Fix |
|---|---|
| `While applying business to execution (B2X) model mapping` / `GBREX0021E: Cannot find attribute 'x'` | A BOM attribute has no XOM getter. `odm check` names it: rename `isX` → `x` in the BOM and `.voc`, or `hasX()` → `isHasX()` in the XOM |
| `GBRED0040E: The ruleset variable 'X' is read but never written` | OUT variable with an empty `initialValue`: set `new <pkg>.<Type>()` in the `.var` |
| `The word 'length' is expected in place of 'x'` | `x` not declared in the BOM, or a numeric computed property verbalized `{x} of {this}` (use a method phrase) |
| `The word 'January' is expected in place of 'equal'` | replace `is equal to` with `is` |
| `The word 'X' is expected / missing / not required` | the rule text doesn't match the `.voc` phrase exactly, or a reserved token is used in a phrase |
| `Ambiguous sentence` | make phrase openings distinct |
| `Invalid type 'X', not assignable from 'Y'` | type mismatch between phrase and argument |
| `Value 'X' is invalid` (decision table) | quote string params: `<![CDATA["X"]]>` |
| `bad major version` | recompile with `odm xom` |
| `A RuleApp name cannot be empty` / `Cannot load operation` | run `odm check`; it pinpoints the broken `.dep`/`.dop` link |

## References (read only what the current step needs)

- `references/best-practices.md`: the 23 design best practices, what `odm check` detects, virtual BOM methods
- `references/prerequisites.md`: where `odm` looks for `rules-compiler.jar`, `odm compiler`, manual fallbacks, checking the JDK
- `references/xom.md`: Java class conventions, Jackson, computed properties
- `references/bom-format.md`: BOM syntax, annotations, a full example, collections
- `references/vocabulary-and-bal.md`: `.voc` syntax, the full BAL constraint list, patterns from past builds
- `references/decision-tables.md` + `assets/templates/decision-table.dta` (ranges) + `assets/templates/lookup-decision-table.dta` (exact values)
- `references/ruleflow-design.md`: reading `odm deps`, choosing the task order, applying it with `odm ruleflow`
- `references/decision-center-deployment.md`: `odm export`, importing and deploying with the ODM Management MCP Server, fallbacks
- `references/decision-server-console-deployment.md`: the RuleApp from `odm build`, deploying it to the RES console, fallbacks
- `schemas/*.ecore`, `schemas/b2x.xsd`: exact ODM metamodels, only for a generated file rejected for an unclear reason. Search them for the class or attribute you need; don't read them whole (`model.ecore` is 67 KB).

`rules-compiler.jar` and `schemas/` come from a licensed ODM install and are not committed. If `schemas/` is missing, ask the user for their ODM Rule Designer path.
