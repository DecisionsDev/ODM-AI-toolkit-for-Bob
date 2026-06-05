# Business Action Language (BAL) Syntax Guide

## Basic BAL Syntax

1. Rules use single quotes for variable references: `'the loan'`, `'the borrower'`
2. Variable names defined in `[ProjectName]Parameters.var`
3. Conditions use natural language from vocabulary: `the credit score of 'the borrower' is less than 200`

## Multiple Action Statement Syntax (CRITICAL)

When the 'then' clause contains multiple action statements, use SEMICOLONS between ALL statements:
- Each action statement should end with a SEMICOLON
- CORRECT: `action1 ; action2 ; action3 ;`
- This applies to ALL rule types: validation, limits checking, offsetting, incentives, etc.

### Example with Single Action

```
if
  the property of 'the variable' is more than 100
then
  set the status of 'the variable' to "APPROVED" ;
```

### Example with Multiple Actions (SEMICOLON-SEPARATED)

```
if
  the property of 'the variable' is more than 100
  and 'the variable' is active
then
  set the status of 'the variable' to "APPROVED" ;
  add "Rule executed" to the messages of 'the variable' ;
  add "Status updated successfully" to the audit log of 'the variable' ;
```

## Null/Undefined Checking

- Use "is null" / "is not null" for object references (NOT "is not defined")
- "is not defined" is NOT valid BAL - always use "is null" / "is not null"
- For boolean properties: use vocabulary phrases exactly as defined

## BAL Parser Constraints (CRITICAL)

These patterns were validated against the IBM ODM Build Command CLI. Violating them causes compilation errors.

### 1. Comparison Operators - NEVER use Java/math symbols

- WRONG: `>=`, `<=`, `>`, `<`, `==`, `!=`
- CORRECT: `is at least`, `is at most`, `is more than`, `is less than`, `is equal to`, `is not equal to`
- Example: `the amount of 'the transaction' is at least 10000`

### 2. Set Membership - NEVER use "is in { }"

- WRONG: `the country code of 'the location' is in { "IR", "KP", "SY" }`
- CORRECT: `the country code of 'the location' is one of { "IR", "KP", "SY" }`

### 3. Boolean Negation - NEVER use "is not <adjective>" or "not (expr)"

- WRONG: `'the transaction' is not whitelisted`
- WRONG: `not ('the transaction' is whitelisted)`
- CORRECT: `it is not true that 'the transaction' is whitelisted`
- NOTE: "is not null" is the ONLY valid "is not X" form - all others require "it is not true that"

### 4. Numeric Computed Properties - NEVER use "{X} of {this}" navigation phrase

The pattern "{X} of {this}" conflicts with ODM's built-in string/collection length operator.

- WRONG vocabulary: `Transaction.elapsedMinutes#phrase.navigation = {elapsed minutes} of {this}`
- WRONG rule: `the elapsed minutes of 'the transaction' is more than 60`
- CORRECT: expose as a method call with inline navigation phrase
- CORRECT vocabulary: `Location.distanceTo(Location)#phrase.navigation = distance in km from {this} to {0}`
- CORRECT rule: `distance in km from (the previous location of 'the transaction') to (the merchant location of 'the transaction') is more than 900`

### 5. Method Call Phrase Syntax - "{this}" position determines "the" prefix

**When phrase starts with a label token (not {this}): use "the" prefix in rules**

Vocabulary:
```
DeviceInfo.ipToMerchantDistance(Location)#phrase.navigation = {IP distance in km} from device {this} to location {0}
```

Rule:
```
the IP distance in km from device (the device info of 'the transaction') to location (the merchant location of 'the transaction') is more than 500
```

**When phrase starts with {this} directly: NO "the" prefix in rules**

Vocabulary:
```
Location.distanceTo(Location)#phrase.navigation = distance in km from {this} to {0}
```

Rule:
```
distance in km from (the previous location of 'the transaction') to (the merchant location of 'the transaction') is more than 900
```

**Navigation expressions as method arguments MUST be wrapped in parentheses:** `(the X of 'the variable')`

### 6. Multi-Argument Method Calls - NEVER use built-in keyword tokens

ODM has reserved keywords: increase, by, decrease, add, remove, points, travel, distance, speed, elapsed

A two-argument method phrase using any reserved token will fail.

- WRONG vocabulary: `RiskScore.addScore(int, String)#phrase.action = add {0} points to {this} for rule {1}`
- WRONG vocabulary: `RiskScore.addScore(int, String)#phrase.action = increase {this} by {0} for rule {1}`

**SOLUTION:** Split into two single-argument methods in XOM, each with a safe phrase

CORRECT XOM:
```java
public void addScore(int points)
public void addTriggeredRule(String ruleName)
```

CORRECT vocabulary:
```
RiskScore.addScore(int)#phrase.action = record score {0} on {this}
RiskScore.addTriggeredRule(String)#phrase.action = record triggered rule {0} on {this}
```

CORRECT rule:
```
record score 40 on the risk score of 'the transaction' ;
record triggered rule "RULE_NAME" on the risk score of 'the transaction' ;
```

### 7. Vocabulary Phrase Token Conflicts - Reserved/Problematic Words

NEVER use these words as phrase label tokens (they conflict with ODM built-ins):
- elapsed, km, km/h, distance, travel, speed, minutes, velocity, span, geolocation
- increase, decrease, by, points, notifications, location (as standalone label)

These words cause parser errors like "The word 'X' is expected" or "The word '+' is missing"

Use safe alternatives: prior, gap, offset, delta, measure, count, tally, record, log

### 8. Boolean Computed Properties in BOM - MUST declare explicitly

Computed boolean properties (XOM getter with no setter) that need vocabulary phrases MUST be declared in BOM as:

```
public readonly boolean propertyName property "factory.ignore" "true";
```

Without this BOM declaration, ODM cannot resolve the property in rules.

### 9. Conditional Logic Constraints - NO else if / otherwise if Support

BAL parser does NOT support `else if` or `otherwise if` constructs. These cause compilation errors.

**WRONG - Parser fails:**
```
if
    condition1
then
    action1 ;
else if
    condition2
then
    action2 ;
```

**WRONG - Parser fails:**
```
if
    condition1
then
    action1 ;
otherwise if
    condition2
then
    action2 ;
otherwise
    action3 ;
```

**CORRECT - Use simple if-then-else:**
```
if
    condition
then
    action1 ;
else
    action2 ;
```

**BETTER - Split into separate rules:**
Create multiple rule files, each handling one condition. Let the ruleflow orchestrate execution order.

Example from Pet Shop Service:
- Rule 1: `check-pet-availability.brl` - Single if-then (no else)
- Rule 2: `calculate-adoption-fee.brl` - Single if-then-else
- Rule 3: `validate-customer-eligibility.brl` - Single if-then (no else)
- Ruleflow executes all three in sequence

**Key Principle:** Keep each rule focused on a single decision point. Complex multi-branch logic should be split across multiple rules.

### 10. Boolean Property Negation - MUST use "it is not true that" prefix

Direct negation of boolean properties fails. Always use the "it is not true that" pattern.

**WRONG:**
```
'customer' does not have experience
the pet of 'the request' is not available
```

**CORRECT:**
```
it is not true that 'customer' has experience
it is not true that the pet of 'the request' is available
```

**Real example from Pet Shop Service:**
```
if
    it is not true that the pet of 'the request' is available
then
    set the status of 'the request' to "REJECTED" ;
    add "Pet is not available for adoption" to the messages of 'the request' ;
```

**Exception:** The phrase "is not null" is valid and does NOT require the "it is not true that" prefix.

The "factory.ignore" annotation prevents ODM from trying to instantiate via this property.

Example:
```
XOM: public boolean isInKnownTransactionCorridor() { return ...; }  // no setter
BOM: public readonly boolean inKnownTransactionCorridor property "factory.ignore" "true";
Vocabulary: Transaction.inKnownTransactionCorridor#phrase.navigation = {this} is whitelisted
```

### 9. Boolean Navigation Phrase Labels - Use single safe adjectives

Boolean navigation phrases use pattern: `{this} is <adjective>`

The adjective MUST be a single word that is NOT an ODM keyword.

- SAFE adjectives: whitelisted, flagged, blocked, active, inactive, approved, rejected
- UNSAFE (cause parser conflicts): known, present, detected, valid, complete, open, new
- For negation in conditions: `it is not true that {this} is whitelisted`

### 10. Vocabulary Ambiguity - Avoid overlapping phrase prefixes

Two phrases starting with the same token sequence cause "Ambiguous sentence" errors.

Example conflict:
- "add {0} to the travel notifications of {this}"
- "add {0} to the risk factors of {this}"

Both start with "add {0} to the" - ODM may confuse them.

**Solution:** Use distinct opening tokens for each phrase
- SAFE: "add {0} to the travel notifications of {this}" vs "record risk factor {0} on {this}"

### 11. Null Checks on Navigation Chains

Always null-check intermediate objects before accessing their properties.

WRONG:
```
the country code of the merchant location of 'the transaction' is one of { "IR" }
```
(fails if merchantLocation is null)

CORRECT:
```
the merchant location of 'the transaction' is not null
and the country code of the merchant location of 'the transaction' is one of { "IR" }
```

### 12. BAL Operator Precedence - Use explicit parentheses

ODM BAL does not follow standard boolean operator precedence.

Always use explicit grouping when mixing AND/OR conditions.

Each condition on its own line with "and" / "or" prefix is safest.

### 13. XOM Method Overloading for BAL Compatibility

When a multi-argument method cannot be expressed cleanly in BAL, add overloaded single-argument versions:
- Keep the original multi-argument method for programmatic use
- Add single-argument variants specifically for BAL consumption
- Recompile XOM and update BOM + vocabulary after adding overloads
- This is the standard pattern for making complex Java APIs BAL-friendly

## Arithmetic Expressions in Rules

Arithmetic operations in BAL use standard operators: `+` `-` `*` `/`

Parentheses are required for complex expressions.

Pattern:
```
add ( expression1 * expression2 ) to the property of 'the variable' ;
```

Real example:
```
add ( the offset deficit of the airline operator of 'the request' * 100 ) to the penalty amount of the airline operator of 'the request' ;
```

String concatenation uses `+` operator:
```
"text " + the property of 'the variable' + " more text"
```

Real example:
```
"Section 3.1: Insufficient offset credits purchased - deficit of " + the offset deficit of the airline operator of 'the request' + " metric tons"
```

## Proven Patterns from Successful Builds

### Multi-Action Rule Pattern

When 'then' clause has multiple actions, EVERY action MUST end with semicolon:

```
if
  condition1
  and condition2
then
  action1 ;
  action2 ;
  action3 ;
```

Real example from Aviation Compliance:
```
if
  the airline operator of 'the request' is not null
  and the offset deficit of the airline operator of 'the request' is more than 0
then
  add ( the offset deficit of the airline operator of 'the request' * 100 ) to the penalty amount of the airline operator of 'the request' ;
  add ( the offset deficit of the airline operator of 'the request' * 100 ) to the total penalty amount of 'the request' ;
  add "Section 7.1: Penalty of $100 per metric ton for unoffset CO2" to the messages of the airline operator of 'the request' ;
```

### Null-Safe Navigation Pattern

ALWAYS check intermediate objects for null before accessing their properties:

```
if
  the parent of 'the request' is not null
  and the child of the parent of 'the request' is not null
  and the property of the child of the parent of 'the request' is more than 100
then
  action ;
```

Real example:
```
if
  the flight of 'the request' is not null
  and the aircraft of 'the request' is not null
  and the aircraft of 'the request' is certified after 2025
  and the CO2 per RPK of the flight of 'the request' is more than 85
then
  add "violation message" to the violations of the flight of 'the request' ;
```

### Boolean Property Patterns

**For boolean properties with standard getter/setter:**

XOM:
```java
private boolean meetsGaea2025Standard;
public boolean isMeetsGaea2025Standard() { return meetsGaea2025Standard; }
public void setMeetsGaea2025Standard(boolean value) { this.meetsGaea2025Standard = value; }
```

BOM:
```
public boolean meetsGaea2025Standard; // auto-mapped
```

Vocabulary navigation:
```
Aircraft.meetsGaea2025Standard#phrase.navigation = {this} meets GAEA 2025 standard
```

Vocabulary action:
```
Aircraft.meetsGaea2025Standard#phrase.action = make it {meets GAEA 2025 standard} that {this} meets GAEA 2025 standard
```

Rule condition:
```
the aircraft of 'the request' meets GAEA 2025 standard
```

Rule negation:
```
it is not true that the aircraft of 'the request' meets GAEA 2025 standard
```

**For computed boolean properties (no setter):**

XOM:
```java
public boolean isCertifiedAfter2025() { return ...; } // no setter
```

BOM:
```
public readonly boolean certifiedAfter2025 property "factory.ignore" "true";
```

Vocabulary:
```
Aircraft.certifiedAfter2025#phrase.navigation = {this} is certified after 2025
```

Rule:
```
the aircraft of 'the request' is certified after 2025