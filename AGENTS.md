# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Where to learn the formats

- [AI_Context.md](AI_Context.md): what every ODM file type is (`.bom`, `.voc`, `.brl`, `.dta`, `.rfl`, `.dop`, `.dep`, …), verified excerpts, cross-file linkage, build status per project, and the error table. Read it before writing or editing any rule project file.
- `projects/<Project>/AI_Context.md`: file-by-file tour of one project. Use the projects that build (all except `Loan_Compliance_Service`) as the reference for new work.
- This file keeps only commands and do/don't rules; explanations live in `AI_Context.md`.

## Commands

- **Compile XOM**: `cd projects/<Domain>/<domain>-xom && javac -cp "lib/*" -d bin src/<pkg>/*.java`
- **Package XOM JAR**: `cd projects/<Domain>/<domain>-xom && jar cf ../../../buildcommand/samples/config-files/<domain>-xom-1.0.0.jar -C bin .`
- **Build / Validate Decision Service (single project)**:
  `cd buildcommand/samples/config-files && java -jar ../../rules-compiler/rules-compiler.jar -config <ProjectName>.properties`
- **Single Test / JSON Execution (POJO test)**:
  `cd projects/<Domain>/<domain>-xom && java -cp "bin:lib/*" com.<pkg>.JsonExample`
- **Generate Rule Project Markdown Report**:
  `python3 tools/odm-report-generator.py "projects/<Domain>/<Project Name>" [output.md]`

## Critical Conventions & Gotchas

- **Java Version**: Requires JDK 17+ (JDK 21 IBM Semeru OpenJ9 recommended); never use JDK 8/11.
- **XOM Serialization**: Do not implement `java.io.Serializable`. Use Jackson POJOs:
  - Add `@JsonInclude(JsonInclude.Include.NON_NULL)` at class level.
  - Add `@JsonIgnore` to all computed getter methods without matching setters (`isX()`, `hasX()`) to prevent JSON round-trip deserialization failure.
- **BOM Format**: Plain text BRL syntax only (never XML). Must start with property headers (`property loadGetterSetterAsProperties "true"`).
- **Boolean BOM Mapping**: Boolean getter `isPregnant()` + `setPregnant()` maps to BOM `public boolean pregnant;` (strip `is` prefix; do NOT declare `isPregnant`).
- **Vocabulary Locales**: Files must match `{project-name}_{LOCALE}.voc` (default `en_US`).
- **BAL Syntax Constraints**:
  - Comparison: Use `is at least`, `is at most`, `is more than`, `is less than` (never `>=`, `<=`, `>`, `<`, `==`). For equality use plain `is` / `is not` (`is equal to` fails: `The word 'January' is expected in place of 'equal'`).
  - Set Membership: Use `the X is one of { "A", "B" }` (never `is in { }`).
  - Negation: Use `it is not true that 'the var' is <adj>` (never `'the var' is not <adj>`).
  - Semicolons: Every action statement in `then` must end with `;`.
  - Reserved Words: Never use `elapsed`, `distance`, `speed`, `minutes`, `points`, `increase`, `decrease`, `by` as custom phrase labels.
- **UUID Linkage**: `.dop` file `<targetRuleProject href="../../ProjectName#[UUID]"/>` must match the exact `<uuid>` from `.ruleproject`.
