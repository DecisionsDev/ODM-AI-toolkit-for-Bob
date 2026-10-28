# AI_Context.md — Luggage Compliance Service

Part of the ODM examples in this repository. For format-level explanations (what each file type is, the syntax rules, the error table) read the root [AI_Context.md](../../AI_Context.md); this file describes only this project.

## Purpose

- Rule project folder: `Luggage Compliance Service/` (name `Luggage Compliance Service`); XOM Java project: `luggage-compliance-xom/`.
- Source policy used to generate it: <https://raw.githubusercontent.com/DecisionsDev/policy-corpus/refs/heads/main/luggage/luggage_policy.txt>

## Build status

Builds successfully (`BUILD SUCCESS`) with IBM Semeru JDK 21 and `rules-compiler.jar`, XOM compiled from `src/`.

## Decision interface

- Variable `request` of type `com.skywings.luggage.LuggageRequest`, verbalized `the request` (rules write `'the request'`), direction `IN_OUT`.
- Operation `LuggageComplianceOperation`: ruleset `Luggage_Compliance_Ruleset`, ruleflow `luggage-compliance-ruleflow`.
- Deployment config: `Luggage_Compliance_Service.dep`, `dep` name `Luggage_Compliance_Service`, RuleApp `LuggageComplianceService`.

## XOM and BOM

Package `com.skywings.luggage`. Business classes and their members (from `bom/luggage-compliance.bom`):

- **BaggageItem**: baggageType, heightCm, itemId, properlyPacked, specialItemType, lengthCm, weightKg, widthCm; computed (`readonly`, `factory.ignore`): exceedsMaximumLimits, oversized, overweightEconomy, totalDimensionsCm
- **LuggageRequest**: baggageItems, domesticFlight, messages, passenger, requestId, status, totalFees, violations; computed (`readonly`, `factory.ignore`): carryOnCount, checkedBaggageCount, compliant, totalCarryOnWeightKg; methods: addBaggageItem, addFee, addMessage, addViolation
- **Passenger**: ageCategory, disability, name, passengerId, travelClass; computed (`readonly`, `factory.ignore`): businessClass, economyClass, firstClass, premiumClass

XOM sources: `src/com/skywings/luggage/AgeCategory.java`, `src/com/skywings/luggage/BaggageItem.java`, `src/com/skywings/luggage/BaggageType.java`, `src/com/skywings/luggage/LuggageRequest.java`, `src/com/skywings/luggage/Passenger.java`, `src/com/skywings/luggage/SpecialItemType.java`, `src/com/skywings/luggage/TravelClass.java`.

## Ruleflow (execution order)

| # | Rule package | Mode |
|---|---|---|
| 1 | carry-on-validation | Fastpath |
| 2 | checked-baggage-validation | Fastpath |
| 3 | excess-fees | RetePlus |
| 4 | special-items | RetePlus |
| 5 | finalization | Fastpath |

Fastpath = sequential execution, no inference. RetePlus = pattern-matching inference where one rule's effect may trigger others.

## Rules by package

### `rules/carry-on-validation/` — Rules for validating carry-on baggage allowances
- `check-business-carry-on-count.brl` (rule): check-business-carry-on-count
- `check-business-first-carry-on-weight.brl` (rule): check-business-first-carry-on-weight
- `check-carry-on-weight.brl` (rule): check-carry-on-weight
- `check-economy-carry-on-count.brl` (rule): check-economy-carry-on-count

### `rules/checked-baggage-validation/` — Rules for validating checked baggage allowances
- `check-business-checked-count.brl` (rule): check-business-checked-count
- `check-economy-checked-count.brl` (rule): check-economy-checked-count
- `check-first-checked-count.brl` (rule): check-first-checked-count

### `rules/excess-fees/` — Rules for calculating excess baggage fees
- `calculate-oversized-fee.brl` (rule): calculate-oversized-fee
- `calculate-overweight-fee.brl` (rule): calculate-overweight-fee
- `reject-maximum-limits.brl` (rule): reject-maximum-limits

### `rules/finalization/` — Rules for finalizing the luggage compliance request
- `set-final-status.brl` (rule): set-final-status

### `rules/special-items/` — Rules for handling special items (sports equipment, musical instruments, mobility aids)
- `check-sports-equipment-packing.brl` (rule): check-sports-equipment-packing
- `waive-mobility-aid-fees.brl` (rule): waive-mobility-aid-fees

## Representative rule

`rules/excess-fees/calculate-overweight-fee.brl` — the BAL inside `<definition><![CDATA[ … ]]>`:

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

## Design notes

- Passenger cabin class drives limits: per-class checks live in `carry-on-validation` and `checked-baggage-validation`.
- `excess-fees` and `special-items` run in RetePlus mode; fee rules iterate bags with a `definitions` block (`set 'bag' to a baggage item in the luggages of 'the request' ;`).
- `finalization/set-final-status.brl` runs last and is an `if … then … else` rule: if `'the request' is compliant` (a computed boolean over the violations) the status becomes `"APPROVED"`, otherwise `"REJECTED"`.
- Uses custom actions such as `add fee 75 to 'the request'` and `add violation "…" to 'the request'`, each backed by a XOM method plus a `#phrase.action` vocabulary entry.

## File inventory

```
Luggage Compliance Service/.ruleproject
Luggage Compliance Service/bom/luggage-compliance.b2xa
Luggage Compliance Service/bom/luggage-compliance.bom
Luggage Compliance Service/bom/luggage-compliance_en_US.voc
Luggage Compliance Service/deployment/LuggageComplianceOperation.dop
Luggage Compliance Service/deployment/Luggage_Compliance_Service.dep
Luggage Compliance Service/rules/LuggageComplianceParameters.var
Luggage Compliance Service/rules/luggage-compliance-ruleflow.rfl
Luggage Compliance Service/rules/carry-on-validation/.rulepackage
Luggage Compliance Service/rules/carry-on-validation/check-business-carry-on-count.brl
Luggage Compliance Service/rules/carry-on-validation/check-business-first-carry-on-weight.brl
Luggage Compliance Service/rules/carry-on-validation/check-carry-on-weight.brl
Luggage Compliance Service/rules/carry-on-validation/check-economy-carry-on-count.brl
Luggage Compliance Service/rules/checked-baggage-validation/.rulepackage
Luggage Compliance Service/rules/checked-baggage-validation/check-business-checked-count.brl
Luggage Compliance Service/rules/checked-baggage-validation/check-economy-checked-count.brl
Luggage Compliance Service/rules/checked-baggage-validation/check-first-checked-count.brl
Luggage Compliance Service/rules/excess-fees/.rulepackage
Luggage Compliance Service/rules/excess-fees/calculate-oversized-fee.brl
Luggage Compliance Service/rules/excess-fees/calculate-overweight-fee.brl
Luggage Compliance Service/rules/excess-fees/reject-maximum-limits.brl
Luggage Compliance Service/rules/finalization/.rulepackage
Luggage Compliance Service/rules/finalization/set-final-status.brl
Luggage Compliance Service/rules/special-items/.rulepackage
Luggage Compliance Service/rules/special-items/check-sports-equipment-packing.brl
Luggage Compliance Service/rules/special-items/waive-mobility-aid-fees.brl
```
