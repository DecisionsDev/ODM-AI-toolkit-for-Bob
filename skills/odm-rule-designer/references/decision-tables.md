# Decision Tables (`.dta`)

## When to use one

Use a decision table in either of these cases:

- **Value lookup, whatever the number of rows.** The requirement gives an output value for each value or band of one or more inputs ("20% for Tier 1, 15% for Tier 2, 10% for Tier 3", a rate per region, a fee per product). Write **one table, even with 2–4 rows**, and not one rule per value. Signs: rules that share the same condition phrase and the same action phrase and differ only in literals, or `odm deps` reporting `X is set by N rules`.
- **Scoring, rating, classification or range lookups** with **5+ rules of the same condition/action shape**.

Write `.brl` rules instead for unrelated rules, for rules whose conditions use different phrases (the table would be sparse), and for anything that needs nested logic.

## How a table runs

A decision table groups rules that share the same conditions and actions. Each row is one action rule: the condition cells of the row form its `if` part, and the action cells form its `then` part. The engine evaluates the rows in order, from the first to the last, and every row whose conditions are met runs its actions. A table can also have a precondition, which applies to every row.

So rows that overlap all fire, and the last one to write a value wins. That's why the table must have no overlaps (rule 3 below) and no gaps (rule 4): the grid then shows business users exactly which row applies to each input.

## Starting point

Copy a template into `rules/<pkg>/<name>.dta`:

- `assets/templates/lookup-decision-table.dta`: **exact values** (`… is <a number>`, or `… is <a string>`).
- `assets/templates/decision-table.dta`: **numeric ranges** (`… is at least <min> and less than <max>`).

Then set `<name>` to the file name without `.dta`, set a fresh `<uuid>` (`java -jar <skill>/scripts/odm.jar uuid`), and adapt it. There is no `odm` command that generates a table. The `.rulepackage` already exists if `odm init` created the package; nothing else needs registering. `odm check` and `odm deps` include `.dta` files.

## Structure

- **ConditionDefinition `C0..`**: the BAL condition with placeholders. For an exact value: `the customer tier of 'the request' is <a number>`. For a range: `… is at least <min> and less than <max>`. `intervalContext` enables gap/overlap checks on numeric ranges.
- **ActionDefinition `A0..`**: for example `set ... to <a number>` or `add <a number> to ...`.
- **Partition/Condition**: one row each.
  - Exact value: one `<Param>` per row, with no `<Text>`. The row reuses the definition's text.
  - Range: the first row has an open lower bound (`<a number> is less than <a number>`) and the last an open upper bound (`<a number> is at least <a number>`). Middle rows use the definition's min and max, with two `<Param>`s.
- **Resources**: `HeaderText` and `Width` per column, per locale.

## Rules

1. **Placeholders:** use `<a number>` for **every** numeric type (`int`, `double`, …) and `<a string>` for strings. `<an integer>` fails with `Unknown word : <an integer>`.
2. String params must be BAL string literals: `<Param><![CDATA["BLOCK"]]></Param>`. Unquoted, they fail with `Value 'BLOCK' is invalid`. Numbers stay bare.
3. Keep `Check.Overlap.ErrorLevel` set to `Error`.
4. Cover every expected input value, and for ranges cover both open ends, so no input falls through. Test values exactly at each boundary. An input that matches no row (for example an unknown tier) should be handled by a validation rule in an earlier package.
5. Give columns meaningful `HeaderText` with the unit (`Representative discount limit (%)`), since that's what business users see.
6. **At most 500 rows** (`odm check` warns above that), and ideally few enough that users don't scroll through several pages. Split a large table by its leading condition, for example one table per product or region, in the same package.
7. **No sparse tables.** If many cells are empty because a condition column only matters for some rows, split the table into several tables, each with only the condition columns its rows need.
8. Don't set a rule priority on a table. Order tables with the ruleflow. See `best-practices.md`.
