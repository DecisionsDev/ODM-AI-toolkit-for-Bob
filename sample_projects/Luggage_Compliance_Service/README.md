Generated with the prompt :

Create an ODM Rule project based on https://raw.githubusercontent.com/DecisionsDev/policy-corpus/refs/heads/main/luggage/luggage_policy.txt

Version v1.0.0 of the bob mode
# ODM Rule Project Documentation

**Project:** Luggage Compliance Service

**Generated:** 2026-03-06 09:53:57

---

## 1. Project Overview

- **Project Name:** Luggage Compliance Service
- **Project UUID:** d0f70290-40a0-488f-958f-d2edb9b439b7
- **Project Type:** Decision Service


## 2. Quality Assessment Summary

### Quality Metrics

- **Rule Documentation:** 0.0% (0/13) - Score: 0.0/20
- **Rule Complexity:** 100.0% simple rules (13/13) - Score: 20.0/20
- **Project Organization:** 5 packages - Score: 15/20
- **BOM Coverage:** 3 classes defined - Score: 15/20
- **Vocabulary Coverage:** Score: 0/20

### Overall Quality Score

**50.0%** (50.0/100) - Grade: **F**

**Quality Interpretation:**

⚠️ **Poor** - Project needs significant improvements in documentation and organization.

### Recommendations

- 📝 **Add Documentation**: Less than 50% of rules have documentation. Add meaningful descriptions to help users understand rule purpose and business logic.
- 📊 **Consider Decision Tables**: For rules with similar structure, consider using decision tables for better readability and maintenance.
- ✅ **Regular Reviews**: Conduct periodic code reviews to maintain quality standards.
- 🧪 **Add Test Cases**: Ensure comprehensive test coverage for all rules and decision tables.
- 📖 **Update Vocabulary**: Keep business vocabulary aligned with domain expert terminology.

---

## 3. Deployment Configuration

### Operation: `LuggageComplianceOperation`

- **Ruleset Name:** `Luggage_Compliance_Ruleset`
- **Using Ruleflow:** true
- **Ruleflow Name:** `luggage-compliance-ruleflow`

**Input/Output Parameters:**

| Variable | Type | Direction |
|----------|------|-----------|
| `request` | `LuggageComplianceParameters` | IN_OUT |


## 4. Ruleflow

### luggage-compliance-ruleflow

**Execution Flow:**

| Step | Task | Execution Mode | Rule Package |
|------|------|----------------|--------------|
| 1 | Carry-On Validation | `Fastpath` | `carry-on-validation` |
| 2 | Checked Baggage Validation | `Fastpath` | `checked-baggage-validation` |
| 3 | Excess Fees Calculation | `RetePlus` | `excess-fees` |
| 4 | Special Items Processing | `RetePlus` | `special-items` |
| 5 | Finalization | `Fastpath` | `finalization` |

**Execution Modes:**

- **Fastpath**: Sequential rule execution where order matters. Rules are evaluated in the order they appear.
- **RetePlus**: Rete algorithm-based execution with pattern matching. Order-independent evaluation.


## 5. Business Rules

### Package: `carry-on-validation`

#### Rule: `check-business-carry-on-count`

```
if
    the passenger of 'the request' is business class
    and the carry-on count of 'the request' is more than 2
then
    add violation "Business class passengers are allowed 2 carry-on bags plus 1 personal item" to 'the request' ;
```

**Conditions:**

- the passenger of 'the request' is business class
- the carry-on count of 'the request' is more than 2

**Actions:**

- add violation "Business class passengers are allowed 2 carry-on bags plus 1 personal item" to 'the request'

---

#### Rule: `check-business-first-carry-on-weight`

```
if
    the passenger of 'the request' is premium class
    and the total carry-on weight of 'the request' is more than 12
then
    add violation "Business/First class carry-on weight exceeds 12 kg limit" to 'the request' ;
```

**Conditions:**

- the passenger of 'the request' is premium class
- the total carry-on weight of 'the request' is more than 12

**Actions:**

- add violation "Business/First class carry-on weight exceeds 12 kg limit" to 'the request'

---

#### Rule: `check-carry-on-weight`

```
if
    the passenger of 'the request' is economy class
    and the total carry-on weight of 'the request' is more than 7
then
    add violation "Economy class carry-on weight exceeds 7 kg limit" to 'the request' ;
```

**Conditions:**

- the passenger of 'the request' is economy class
- the total carry-on weight of 'the request' is more than 7

**Actions:**

- add violation "Economy class carry-on weight exceeds 7 kg limit" to 'the request'

---

#### Rule: `check-economy-carry-on-count`

```
if
    the passenger of 'the request' is economy class
    and the carry-on count of 'the request' is more than 1
then
    add violation "Economy class passengers are allowed only 1 carry-on bag plus 1 personal item" to 'the request' ;
```

**Conditions:**

- the passenger of 'the request' is economy class
- the carry-on count of 'the request' is more than 1

**Actions:**

- add violation "Economy class passengers are allowed only 1 carry-on bag plus 1 personal item" to 'the request'

---

### Package: `checked-baggage-validation`

#### Rule: `check-business-checked-count`

```
if
    the passenger of 'the request' is business class
    and the checked baggage count of 'the request' is more than 2
then
    add violation "Business class passengers are allowed 2 checked bags" to 'the request' ;
```

**Conditions:**

- the passenger of 'the request' is business class
- the checked baggage count of 'the request' is more than 2

**Actions:**

- add violation "Business class passengers are allowed 2 checked bags" to 'the request'

---

#### Rule: `check-economy-checked-count`

```
if
    the passenger of 'the request' is economy class
    and the checked baggage count of 'the request' is more than 1
then
    add violation "Economy class passengers are allowed 1 checked bag" to 'the request' ;
```

**Conditions:**

- the passenger of 'the request' is economy class
- the checked baggage count of 'the request' is more than 1

**Actions:**

- add violation "Economy class passengers are allowed 1 checked bag" to 'the request'

---

#### Rule: `check-first-checked-count`

```
if
    the passenger of 'the request' is first class
    and the checked baggage count of 'the request' is more than 3
then
    add violation "First class passengers are allowed 3 checked bags" to 'the request' ;
```

**Conditions:**

- the passenger of 'the request' is first class
- the checked baggage count of 'the request' is more than 3

**Actions:**

- add violation "First class passengers are allowed 3 checked bags" to 'the request'

---

### Package: `excess-fees`

#### Rule: `calculate-oversized-fee`

```
definitions
    set 'bag' to a baggage item in the luggages of 'the request' ;
if
    'bag' is oversized
then
    add fee 100 to 'the request' ;
    add message "Oversized fee of $100 applied to bag " + the item ID of 'bag' to 'the request' ;
```

**Conditions:**

- definitions
    set 'bag' to a baggage item in the luggages of 'the request' ;

    'bag' is oversized

**Actions:**

- add fee 100 to 'the request'
- add message "Oversized fee of $100 applied to bag " + the item ID of 'bag' to 'the request'

---

#### Rule: `calculate-overweight-fee`

```
definitions
    set 'bag' to a baggage item in the luggages of 'the request' ;
if
    'bag' is overweight for economy
    and the weight of 'bag' is at most 32
then
    add fee 75 to 'the request' ;
    add message "Overweight fee of $75 applied to bag " + the item ID of 'bag' to 'the request' ;
```

**Conditions:**

- definitions
    set 'bag' to a baggage item in the luggages of 'the request' ;

    'bag' is overweight for economy
- the weight of 'bag' is at most 32

**Actions:**

- add fee 75 to 'the request'
- add message "Overweight fee of $75 applied to bag " + the item ID of 'bag' to 'the request'

---

#### Rule: `reject-maximum-limits`

```
definitions
    set 'bag' to a baggage item in the luggages of 'the request' ;
if
    'bag' exceeds maximum limits
then
    add violation "Bag " + the item ID of 'bag' + " exceeds maximum limits (32 kg or 203 cm) - must be shipped as cargo" to 'the request' ;
```

**Conditions:**

- definitions
    set 'bag' to a baggage item in the luggages of 'the request' ;

    'bag' exceeds maximum limits

**Actions:**

- add violation "Bag " + the item ID of 'bag' + " exceeds maximum limits (32 kg or 203 cm) - must be shipped as cargo" to 'the request'

---

### Package: `finalization`

#### Rule: `set-final-status`

```
if
    'the request' is compliant
then
    set the status of 'the request' to "APPROVED" ;
    add message "Luggage compliance check passed" to 'the request' ;
else
    set the status of 'the request' to "REJECTED" ;
    add message "Luggage compliance check failed - see violations" to 'the request' ;
```

**Conditions:**

- 'the request' is compliant

**Actions:**

- set the status of 'the request' to "APPROVED"
- add message "Luggage compliance check passed" to 'the request'
- else
    set the status of 'the request' to "REJECTED"
- add message "Luggage compliance check failed - see violations" to 'the request'

---

### Package: `special-items`

#### Rule: `check-sports-equipment-packing`

```
definitions
    set 'bag' to a baggage item in the luggages of 'the request' ;
if
    the special item type of 'bag' is SPORTS_EQUIPMENT
    and it is not true that 'bag' is properly packed
then
    add violation "Sports equipment must be properly packed in protective cases" to 'the request' ;
```

**Conditions:**

- definitions
    set 'bag' to a baggage item in the luggages of 'the request' ;

    the special item type of 'bag' is SPORTS_EQUIPMENT
- it is not true that 'bag' is properly packed

**Actions:**

- add violation "Sports equipment must be properly packed in protective cases" to 'the request'

---

#### Rule: `waive-mobility-aid-fees`

```
definitions
    set 'bag' to a baggage item in the luggages of 'the request' ;
if
    the special item type of 'bag' is MOBILITY_AID
    and the passenger of 'the request' has  a disability 
then
    add message "Mobility aid transported free of charge for passenger with disability" to 'the request' ;
```

**Conditions:**

- definitions
    set 'bag' to a baggage item in the luggages of 'the request' ;

    the special item type of 'bag' is MOBILITY_AID
- the passenger of 'the request' has  a disability

**Actions:**

- add message "Mobility aid transported free of charge for passenger with disability" to 'the request'

---


## 6. Decision Tables

*No decision tables found in this project.*


## 7. Rule Variables

### LuggageComplianceParameters

| Variable Name | Type | Verbalization |
|---------------|------|---------------|
| `request` | `com.skywings.luggage.LuggageRequest` | the request |


## 8. Business Object Model (BOM)

### luggage-compliance

**Package:** `com.skywings.luggage`

#### Class: `BaggageItem`

**Properties:**

- `public com.skywings.luggage.BaggageType baggageType`
- `public int heightCm`
- `public string itemId`
- `public boolean properlyPacked`
- `public com.skywings.luggage.SpecialItemType specialItemType`
- `public int lengthCm`
- `public double weightKg`
- `public int widthCm`

#### Class: `LuggageRequest`

**Properties:**

- `public boolean domesticFlight`
- `public com.skywings.luggage.Passenger passenger`
- `public string requestId`
- `public string status`
- `public double totalFees`

**Methods:**

- `void addBaggageItem(com.skywings.luggage.BaggageItem item)`
- `void addFee(double fee)`
- `void addMessage(string message)`
- `void addViolation(string violation)`

#### Class: `Passenger`

**Properties:**

- `public com.skywings.luggage.AgeCategory ageCategory`
- `public boolean disability`
- `public string name`
- `public string passengerId`
- `public com.skywings.luggage.TravelClass travelClass`


## 9. Business Vocabulary

### luggage-compliance_en_US

#### BaggageItem - *baggage item*

| Property | Type | Phrase |
|----------|------|--------|
| `baggageType` | Action | set the baggage type of {this} to {baggage type} |
| `baggageType` | Navigation | {baggage type} of {this} |
| `exceedsMaximumLimits` | Navigation | {this} exceeds maximum limits |
| `heightCm` | Action | set the height of {this} to {height cm} cm |
| `heightCm` | Navigation | {height} of {this} |
| `itemId` | Action | set the item ID of {this} to {item id} |
| `itemId` | Navigation | {item ID} of {this} |
| `lengthCm` | Action | set the length of {this} to {length cm} cm |
| `lengthCm` | Navigation | {length} of {this} |
| `oversized` | Navigation | {this} is oversized |
| `overweightEconomy` | Navigation | {this} is overweight for economy |
| `properlyPacked` | Action | make it {properly packed} that {this} is properly packed |
| `properlyPacked` | Navigation | {this} is properly packed |
| `specialItemType` | Action | set the special item type of {this} to {special item type} |
| `specialItemType` | Navigation | {special item type} of {this} |
| `totalDimensionsCm` | Navigation | {total dimensions} of {this} |
| `weightKg` | Action | set the weight of {this} to {weight kg} kg |
| `weightKg` | Navigation | {weight} of {this} |
| `widthCm` | Action | set the width of {this} to {width cm} cm |
| `widthCm` | Navigation | {width} of {this} |

#### LuggageRequest - *luggage request*

| Property | Type | Phrase |
|----------|------|--------|
| `baggageItems` | Navigation | {luggage} of {this} |
| `carryOnCount` | Navigation | {carry-on count} of {this} |
| `checkedBaggageCount` | Navigation | {checked baggage count} of {this} |
| `compliant` | Navigation | {this} is compliant |
| `domesticFlight` | Action | make it {domestic flight} that {this} is a domestic flight |
| `domesticFlight` | Navigation | {this} is a domestic flight |
| `messages` | Navigation | {messages} of {this} |
| `passenger` | Action | set the passenger of {this} to {passenger} |
| `passenger` | Navigation | {passenger} of {this} |
| `requestId` | Action | set the request ID of {this} to {request id} |
| `requestId` | Navigation | {request ID} of {this} |
| `status` | Action | set the status of {this} to {status} |
| `status` | Navigation | {status} of {this} |
| `totalCarryOnWeightKg` | Navigation | {total carry-on weight} of {this} |
| `totalFees` | Action | set the total fees of {this} to {total fees} |
| `totalFees` | Navigation | {total fees} of {this} |
| `violations` | Navigation | {violations} of {this} |

#### Passenger - *passenger*

| Property | Type | Phrase |
|----------|------|--------|
| `ageCategory` | Action | set the age category of {this} to {age category} |
| `ageCategory` | Navigation | {age category} of {this} |
| `businessClass` | Navigation | {this} is business class |
| `economyClass` | Navigation | {this} is economy class |
| `disability` | Action | make it {disability} that {this} has a disability |
| `disability` | Navigation | {this} has a disability |
| `firstClass` | Navigation | {this} is first class |
| `name` | Action | set the name of {this} to {name} |
| `name` | Navigation | {name} of {this} |
| `passengerId` | Action | set the passenger ID of {this} to {passenger id} |
| `passengerId` | Navigation | {passenger ID} of {this} |
| `premiumClass` | Navigation | {this} is premium class |
| `travelClass` | Action | set the travel class of {this} to {travel class} |
| `travelClass` | Navigation | {travel class} of {this} |


---

*Report generated by ODM Report Generator*
