# AI_Context.md — Aviation Pollution Compliance

Part of the ODM examples in this repository. For format-level explanations (what each file type is, the syntax rules, the error table) read the root [AI_Context.md](../../AI_Context.md); this file describes only this project.

## Purpose

- Rule project folder: `Aviation Pollution Compliance/` (name `Aviation Pollution Compliance`); XOM Java project: `aviation-compliance-xom/`.
- Source policy used to generate it: <https://raw.githubusercontent.com/DecisionsDev/policy-corpus/refs/heads/main/air_transport/airplane_pollution_compliance.txt>

## Build status

Builds successfully (`BUILD SUCCESS`) with IBM Semeru JDK 21 and `rules-compiler.jar`, XOM compiled from `src/`.

## Decision interface

- Variable `request` of type `com.aviation.compliance.ComplianceRequest`, verbalized `the request` (rules write `'the request'`), direction `IN_OUT`.
- Operation `AviationPollutionComplianceOperation`: ruleset `Aviation_Pollution_Compliance_Ruleset`, ruleflow `aviation-compliance-ruleflow`.
- Deployment config: `Aviation_Pollution_Compliance.dep`, `dep` name `Aviation_Pollution_Compliance`, RuleApp `AviationPollutionCompliance`.

## XOM and BOM

Package `com.aviation.compliance`. Business classes and their members (from `bom/aviation-pollution-compliance.bom`):

- **Aircraft**: aircraftId, aircraftModel, certificationDate, ratedThrustKn, certifiedNoxGramsPerKn, certifiedCo2PerRpk, meetsGaea2025Standard, requiresRetrofit, retrofitDeadline, phaseOutDate; computed (`readonly`, `factory.ignore`): certifiedAfter2025
- **ComplianceRequest**: flight, aircraft, airlineOperator, overallViolations, overallMessages, overallComplianceStatus, totalPenaltyAmount; methods: addOverallMessage, addOverallViolation, addToPenalty
- **Flight**: aircraftId, flightNumber, departureAirport, arrivalAirport, flightDate, flightTimeHours, fuelType, fuelVolumeKg, co2EmissionsKg, noxEmissionsGrams, particulateMatterMg, safBlendPercentage, safCertified, safLifecycleReduction, apuUsed, fegpAvailable, passengerCount, distanceKm, violations, messages, complianceStatus; computed (`readonly`, `factory.ignore`): co2PerRpk, pmPerKgFuel, compliant; methods: addMessage, addViolation
- **Operator**: operatorId, operatorName, annualCo2EmissionsMetricTons, baselineEmissions2019_2020, offsetCreditsRequired, offsetCreditsPurchased, safCreditsEarned, violations, messages, penaltyAmount, routeRightsSuspended; computed (`readonly`, `factory.ignore`): excessEmissions, netOffsetObligation, offsetDeficit; methods: addMessage, addPenalty, addViolation

XOM sources: `src/com/aviation/compliance/Aircraft.java`, `src/com/aviation/compliance/ComplianceRequest.java`, `src/com/aviation/compliance/Flight.java`, `src/com/aviation/compliance/Operator.java`.

## Ruleflow (execution order)

| # | Rule package | Mode |
|---|---|---|
| 1 | monitoring | Fastpath |
| 2 | emissions-limits | Fastpath |
| 3 | fleet-compliance | Fastpath |
| 4 | corsia | RetePlus |
| 5 | saf-incentives | Fastpath |
| 6 | ground-operations | Fastpath |
| 7 | penalties | Fastpath |

Fastpath = sequential execution, no inference. RetePlus = pattern-matching inference where one rule's effect may trigger others.

## Rules by package

### `rules/corsia/` — Rules for CORSIA carbon offsetting compliance
- `check-corsia-participation.brl` (rule): check-corsia-participation
- `check-offset-deficit.brl` (rule): check-offset-deficit

### `rules/emissions-limits/` — Rules for emissions limits compliance
- `check-co2-per-rpk.brl` (rule): check-co2-per-rpk
- `check-nox-emissions.brl` (rule): check-nox-emissions
- `check-particulate-matter.brl` (rule): check-particulate-matter

### `rules/fleet-compliance/` — Rules for fleet compliance and aircraft certification
- `check-aircraft-certification.brl` (rule): check-aircraft-certification

### `rules/ground-operations/` — Rules for ground operations compliance
- `check-apu-usage.brl` (rule): check-apu-usage

### `rules/monitoring/` — Rules for emissions monitoring validation
- `validate-flight-data.brl` (rule): validate-flight-data

### `rules/penalties/` — Rules for calculating penalties and determining compliance status
- `calculate-offset-penalty.brl` (rule): calculate-offset-penalty

### `rules/saf-incentives/` — Rules for Sustainable Aviation Fuel incentives
- `calculate-saf-credits.brl` (rule): calculate-saf-credits

## Representative rule

`rules/saf-incentives/calculate-saf-credits.brl` — the BAL inside `<definition><![CDATA[ … ]]>`:

```
if
    the flight of 'the request' is not null
    and the airline operator of 'the request' is not null
    and the SAF blend percentage of the flight of 'the request' is at least 10
    and the flight of 'the request' has certified SAF
    and the SAF lifecycle reduction percentage of the flight of 'the request' is at least 80
then
    set the SAF credits earned of the airline operator of 'the request' to ( the SAF credits earned of the airline operator of 'the request' + ( the fuel volume in kg of the flight of 'the request' * 2.5 ) ) ;
    add "Section 4.2: SAF credit earned - 2.5 kg CO2-eq per liter for lifecycle reduction >= 80%" to the messages of the flight of 'the request' ;
```

## Design notes

- Seven packages named after policy sections (`emissions-limits`, `corsia`, `penalties`, `saf-incentives`, …). Rule messages cite the policy section (e.g. `"Section 7.1: Penalty of $100 per metric ton …"`).
- The property `airlineOperator` replaces the natural but reserved name `operator`.
- `penalties/calculate-offset-penalty.brl` shows parenthesised arithmetic; every rule tests intermediate objects for `is not null` first.
- Only `corsia` runs in RetePlus; everything else is Fastpath.

## File inventory

```
Aviation Pollution Compliance/.ruleproject
Aviation Pollution Compliance/bom/aviation-pollution-compliance.b2xa
Aviation Pollution Compliance/bom/aviation-pollution-compliance.bom
Aviation Pollution Compliance/bom/aviation-pollution-compliance_en_US.voc
Aviation Pollution Compliance/deployment/AviationPollutionComplianceOperation.dop
Aviation Pollution Compliance/deployment/Aviation_Pollution_Compliance.dep
Aviation Pollution Compliance/rules/AviationPollutionComplianceParameters.var
Aviation Pollution Compliance/rules/aviation-compliance-ruleflow.rfl
Aviation Pollution Compliance/rules/corsia/.rulepackage
Aviation Pollution Compliance/rules/corsia/check-corsia-participation.brl
Aviation Pollution Compliance/rules/corsia/check-offset-deficit.brl
Aviation Pollution Compliance/rules/emissions-limits/.rulepackage
Aviation Pollution Compliance/rules/emissions-limits/check-co2-per-rpk.brl
Aviation Pollution Compliance/rules/emissions-limits/check-nox-emissions.brl
Aviation Pollution Compliance/rules/emissions-limits/check-particulate-matter.brl
Aviation Pollution Compliance/rules/fleet-compliance/.rulepackage
Aviation Pollution Compliance/rules/fleet-compliance/check-aircraft-certification.brl
Aviation Pollution Compliance/rules/ground-operations/.rulepackage
Aviation Pollution Compliance/rules/ground-operations/check-apu-usage.brl
Aviation Pollution Compliance/rules/monitoring/.rulepackage
Aviation Pollution Compliance/rules/monitoring/validate-flight-data.brl
Aviation Pollution Compliance/rules/penalties/.rulepackage
Aviation Pollution Compliance/rules/penalties/calculate-offset-penalty.brl
Aviation Pollution Compliance/rules/saf-incentives/.rulepackage
Aviation Pollution Compliance/rules/saf-incentives/calculate-saf-credits.brl
```
