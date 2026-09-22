# Coding Rules (Non-Obvious Only)

- **XOM JSON Annotations**: Always annotate XOM classes with `@JsonInclude(JsonInclude.Include.NON_NULL)` and mark all computed/helper getters (no matching setter) with `@JsonIgnore` to avoid `UnrecognizedPropertyException`.
- **Method Overloading for BAL**: Split multi-argument Java methods into overloaded single-argument methods (e.g. `addScore(int)` and `addTriggeredRule(String)`) before exposing in BOM/vocabulary to prevent BAL parser conflicts with reserved keywords (`points`, `by`, `for`).
- **Parentheses for BAL Arguments**: Method arguments that are navigation expressions must be wrapped in parentheses: `distance in km from (the previous location of 'the transaction') to (...)`.
- **Null Safety in BAL**: Always check intermediate objects before chained properties: `the merchant location of 'the transaction' is not null and the country code of the merchant location of 'the transaction' is one of { "IR" }`.
- **Variable Verbalization**: In `.var` files, `name` attribute must have no spaces (e.g. `name="specimen"`), while `verbalization` has "the" prefix (`verbalization="the specimen"`). Rules reference `'the specimen'`.
- **Decision Operation Parameters**: Set `direction="IN_OUT"` in `.dop` files for objects modified by rules; default without direction is read-only `IN`.
