---
name: odm-rule-designer
description: Build and fix IBM ODM (Operational Decision Manager) Decision Services — XOM, BOM, vocabulary, BAL rules, decision tables, ruleflows, deployment — and compile them locally; analyse dependencies between rules and design the ruleflow from them. Use for anything about IBM ODM, business rules, BOM/XOM/BAL, .ruleproject, rules-compiler, ruleset build errors, rule order or a rule that never fires, or turning business requirements into executable rules — in any language (e.g. French "règles métier", "dépendances entre les règles", "ruleflow", "ordre d'exécution des règles").
---

# IBM ODM Rule Designer

Layers propagate upward only: **XOM** (Java POJOs) → **BOM** (text BRL + vocabulary) → **Rules** (BAL). The vocabulary is an API contract; BOM does not auto-sync with XOM.

`scripts/odm.jar` (`odm` below; source in `scripts/Odm.java`, JDK only, no Python) generates all XML boilerplate with consistent UUIDs and cross-links. **Never hand-write** `.project`, `.classpath`, the XOM `pom.xml`, `.ruleproject`, `.rulepackage`, `.var`, `.rfl`, `.dop`, `.dep`, `.b2xa`, `.brl` wrappers, or the build `.properties`; you only author Java classes, the `.bom` body, the `.voc` body, and BAL rule bodies.

Answer in the user's language. Keep ODM identifiers, BAL, and commands as they are.

## Workflow

Run `odm` with any **JDK 17+** (not a JRE: `xom` needs `javac`). If `odm.jar` is missing, `java <skill>/scripts/Odm.java <subcommand> …` runs the source directly.

**JDK alignment.** The rules compiler must run on the exact JDK of its ODM release. `odm` reads the release from the `rules-compiler.jar` manifest (`Implementation-Version`), and only ODM 9.x is supported:

| ODM | Rules compiler JDK | XOM `--release` |
|---|---|---|
| 9.0.x | 17 | 17 |
| 9.5.x, 9.6.x | 21 | 17 |
| 9.7.x | 25 | 17 |

`build` launches the compiler as a separate process on that JDK, so `odm` itself can run on any JDK. The JDK is looked up in this order: `$ODM_JAVA_HOME`, `/usr/libexec/java_home -v <n>` (macOS), SDKMAN, `$JAVA_HOME`. A JDK with the wrong version is refused, never used. Run `odm jdk` before the first build to see the ODM release and the JDK it resolved. If the required JDK is missing, report the error to the user (install that JDK, or set `ODM_JAVA_HOME`). Don't work around it with another JDK.

1. **Names.** camelCase rule project (`LoanApproval`), kebab XOM project (`loan-approval-xom`), kebab base name (`loan-approval`), locale `en_US` unless told otherwise.
2. **Scaffold:**
   ```bash
   java -jar <skill>/scripts/odm.jar init --dir <parent> --name LoanApproval --xom loan-approval-xom \
     --base loan-approval --package com.example.loan --var request:LoanRequest \
     --packages validation:Fastpath,scoring:RetePlus --fetch-jackson
   ```
   `--var name:Type[:IN|OUT|IN_OUT]` is repeatable (default `IN_OUT`, verbalized `the <name>`). Packages are ruleflow tasks in order; one mode each (`Fastpath` sequential, `RetePlus` inference).
3. **XOM** — write classes under `src/`; read `references/xom.md` first. Then `odm xom <xom-dir>` (compiles with `--release 17`, the lowest JDK of any 9.x release, so RES can be older than the compiler, and packages the jar; needs only a JDK — never use `mvn` for the XOM, Maven may not be installed).
4. **BOM + vocabulary** — append to the generated `bom/<base>.bom` and `bom/<base>_<locale>.voc`; read `references/bom-format.md` and `references/vocabulary-and-bal.md`.
5. **Rules** — one call per rule, BAL body on stdin:
   ```bash
   java -jar <skill>/scripts/odm.jar rule <RuleProjectDir> validation "check amount" <<'EOF'
   if
     the amount of 'the request' is more than 1000
   then
     add "Amount too high" to the messages of 'the request' ;
   EOF
   ```
   For 5+ rules with the same shape, use a decision table: `references/decision-tables.md`.
6. **Check, then build** — repeat until `BUILD SUCCESS`:
   ```bash
   java -jar <skill>/scripts/odm.jar check <RuleProjectDir>
   java -jar <skill>/scripts/odm.jar build <parent>/<Name>.properties
   ```
   `check` validates UUID uniqueness and links, file names, ruleflow packages, and lints BAL. `build` runs `tools/rules-compiler.jar` on the JDK its ODM release requires (override the jar with `--jar` or `ODM_RULES_COMPILER`) and prints only error/result lines (`--full` for the whole log). If the jar is missing but a `build_ruleset` MCP tool is connected, use that (`projectPath`, `xomJarPath`, `rulesetName`, optional `decisionOperation`). If neither is available, say so and don't claim the rules are validated.

## Rule dependencies → ruleflow

To find which rules depend on which, why a rule never fires, or what the ruleflow should be, read `references/ruleflow-design.md`, then:
```bash
java -jar <skill>/scripts/odm.jar deps <RuleProjectDir>        # who writes what others read, checked against the current ruleflow
java -jar <skill>/scripts/odm.jar ruleflow <RuleProjectDir> --packages a:Fastpath,b:RetePlus   # rewrite the .rfl (keeps name + UUID)
```
Also run `deps` after adding rules to a package another task reads from. An `ERROR … reads X before it is written` is a rule that sees only the input value.

## Vocabulary navigation phrases — label-first, never placeholder-first

Navigation phrases in the `.voc` **must** start with a label token, not a placeholder.

| ❌ Breaks at build | ✅ Correct |
|---|---|
| `{amount} of {this}` | `the amount of {this}` |
| `{riskScore} of {this}` | `the risk score of {this}` |
| `{cardholder} of {this}` | `the cardholder of {this}` |

A placeholder-first navigation phrase causes the rules compiler to fail with `The word '<condition>' is missing` on the **first rule in the first package (alphabetically)** that reads that property — even though the rule body itself is perfectly correct. The error looks like a global parse failure and gives no hint that the vocabulary is the source.

The rule: every scalar/object property navigation phrase must begin with a human-readable label so rules can write `the <label> of 'the variable'`. Boolean adjective phrases (`{this} is blocked`, `{this} has previous transactions in region`) are already label-free by design and are correct as-is.

## Rules that break builds (inline so you rarely need the references)

- **Declare every XOM property in the BOM.** The compiler does not read the XOM to infer properties: an undeclared property fails in rules (e.g. `The word 'length' is expected in place of 'amount'`).
- Boolean `isX()`/`setX()` → BOM `public boolean x;` (never `isX`). Computed getters with no setter → `public readonly <type> x property "factory.ignore" "true";` plus `@JsonIgnore` in Java.
- **Computed `isX()` with no setter: never name the BOM property `isX`.** The BOM name must be the setter name minus `set`. For a getter `isCrossBorder()` with no setter, the BOM name is `crossBorder` (from `setCrossBorder`). Naming it `isCrossBorder` in the BOM makes `odm check` warn `boolean 'isCrossBorder' — BOM name is the setter name without 'set' (drop 'is')`. Fix: either rename the XOM getter to `isIsCrossBorder()` so the BOM entry correctly becomes `isCrossBorder`, or use a non-`is` prefix (`getCrossBorder()`) so the BOM entry is simply `crossBorder`.
- **`hasX()` is not a getter.** B2X only binds `getX()`/`isX()`, so a plain `hasX()` fails with `GBREX0021E: Cannot find attribute 'hasX'`. Name it `isHasX()` → BOM `public boolean hasX;`.
- Keep the XOM at `--release 17` (the default). `odm xom` refuses a `--release` newer than the ODM release's JDK, because that fails with `bad major version`.
- BAL comparisons: `is more than`, `is less than`, `is at least`, `is at most`; equality is plain `is` / `is not`. **Never** `is equal to` (fails with `'January' expected`), nor symbols (`>`, `>=`, `==`).
- `is one of { "A", "B" }`, never `is in`. Negation: `it is not true that <condition>`. `is not null`, never `is not defined`.
- **Never `X is not (<expression>)`.** `is not` takes a literal or `null`. To compare two navigated values, bind one in `definitions` (`set 'v' to <expr> ;`) and write `it is not true that <X> is 'v'`.
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
| `Cannot find attribute 'hasX' in execution class` | rename the XOM getter `hasX()` → `isHasX()` (`hasX()` is not a JavaBean getter) |
| `The word '<condition>' is missing` (first rule of first alphabetical package; rule body looks correct) | Vocabulary navigation phrase is placeholder-first (`{x} of {this}`). Change to label-first: `the x of {this}`. |
| lint: `negate with 'it is not true that ...'` | replace `X is not (expr)` with a `definitions` variable: `set 'v' to expr ;` then `it is not true that X is 'v'` |
| `The word 'length' is expected in place of 'x'` | property `x` not declared in BOM, or numeric computed property used with `{x} of {this}` (use a method phrase) |
| `The word 'January' is expected in place of 'equal'` | replace `is equal to` with `is` |
| `The word 'X' is expected / missing / not required` | phrase doesn't match the vocabulary exactly, or reserved token in a phrase |
| `Ambiguous sentence` | make phrase openings distinct |
| `Invalid type 'X', not assignable from 'Y'` | type mismatch between phrase and argument |
| `Value 'X' is invalid` (decision table) | quote string params: `<![CDATA["X"]]>` |
| `bad major version` | recompile with `odm xom` |
| `A RuleApp name cannot be empty` / `Cannot load operation` | re-run `odm check`; it pinpoints the broken `.dep`/`.dop` link |

## References (read only what the current step needs)

- `references/xom.md`: Java class conventions, Jackson, computed properties
- `references/bom-format.md`: BOM syntax and annotations, a full example, collections
- `references/vocabulary-and-bal.md`: `.voc` syntax, the full BAL constraint list, and patterns from past builds
- `references/decision-tables.md` + `assets/templates/decision-table.dta`
- `references/ruleflow-design.md`: reading `odm deps`, choosing the task order and modes, applying it with `odm ruleflow`
- `schemas/*.ecore`, `schemas/b2x.xsd`: exact ODM metamodels, useful only when a generated file is rejected for an unclear reason. They are large (model.ecore is 67 KB), so `grep` for the class or attribute you need instead of reading them whole.

`tools/` and `schemas/` come from a licensed ODM install and are not committed. If they're missing, ask the user for their ODM Rule Designer path.
