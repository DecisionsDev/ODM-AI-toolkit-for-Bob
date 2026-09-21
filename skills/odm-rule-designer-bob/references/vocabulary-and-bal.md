# Vocabulary, Variables, and BAL

## Vocabulary (`.voc`) — properties format, NOT XML

- Naming: `{base-name}_{LOCALE}.voc` (e.g. `loan-validation_en_US.voc`). Locale is REQUIRED; default `en_US`.
- Start with:
  ```
  # Vocabulary Properties
  uuid = [unique-uuid]
  ```
- Property syntax:
  ```
  package.ClassName#concept.label = natural language name
  package.ClassName.property#phrase.navigation = {property} of {this}
  package.ClassName.property#phrase.action = set the {property} of {this} to {property}
  package.ClassName.method(Type)#phrase.action = action with {0} placeholder
  ```
- Boolean phrases:
  ```
  package.ClassName.booleanProp#phrase.navigation = {this} is [concept]
  package.ClassName.booleanProp#phrase.action = make it {boolean prop} that {this} is [concept]
  ```
- Method actions use `{0}`, `{1}` for parameters.
- `#phrase.navigation` → read expressions; `#phrase.action` → write expressions. These must match XOM getter/setter patterns. Vocabulary changes require a rule-file refresh (rules cache phrase mappings).

## Variables (`.var`)

- `name` MUST have no spaces: `name="request"`, `name="transaction"`.
- `verbalization` carries natural language with `the` prefix: `verbalization="the request"`.
- Rules reference the verbalization in single quotes: `'the request'`.
- Deployment operations reference the `name` (no spaces).

```xml
<variables name="transaction" type="fraud.Transaction" initialValue="" verbalization="the transaction"/>
```

## BAL syntax basics

- Variable references in single quotes: `'the loan'`, `'the borrower'`.
- Conditions use vocabulary phrases: `the credit score of 'the borrower' is less than 200`.
- **Multiple actions in `then`: separate ALL statements with SEMICOLONS, each ending in `;`** (applies to every rule type):
  ```
  if
    the property of 'the variable' is more than 100
    and 'the variable' is active
  then
    set the status of 'the variable' to "APPROVED" ;
    add "Rule executed" to the messages of 'the variable' ;
  ```
- Null checks: use `is null` / `is not null` for object references. `is not defined` is NOT valid BAL.

## BAL parser constraints (validated against rules-compiler.jar)

Violating any of these causes compilation errors. Apply all when writing rules.

1. **Comparison operators — never Java/math symbols, and never "is equal to"/"is not equal to".** Use `is at least`, `is at most`, `is more than`, `is less than`. Not `>= <= > < == !=`. **For equality/inequality (string or numeric), use plain `is` / `is not`** — e.g. `the decision of 'the request' is "PENDING"`, `the risk score of 'the request' is 0`, `the issuing country of 'the request' is not the initiation country of 'the request'`. Confirmed against rules-compiler.jar: `is equal to` does NOT mean generic equality — the word `equal` triggers a hidden built-in date/month grammar production and fails with `The word 'January' is expected in place of 'equal'`. This contradicts some older IBM ODM documentation/examples that show `is equal to`; trust the empirical behavior over those.
2. **Set membership — use `is one of { ... }`**, not `is in { ... }`.
3. **Boolean negation — use `it is not true that <expr>`.** Not `is not <adjective>` or `not (expr)`. (`is not null` is the only valid `is not X` form.)
4. **Numeric computed properties — never use `{X} of {this}` navigation** (conflicts with ODM's length operator). Expose as a method call with an inline navigation phrase instead, e.g. `distance in km from {this} to {0}`, called as `distance in km from (the previous location of 'the transaction') to (the merchant location of 'the transaction')`.
5. **Method-call phrase `{this}` position controls the `the` prefix.** If the phrase starts with a label token, use `the` in rules; if it starts with `{this}`, no `the`. Navigation expressions used as method arguments MUST be wrapped in parentheses.
6. **Multi-argument method calls — never use reserved keyword tokens** (`increase, by, decrease, add, remove, points, travel, distance, speed, elapsed`). Split into two single-argument XOM methods with safe phrases instead — see "XOM method overloading" below.
7. **Reserved/problematic phrase tokens** — never use as labels: `elapsed, km, km/h, distance, travel, speed, minutes, velocity, span, geolocation, increase, decrease, by, points, notifications, location`. Safe alternatives: `prior, gap, offset, delta, measure, count, tally, record, log`.
8. **Computed boolean properties MUST be declared in BOM** as `public readonly boolean propertyName property "factory.ignore" "true";` (see `bom-format.md`).
9. **Boolean navigation adjectives — single safe word.** Safe: `whitelisted, flagged, blocked, active, inactive, approved, rejected`. Unsafe: `known, present, detected, valid, complete, open, new`.
10. **Avoid overlapping phrase prefixes** — two phrases starting with the same token sequence cause "Ambiguous sentence" errors. Use distinct opening tokens (see disambiguation strategy below).
11. **Null-check every intermediate object in a navigation chain before using it**, in a separate `and` clause, before the property access:
    ```
    the merchant location of 'the transaction' is not null
    and the country code of the merchant location of 'the transaction' is one of { "IR" }
    ```
    Skipping this throws a null-navigation error at runtime if the intermediate object is absent.
12. **Operator precedence is not standard boolean precedence** — always use explicit parentheses when mixing AND/OR, or put each condition on its own line with an explicit `and`/`or` prefix (safest).
13. **XOM method overloading for BAL compatibility** — when a multi-argument method can't be expressed cleanly in BAL (rule 6), add single-argument overloads specifically for BAL consumption, keeping the original multi-argument method for programmatic use. Recompile XOM and update BOM + vocabulary after adding overloads.

## Proven patterns (validated in production builds)

**Null-safe navigation, full example:**
```
if
  the flight of 'the request' is not null
  and the aircraft of 'the request' is not null
  and the aircraft of 'the request' is certified after 2025
  and the CO2 per RPK of the flight of 'the request' is more than 85
then
  add "violation message" to the violations of the flight of 'the request' ;
```

**Vocabulary phrase disambiguation** — when two phrases share an opening token sequence, ODM's parser confuses them:
- WRONG: `add {0} to the violations of {this}` vs `add {0} to the messages of {this}` — both start `add {0} to the`.
- FIX: give each collection a genuinely distinct phrase, e.g. `add {0} to the violations of {this}` vs `record message {0} on {this}`. A shared verb pattern (`add {0} to the X of {this}`) is fine as long as `X` makes the *full* phrase unique.

**Arithmetic expressions:**
```
add ( the offset deficit of the airline operator of 'the request' * 100 ) to the penalty amount of the airline operator of 'the request' ;
```
Parentheses are required for complex expressions. String concatenation uses `+`:
```
"Section 3.1: Insufficient offset credits purchased - deficit of " + the offset deficit of the airline operator of 'the request' + " metric tons"
```

**Boolean vocabulary, both forms:**
```
Standard (getter+setter):     Aircraft.meetsGaea2025Standard#phrase.navigation = {this} meets GAEA 2025 standard
Computed (getter only, no setter, factory.ignore in BOM): Aircraft.certifiedAfter2025#phrase.navigation = {this} is certified after 2025
Negation (either form):       it is not true that the aircraft of 'the request' meets GAEA 2025 standard
```

**Vocabulary word choice** — avoid ambiguous common words as phrase tokens: `overall`, `compliance`, `total`, `count` tend to collide across phrases. Prefer specific terms (`recorded violations` over `overall violations`).

## Rule package organization

- Rules live in `rules/[package-name]/` and map to ruleflow tasks.
- Package names in the ruleflow (`<Package Name="validation"/>`) must match directory names exactly.
- `.rulepackage` files are metadata — don't edit manually.
- In Fastpath mode, package organization affects execution order.
