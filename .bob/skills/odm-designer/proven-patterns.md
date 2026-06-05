# Proven Patterns from Successful Builds

These patterns were validated through iterative Build Command testing and represent working solutions to common ODM compilation challenges.

## 1. Computed Property Pattern in BOM

Computed properties (XOM methods with no setter) MUST be declared in BOM with specific annotations.

### Pattern for Numeric Computed Properties

**XOM:**
```java
public double getCo2PerRpk() { 
    return co2EmissionsKg / (passengerCount * distanceKm); 
}
```

**BOM:**
```
public readonly double co2PerRpk property "factory.ignore" "true";
```

**Vocabulary:**
```
Flight.co2PerRpk#phrase.navigation = {CO2 per RPK} of {this}
```

### Pattern for Boolean Computed Properties

**XOM:**
```java
public boolean isCertifiedAfter2025() { 
    return certificationDate.after(new Date(2025, 0, 1)); 
}
```

**BOM:**
```
public readonly boolean certifiedAfter2025 property "factory.ignore" "true";
```

**Vocabulary:**
```
Aircraft.certifiedAfter2025#phrase.navigation = {this} is certified after 2025
```

**Key Points:**
- The "factory.ignore" annotation prevents ODM from trying to instantiate via this property
- The readonly modifier indicates the property cannot be modified in rules

## 2. Property Naming Consistency Across Layers

All layers must use the same base property name, avoiding reserved keywords.

**XOM:**
```java
private Operator airlineOperator;
public Operator getAirlineOperator() { return airlineOperator; }
public void setAirlineOperator(Operator airlineOperator) { this.airlineOperator = airlineOperator; }
```

**BOM:**
```
public com.aviation.compliance.Operator airlineOperator;
```

**Vocabulary navigation:**
```
ComplianceRequest.airlineOperator#phrase.navigation = {airline operator} of {this}
```

**Vocabulary action:**
```
ComplianceRequest.airlineOperator#phrase.action = set the airline operator of {this} to {airline operator}
```

**Rule usage:**
```
the airline operator of 'the request'
```

**CRITICAL:** All layers must use the same base property name (airlineOperator), not reserved keywords like "operator"

## 3. Collection Property Patterns

**BOM:**
```
public readonly java.util.Collection violations domain 0,* class string;
```

**Key Points:**
- Always use readonly for collections to prevent replacement, only allow add/remove operations
- Vocabulary for collection: `Flight.violations#phrase.navigation = {violations} of {this}`
- Vocabulary for add method: `Flight.addViolation(java.lang.String)#phrase.action = add {0} to the violations of {this}`
- Rule usage: `add "violation message" to the violations of the flight of 'the request' ;`
- NEVER try to check if collection is empty directly - use computed boolean property instead

## 4. Multi-Action Rule Pattern (CRITICAL)

When 'then' clause has multiple actions, EVERY action MUST end with semicolon.

**Pattern:**
```
if
  condition1
  and condition2
then
  action1 ;
  action2 ;
  action3 ;
```

**Real example from Aviation Compliance:**
```
if
  the airline operator of 'the request' is not null
  and the offset deficit of the airline operator of 'the request' is more than 0
then
  add ( the offset deficit of the airline operator of 'the request' * 100 ) to the penalty amount of the airline operator of 'the request' ;
  add ( the offset deficit of the airline operator of 'the request' * 100 ) to the total penalty amount of 'the request' ;
  add "Section 7.1: Penalty of $100 per metric ton for unoffset CO2" to the messages of the airline operator of 'the request' ;
```

## 5. Null-Safe Navigation Pattern

ALWAYS check intermediate objects for null before accessing their properties.

**Pattern:**
```
if
  the parent of 'the request' is not null
  and the child of the parent of 'the request' is not null
  and the property of the child of the parent of 'the request' is more than 100
then
  action ;
```

**Real example:**
```
if
  the flight of 'the request' is not null
  and the aircraft of 'the request' is not null
  and the aircraft of 'the request' is certified after 2025
  and the CO2 per RPK of the flight of 'the request' is more than 85
then
  add "violation message" to the violations of the flight of 'the request' ;
```

## 6. Vocabulary Phrase Disambiguation Strategy

When two phrases share common prefix tokens, ODM parser gets confused.

**WRONG:** Both phrases start with "add {0} to the"
- "add {0} to the violations of {this}"
- "add {0} to the messages of {this}"

**SOLUTION:** Use distinct opening tokens or different verb patterns
- CORRECT: "add {0} to the violations of {this}" vs "record message {0} on {this}"

**Aviation Compliance Success Pattern:**
Uses consistent "add {0} to the X of {this}" pattern successfully because:
- Each collection has a unique name (violations, messages, overallViolations, overallMessages)
- The full phrase including collection name is unique
- No two phrases share the exact same token sequence

## 7. Arithmetic Expression Pattern in Rules

Arithmetic operations in BAL use standard operators: `+` `-` `*` `/`

Parentheses are required for complex expressions.

**Pattern:**
```
add ( expression1 * expression2 ) to the property of 'the variable' ;
```

**Real example:**
```
add ( the offset deficit of the airline operator of 'the request' * 100 ) to the penalty amount of the airline operator of 'the request' ;
```

**String concatenation uses + operator:**
```
"text " + the property of 'the variable' + " more text"
```

**Real example:**
```
"Section 3.1: Insufficient offset credits purchased - deficit of " + the offset deficit of the airline operator of 'the request' + " metric tons"
```

## 8. Boolean Property Vocabulary Patterns

### For Boolean Properties with Standard Getter/Setter

**XOM:**
```java
private boolean meetsGaea2025Standard;
public boolean isMeetsGaea2025Standard() { return meetsGaea2025Standard; }
public void setMeetsGaea2025Standard(boolean value) { this.meetsGaea2025Standard = value; }
```

**BOM:**
```
public boolean meetsGaea2025Standard; // auto-mapped
```

**Vocabulary navigation:**
```
Aircraft.meetsGaea2025Standard#phrase.navigation = {this} meets GAEA 2025 standard
```

**Vocabulary action:**
```
Aircraft.meetsGaea2025Standard#phrase.action = make it {meets GAEA 2025 standard} that {this} meets GAEA 2025 standard
```

**Rule condition:**
```
the aircraft of 'the request' meets GAEA 2025 standard
```

**Rule negation:**
```
it is not true that the aircraft of 'the request' meets GAEA 2025 standard
```

### For Computed Boolean Properties (No Setter)

**XOM:**
```java
public boolean isCertifiedAfter2025() { return ...; } // no setter
```

**BOM:**
```
public readonly boolean certifiedAfter2025 property "factory.ignore" "true";
```

**Vocabulary:**
```
Aircraft.certifiedAfter2025#phrase.navigation = {this} is certified after 2025
```

**Rule:**
```
the aircraft of 'the request' is certified after 2025
```

## 9. Successful Vocabulary Naming Conventions

Use descriptive, domain-specific terms that avoid ODM reserved keywords.

**SAFE terms used in Aviation Compliance:**
- airline operator, flight, aircraft, compliance request, violations, messages, emissions, offset, penalty

**AVOIDED terms:**
- operator (reserved), overall (ambiguous), total (ambiguous), count (ambiguous)

**When a term causes conflicts, add qualifying prefix:**
- "airline operator" instead of "operator"

**Collection names should be plural and descriptive:**
- violations, messages, overallViolations, overallMessages

## 10. Working BOM Structure Pattern

Start with property declarations, then declare package, then classes with their members.

**Pattern:**
```
property loadGetterSetterAsProperties "true"
property origin "xom:/ProjectName/xom-project"
property uuid "unique-uuid"
package com.example.domain;

public class ClassName
{
    // Simple properties (auto-mapped from XOM getters/setters)
    public string propertyName;
    
    // Computed properties with factory.ignore annotation
    public readonly double computedProperty property "factory.ignore" "true";
    
    // Collections as readonly with domain specification
    public readonly java.util.Collection collectionName domain 0,* class string;
    
    // Constructors with forConversion annotation for primary constructor
    public ClassName(Type1, Type2) property "ilog.rules.engine.dataio.forConversion" "true";
    
    // Default constructor
    public ClassName();
    
    // Methods (add, remove, compute, etc.)
    public void methodName(Type arg);
}
```

**Real example structure from Aviation Compliance ComplianceRequest:**
```

## 11. BAL Conditional Logic Constraints (CRITICAL)

BAL parser has strict limitations on conditional structures that differ from standard programming languages.

### AVOID: else if / otherwise if constructs

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
else
    action3 ;
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

### SOLUTION: Use separate if-then statements

**CORRECT - Separate rules or conditions:**
```
if
    condition1
then
    action1 ;
    
if
    condition2
    and not condition1
then
    action2 ;
    
if
    not condition1
    and not condition2
then
    action3 ;
```

**BETTER - Split into multiple rules:**
Create separate rule files for each condition, letting the ruleflow orchestrate execution order.

### Boolean Property Negation Pattern

**WRONG:**
```
'customer' does not have experience
```

**CORRECT:**
```
it is not true that 'customer' has experience
```

**Real example from Pet Shop Service:**
```
if
    it is not true that the pet of 'the request' is available
then
    set the status of 'the request' to "REJECTED" ;
    add "Pet is not available for adoption" to the messages of 'the request' ;
```

### Key Takeaways

1. BAL does not support `else if` or `otherwise if` - use separate if statements or separate rules
2. Boolean negation requires `it is not true that` prefix, not direct negation
3. Complex conditional logic should be split across multiple rules
4. Let the ruleflow handle execution order rather than complex if-else chains
5. Each rule should focus on a single decision point

**Pet Shop Service Success Pattern:**
- Rule 1: Check pet availability (single condition)
- Rule 2: Calculate adoption fee (single condition with else)
- Rule 3: Validate customer age (single condition, no else needed)
- Ruleflow orchestrates the three rules in sequence

public com.aviation.compliance.Flight flight;
public com.aviation.compliance.Aircraft aircraft;
public com.aviation.compliance.Operator airlineOperator;
public readonly java.util.Collection overallViolations domain 0,* class string;
public readonly java.util.Collection overallMessages domain 0,* class string;
public string overallComplianceStatus;
public double totalPenaltyAmount;
public ComplianceRequest(Flight, Aircraft, Operator) property "ilog.rules.engine.dataio.forConversion" "true";
public ComplianceRequest();
public void addOverallViolation(string arg);
public void addOverallMessage(string arg);
public void addToPenalty(double arg);
```

## 11. Vocabulary Phrase Ambiguity Prevention

Avoid using common words that appear in multiple phrase contexts.

**Words like "overall", "compliance", "total", "count" can cause ambiguity.**

When a vocabulary phrase causes "Ambiguous sentence" errors, use more specific terms.

**Example:**
- Instead of "overall violations", use "recorded violations" or "detected violations"

Test vocabulary phrases by ensuring they don't share common prefix patterns.

## 12. Collection Property Access in Rules

When checking if a collection is empty, use vocabulary method phrases, not direct property access.

**WRONG:**
```
the violations of 'the request' is empty
```

**CORRECT:**
Create a computed boolean property in XOM:
```java
public boolean hasAnyViolations() { return !violations.isEmpty(); }
```

Then use vocabulary:
```
ComplianceRequest.anyViolations#phrase.navigation = {this} has violations
```

Rule:
```
it is not true that 'the request' has violations
```

## 13. Project Naming Convention

ALL project names MUST use camelCase format WITHOUT spaces.

**Rule project name:** Use camelCase (e.g., "SimpleLoanApproval", "FraudDetection", "CardiovascularRisk")

**XOM project name:** Use camelCase with "-xom" suffix (e.g., "simple-loan-xom", "fraud-detection-xom")

**NEVER use spaces in project names:**
- WRONG: "Simple Loan Approval"
- CORRECT: "SimpleLoanApproval"

This applies to ALL references: .project files, .ruleproject files, deployment operations, folder names.

**When user provides a name with spaces, automatically convert to camelCase.**

**Examples of correct naming:**
- "Loan Approval" → "LoanApproval" (rule project) + "loan-approval-xom" (XOM project)
- "Credit Card Fraud" → "CreditCardFraud" (rule project) + "credit-card-fraud-xom" (XOM project)
- "Patient Risk Assessment" → "PatientRiskAssessment" (rule project) + "patient-risk-xom" (XOM project)

## 14. Three-Layer Architecture (XOM-BOM-Rules)

1. **XOM layer:** Java POJOs - technical object model
2. **BOM layer:** Business vocabulary - bridges technical and business
3. **Rules layer:** Business logic - natural language rules

**Changes propagate upward only:** XOM → BOM → Rules (not bidirectional)

BOM files define business language and map Java XOM classes to business-friendly names.

**Vocabulary as API contract:** Changing vocabulary breaks existing rules (breaking change)

## 15. Computed Properties in XOM

Use lazy computation pattern with caching:

```java
if (yearlyRepayment == 0 && yearlyInterestRate != 0 && duration != 0) {
    // compute and cache
}
```

**Key Points:**
- Computed properties cache results - not recalculated on every access
- BOM exposes as readonly property but XOM has no setter - computed only
- Don't add setters for computed properties - breaks the lazy pattern
- Check for zero values before computing