# AGENTS.md

This file provides guidance to agents when working with code in this repository.

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

- **Java Version**: Only ODM 9.x is supported; never use JDK 8/11. The rules compiler must run on the exact JDK of its ODM release: 9.0.x → JDK 17, 9.5.x/9.6.x → JDK 21, 9.7.x → JDK 25 (read from `Implementation-Version` in the `rules-compiler.jar` manifest; `odm jdk` shows it). Set `ODM_JAVA_HOME` to choose that JDK. Compile the XOM with `--release 17`. IBM Semeru OpenJ9 recommended.
- **XOM Serialization**: Do not implement `java.io.Serializable`. Use Jackson POJOs:
  - Add `@JsonInclude(JsonInclude.Include.NON_NULL)` at class level.
  - Add `@JsonIgnore` to all computed getter methods without matching setters (`isX()`, `hasX()`) to prevent JSON round-trip deserialization failure.
- **BOM Format**: Plain text BRL syntax only (never XML). Must start with property headers (`property loadGetterSetterAsProperties "true"`).
- **Boolean BOM Mapping**: Boolean getter `isPregnant()` + `setPregnant()` maps to BOM `public boolean pregnant;` (strip `is` prefix; do NOT declare `isPregnant`).
- **Vocabulary Locales**: Files must match `{project-name}_{LOCALE}.voc` (default `en_US`).
- **BAL Syntax Constraints**:
  - Comparison: Use `is at least`, `is at most`, `is more than`, `is less than`, `is equal to` (never `>=`, `<=`, `>`, `<`, `==`).
  - Set Membership: Use `the X is one of { "A", "B" }` (never `is in { }`).
  - Negation: Use `it is not true that 'the var' is <adj>` (never `'the var' is not <adj>`).
  - Semicolons: Every action statement in `then` must end with `;`.
  - Reserved Words: Never use `elapsed`, `distance`, `speed`, `minutes`, `points`, `increase`, `decrease`, `by` as custom phrase labels.
- **UUID Linkage**: `.dop` file `<targetRuleProject href="../../ProjectName#[UUID]"/>` must match the exact `<uuid>` from `.ruleproject`.
