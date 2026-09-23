---
name: odm-rule-designer
description: Build and fix IBM ODM (Operational Decision Manager) Decision Services — XOM, BOM, vocabulary, BAL rules, decision tables, ruleflows, deployment — and compile them locally. Use for anything about IBM ODM, business rules, BOM/XOM/BAL, .ruleproject, rules-compiler, ruleset build errors, or turning business requirements into executable rules.
---

# IBM ODM Rule Designer

Layers propagate upward only: **XOM** (Java POJOs) → **BOM** (text BRL + vocabulary) → **Rules** (BAL). The vocabulary is an API contract; BOM does not auto-sync with XOM.

`scripts/odm.py` generates all XML boilerplate with consistent UUIDs and cross-links. **Never hand-write** `.project`, `.classpath`, `.ruleproject`, `.rulepackage`, `.var`, `.rfl`, `.dop`, `.dep`, `.b2xa`, `.brl` wrappers, or the build `.properties`; you only author Java classes, the `.bom` body, the `.voc` body, and BAL rule bodies.

## Workflow

1. **Names.** camelCase rule project (`LoanApproval`), kebab XOM project (`loan-approval-xom`), kebab base name (`loan-approval`), locale `en_US` unless told otherwise.
2. **Scaffold:**
   ```bash
   python3 <skill>/scripts/odm.py init --dir <parent> --name LoanApproval --xom loan-approval-xom \
     --base loan-approval --package com.example.loan --var request:LoanRequest \
     --packages validation:Fastpath,scoring:RetePlus --fetch-jackson
   ```
   `--var name:Type[:IN|OUT|IN_OUT]` is repeatable (default `IN_OUT`, verbalized `the <name>`). Packages are ruleflow tasks in order; one mode each (`Fastpath` sequential, `RetePlus` inference).
3. **XOM** — write classes under `src/`; read `references/xom.md` first. Then `odm.py xom <xom-dir>` (compiles with `--release 17` and packages the jar).
4. **BOM + vocabulary** — append to the generated `bom/<base>.bom` and `bom/<base>_<locale>.voc`; read `references/bom-format.md` and `references/vocabulary-and-bal.md`.
5. **Rules** — one call per rule, BAL body on stdin:
   ```bash
   python3 <skill>/scripts/odm.py rule <RuleProjectDir> validation "check amount" <<'EOF'
   if
     the amount of 'the request' is more than 1000
   then
     add "Amount too high" to the messages of 'the request' ;
   EOF
   ```
   For 5+ rules with the same shape, use a decision table: `references/decision-tables.md`.
6. **Check, then build** — repeat until `BUILD SUCCESS`:
   ```bash
   python3 <skill>/scripts/odm.py check <RuleProjectDir>
   python3 <skill>/scripts/odm.py build <parent>/<Name>.properties
   ```
   `check` validates UUID uniqueness and links, file names, ruleflow packages, and lints BAL. `build` runs `tools/rules-compiler.jar` and prints only error/result lines (`--full` for the whole log). If the jar is missing but a `build_ruleset` MCP tool is connected, use that (`projectPath`, `xomJarPath`, `rulesetName`, optional `decisionOperation`). If neither is available, say so and don't claim the rules are validated.

## Rules that break builds (inline so you rarely need the references)

- **Declare every XOM property in the BOM.** The compiler does not read the XOM to infer properties: an undeclared property fails in rules (e.g. `The word 'length' is expected in place of 'amount'`).
- Boolean `isX()`/`setX()` → BOM `public boolean x;` (never `isX`). Computed getters with no setter → `public readonly <type> x property "factory.ignore" "true";` plus `@JsonIgnore` in Java.
- XOM bytecode must be ≤ Java 21 (`odm.py xom` uses `--release 17`), or you get `bad major version`.
- BAL comparisons: `is more than`, `is less than`, `is at least`, `is at most`; equality is plain `is` / `is not`. **Never** `is equal to` (fails with `'January' expected`), nor symbols (`>`, `>=`, `==`).
- `is one of { "A", "B" }`, never `is in`. Negation: `it is not true that <condition>`. `is not null`, never `is not defined`.
- Every statement in `then` ends with ` ;`.
- Null-check each intermediate object in a navigation chain in its own `and` clause before using it.
- Collections: never `is empty`. Expose a computed boolean instead.
- Reserved names: `operator`, `function`, `rule`, `package`, `import` can't be property names.
- Phrases must not share an opening token sequence (`Ambiguous sentence`).
- The compiler stops at the **first failing package** (alphabetical order). A clean report for other packages proves nothing, so fix the first error and rebuild.

## Error → fix

| Compiler message | Fix |
|---|---|
| `Cannot find attribute 'isX' in execution class` | BOM boolean named `x`, not `isX` |
| `The word 'length' is expected in place of 'x'` | property `x` not declared in BOM, or numeric computed property used with `{x} of {this}` (use a method phrase) |
| `The word 'January' is expected in place of 'equal'` | replace `is equal to` with `is` |
| `The word 'X' is expected / missing / not required` | phrase doesn't match the vocabulary exactly, or reserved token in a phrase |
| `Ambiguous sentence` | make phrase openings distinct |
| `Invalid type 'X', not assignable from 'Y'` | type mismatch between phrase and argument |
| `Value 'X' is invalid` (decision table) | quote string params: `<![CDATA["X"]]>` |
| `bad major version` | recompile with `odm.py xom` |
| `A RuleApp name cannot be empty` / `Cannot load operation` | re-run `odm.py check`; it pinpoints the broken `.dep`/`.dop` link |

## References (read only what the current step needs)

- `references/xom.md`: Java class conventions, Jackson, computed properties
- `references/bom-format.md`: BOM syntax and annotations, a full example, collections
- `references/vocabulary-and-bal.md`: `.voc` syntax, the full BAL constraint list, and patterns from past builds
- `references/decision-tables.md` + `assets/templates/decision-table.dta`
- `schemas/*.ecore`, `schemas/b2x.xsd`: exact ODM metamodels, useful only when a generated file is rejected for an unclear reason. They are large (model.ecore is 67 KB), so `grep` for the class or attribute you need instead of reading them whole.

`tools/` and `schemas/` come from a licensed ODM install and are not committed. If they're missing, ask the user for their ODM Rule Designer path.
