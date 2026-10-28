# AI_Context.md — CrossBorder Fraud Detection

Part of the ODM examples in this repository. For format-level explanations (what each file type is, the syntax rules, the error table) read the root [AI_Context.md](../../AI_Context.md); this file describes only this project.

## Purpose

- Rule project folder: `CrossBorderFraudDetection/` (name `CrossBorderFraudDetection`); XOM Java project: `cross-border-fraud-xom/`.
- Decision domain: Cross-border transaction compliance checking and fraud risk scoring.

## Build status

Builds successfully (`BUILD SUCCESS`) with IBM Semeru JDK 21 and `rules-compiler.jar`, XOM compiled from `src/`.

## Decision interface

- Variable `transaction` of type `com.fraud.crossborder.Transaction`, verbalized `the transaction` (rules write `'the transaction'`), direction `IN_OUT`.
- Operation `CrossBorderFraudDetectionOperation`: ruleset `CrossBorderFraudDetection_Ruleset`, ruleflow `cross-border-fraud-flow`.
- Deployment config: `CrossBorderFraudDetectionDeployment.dep`, `dep` name `CrossBorderFraudDetectionDeployment`, RuleApp `CrossBorderFraudDetectionDeployment`.

## XOM and BOM

Package `com.fraud.crossborder`. Business classes and their members (from `bom/cross-border-fraud.bom`):

- **Customer**: customerId, residenceCountryCode, kycComplete, hasVerifiedAddress, hasIdDocument, hasBusinessProfile, riskTier, knownTransactionCountries, travelNotifications; methods: addKnownTransactionCountry, addTravelNotification, hasTransactionHistoryIn, hasTravelNotificationFor
- **RiskScore**: totalScore, maxScore, confidenceLevel, sanctionedCountryDetected, amlJurisdictionDetected, spendingSurgeDetected, exoticCurrencyDetected, offshoreProviderDetected, greyZoneMerchantDetected, kycIncompleteDetected, mandatoryReportingRequired, triggeredRules, riskFactors; methods: addScore, addTriggeredRule, addRiskFactor, exceedsThreshold
- **Transaction**: transactionId, amount, currency, cardIssuingCountry, merchantCountryCode, merchantCategory, merchantIsOffshore, merchantIsOnGreyList, merchantIsOnRegulatedWhitelist, ipCountryCode, customer, crossBorderTransactionCountLast24Hours, totalCrossBorderAmountLast24Hours, distinctCurrencyZonesLast24Hours, travelDeclared, sanctionedCountry, highRiskAmlJurisdiction, exoticCurrency, mandatoryReporting, riskScore, fraudDecision, decisionReasonCode, recommendedAction, auditMessages; computed (`factory.ignore`): crossBorder, ipMerchantCountryMismatch; methods: addAuditMessage

XOM sources: `src/com/fraud/crossborder/*.java`.

## Ruleflow (execution order)

| # | Rule package | Mode |
|---|---|---|
| 1 | data-enrichment | Fastpath |
| 2 | compliance-prescreening | RetePlus |
| 3 | risk-scoring | RetePlus |
| 4 | fraud-decision | Fastpath |

Fastpath = sequential execution, no inference. RetePlus = pattern-matching inference where one rule's effect may trigger others.

## Rules by package

### `rules/data-enrichment/` — Data enrichment and initialization
- `initialize-risk-score.brl` (rule): initialize-risk-score
- `set-default-fraud-decision.brl` (rule): set-default-fraud-decision
- `validate-required-fields.brl` (rule): validate-required-fields

### `rules/compliance-prescreening/` — Compliance pre-screening and flags
- `detect-exotic-currency.brl` (rule): detect-exotic-currency
- `detect-grey-zone-merchant.brl` (rule): detect-grey-zone-merchant
- `detect-high-risk-aml-jurisdiction.brl` (rule): detect-high-risk-aml-jurisdiction
- `detect-kyc-incomplete.brl` (rule): detect-kyc-incomplete
- `detect-mandatory-reporting.brl` (rule): detect-mandatory-reporting
- `detect-offshore-provider.brl` (rule): detect-offshore-provider
- `detect-sanctioned-country.brl` (rule): detect-sanctioned-country
- `detect-spending-surge.brl` (rule): detect-spending-surge

### `rules/risk-scoring/` — Risk score calculation and aggregation
- `score-confidence-level.brl` (rule): score-confidence-level
- `score-exotic-currency.brl` (rule): score-exotic-currency
- `score-grey-zone-merchant.brl` (rule): score-grey-zone-merchant
- `score-high-risk-aml-jurisdiction.brl` (rule): score-high-risk-aml-jurisdiction
- `score-offshore-provider.brl` (rule): score-offshore-provider
- `score-sanctioned-country.brl` (rule): score-sanctioned-country
- `score-spending-surge.brl` (rule): score-spending-surge

### `rules/fraud-decision/` — Final fraud decisions
- `approve-low-risk.brl` (rule): approve-low-risk
- `block-high-score.brl` (rule): block-high-score
- `block-sanctioned-transaction.brl` (rule): block-sanctioned-transaction
- `exempt-eea-sepa-transaction.brl` (rule): exempt-eea-sepa-transaction
- `flag-compliance-review.brl` (rule): flag-compliance-review

## File inventory

```
CrossBorderFraudDetection/.ruleproject
CrossBorderFraudDetection/bom/cross-border-fraud.b2xa
CrossBorderFraudDetection/bom/cross-border-fraud.bom
CrossBorderFraudDetection/bom/cross-border-fraud_en_US.voc
CrossBorderFraudDetection/deployment/CrossBorderFraudDetectionDeployment.dep
CrossBorderFraudDetection/deployment/CrossBorderFraudDetectionOperation.dop
CrossBorderFraudDetection/rules/CrossBorderFraudDetectionParameters.var
CrossBorderFraudDetection/rules/cross-border-fraud-flow.rfl
CrossBorderFraudDetection/rules/data-enrichment/.rulepackage
CrossBorderFraudDetection/rules/data-enrichment/initialize-risk-score.brl
CrossBorderFraudDetection/rules/data-enrichment/set-default-fraud-decision.brl
CrossBorderFraudDetection/rules/data-enrichment/validate-required-fields.brl
CrossBorderFraudDetection/rules/compliance-prescreening/.rulepackage
CrossBorderFraudDetection/rules/compliance-prescreening/detect-exotic-currency.brl
CrossBorderFraudDetection/rules/compliance-prescreening/detect-grey-zone-merchant.brl
CrossBorderFraudDetection/rules/compliance-prescreening/detect-high-risk-aml-jurisdiction.brl
CrossBorderFraudDetection/rules/compliance-prescreening/detect-kyc-incomplete.brl
CrossBorderFraudDetection/rules/compliance-prescreening/detect-mandatory-reporting.brl
CrossBorderFraudDetection/rules/compliance-prescreening/detect-offshore-provider.brl
CrossBorderFraudDetection/rules/compliance-prescreening/detect-sanctioned-country.brl
CrossBorderFraudDetection/rules/compliance-prescreening/detect-spending-surge.brl
CrossBorderFraudDetection/rules/risk-scoring/.rulepackage
CrossBorderFraudDetection/rules/risk-scoring/score-confidence-level.brl
CrossBorderFraudDetection/rules/risk-scoring/score-exotic-currency.brl
CrossBorderFraudDetection/rules/risk-scoring/score-grey-zone-merchant.brl
CrossBorderFraudDetection/rules/risk-scoring/score-high-risk-aml-jurisdiction.brl
CrossBorderFraudDetection/rules/risk-scoring/score-offshore-provider.brl
CrossBorderFraudDetection/rules/risk-scoring/score-sanctioned-country.brl
CrossBorderFraudDetection/rules/risk-scoring/score-spending-surge.brl
CrossBorderFraudDetection/rules/fraud-decision/.rulepackage
CrossBorderFraudDetection/rules/fraud-decision/approve-low-risk.brl
CrossBorderFraudDetection/rules/fraud-decision/block-high-score.brl
CrossBorderFraudDetection/rules/fraud-decision/block-sanctioned-transaction.brl
CrossBorderFraudDetection/rules/fraud-decision/exempt-eea-sepa-transaction.brl
CrossBorderFraudDetection/rules/fraud-decision/flag-compliance-review.brl
```
