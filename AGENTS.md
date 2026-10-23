# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## ODM work: use the skill

For anything about IBM ODM (XOM, BOM, vocabulary, BAL rules, decision tables, ruleflows, build, deployment), use the **odm-rule-designer** skill in [`skills/odm-rule-designer/`](skills/odm-rule-designer/SKILL.md). It is the single source of truth for ODM conventions and build gotchas, and its `odm` CLI (`skills/odm-rule-designer/scripts/odm.jar`) scaffolds, checks, compiles and exports projects. Don't restate or override its rules here.

If your agent doesn't load skills automatically, read `skills/odm-rule-designer/SKILL.md` before you change an ODM project.

## Repository layout

- `projects/<Domain>/<Project Name>/`: rule projects, each next to its XOM project `<domain>-xom/`
- `skills/odm-rule-designer/`: the skill (instructions, references, `odm` CLI)
- `buildcommand/rules-compiler/`: the IBM rules compiler, installed with `odm compiler` (licensed: never commit it)
- `tools/odm-report-generator.py`: Markdown report of a rule project

## Commands

- **Generate a rule project Markdown report**:
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
