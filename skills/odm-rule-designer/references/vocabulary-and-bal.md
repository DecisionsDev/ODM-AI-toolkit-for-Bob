# Vocabulary and BAL

## Vocabulary (`bom/<base>_<locale>.voc`, properties format)

`init` writes the header (`# Vocabulary Properties`, `uuid = ...`). Append one block per class:

```
com.example.loan.LoanRequest#concept.label = loan request
com.example.loan.LoanRequest.amount#phrase.navigation = {amount} of {this}
com.example.loan.LoanRequest.amount#phrase.action = set the amount of {this} to {amount}
com.example.loan.LoanRequest.approved#phrase.navigation = {this} is approved
com.example.loan.LoanRequest.approved#phrase.action = make it {approved} that {this} is approved
com.example.loan.LoanRequest.highRisk#phrase.navigation = {this} is flagged
com.example.loan.LoanRequest.messages#phrase.navigation = {messages} of {this}
com.example.loan.LoanRequest.addMessage(java.lang.String)#phrase.action = add {0} to the messages of {this}
com.example.loan.Location.distanceTo(com.example.loan.Location)#phrase.navigation = gap from {this} to {0}
```

- `navigation` is used for reads and `action` for writes. Method parameters are `{0}`, `{1}`, and method keys use fully qualified Java parameter types.
- The phrase key must match a BOM member exactly (`approved`, not `isApproved`).
- Rules reference variables by verbalization in quotes: `'the request'`. `init` gives every variable the verbalization `the <name>`.

## BAL constraints (validated against rules-compiler.jar)

1. **Comparisons:** `is more than`, `is less than`, `is at least`, `is at most`. **Equality:** plain `is` / `is not`, which works for strings, numbers, and objects. `is equal to` does not mean equality: `equal` triggers a date grammar and fails with `'January' expected`. This holds even though older IBM docs show `is equal to`.
2. **Sets:** `is one of { "A", "B" }`, never `is in`.
3. **Negation:** `it is not true that <condition>`. `is not null` is the only valid `is not <X>` form. Never `is not defined` or `not (...)`.
4. **`then` statements** each end with ` ;`.
5. **Null-safe navigation:** before navigating through an object, check it in its own `and` line:
   ```
   the borrower of 'the request' is not null
   and the credit score of the borrower of 'the request' is less than 500
   ```
6. **Mixing and/or:** precedence isn't standard. Put each condition on its own line and add explicit parentheses.
7. **Numeric computed values:** never use `{x} of {this}` navigation (it clashes with the length operator). Use a method phrase such as `gap from {this} to {0}`. Wrap navigation arguments in parentheses: `gap from (the origin of 'the trip') to (the destination of 'the trip')`.
8. **Method phrase start:** if a phrase starts with a label token, rules write `the ...`. If it starts with `{this}`, rules don't write `the`.
9. **Multi-argument methods / reserved tokens:** in method phrases, don't use these as label words: `elapsed, km/h, distance, travel, speed, minutes, velocity, span, geolocation, increase, decrease, by, points, notifications, add, remove`. Safe substitutes: `prior, gap, offset, delta, measure, count, tally, record, log`. If a multi-argument method can't be phrased safely, add single-argument XOM overloads for BAL (then run `odm.py xom` and update the BOM and vocabulary).
10. **Boolean adjectives:** use one safe word (`flagged, blocked, active, inactive, approved, rejected, whitelisted`). Avoid `known, present, detected, valid, complete, open, new`.
11. **Distinct phrase openings:** two phrases starting with the same tokens give `Ambiguous sentence`. `add {0} to the violations of {this}` and `add {0} to the messages of {this}` can coexist only because the full phrases differ; when in doubt, use a different verb (`record message {0} on {this}`). Also avoid generic words that collide easily (`overall`, `total`, `count`, `compliance`).

## Patterns from past builds

```
if
  the flight of 'the request' is not null
  and the aircraft of 'the request' is not null
  and the aircraft of 'the request' is certified after 2025
  and the status of 'the request' is not "CLOSED"
  and the region of 'the request' is one of { "EU", "UK" }
  and it is not true that 'the request' has violations
then
  add ( the offset deficit of the airline operator of 'the request' * 100 ) to the penalty amount of the airline operator of 'the request' ;
  add "Deficit of " + the offset deficit of the airline operator of 'the request' + " t" to the violations of 'the request' ;
  make it true that 'the request' is flagged ;
```

Arithmetic needs parentheses, and strings are concatenated with `+`.

## Rule packages

A package is a directory `rules/<pkg>/` with a `.rulepackage`. `odm.py rule` creates both. The package name must match a `<Package Name="...">` in the `.rfl`. In Fastpath tasks, rule order affects execution.
