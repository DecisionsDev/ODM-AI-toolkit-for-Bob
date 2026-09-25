# AI_Context.md — AML Detection Service

Part of the ODM examples in this repository. For format-level explanations (what each file type is, the syntax rules, the error table) read the root [AI_Context.md](../../AI_Context.md); this file describes only this project.

## Purpose

- Rule project folder: `AML Detection Service/` (name `AML Detection Service`); XOM Java project: `aml-detection-xom/`.
- Source policy used to generate it: <https://raw.githubusercontent.com/DecisionsDev/policy-corpus/refs/heads/main/banking/aml/anti_money_laundering_simple_detection.txt>

## Build status

Builds successfully (`BUILD SUCCESS`) with IBM Semeru JDK 21 and `rules-compiler.jar`, XOM compiled from `src/`.

## Decision interface

- Variable `request` of type `com.banking.aml.AMLRequest`, verbalized `the request` (rules write `'the request'`), direction `IN_OUT`.
- Operation `AMLDetectionOperation`: ruleset `AML_Detection_Ruleset`, ruleflow `aml-detection-ruleflow`.
- Deployment config: `AMLDetection.dep`, `dep` name `AMLDetection`, RuleApp `AMLDetection`.

## XOM and BOM

Package `com.banking.aml`. Business classes and their members (from `bom/aml-detection.bom`):

- **Transaction**: transactionId, customerId, accountId, amount, currency, timestamp, transactionType, counterpartyAccountId, counterpartyJurisdiction, crossBorder, roundNumber; computed (`readonly`, `factory.ignore`): amountRoundNumber
- **Customer**: customerId, name, industry, geographicLocation, riskScore, kycComplete, kycLastUpdated, priorCrossBorderActivity, linkedAccountIds; computed (`readonly`, `factory.ignore`): highRisk, mediumRisk, kycOutdated; methods: addLinkedAccount
- **Account**: accountId, customerId, lastActivityDate, dormant, daysSinceLastActivity, reactivationDate; computed (`readonly`, `factory.ignore`): dormantAccount, recentlyReactivated
- **Alert**: alertId, ruleTriggered, severity, description, transactionId, customerId, accountId, alertTimestamp, escalated
- **AMLRequest**: currentTransaction, customer, account, recentTransactions, alerts, highRiskJurisdictions, ruleTriggeredCount, requiresManualReview, escalateToCompliance, overallRiskLevel; computed (`readonly`, `factory.ignore`): totalRecentTransactionAmount, recentTransactionCount, multipleRulesTriggered; methods: addAlert, addHighRiskJurisdiction, addRecentTransaction, createAndAddAlert, incrementRuleCount, isHighRiskJurisdiction

XOM sources: `src/com/banking/aml/AMLRequest.java`, `src/com/banking/aml/Account.java`, `src/com/banking/aml/Alert.java`, `src/com/banking/aml/Customer.java`, `src/com/banking/aml/Transaction.java`.

## Ruleflow (execution order)

| # | Rule package | Mode |
|---|---|---|
| 1 | high-value-transactions | RetePlus |
| 2 | jurisdiction-monitoring | RetePlus |
| 3 | customer-risk-scoring | RetePlus |
| 4 | account-monitoring | RetePlus |
| 5 | escalation | Fastpath |

Fastpath = sequential execution, no inference. RetePlus = pattern-matching inference where one rule's effect may trigger others.

## Rules by package

### `rules/account-monitoring/` — Rules for dormant account reactivation and rapid fund movement
- `flag-dormant-account-large-deposit.brl` (rule): flag-dormant-account-large-deposit

### `rules/customer-risk-scoring/` — Rules for customer risk scoring and assessment
- `flag-high-risk-customer-transaction.brl` (rule): flag-high-risk-customer-transaction
- `require-manual-review-medium-risk.brl` (rule): require-manual-review-medium-risk

### `rules/escalation/` — Rules for alert escalation and compliance reporting
- `escalate-multiple-rules-triggered.brl` (rule): escalate-multiple-rules-triggered

### `rules/high-value-transactions/` — Rules for detecting high-value transactions
- `flag-cumulative-24h-transactions.brl` (rule): flag-cumulative-24h-transactions
- `flag-single-high-value-transaction.brl` (rule): flag-single-high-value-transaction

### `rules/jurisdiction-monitoring/` — Rules for monitoring high-risk jurisdictions
- `flag-first-cross-border-transaction.brl` (rule): flag-first-cross-border-transaction
- `flag-high-risk-jurisdiction.brl` (rule): flag-high-risk-jurisdiction

## Representative rule

`rules/account-monitoring/flag-dormant-account-large-deposit.brl` — the BAL inside `<definition><![CDATA[ … ]]>`:

```
if
    the account of 'the request' is not null
    and the current transaction of 'the request' is not null
    and the account of 'the request' is dormant account
    and the account of 'the request' is recently reactivated
    and the amount of the current transaction of 'the request' is at least 5000
then
    set the overall risk level of 'the request' to "HIGH" ;
    increment rule count of 'the request' ;
    create alert on 'the request' with rule "RULE_4_DORMANT_REACTIVATION" severity "HIGH" and description "Dormant account reactivated with deposit >= $5,000 USD" ;
```

## Design notes

- Root object `AMLRequest` bundles a `Transaction`, `Customer`, `Account` and a list of `Alert`s.
- Rules create alerts through a custom action: `create alert on 'the request' with rule "…" severity "HIGH" and description "…" ;`, and bump a counter with `increment rule count of 'the request' ;`.
- `escalation` (Fastpath) runs last: `'the request' has multiple rules triggered` (three or more, per the rule's alert text) sets the overall risk level to `"CRITICAL"` and creates an alert. The four detection packages before it run in RetePlus.
- A XOM jar and compiled `bin/` folder are committed next to `src/`.

## File inventory

```
AML Detection Service/.ruleproject
AML Detection Service/bom/aml-detection.b2xa
AML Detection Service/bom/aml-detection.bom
AML Detection Service/bom/aml-detection_en_US.voc
AML Detection Service/deployment/AMLDetection.dep
AML Detection Service/deployment/AMLDetectionOperation.dop
AML Detection Service/rules/AMLDetectionParameters.var
AML Detection Service/rules/aml-detection-ruleflow.rfl
AML Detection Service/rules/account-monitoring/.rulepackage
AML Detection Service/rules/account-monitoring/flag-dormant-account-large-deposit.brl
AML Detection Service/rules/customer-risk-scoring/.rulepackage
AML Detection Service/rules/customer-risk-scoring/flag-high-risk-customer-transaction.brl
AML Detection Service/rules/customer-risk-scoring/require-manual-review-medium-risk.brl
AML Detection Service/rules/escalation/.rulepackage
AML Detection Service/rules/escalation/escalate-multiple-rules-triggered.brl
AML Detection Service/rules/high-value-transactions/.rulepackage
AML Detection Service/rules/high-value-transactions/flag-cumulative-24h-transactions.brl
AML Detection Service/rules/high-value-transactions/flag-single-high-value-transaction.brl
AML Detection Service/rules/jurisdiction-monitoring/.rulepackage
AML Detection Service/rules/jurisdiction-monitoring/flag-first-cross-border-transaction.brl
AML Detection Service/rules/jurisdiction-monitoring/flag-high-risk-jurisdiction.brl
```
