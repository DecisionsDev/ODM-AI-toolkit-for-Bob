# Decision Tables (`.dta`)

Use a decision table for scoring, rating, classification, or range lookups once there are **5+ rules with the same condition/action shape**. For 2–3 unrelated rules, or anything needing nested logic, write `.brl` rules.

Start from `assets/templates/decision-table.dta`: copy it into `rules/<pkg>/<name>.dta`, set a fresh `<uuid>` (`java -jar <skill>/scripts/odm.jar uuid`), and adapt it. `odm check` includes `.dta` files in its UUID and string-param checks.

## Structure

- **ConditionDefinition `C0..`**: the BAL template with `<min>`/`<max>`/`<value>` placeholders. `intervalContext` enables gap/overlap checks on numeric ranges.
- **ActionDefinition `A0..`**: for example `set ... to <a number>` or `add <a number> to ...`.
- **Partition/Condition**: one row each. The first row has an open lower bound (`<a number> is less than <a number>`) and the last an open upper bound (`<a number> is at least <a number>`). Middle rows use the definition's min/max with two `<Param>`s.
- **Resources**: `HeaderText` and `Width` per column, per locale.

## Rules

1. String params must be BAL string literals: `<Param><![CDATA["BLOCK"]]></Param>`. Unquoted, they fail with `Value 'BLOCK' is invalid`. Numbers stay bare.
2. Keep `Check.Overlap.ErrorLevel` set to `Error`.
3. Cover both open ends so no input falls through, and test values exactly at each boundary.
4. Give columns meaningful `HeaderText`, since that's what business users see.
5. **At most 500 rows** (`odm check` warns above that), and ideally few enough that users don't scroll through several pages. Split a large table by its leading condition, for example one table per product or region, in the same package.
6. **No sparse tables.** If many cells are empty because a condition column only matters for some rows, split the table into several tables, each with only the condition columns its rows need.
7. Don't set a rule priority on a table. Order tables with the ruleflow. See `best-practices.md`.
