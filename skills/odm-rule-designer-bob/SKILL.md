---
name: odm-rule-designer-bob
description: Create complete, production-ready IBM Operational Decision Manager (ODM) Decision Services for any business domain — XOM Java classes, BOM, vocabulary, BAL rules, ruleflows, decision tables, and deployment operations — with a bundled Build Command compiler and EMF/XSD schemas for local validation. Use this skill whenever the user mentions IBM ODM, Operational Decision Manager, Decision Services, business rules, BOM/XOM/BAL/ruleflow/ruleset, .ruleproject, rules-compiler, or wants to turn business requirements into executable rules — even if they don't name ODM explicitly. Also use when fixing ODM project errors like "Classic rule projects are not supported", "Cannot find attribute in execution class", "A RuleApp name cannot be empty", UUID errors, or ruleset build failures.
---

# IBM ODM Rule Designer (Bob-derived)

You are an expert in IBM Operational Decision Manager (ODM). This skill generates complete Decision Service projects that follow IBM best practices and avoid the common configuration errors that break ODM builds. It was distilled from the "IBM Bob" custom mode's field-tested instructions (`.bob/custom_modes.yaml` in this repo) and ships its own local build tooling and schemas — see "Bundled resources" below.

A Decision Service is built from three layers that propagate **upward only** (XOM → BOM → Rules):

1. **XOM** (eXecution Object Model) — Java POJOs, the technical object model
2. **BOM** (Business Object Model) — business vocabulary that bridges Java and business language
3. **Rules** — business logic written in natural-language BAL (Business Action Language)

The vocabulary acts as an API contract: changing it breaks existing rules. Changes to XOM require BOM regeneration — the BOM does **not** auto-sync.

## Workflow

Follow these steps in order. Each references a detailed guide in `references/` — read the relevant file before producing that artifact rather than working from memory, because the formats are exact and unforgiving.

1. **Gather the domain.** Ask the user for the business domain and its key concepts (entities, properties, decisions). If locale isn't specified, default to `en_US`.
2. **Choose camelCase project names, no spaces**, immediately — e.g. "Loan Approval" → rule project `LoanApproval`, XOM project `loan-approval-xom`. This name is used everywhere downstream; getting it wrong means renaming across every file later. See `references/rule-project-config.md`.
3. **Create the XOM project** — Java classes with `.project` and `.classpath`, Jackson (not `Serializable`) for JSON, `@JsonIgnore`/`@JsonInclude` annotations. See `references/xom.md`. Compile immediately with `javac`.
4. **Create the rule project** — `.ruleproject` and `.project` with the correct Decision Service flags and natures. See `references/rule-project-config.md`.
5. **Create the BOM** in text BRL format (never XML). See `references/bom-format.md` — pay special attention to the boolean getter/setter naming rules (the #1 source of "Cannot find attribute" errors) and the reserved-keyword list (`operator`, `function`, `rule`, `package`, `import`).
6. **Create the vocabulary** (`.voc`) in properties format with the `_LOCALE` suffix. See `references/vocabulary-and-bal.md`.
7. **Create the `.b2xa`** file for ARL association (same base name as `.bom`/`.voc`). See `references/bom-format.md`.
8. **Create the variables** (`.var`) file. See `references/vocabulary-and-bal.md`.
9. **Create rule packages and BAL rules** (`.brl`) — or a **decision table** (`.dta`) for scoring/rating rules with 5+ similar rows. See `references/vocabulary-and-bal.md` for the BAL parser constraints and `references/decision-tables.md` for when to use a table instead of individual rules.
10. **Create the ruleflow** (`.rfl`) with appropriate execution modes. See `references/ruleflow-deployment.md`.
11. **Create the deployment operation and `.dep`** with complete configuration, matching UUID, a non-empty `ruleAppName`, and the mandatory `ruleset.version` property (e.g. `1.0`) on every operation in the `.dep`. See `references/ruleflow-deployment.md`.
12. **Validate the build** — see the Validation section below. **This is mandatory.** Repeat fix-and-rebuild until the build succeeds.
13. **Verify UUID uniqueness** — see `references/validation.md`.

## File naming (critical)

The `.bom`, `.b2xa`, and `.voc` files MUST share the same descriptive base name. ONLY the vocabulary file carries the locale suffix:

- `blood-transfusion.bom`, `blood-transfusion.b2xa`, `blood-transfusion_en_US.voc` ✓
- `transfusion.bom`, `com.hospital.transfusion.b2xa`, `blood-transfusion_en_US.voc` ✗ (mismatched bases)
- `blood-transfusion_en_US.bom` ✗ (locale belongs only on `.voc`)

## Bundled resources

This skill carries its own local tooling, unlike a typical skill that only has text references:

- **`tools/rules-compiler.jar`** — IBM's Build Command CLI, ready to invoke directly (no download/Docker step). See "Validation" below.
- **`schemas/`** — the actual EMF `.ecore`/`.genmodel` metamodels and the `b2x.xsd` schema, extracted from a licensed ODM Rule Designer installation, covering `.brl`, `.ruleproject`/`.rulepackage`/`.var`/BOM/XOM paths, `.rfl`, `.dop`'s `QueryExtractor`, and `.b2xa`. Consult these when a generated file is rejected and the cause isn't obvious from the templates or reference docs — they are the ground truth for exact element/attribute structure.

**Neither is committed to this repo's git history** (see the repo's `.gitignore`) — both come from a licensed IBM product and stay local-only. If you're setting this skill up fresh and these are missing, regenerate them from a local ODM Rule Designer install (ask the user for its path) rather than assuming they exist.

## Validation — building the ruleset

Generating files is not enough; the rules must actually compile into a RuleApp. Use the bundled compiler first:

```
java -jar tools/rules-compiler.jar -config <ProjectName>.properties
```

Exit code `0` = BUILD SUCCESS (RuleApp JAR produced). Exit code `2` = BUILD FAILURE (with a specific rule + error message).

- **Path A (default): the bundled `tools/rules-compiler.jar`** — always available in this skill, no setup needed. Follow the step-by-step procedure in `references/validation.md`.
- **Path B: the `build_ruleset` MCP tool**, if connected — equally valid, useful when you'd rather not shell out to the local JVM. See `references/validation.md` for the tool contract.
- **Path C: neither available** — say so explicitly. Compile the XOM Java with `javac` to catch Java-level errors, then tell the user the rules themselves could not be compiled. Do **not** claim the project is validated.

When the build fails, read the failing rule, consult the BAL parser constraints in `references/vocabulary-and-bal.md`, fix one error at a time (later errors are often cascading from the first), and rebuild. After any XOM change, recompile Java and rebuild the XOM JAR before re-running the build.

## Reference files

- `references/rule-project-config.md` — camelCase project naming convention, `.ruleproject`/`.project` flags, natures, XOM linking, three-layer architecture
- `references/xom.md` — XOM Java project structure, Jackson JSON serialization (`@JsonIgnore`/`@JsonInclude`), computed-property pattern
- `references/bom-format.md` — BOM BRL format, boolean naming rules, reserved keywords, annotations, `.b2xa`
- `references/vocabulary-and-bal.md` — vocabulary format, variables, the full BAL parser constraint list, proven patterns from production builds
- `references/ruleflow-deployment.md` — ruleflow modes, package organization, deployment operation + `.dep` `ruleAppName`, UUID linking
- `references/decision-tables.md` — when to use a `.dta` decision table instead of individual rules
- `references/validation.md` — the build procedure (bundled jar + MCP path), UUID generation and uniqueness verification
- `assets/templates/odm-file-templates.md` — ready-to-adapt file templates for every artifact type (`.brl`, `.var`, `.dop`, `.dep`, `.bom`, `.voc`, `.rfl`, `.rulepackage`, `.ruleproject`, `.project`, `.classpath`, `.b2xa`, `.dta`)
- `schemas/` — raw EMF/XSD ground truth (see "Bundled resources" above)

Adapt all templates to the specific business domain while keeping the structure and format exact.
