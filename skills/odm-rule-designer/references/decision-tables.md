# Decision Tables (`.dta`)

Decision tables are ideal for scoring, rating, and classification rules where multiple conditions map to specific actions or values — a tabular view that's easier to maintain than many individual rules with the same shape. Rows execute top to bottom; the engine spots overlaps and gaps automatically.

## When to use one

- Scoring applications (credit scoring, risk assessment, eligibility determination)
- Rating systems (insurance premiums, pricing tiers, service levels)
- Classification rules (categorization based on multiple criteria)
- Lookup tables with range-based conditions
- Any scenario with 5+ rules sharing the same condition/action structure

For the full XML structure and a worked credit-scoring example, see `assets/templates/odm-file-templates.md`.

## Key components

1. **ConditionDefinitions** — one per condition column (`C0`, `C1`, ...). `ExpressionDefinition` holds the BAL condition template with `<min>`, `<max>`, `<value>` placeholders. `intervalContext` can enable automatic gap/overlap checking for numeric ranges.
2. **ActionDefinitions** — one per action column (`A0`, `A1`, ...). Common patterns: set a variable, add to a score, assign a category.
3. **Contents/Partition** — the actual rows. Each `Condition` is one row with real parameter values.
   - First row: open lower bound, e.g. `<a number> is less than <a number>`.
   - Last row: open upper bound, e.g. `<a number> is at least <a number>`.
   - Middle rows: explicit min/max from the `ConditionDefinition`.
4. **Resources** — column headers (`HeaderText`), `Width`, and per-locale `ResourceSet`.

## Best practices

1. **String action values must be quoted as BAL string literals inside the `<Param>` CDATA**, e.g. `<Param><![CDATA["BLOCK"]]></Param>`, not `<Param><![CDATA[BLOCK]]></Param>`. The unquoted form fails at build time with `Value 'BLOCK' is invalid` — confirmed against rules-compiler.jar. Numeric params (as in the condition columns) stay bare, e.g. `<Param><![CDATA[31]]></Param>`.
2. Use decision tables once you have 5+ rules with identical structure — don't force a table onto 2-3 unrelated rules.
3. Enable overlap checking (`Check.Overlap.ErrorLevel`) to catch conflicting row conditions early.
4. Give columns meaningful `HeaderText` — this is what a business user sees in Rule Designer/Decision Center.
5. Always cover the edges with open-bound first/last rows so no input value falls through uncovered.
6. Keep condition expressions simple — anything requiring nested logic belongs in a regular `.brl` rule, not a table cell.
7. Test boundary values explicitly (exactly at a `min`/`max`) to confirm range coverage is correct.
