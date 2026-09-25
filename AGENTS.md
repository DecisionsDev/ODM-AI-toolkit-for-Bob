# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Repository layout

```
/
├── sample_projects/                        # Sample ODM Decision Service projects (reference)
│   ├── <Project_Dir>/               # One directory per project
│   │   ├── <Rule Project Name>/     # Rule project (contains .ruleproject)
│   │   ├── <domain>-xom/            # XOM Java project
│   │   ├── <ProjectName>.properties # Build Command properties for this project
│   │   ├── AI_Context.md            # File-by-file tour of this project
│   │   └── README.md
│   └── README.md
├── tools/
│   ├── odm-report-generator.py      # Markdown report + quality assessment generator
│   └── README-ODM-REPORT-GENERATOR.md
├── buildcommand/                    # Shared build tooling (extracted from ODM container)
│   └── rules-compiler/
│       └── rules-compiler.jar       # IBM ODM Build Command CLI (~52 MB)
├── odm.sh                           # Container manager: start|stop|restart|status
├── AGENTS.md                        # ← this file
└── AI_Context.md                    # ODM file-format reference
```

## Where to learn the formats

- [AI_Context.md](AI_Context.md): what every ODM file type is (`.bom`, `.voc`, `.brl`, `.dta`, `.rfl`, `.dop`, `.dep`, …), verified excerpts, cross-file linkage, build status per project, and the error table. Read it before writing or editing any rule project file.
- `sample_projects/<Project_Dir>/AI_Context.md`: file-by-file tour of one project. Use the projects that build (all except `Loan_Compliance_Service`) as the reference for new work.
- This file keeps only commands and do/don't rules; explanations live in `AI_Context.md`.

---

## ODM Docker Container — Canonical Runtime

**The local ODM Docker container is the single canonical runtime for all ODM-related tasks in this repository.**
Never attempt to use a separate ODM installation, a cloud instance, or a temporary `docker run` invocation for any task that the local container can serve.

| Property        | Value                                                |
|-----------------|------------------------------------------------------|
| Container name  | `odm`                                                |
| Image           | `icr.io/cpopen/odm-k8s/odm:latest`                  |
| HTTP port       | `9060` → `http://localhost:9060`                     |
| HTTPS port      | `9443` → `https://localhost:9443`                    |
| Decision Center | `http://localhost:9060/decisioncenter/t/home`         |
| Credentials     | `odmAdmin` / `odmAdmin`                              |
| Manager script  | `./odm.sh {start\|stop\|restart\|status}` (repo root)|

### Container rules

1. **Always check container state first.** Run `./odm.sh status` before any ODM operation.
2. **Download `rules-compiler.jar` from this container — never `docker run` a new one.** `buildcommand.zip` is at `http://localhost:9060/decisioncenter/assets/buildcommand.zip`. Do not re-download if already present and ≥ 10 MB.
3. **Deploying a RuleApp** — use the Decision Center REST API: `POST http://localhost:9060/decisioncenter-api/v1/deployments` with `Authorization: Basic odmAdmin:odmAdmin`.
4. **Never `docker rm` or `docker stop` the container** unless the user explicitly requests it.
5. **Never create a second ODM container** (`odm-buildcmd`, `odm-test`, etc.). Use `./odm.sh restart` instead.

---

## Commands

- **Check container**: `./odm.sh status`
- **Download buildcommand** (first time only — from the running ODM container):
  ```bash
  mkdir -p buildcommand/rules-compiler
  curl -s http://localhost:9060/decisioncenter/assets/buildcommand.zip --output buildcommand.zip
  unzip -o buildcommand.zip -d buildcommand 'rules-compiler/*'
  ls -lh buildcommand/rules-compiler/rules-compiler.jar   # expect ~52 MB
  ```
- **Compile XOM** (MUST use `--release 21`):
  `cd sample_projects/<Project_Dir>/<domain>-xom && javac --release 21 -cp "lib/*" -d bin src/<pkg>/*.java`
- **Package XOM JAR**:
  `cd sample_projects/<Project_Dir>/<domain>-xom && jar cf <domain>-xom.jar -C bin .`
- **Build / Validate Decision Service (single project)**:
  ```bash
  cd sample_projects/<Project_Dir>
  java -jar ../../buildcommand/rules-compiler/rules-compiler.jar -config <ProjectName>.properties
  ```
  - Build properties file: `sample_projects/<Project_Dir>/<ProjectName>.properties` (paths inside are relative to that dir)
  - **CRITICAL — `dep` key must equal the `.dep` `<name>` value**, not the ruleApp name. Wrong `dep` value → `"The deployment configuration named 'X' was not found"`.
- **Single Test / JSON Execution (POJO test)**:
  `cd sample_projects/<Project_Dir>/<domain>-xom && java -cp "bin:lib/*" com.<pkg>.JsonExample`
- **Generate Rule Project Markdown Report**:
  `python3 tools/odm-report-generator.py "sample_projects/<Project_Dir>/<Rule Project Name>" [output.md]`
  - See `tools/README-ODM-REPORT-GENERATOR.md` for full usage, quality scoring, and CI/CD integration.

---

## Critical Conventions & Gotchas

- **Java Version**: JDK 21 is mandatory (`--release 21` flag required when compiling XOM). Never use JDK 8, 11, or 26+. See the JDK compatibility table in [AI_Context.md](AI_Context.md).
- **XOM Serialization**: Do not implement `java.io.Serializable`. Use Jackson POJOs:
  - Add `@JsonInclude(JsonInclude.Include.NON_NULL)` at class level.
  - Add `@JsonIgnore` to all computed getter methods without matching setters (`isX()`, `hasX()`) to prevent JSON round-trip deserialization failure.
  - Initialise all nested objects in the default constructor — **never instantiate objects inside BAL rules**.
- **BOM Format**: Plain text BRL syntax only (never XML). Must start with property headers (`property loadGetterSetterAsProperties "true"`).
- **BOM/XOM three-pattern rule** (see [AI_Context.md §4.2](AI_Context.md) for full detail):
  - **Pattern 1** — standard getter+setter: omit from BOM (auto-mapped).
  - **Pattern 2** — `isX()` getter, no setter: declare `public readonly boolean x property "factory.ignore" "true";`.
  - **Pattern 3** — `hasX()` / `requiresX()` / `canX()` getter, no setter: declare as method call `public boolean hasX();` (parentheses required). Vocabulary phrase must also include `()`. Declaring as a property causes `GBREX0021E Cannot find attribute 'hasX' in execution class`.
- **Boolean BOM Mapping**: Boolean getter `isPregnant()` + `setPregnant()` maps to BOM `public boolean pregnant;` (strip `is` prefix; do NOT declare `isPregnant`).
- **Vocabulary Locales**: Files must match `{project-name}_{LOCALE}.voc` (default `en_US`).
- **Vocabulary unsafe tokens** — never use inside `{…}` phrase labels: `score`, `risk`, `elapsed`, `km`, `distance`, `travel`, `speed`, `increase`, `decrease`, `by`, `points`, `to`, `from`, `at`, `with`. All tokens inside `{…}` must be **all-lowercase** (case mismatch → `"The word 'XYZ' is expected in place of 'xyz'"`).
- **BAL Syntax Constraints**:
  - Comparison: Use `is at least`, `is at most`, `is more than`, `is less than` (never `>=`, `<=`, `>`, `<`, `==`).
  - String equality: plain `is "X"` / `it is not true that … is "X"` (never `is equal to "X"` → `The word 'January' is expected in place of 'equal'`).
  - Set Membership: `the X is one of { "A", "B" }` (never `is in { }`).
  - Negation: `it is not true that 'the var' is <adj>` (never `'the var' is not <adj>`).
  - Null check: `is null` / `is not null` (never `is not defined`).
  - Semicolons: every action statement in `then` must end with ` ;`.
  - No comments inside CDATA: never put `/* */` inside `<definition><![CDATA[…]]>` — BAL has no comment syntax (cascading `"The word '/' is not required"` errors).
  - Reserved words: never use `elapsed`, `distance`, `speed`, `minutes`, `points`, `increase`, `decrease`, `by` as custom phrase labels.
- **UUID Linkage**: `.dop` `<targetRuleProject href="../../ProjectName#[UUID]"/>` must match the exact `<uuid>` from `.ruleproject`. See [AI_Context.md §5](AI_Context.md) for the full cross-file linkage checklist.
- **Constructor synchronisation**: before declaring a constructor in BOM, verify the matching constructor (same parameter count, types, order) exists in XOM. Missing constructors produce `GBRET0009E: Failed to transform usage of constructor`.
- **`dep` key**: must equal the `.dep` `<name>` element value, not the ruleApp or project name.

---

## Dependency Analysis (Mandatory Before Writing Rules)

Before writing a single `.brl` rule or `.rfl` ruleflow, complete a dependency analysis. See [AI_Context.md §10](AI_Context.md) for the full workflow. In brief:

1. Build a **Read/Write matrix** — which package reads and writes which fields.
2. Derive **package execution order** from the matrix (data-flow dependencies).
3. Identify **mutual exclusion groups** — rules in the same RetePlus package that must not both fire share a guard on the decision field (e.g., `fraudDecision is "PENDING"`).
4. Ask the user to validate the ordering before generating any project files.
5. Create a `glossary.md` or `DEPENDENCIES.md` in the project root documenting the above.

---

## Quick error → fix reference

| Error | Fix |
|-------|-----|
| `GBREX0011E: Cannot find method 'resume()'` | Switch build JVM to JDK 21 |
| `UnsupportedClassVersionError` on XOM during B2X | Recompile XOM with `javac --release 21 …` |
| `GBREX0021E: Cannot find attribute 'hasX'` | Change BOM to method syntax: `public boolean hasX();` |
| `GBRET0009E: Failed to transform constructor` | Add matching constructor to XOM, recompile |
| `"The deployment configuration named 'X' was not found"` | Set `dep` = `.dep` `<name>` value (not the ruleApp name) |
| `"The word 'is' is expected in place of 'to'"` | Use `make it true/false that {this} is {adjective}` for booleans |
| `"The word 'January' is expected in place of 'equal'"` | Use bare `is "X"` for string equality, not `is equal to "X"` |
| `"The word '/' is not required"` + cascading errors | Remove `/* */` comment from inside `<definition><![CDATA[…]]>` |
| `{score} of {this}` phrase conflict | Rename phrase label to `{scoring}` |
| `{risk tier}` phrase conflict | Use `{country tier}` or another non-conflicting label |
| `"The word 'XYZ' is expected in place of 'xyz'"` | Lowercase all tokens inside `{…}` in vocabulary |
| `"The word 'each' is expected in place of 'a'"` | Initialise nested objects in XOM constructor — never create in rules |
| `Cannot find attribute 'isX'` | Strip `is` prefix: `public boolean x;` not `public boolean isX;` |
| `Ambiguous sentence` | Two vocabulary phrases share a prefix — make the full phrase unique |
| `A RuleApp name cannot be empty` | Add `ruleAppName="…"` to `.dep` root element |
| `Classic rule projects are not supported` | Add `isADecisionService="true"` and `OperationFolder` named `deployment` in `.ruleproject` |
| `Cannot load operation` | UUID mismatch — re-read `.dop` `<uuid>` and align `.dep` and `.ruleproject` |
