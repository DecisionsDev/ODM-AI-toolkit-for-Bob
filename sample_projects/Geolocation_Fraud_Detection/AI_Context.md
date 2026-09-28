# AI_Context.md — Geolocation Fraud Detection

Part of the ODM examples in this repository. For format-level explanations (what each file type is, the syntax rules, the error table) read the root [AI_Context.md](../../AI_Context.md); this file describes only this project.

## Purpose

- Rule project folder: `Geolocation Fraud Detection/` (name `Geolocation Fraud Detection`); XOM Java project: `geolocation-fraud-xom/`.
- Decision domain: Financial transaction fraud validation based on geolocation and velocity data.

## Build status

Builds successfully (`BUILD SUCCESS`) with IBM Semeru JDK 21 and `rules-compiler.jar`, XOM compiled from `src/`.

## Decision interface

- Variable `transaction` of type `com.fraud.geolocation.Transaction`, verbalized `the transaction` (rules write `'the transaction'`), direction `IN_OUT`.
- Operation `GeolocationFraudDetectionOperation`: ruleset `Geolocation_Fraud_Detection_Ruleset`, ruleflow `geolocation-fraud-flow`.
- Deployment config: `Geolocation Fraud Detection.dep`, `dep` name `Geolocation Fraud Detection`, RuleApp `Geolocation_Fraud_Detection`.

## XOM and BOM

Package `com.fraud.geolocation`. Business classes and their members (from `bom/geolocation-fraud.bom`):

- **Customer**: customerId, firstName, lastName, homeCountry, homeCountryCode, homeLocation, accountAgeDays, averageTransactionAmount, totalTransactionCount, highValueCustomer, knownTransactionCountries, travelNotifications; methods: addKnownTransactionCountry, addTravelNotification, hasTransactionHistoryIn, hasTravelNotificationFor
- **DeviceInfo**: deviceFingerprint, deviceType, ipAddress, userAgent, ipGeolocation, knownDevice, proxyDetected, torDetected, vpnDetected; methods: ipToMerchantDistance
- **Location**: latitude, longitude, country, countryCode, region, city, postalCode; methods: distanceTo
- **RiskScore**: totalScore, maxScore, confidenceLevel, riskFactors, triggeredRules, impossibleTravelDetected, highRiskCountryDetected, ipLocationMismatchDetected, unusualCorridorDetected, velocityAnomalyDetected, cardPresenceMismatchDetected; computed (`factory.ignore`): normalizedScore; methods: addScore, addRiskFactor, addTriggeredRule, exceedsThreshold
- **Transaction**: transactionId, amount, currency, transactionTimestampMs, cardNumber, cardType, cardPresent, merchantId, merchantName, merchantCategory, merchantLocation, previousTransactionLocation, previousTransactionTimestampMs, distinctCountriesLast24Hours, distinctCountriesLast7Days, transactionCountLast1Hour, transactionCountLast24Hours, totalAmountLast1Hour, totalAmountLast24Hours, customer, deviceInfo, riskScore, fraudDecision, decisionReasonCode, recommendedAction, auditMessages; computed (`factory.ignore`): inKnownTransactionCorridor; methods: addAuditMessage

XOM sources: `src/com/fraud/geolocation/*.java`.

## Ruleflow (execution order)

| # | Rule package | Mode |
|---|---|---|
| 1 | data-enrichment | Fastpath |
| 2 | geolocation-prescreening | RetePlus |
| 3 | risk-scoring | RetePlus |
| 4 | fraud-decision | Fastpath |

Fastpath = sequential execution, no inference. RetePlus = pattern-matching inference where one rule's effect may trigger others.

## Rules by package

### `rules/data-enrichment/` — Data enrichment and initialization
- `initialize-risk-score.brl` (rule): initialize-risk-score
- `set-default-fraud-decision.brl` (rule): set-default-fraud-decision
- `validate-required-fields.brl` (rule): validate-required-fields

### `rules/geolocation-prescreening/` — Geolocation anomaly pre-screening
- `detect-card-presence-location-anomaly.brl` (rule): detect-card-presence-location-anomaly
- `detect-high-risk-country.brl` (rule): detect-high-risk-country
- `detect-impossible-travel.brl` (rule): detect-impossible-travel
- `detect-ip-location-mismatch.brl` (rule): detect-ip-location-mismatch
- `detect-multi-country-velocity.brl` (rule): detect-multi-country-velocity
- `detect-unusual-transaction-corridor.brl` (rule): detect-unusual-transaction-corridor
- `detect-vpn-proxy-tor.brl` (rule): detect-vpn-proxy-tor

### `rules/risk-scoring/` — Risk score calculation and aggregation
- `score-card-presence-mismatch.brl` (rule): score-card-presence-mismatch
- `score-high-risk-country.brl` (rule): score-high-risk-country
- `score-impossible-travel.brl` (rule): score-impossible-travel
- `score-ip-location-mismatch.brl` (rule): score-ip-location-mismatch
- `score-unusual-corridor.brl` (rule): score-unusual-corridor
- `score-velocity-anomaly.brl` (rule): score-velocity-anomaly
- `set-confidence-level.brl` (rule): set-confidence-level

### `rules/fraud-decision/` — Final fraud decisions
- `approve-low-risk.brl` (rule): approve-low-risk
- `decline-high-risk-country.brl` (rule): decline-high-risk-country
- `decline-impossible-travel.brl` (rule): decline-impossible-travel
- `review-high-risk-country-single.brl` (rule): review-high-risk-country-single
- `review-moderate-risk.brl` (rule): review-moderate-risk

## File inventory

```
Geolocation Fraud Detection/.ruleproject
Geolocation Fraud Detection/bom/geolocation-fraud.b2xa
Geolocation Fraud Detection/bom/geolocation-fraud.bom
Geolocation Fraud Detection/bom/geolocation-fraud_en_US.voc
Geolocation Fraud Detection/deployment/Geolocation Fraud Detection.dep
Geolocation Fraud Detection/deployment/Geolocation Fraud DetectionOperation.dop
Geolocation Fraud Detection/rules/Geolocation Fraud DetectionParameters.var
Geolocation Fraud Detection/rules/geolocation-fraud-flow.rfl
Geolocation Fraud Detection/rules/data-enrichment/.rulepackage
Geolocation Fraud Detection/rules/data-enrichment/initialize-risk-score.brl
Geolocation Fraud Detection/rules/data-enrichment/set-default-fraud-decision.brl
Geolocation Fraud Detection/rules/data-enrichment/validate-required-fields.brl
Geolocation Fraud Detection/rules/geolocation-prescreening/.rulepackage
Geolocation Fraud Detection/rules/geolocation-prescreening/detect-card-presence-location-anomaly.brl
Geolocation Fraud Detection/rules/geolocation-prescreening/detect-high-risk-country.brl
Geolocation Fraud Detection/rules/geolocation-prescreening/detect-impossible-travel.brl
Geolocation Fraud Detection/rules/geolocation-prescreening/detect-ip-location-mismatch.brl
Geolocation Fraud Detection/rules/geolocation-prescreening/detect-multi-country-velocity.brl
Geolocation Fraud Detection/rules/geolocation-prescreening/detect-unusual-transaction-corridor.brl
Geolocation Fraud Detection/rules/geolocation-prescreening/detect-vpn-proxy-tor.brl
Geolocation Fraud Detection/rules/risk-scoring/.rulepackage
Geolocation Fraud Detection/rules/risk-scoring/score-card-presence-mismatch.brl
Geolocation Fraud Detection/rules/risk-scoring/score-high-risk-country.brl
Geolocation Fraud Detection/rules/risk-scoring/score-impossible-travel.brl
Geolocation Fraud Detection/rules/risk-scoring/score-ip-location-mismatch.brl
Geolocation Fraud Detection/rules/risk-scoring/score-unusual-corridor.brl
Geolocation Fraud Detection/rules/risk-scoring/score-velocity-anomaly.brl
Geolocation Fraud Detection/rules/risk-scoring/set-confidence-level.brl
Geolocation Fraud Detection/rules/fraud-decision/.rulepackage
Geolocation Fraud Detection/rules/fraud-decision/approve-low-risk.brl
Geolocation Fraud Detection/rules/fraud-decision/decline-high-risk-country.brl
Geolocation Fraud Detection/rules/fraud-decision/decline-impossible-travel.brl
Geolocation Fraud Detection/rules/fraud-decision/review-high-risk-country-single.brl
Geolocation Fraud Detection/rules/fraud-decision/review-moderate-risk.brl
```
