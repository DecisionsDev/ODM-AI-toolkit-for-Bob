Generated with the prompt :

generate an ODM Rule project based on https://raw.githubusercontent.com/DecisionsDev/policy-corpus/refs/heads/main/air_transport/airplane_pollution_compliance.txt

Version v1.0.0 of the bob mode
# ODM Rule Project Documentation

**Project:** Aviation Pollution Compliance

**Generated:** 2026-03-06 09:53:04

---

## 1. Project Overview

- **Project Name:** Aviation Pollution Compliance
- **Project UUID:** C00282B1-32EA-4B36-8514-683A18799D62
- **Project Type:** Decision Service


## 2. Quality Assessment Summary

### Quality Metrics

- **Rule Documentation:** 0.0% (0/10) - Score: 0.0/20
- **Rule Complexity:** 60.0% simple rules (6/10) - Score: 12.0/20
- **Project Organization:** 7 packages - Score: 15/20
- **BOM Coverage:** 4 classes defined - Score: 20/20
- **Vocabulary Coverage:** Score: 0/20

### Overall Quality Score

**47.0%** (47.0/100) - Grade: **F**

**Quality Interpretation:**

⚠️ **Poor** - Project needs significant improvements in documentation and organization.

### Issues Found (4)

#### 🟡 Medium Priority Issues (4)

- **check-corsia-participation**: Complex rule with 8 conditions and 2 actions (Type: complexity)
- **check-offset-deficit**: Complex rule with 6 conditions and 2 actions (Type: complexity)
- **calculate-saf-credits**: Complex rule with 8 conditions and 2 actions (Type: complexity)
- **calculate-offset-penalty**: Complex rule with 9 conditions and 3 actions (Type: complexity)

### Recommendations

- 📝 **Add Documentation**: Less than 50% of rules have documentation. Add meaningful descriptions to help users understand rule purpose and business logic.
- 🔧 **Simplify Complex Rules**: 4 rule(s) have high complexity. Consider breaking them into smaller, more maintainable rules.
- ✅ **Regular Reviews**: Conduct periodic code reviews to maintain quality standards.
- 🧪 **Add Test Cases**: Ensure comprehensive test coverage for all rules and decision tables.
- 📖 **Update Vocabulary**: Keep business vocabulary aligned with domain expert terminology.

---

## 3. Deployment Configuration

### Operation: `AviationPollutionComplianceOperation`

- **Ruleset Name:** `Aviation_Pollution_Compliance_Ruleset`
- **Using Ruleflow:** true
- **Ruleflow Name:** `aviation-compliance-ruleflow`

**Input/Output Parameters:**

| Variable | Type | Direction |
|----------|------|-----------|
| `request` | `AviationPollutionComplianceParameters` | IN_OUT |


## 4. Ruleflow

### aviation-compliance-ruleflow

**Execution Flow:**

| Step | Task | Execution Mode | Rule Package |
|------|------|----------------|--------------|
| 1 | Monitoring Validation | `Fastpath` | `monitoring` |
| 2 | Emissions Limits | `Fastpath` | `emissions-limits` |
| 3 | Fleet Compliance | `Fastpath` | `fleet-compliance` |
| 4 | CORSIA Offsetting | `RetePlus` | `corsia` |
| 5 | SAF Incentives | `Fastpath` | `saf-incentives` |
| 6 | Ground Operations | `Fastpath` | `ground-operations` |
| 7 | Penalties and Status | `Fastpath` | `penalties` |

**Execution Modes:**

- **Fastpath**: Sequential rule execution where order matters. Rules are evaluated in the order they appear.
- **RetePlus**: Rete algorithm-based execution with pattern matching. Order-independent evaluation.


## 5. Business Rules

### Package: `corsia`

#### Rule: `check-corsia-participation`

```
if
    the airline operator of 'the request' is not null
    and the annual CO2 emissions in metric tons of the airline operator of 'the request' is more than 10000
then
    set the offset credits required of the airline operator of 'the request' to the excess emissions of the airline operator of 'the request' ;
    add "Section 3.1: Operator must participate in GAEA Offsetting Mechanism" to the messages of the airline operator of 'the request' ;
```

**Conditions:**

- the airline operator of 'the request' is not null
- the annual CO2 emissions in metric tons of the airline operator of 'the request' is more than 10000

**Actions:**

- set the offset credits required of the airline operator of 'the request' to the excess emissions of the airline operator of 'the request'
- add "Section 3.1: Operator must participate in GAEA Offsetting Mechanism" to the messages of the airline operator of 'the request'

---

#### Rule: `check-offset-deficit`

```
if
    the airline operator of 'the request' is not null
    and the offset deficit of the airline operator of 'the request' is more than 0
then
    add "Section 3.1: Insufficient offset credits purchased - deficit of " + the offset deficit of the airline operator of 'the request' + " metric tons" to the violations of the airline operator of 'the request' ;
    add "CORSIA_VIOLATION" to the overall violations of 'the request' ;
```

**Conditions:**

- the airline operator of 'the request' is not null
- the offset deficit of the airline operator of 'the request' is more than 0

**Actions:**

- add "Section 3.1: Insufficient offset credits purchased - deficit of " + the offset deficit of the airline operator of 'the request' + " metric tons" to the violations of the airline operator of 'the request'
- add "CORSIA_VIOLATION" to the overall violations of 'the request'

---

### Package: `emissions-limits`

#### Rule: `check-co2-per-rpk`

```
if
    the flight of 'the request' is not null
    and the aircraft of 'the request' is not null
    and the aircraft of 'the request' is certified after 2025
    and the CO2 per RPK of the flight of 'the request' is more than 85
then
    add "Section 2.1: CO2 per RPK exceeds 85 grams limit for aircraft certified after 2025" to the violations of the flight of 'the request' ;
    add "EMISSIONS_LIMIT_VIOLATION" to the overall violations of 'the request' ;
```

**Conditions:**

- the flight of 'the request' is not null
- the aircraft of 'the request' is not null
- the aircraft of 'the request' is certied after 2025
- the CO2 per RPK of the flight of 'the request' is more than 85

**Actions:**

- add "Section 2.1: CO2 per RPK exceeds 85 grams limit for aircraft certified after 2025" to the violations of the flight of 'the request'
- add "EMISSIONS_LIMIT_VIOLATION" to the overall violations of 'the request'

---

#### Rule: `check-nox-emissions`

```
if
    the flight of 'the request' is not null
    and the aircraft of 'the request' is not null
    and the rated thrust in kN of the aircraft of 'the request' is more than 0
    and the NOx emissions in grams of the flight of 'the request' is more than ( the rated thrust in kN of the aircraft of 'the request' * 15 )
then
    add "Section 2.2: NOx emissions exceed 15 grams per kN limit during LTO cycle" to the violations of the flight of 'the request' ;
    add "EMISSIONS_LIMIT_VIOLATION" to the overall violations of 'the request' ;
```

**Conditions:**

- the flight of 'the request' is not null
- the aircraft of 'the request' is not null
- the rated thrust in kN of the aircraft of 'the request' is more than 0
- the NOx emissions in grams of the flight of 'the request' is more than ( the rated thrust in kN of the aircraft of 'the request' * 15 )

**Actions:**

- add "Section 2.2: NOx emissions exceed 15 grams per kN limit during LTO cycle" to the violations of the flight of 'the request'
- add "EMISSIONS_LIMIT_VIOLATION" to the overall violations of 'the request'

---

#### Rule: `check-particulate-matter`

```
if
    the flight of 'the request' is not null
    and the PM per kg fuel of the flight of 'the request' is more than 20
then
    add "Section 2.3: Particulate matter exceeds 20 mg/kg fuel limit during LTO operations" to the violations of the flight of 'the request' ;
    add "EMISSIONS_LIMIT_VIOLATION" to the overall violations of 'the request' ;
```

**Conditions:**

- the flight of 'the request' is not null
- the PM per kg fuel of the flight of 'the request' is more than 20

**Actions:**

- add "Section 2.3: Particulate matter exceeds 20 mg/kg fuel limit during LTO operations" to the violations of the flight of 'the request'
- add "EMISSIONS_LIMIT_VIOLATION" to the overall violations of 'the request'

---

### Package: `fleet-compliance`

#### Rule: `check-aircraft-certification`

```
if
    the aircraft of 'the request' is not null
    and the aircraft of 'the request' is certified after 2025
    and it is not true that the aircraft of 'the request' meets GAEA 2025 standard
then
    add "Section 6.1: Aircraft certified after 2025 does not meet GAEA 2025 CO2 standard" to the violations of the airline operator of 'the request' ;
    add "FLEET_COMPLIANCE_VIOLATION" to the overall violations of 'the request' ;
```

**Conditions:**

- the aircraft of 'the request' is not null
- the aircraft of 'the request' is certied after 2025
- it is not true that the aircraft of 'the request' meets GAEA 2025 st
- ard

**Actions:**

- add "Section 6.1: Aircraft certified after 2025 does not meet GAEA 2025 CO2 standard" to the violations of the airline operator of 'the request'
- add "FLEET_COMPLIANCE_VIOLATION" to the overall violations of 'the request'

---

### Package: `ground-operations`

#### Rule: `check-apu-usage`

```
if
    the flight of 'the request' is not null
    and the flight of 'the request' used APU
    and the flight of 'the request' had FEGP available
then
    add "Section 5.1: APU used when FEGP was available - violation unless operational emergency" to the violations of the flight of 'the request' ;
    add "GROUND_OPERATIONS_VIOLATION" to the overall violations of 'the request' ;
```

**Conditions:**

- the flight of 'the request' is not null
- the flight of 'the request' used APU
- the flight of 'the request' had FEGP available

**Actions:**

- add "Section 5.1: APU used when FEGP was available - violation unless operational emergency" to the violations of the flight of 'the request'
- add "GROUND_OPERATIONS_VIOLATION" to the overall violations of 'the request'

---

### Package: `monitoring`

#### Rule: `validate-flight-data`

```
if
    the flight of 'the request' is not null
    and the aircraft ID of the flight of 'the request' is null
then
    add "Section 1.2: Missing aircraft ID in flight data" to the violations of the flight of 'the request' ;
    add "MONITORING_VIOLATION" to the overall violations of 'the request' ;
```

**Conditions:**

- the flight of 'the request' is not null
- the aircraft ID of the flight of 'the request' is null

**Actions:**

- add "Section 1.2: Missing aircraft ID in flight data" to the violations of the flight of 'the request'
- add "MONITORING_VIOLATION" to the overall violations of 'the request'

---

### Package: `penalties`

#### Rule: `calculate-offset-penalty`

```
if
    the airline operator of 'the request' is not null
    and the offset deficit of the airline operator of 'the request' is more than 0
then
    add ( the offset deficit of the airline operator of 'the request' * 100 ) to the penalty amount of the airline operator of 'the request' ;
    add ( the offset deficit of the airline operator of 'the request' * 100 ) to the total penalty amount of 'the request' ;
    add "Section 7.1: Penalty of $100 per metric ton for unoffset CO2" to the messages of the airline operator of 'the request' ;
```

**Conditions:**

- the airline operator of 'the request' is not null
- the offset deficit of the airline operator of 'the request' is more than 0

**Actions:**

- add ( the offset deficit of the airline operator of 'the request' * 100 ) to the penalty amount of the airline operator of 'the request'
- add ( the offset deficit of the airline operator of 'the request' * 100 ) to the total penalty amount of 'the request'
- add "Section 7.1: Penalty of $100 per metric ton for unoffset CO2" to the messages of the airline operator of 'the request'

---

### Package: `saf-incentives`

#### Rule: `calculate-saf-credits`

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

**Conditions:**

- the flight of 'the request' is not null
- the airline operator of 'the request' is not null
- the SAF blend percentage of the flight of 'the request' is at least 10
- the flight of 'the request' has certied SAF
- the SAF lecycle reduction percentage of the flight of 'the request' is at least 80

**Actions:**

- set the SAF credits earned of the airline operator of 'the request' to ( the SAF credits earned of the airline operator of 'the request' + ( the fuel volume in kg of the flight of 'the request' * 2.5 ) )
- add "Section 4.2: SAF credit earned - 2.5 kg CO2-eq per liter for lifecycle reduction >= 80%" to the messages of the flight of 'the request'

---


## 6. Decision Tables

*No decision tables found in this project.*


## 7. Rule Variables

### AviationPollutionComplianceParameters

| Variable Name | Type | Verbalization |
|---------------|------|---------------|
| `request` | `com.aviation.compliance.ComplianceRequest` | the request |


## 8. Business Object Model (BOM)

### aviation-pollution-compliance

**Package:** `com.aviation.compliance`

#### Class: `Aircraft`

**Properties:**

- `public string aircraftId`
- `public string aircraftModel`
- `public java.util.Date certificationDate`
- `public double ratedThrustKn`
- `public double certifiedNoxGramsPerKn`
- `public double certifiedCo2PerRpk`
- `public boolean meetsGaea2025Standard`
- `public boolean requiresRetrofit`
- `public java.util.Date retrofitDeadline`
- `public java.util.Date phaseOutDate`

#### Class: `ComplianceRequest`

**Properties:**

- `public com.aviation.compliance.Flight flight`
- `public com.aviation.compliance.Aircraft aircraft`
- `public com.aviation.compliance.Operator airlineOperator`
- `public string overallComplianceStatus`
- `public double totalPenaltyAmount`

**Methods:**

- `void addOverallViolation(string arg)`
- `void addOverallMessage(string arg)`
- `void addToPenalty(double arg)`

#### Class: `Flight`

**Properties:**

- `public string aircraftId`
- `public string flightNumber`
- `public string departureAirport`
- `public string arrivalAirport`
- `public java.util.Date flightDate`
- `public double flightTimeHours`
- `public string fuelType`
- `public double fuelVolumeKg`
- `public double co2EmissionsKg`
- `public double noxEmissionsGrams`
- `public double particulateMatterMg`
- `public double safBlendPercentage`
- `public boolean safCertified`
- `public double safLifecycleReduction`
- `public boolean apuUsed`
- `public boolean fegpAvailable`
- `public int passengerCount`
- `public double distanceKm`
- `public string complianceStatus`

**Methods:**

- `void addViolation(string arg)`
- `void addMessage(string arg)`

#### Class: `Operator`

**Properties:**

- `public string operatorId`
- `public string operatorName`
- `public double annualCo2EmissionsMetricTons`
- `public double baselineEmissions2019_2020`
- `public double offsetCreditsRequired`
- `public double offsetCreditsPurchased`
- `public double safCreditsEarned`
- `public double penaltyAmount`
- `public boolean routeRightsSuspended`

**Methods:**

- `void addViolation(string arg)`
- `void addMessage(string arg)`
- `void addPenalty(double arg)`


## 9. Business Vocabulary

### aviation-pollution-compliance_en_US

#### Aircraft - *aircraft*

| Property | Type | Phrase |
|----------|------|--------|
| `aircraftId` | Navigation | {aircraft ID} of {this} |
| `aircraftId` | Action | set the aircraft ID of {this} to {aircraft ID} |
| `aircraftModel` | Navigation | {aircraft model} of {this} |
| `aircraftModel` | Action | set the aircraft model of {this} to {aircraft model} |
| `certificationDate` | Navigation | {certification date} of {this} |
| `certificationDate` | Action | set the certification date of {this} to {certification date} |
| `ratedThrustKn` | Navigation | {rated thrust in kN} of {this} |
| `ratedThrustKn` | Action | set the rated thrust in kN of {this} to {rated thrust in kN} |
| `certifiedNoxGramsPerKn` | Navigation | {certified NOx grams per kN} of {this} |
| `certifiedNoxGramsPerKn` | Action | set the certified NOx grams per kN of {this} to {certified NOx grams per kN} |
| `certifiedCo2PerRpk` | Navigation | {certified CO2 per RPK} of {this} |
| `certifiedCo2PerRpk` | Action | set the certified CO2 per RPK of {this} to {certified CO2 per RPK} |
| `meetsGaea2025Standard` | Navigation | {this} meets GAEA 2025 standard |
| `meetsGaea2025Standard` | Action | make it {meets GAEA 2025 standard} that {this} meets GAEA 2025 standard |
| `requiresRetrofit` | Navigation | {this} requires retrofit |
| `requiresRetrofit` | Action | make it {requires retrofit} that {this} requires retrofit |
| `retrofitDeadline` | Navigation | {retrofit deadline} of {this} |
| `retrofitDeadline` | Action | set the retrofit deadline of {this} to {retrofit deadline} |
| `phaseOutDate` | Navigation | {phase out date} of {this} |
| `phaseOutDate` | Action | set the phase out date of {this} to {phase out date} |
| `certifiedAfter2025` | Navigation | {this} is certified after 2025 |

#### ComplianceRequest - *compliance request*

| Property | Type | Phrase |
|----------|------|--------|
| `flight` | Navigation | {flight} of {this} |
| `flight` | Action | set the flight of {this} to {flight} |
| `aircraft` | Navigation | {aircraft} of {this} |
| `aircraft` | Action | set the aircraft of {this} to {aircraft} |
| `airlineOperator` | Navigation | {airline operator} of {this} |
| `airlineOperator` | Action | set the airline operator of {this} to {airline operator} |
| `overallViolations` | Navigation | {recorded violations} of {this} |
| `overallMessages` | Navigation | {recorded messages} of {this} |
| `overallComplianceStatus` | Navigation | {overall compliance status} of {this} |
| `overallComplianceStatus` | Action | set the overall compliance status of {this} to {overall compliance status} |
| `totalPenaltyAmount` | Navigation | {total penalty amount} of {this} |
| `totalPenaltyAmount` | Action | set the total penalty amount of {this} to {total penalty amount} |
| `anyViolations` | Navigation | {this} has any violations |
| `totalViolationCount` | Navigation | {number of violations} of {this} |

#### Flight - *flight*

| Property | Type | Phrase |
|----------|------|--------|
| `aircraftId` | Navigation | {aircraft ID} of {this} |
| `aircraftId` | Action | set the aircraft ID of {this} to {aircraft ID} |
| `flightNumber` | Navigation | {flight number} of {this} |
| `flightNumber` | Action | set the flight number of {this} to {flight number} |
| `departureAirport` | Navigation | {departure airport} of {this} |
| `departureAirport` | Action | set the departure airport of {this} to {departure airport} |
| `arrivalAirport` | Navigation | {arrival airport} of {this} |
| `arrivalAirport` | Action | set the arrival airport of {this} to {arrival airport} |
| `flightDate` | Navigation | {flight date} of {this} |
| `flightDate` | Action | set the flight date of {this} to {flight date} |
| `flightTimeHours` | Navigation | {flight time in hours} of {this} |
| `flightTimeHours` | Action | set the flight time in hours of {this} to {flight time in hours} |
| `fuelType` | Navigation | {fuel type} of {this} |
| `fuelType` | Action | set the fuel type of {this} to {fuel type} |
| `fuelVolumeKg` | Navigation | {fuel volume in kg} of {this} |
| `fuelVolumeKg` | Action | set the fuel volume in kg of {this} to {fuel volume in kg} |
| `co2EmissionsKg` | Navigation | {CO2 emissions in kg} of {this} |
| `co2EmissionsKg` | Action | set the CO2 emissions in kg of {this} to {CO2 emissions in kg} |
| `noxEmissionsGrams` | Navigation | {NOx emissions in grams} of {this} |
| `noxEmissionsGrams` | Action | set the NOx emissions in grams of {this} to {NOx emissions in grams} |
| `particulateMatterMg` | Navigation | {particulate matter in mg} of {this} |
| `particulateMatterMg` | Action | set the particulate matter in mg of {this} to {particulate matter in mg} |
| `safBlendPercentage` | Navigation | {SAF blend percentage} of {this} |
| `safBlendPercentage` | Action | set the SAF blend percentage of {this} to {SAF blend percentage} |
| `safCertified` | Navigation | {this} has certified SAF |
| `safCertified` | Action | make it {SAF certified} that {this} has certified SAF |
| `safLifecycleReduction` | Navigation | {SAF lifecycle reduction percentage} of {this} |
| `safLifecycleReduction` | Action | set the SAF lifecycle reduction percentage of {this} to {SAF lifecycle reduction percentage} |
| `apuUsed` | Navigation | {this} used APU |
| `apuUsed` | Action | make it {APU used} that {this} used APU |
| `fegpAvailable` | Navigation | {this} had FEGP available |
| `fegpAvailable` | Action | make it {FEGP available} that {this} had FEGP available |
| `passengerCount` | Navigation | {passenger count} of {this} |
| `passengerCount` | Action | set the passenger count of {this} to {passenger count} |
| `distanceKm` | Navigation | {flight distance in km} of {this} |
| `distanceKm` | Action | set the flight distance in km of {this} to {flight distance in km} |
| `violations` | Navigation | {violations} of {this} |
| `messages` | Navigation | {messages} of {this} |
| `complianceStatus` | Navigation | {compliance status} of {this} |
| `complianceStatus` | Action | set the compliance status of {this} to {compliance status} |
| `co2PerRpk` | Navigation | {CO2 per RPK} of {this} |
| `pmPerKgFuel` | Navigation | {PM per kg fuel} of {this} |
| `compliant` | Navigation | {this} is compliant |

#### Operator - *operator*

| Property | Type | Phrase |
|----------|------|--------|
| `operatorId` | Navigation | {operator ID} of {this} |
| `operatorId` | Action | set the operator ID of {this} to {operator ID} |
| `operatorName` | Navigation | {operator name} of {this} |
| `operatorName` | Action | set the operator name of {this} to {operator name} |
| `annualCo2EmissionsMetricTons` | Navigation | {annual CO2 emissions in metric tons} of {this} |
| `annualCo2EmissionsMetricTons` | Action | set the annual CO2 emissions in metric tons of {this} to {annual CO2 emissions in metric tons} |
| `baselineEmissions2019_2020` | Navigation | {baseline emissions 2019-2020} of {this} |
| `baselineEmissions2019_2020` | Action | set the baseline emissions 2019-2020 of {this} to {baseline emissions 2019-2020} |
| `offsetCreditsRequired` | Navigation | {offset credits required} of {this} |
| `offsetCreditsRequired` | Action | set the offset credits required of {this} to {offset credits required} |
| `offsetCreditsPurchased` | Navigation | {offset credits purchased} of {this} |
| `offsetCreditsPurchased` | Action | set the offset credits purchased of {this} to {offset credits purchased} |
| `safCreditsEarned` | Navigation | {SAF credits earned} of {this} |
| `safCreditsEarned` | Action | set the SAF credits earned of {this} to {SAF credits earned} |
| `violations` | Navigation | {violations} of {this} |
| `messages` | Navigation | {messages} of {this} |
| `penaltyAmount` | Navigation | {penalty amount} of {this} |
| `penaltyAmount` | Action | set the penalty amount of {this} to {penalty amount} |
| `routeRightsSuspended` | Navigation | {this} has route rights suspended |
| `routeRightsSuspended` | Action | make it {route rights suspended} that {this} has route rights suspended |
| `excessEmissions` | Navigation | {excess emissions} of {this} |
| `netOffsetObligation` | Navigation | {net offset obligation} of {this} |
| `offsetDeficit` | Navigation | {offset deficit} of {this} |


---

*Report generated by ODM Report Generator*
