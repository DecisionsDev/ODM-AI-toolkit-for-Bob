Generated with the prompt :

Create an ODM rule project based on https://raw.githubusercontent.com/DecisionsDev/policy-corpus/refs/heads/main/banking/aml/anti_money_laundering_simple_detection.txt

Version v1.0.0 of the bob mode 
# ODM Rule Project Documentation

**Project:** AML Detection Service

**Generated:** 2026-03-06 09:52:48

---

## 1. Project Overview

- **Project Name:** AML Detection Service
- **Project UUID:** a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d
- **Project Type:** Decision Service


## 2. Quality Assessment Summary

### Quality Metrics

- **Rule Documentation:** 0.0% (0/8) - Score: 0.0/20
- **Rule Complexity:** 75.0% simple rules (6/8) - Score: 15.0/20
- **Project Organization:** 5 packages - Score: 15/20
- **BOM Coverage:** 5 classes defined - Score: 20/20
- **Vocabulary Coverage:** Score: 0/20

### Overall Quality Score

**50.0%** (50.0/100) - Grade: **F**

**Quality Interpretation:**

⚠️ **Poor** - Project needs significant improvements in documentation and organization.

### Issues Found (2)

#### 🟡 Medium Priority Issues (2)

- **flag-first-cross-border-transaction**: Complex rule with 9 conditions and 3 actions (Type: complexity)
- **flag-dormant-account-large-deposit**: Complex rule with 7 conditions and 3 actions (Type: complexity)

### Recommendations

- 📝 **Add Documentation**: Less than 50% of rules have documentation. Add meaningful descriptions to help users understand rule purpose and business logic.
- 🔧 **Simplify Complex Rules**: 2 rule(s) have high complexity. Consider breaking them into smaller, more maintainable rules.
- ✅ **Regular Reviews**: Conduct periodic code reviews to maintain quality standards.
- 🧪 **Add Test Cases**: Ensure comprehensive test coverage for all rules and decision tables.
- 📖 **Update Vocabulary**: Keep business vocabulary aligned with domain expert terminology.

---

## 3. Deployment Configuration

### Operation: `AMLDetectionOperation`

- **Ruleset Name:** `AML_Detection_Ruleset`
- **Using Ruleflow:** true
- **Ruleflow Name:** `aml-detection-ruleflow`

**Input/Output Parameters:**

| Variable | Type | Direction |
|----------|------|-----------|
| `request` | `AMLDetectionParameters` | IN_OUT |


## 4. Ruleflow

### aml-detection-ruleflow

**Execution Flow:**

| Step | Task | Execution Mode | Rule Package |
|------|------|----------------|--------------|
| 1 | High Value Transactions | `RetePlus` | `high-value-transactions` |
| 2 | Jurisdiction Monitoring | `RetePlus` | `jurisdiction-monitoring` |
| 3 | Customer Risk Scoring | `RetePlus` | `customer-risk-scoring` |
| 4 | Account Monitoring | `RetePlus` | `account-monitoring` |
| 5 | Escalation | `Fastpath` | `escalation` |

**Execution Modes:**

- **Fastpath**: Sequential rule execution where order matters. Rules are evaluated in the order they appear.
- **RetePlus**: Rete algorithm-based execution with pattern matching. Order-independent evaluation.


## 5. Business Rules

### Package: `account-monitoring`

#### Rule: `flag-dormant-account-large-deposit`

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

**Conditions:**

- the account of 'the request' is not null
- the current transaction of 'the request' is not null
- the account of 'the request' is dormant account
- the account of 'the request' is recently reactivated
- the amount of the current transaction of 'the request' is at least 5000

**Actions:**

- set the overall risk level of 'the request' to "HIGH"
- increment rule count of 'the request'
- create alert on 'the request' with rule "RULE_4_DORMANT_REACTIVATION" severity "HIGH" and description "Dormant account reactivated with deposit >= $5,000 USD"

---

### Package: `customer-risk-scoring`

#### Rule: `flag-high-risk-customer-transaction`

```
if
    the customer of 'the request' is not null
    and the customer of 'the request' is high risk customer
then
    set the overall risk level of 'the request' to "CRITICAL" ;
    increment rule count of 'the request' ;
    create alert on 'the request' with rule "RULE_5_HIGH_RISK_CUSTOMER" severity "CRITICAL" and description "Transaction by high-risk customer (risk score >= 0.8)" ;
```

**Conditions:**

- the customer of 'the request' is not null
- the customer of 'the request' is high risk customer

**Actions:**

- set the overall risk level of 'the request' to "CRITICAL"
- increment rule count of 'the request'
- create alert on 'the request' with rule "RULE_5_HIGH_RISK_CUSTOMER" severity "CRITICAL" and description "Transaction by high-risk customer (risk score >= 0.8)"

---

#### Rule: `require-manual-review-medium-risk`

```
if
    the customer of 'the request' is not null
    and the current transaction of 'the request' is not null
    and the customer of 'the request' is medium risk customer
    and the amount of the current transaction of 'the request' is more than 5000
then
    set the overall risk level of 'the request' to "MEDIUM" ;
    increment rule count of 'the request' ;
    create alert on 'the request' with rule "RULE_5_MEDIUM_RISK_MANUAL_REVIEW" severity "MEDIUM" and description "Medium-risk customer transaction > $5,000 requires manual review" ;
```

**Conditions:**

- the customer of 'the request' is not null
- the current transaction of 'the request' is not null
- the customer of 'the request' is medium risk customer
- the amount of the current transaction of 'the request' is more than 5000

**Actions:**

- set the overall risk level of 'the request' to "MEDIUM"
- increment rule count of 'the request'
- create alert on 'the request' with rule "RULE_5_MEDIUM_RISK_MANUAL_REVIEW" severity "MEDIUM" and description "Medium-risk customer transaction > $5,000 requires manual review"

---

### Package: `escalation`

#### Rule: `escalate-multiple-rules-triggered`

```
if
    'the request' has multiple rules triggered
then
    set the overall risk level of 'the request' to "CRITICAL" ;
    create alert on 'the request' with rule "RULE_9_ESCALATION" severity "CRITICAL" and description "Multiple rules triggered (>= 3) - escalate to compliance review" ;
```

**Conditions:**

- 'the request' has multiple rules triggered

**Actions:**

- set the overall risk level of 'the request' to "CRITICAL"
- create alert on 'the request' with rule "RULE_9_ESCALATION" severity "CRITICAL" and description "Multiple rules triggered (>= 3) - escalate to compliance review"

---

### Package: `high-value-transactions`

#### Rule: `flag-cumulative-24h-transactions`

```
if
    the total recent transaction amount of 'the request' is at least 10000
then
    set the overall risk level of 'the request' to "HIGH" ;
    increment rule count of 'the request' ;
    create alert on 'the request' with rule "RULE_1_HIGH_VALUE_CUMULATIVE" severity "HIGH" and description "Cumulative transactions in 24-hour window >= $10,000 USD" ;
```

**Conditions:**

- the total recent transaction amount of 'the request' is at least 10000

**Actions:**

- set the overall risk level of 'the request' to "HIGH"
- increment rule count of 'the request'
- create alert on 'the request' with rule "RULE_1_HIGH_VALUE_CUMULATIVE" severity "HIGH" and description "Cumulative transactions in 24-hour window >= $10,000 USD"

---

#### Rule: `flag-single-high-value-transaction`

```
if
    the current transaction of 'the request' is not null
    and the amount of the current transaction of 'the request' is at least 10000
then
    set the overall risk level of 'the request' to "HIGH" ;
    increment rule count of 'the request' ;
    create alert on 'the request' with rule "RULE_1_HIGH_VALUE_SINGLE" severity "HIGH" and description "Single transaction amount >= $10,000 USD" ;
```

**Conditions:**

- the current transaction of 'the request' is not null
- the amount of the current transaction of 'the request' is at least 10000

**Actions:**

- set the overall risk level of 'the request' to "HIGH"
- increment rule count of 'the request'
- create alert on 'the request' with rule "RULE_1_HIGH_VALUE_SINGLE" severity "HIGH" and description "Single transaction amount >= $10,000 USD"

---

### Package: `jurisdiction-monitoring`

#### Rule: `flag-first-cross-border-transaction`

```
if
    the customer of 'the request' is not null
    and the current transaction of 'the request' is not null
    and it is not true that the customer of 'the request' has prior cross border activity
    and the current transaction of 'the request' is cross border
    and the amount of the current transaction of 'the request' is at least 5000
then
    set the overall risk level of 'the request' to "MEDIUM" ;
    increment rule count of 'the request' ;
    create alert on 'the request' with rule "RULE_3_FIRST_CROSS_BORDER" severity "MEDIUM" and description "First cross-border transaction >= $5,000 USD" ;
```

**Conditions:**

- the customer of 'the request' is not null
- the current transaction of 'the request' is not null
- it is not true that the customer of 'the request' has prior cross border activity
- the current transaction of 'the request' is cross border
- the amount of the current transaction of 'the request' is at least 5000

**Actions:**

- set the overall risk level of 'the request' to "MEDIUM"
- increment rule count of 'the request'
- create alert on 'the request' with rule "RULE_3_FIRST_CROSS_BORDER" severity "MEDIUM" and description "First cross-border transaction >= $5,000 USD"

---

#### Rule: `flag-high-risk-jurisdiction`

```
if
    the current transaction of 'the request' is not null
    and the counterparty jurisdiction of the current transaction of 'the request' is not null
    and ( the counterparty jurisdiction of the current transaction of 'the request' ) is high risk jurisdiction in 'the request'
then
    set the overall risk level of 'the request' to "HIGH" ;
    increment rule count of 'the request' ;
    create alert on 'the request' with rule "RULE_3_HIGH_RISK_JURISDICTION" severity "HIGH" and description "Transaction with high-risk jurisdiction" ;
```

**Conditions:**

- the current transaction of 'the request' is not null
- the counterparty jurisdiction of the current transaction of 'the request' is not null
- ( the counterparty jurisdiction of the current transaction of 'the request' ) is high risk jurisdiction in 'the request'

**Actions:**

- set the overall risk level of 'the request' to "HIGH"
- increment rule count of 'the request'
- create alert on 'the request' with rule "RULE_3_HIGH_RISK_JURISDICTION" severity "HIGH" and description "Transaction with high-risk jurisdiction"

---


## 6. Decision Tables

*No decision tables found in this project.*


## 7. Rule Variables

### AMLDetectionParameters

| Variable Name | Type | Verbalization |
|---------------|------|---------------|
| `request` | `com.banking.aml.AMLRequest` | the request |


## 8. Business Object Model (BOM)

### aml-detection

**Package:** `com.banking.aml`

#### Class: `Transaction`

**Properties:**

- `public string transactionId`
- `public string customerId`
- `public string accountId`
- `public double amount`
- `public string currency`
- `public java.util.Date timestamp`
- `public string transactionType`
- `public string counterpartyAccountId`
- `public string counterpartyJurisdiction`
- `public boolean crossBorder`
- `public boolean roundNumber`

#### Class: `Customer`

**Properties:**

- `public string customerId`
- `public string name`
- `public string industry`
- `public string geographicLocation`
- `public double riskScore`
- `public boolean kycComplete`
- `public java.util.Date kycLastUpdated`
- `public boolean priorCrossBorderActivity`

**Methods:**

- `void addLinkedAccount(string arg)`

#### Class: `Account`

**Properties:**

- `public string accountId`
- `public string customerId`
- `public java.util.Date lastActivityDate`
- `public boolean dormant`
- `public int daysSinceLastActivity`
- `public java.util.Date reactivationDate`

#### Class: `Alert`

**Properties:**

- `public string alertId`
- `public string ruleTriggered`
- `public string severity`
- `public string description`
- `public string transactionId`
- `public string customerId`
- `public string accountId`
- `public java.util.Date alertTimestamp`
- `public boolean escalated`

#### Class: `AMLRequest`

**Properties:**

- `public com.banking.aml.Transaction currentTransaction`
- `public com.banking.aml.Customer customer`
- `public com.banking.aml.Account account`
- `public int ruleTriggeredCount`
- `public boolean requiresManualReview`
- `public boolean escalateToCompliance`
- `public string overallRiskLevel`

**Methods:**

- `void addAlert(com.banking.aml.Alert arg)`
- `void addRecentTransaction(com.banking.aml.Transaction arg)`
- `void addHighRiskJurisdiction(string arg)`
- `void incrementRuleCount()`
- `boolean isHighRiskJurisdiction(string arg)`
- `void createAndAddAlert(string arg1, string arg2, string arg3)`


## 9. Business Vocabulary

### aml-detection_en_US

#### Transaction - *transaction*

| Property | Type | Phrase |
|----------|------|--------|
| `transactionId` | Navigation | {transaction ID} of {this} |
| `transactionId` | Action | set the transaction ID of {this} to {transaction ID} |
| `customerId` | Navigation | {customer ID} of {this} |
| `customerId` | Action | set the customer ID of {this} to {customer ID} |
| `accountId` | Navigation | {account ID} of {this} |
| `accountId` | Action | set the account ID of {this} to {account ID} |
| `amount` | Navigation | {amount} of {this} |
| `amount` | Action | set the amount of {this} to {amount} |
| `currency` | Navigation | {currency} of {this} |
| `currency` | Action | set the currency of {this} to {currency} |
| `timestamp` | Navigation | {timestamp} of {this} |
| `timestamp` | Action | set the timestamp of {this} to {timestamp} |
| `transactionType` | Navigation | {transaction type} of {this} |
| `transactionType` | Action | set the transaction type of {this} to {transaction type} |
| `counterpartyAccountId` | Navigation | {counterparty account ID} of {this} |
| `counterpartyAccountId` | Action | set the counterparty account ID of {this} to {counterparty account ID} |
| `counterpartyJurisdiction` | Navigation | {counterparty jurisdiction} of {this} |
| `counterpartyJurisdiction` | Action | set the counterparty jurisdiction of {this} to {counterparty jurisdiction} |
| `crossBorder` | Navigation | {this} is cross border |
| `crossBorder` | Action | make it {cross border} that {this} is cross border |
| `roundNumber` | Navigation | {this} is round number |
| `roundNumber` | Action | make it {round number} that {this} is round number |
| `amountRoundNumber` | Navigation | {this} has round number amount |

#### Customer - *customer*

| Property | Type | Phrase |
|----------|------|--------|
| `customerId` | Navigation | {customer ID} of {this} |
| `customerId` | Action | set the customer ID of {this} to {customer ID} |
| `name` | Navigation | {name} of {this} |
| `name` | Action | set the name of {this} to {name} |
| `industry` | Navigation | {industry} of {this} |
| `industry` | Action | set the industry of {this} to {industry} |
| `geographicLocation` | Navigation | {geographic location} of {this} |
| `geographicLocation` | Action | set the geographic location of {this} to {geographic location} |
| `riskScore` | Navigation | {risk score} of {this} |
| `riskScore` | Action | set the risk score of {this} to {risk score} |
| `kycComplete` | Navigation | {this} has KYC complete |
| `kycComplete` | Action | make it {KYC complete} that {this} has KYC complete |
| `kycLastUpdated` | Navigation | {KYC last updated date} of {this} |
| `kycLastUpdated` | Action | set the KYC last updated date of {this} to {KYC last updated date} |
| `priorCrossBorderActivity` | Navigation | {this} has prior cross border activity |
| `priorCrossBorderActivity` | Action | make it {prior cross border activity} that {this} has prior cross border activity |
| `linkedAccountIds` | Navigation | {linked account IDs} of {this} |
| `highRisk` | Navigation | {this} is high risk customer |
| `mediumRisk` | Navigation | {this} is medium risk customer |
| `kycOutdated` | Navigation | {this} has outdated KYC |

#### Account - *account*

| Property | Type | Phrase |
|----------|------|--------|
| `accountId` | Navigation | {account ID} of {this} |
| `accountId` | Action | set the account ID of {this} to {account ID} |
| `customerId` | Navigation | {customer ID} of {this} |
| `customerId` | Action | set the customer ID of {this} to {customer ID} |
| `lastActivityDate` | Navigation | {last activity date} of {this} |
| `lastActivityDate` | Action | set the last activity date of {this} to {last activity date} |
| `dormant` | Navigation | {this} is dormant |
| `dormant` | Action | make it {dormant} that {this} is dormant |
| `daysSinceLastActivity` | Navigation | {days since last activity} of {this} |
| `daysSinceLastActivity` | Action | set the days since last activity of {this} to {days since last activity} |
| `reactivationDate` | Navigation | {reactivation date} of {this} |
| `reactivationDate` | Action | set the reactivation date of {this} to {reactivation date} |
| `dormantAccount` | Navigation | {this} is dormant account |
| `recentlyReactivated` | Navigation | {this} is recently reactivated |

#### Alert - *alert*

| Property | Type | Phrase |
|----------|------|--------|
| `alertId` | Navigation | {alert ID} of {this} |
| `alertId` | Action | set the alert ID of {this} to {alert ID} |
| `ruleTriggered` | Navigation | {rule triggered} of {this} |
| `ruleTriggered` | Action | set the rule triggered of {this} to {rule triggered} |
| `severity` | Navigation | {severity} of {this} |
| `severity` | Action | set the severity of {this} to {severity} |
| `description` | Navigation | {description} of {this} |
| `description` | Action | set the description of {this} to {description} |
| `transactionId` | Navigation | {transaction ID} of {this} |
| `transactionId` | Action | set the transaction ID of {this} to {transaction ID} |
| `customerId` | Navigation | {customer ID} of {this} |
| `customerId` | Action | set the customer ID of {this} to {customer ID} |
| `accountId` | Navigation | {account ID} of {this} |
| `accountId` | Action | set the account ID of {this} to {account ID} |
| `alertTimestamp` | Navigation | {alert timestamp} of {this} |
| `alertTimestamp` | Action | set the alert timestamp of {this} to {alert timestamp} |
| `escalated` | Navigation | {this} is escalated |
| `escalated` | Action | make it {escalated} that {this} is escalated |

#### AMLRequest - *AML request*

| Property | Type | Phrase |
|----------|------|--------|
| `currentTransaction` | Navigation | {current transaction} of {this} |
| `currentTransaction` | Action | set the current transaction of {this} to {current transaction} |
| `customer` | Navigation | {customer} of {this} |
| `customer` | Action | set the customer of {this} to {customer} |
| `account` | Navigation | {account} of {this} |
| `account` | Action | set the account of {this} to {account} |
| `recentTransactions` | Navigation | {recent transactions} of {this} |
| `alerts` | Navigation | {alerts} of {this} |
| `highRiskJurisdictions` | Navigation | {high risk jurisdictions} of {this} |
| `ruleTriggeredCount` | Navigation | {rule triggered count} of {this} |
| `ruleTriggeredCount` | Action | set the rule triggered count of {this} to {rule triggered count} |
| `requiresManualReview` | Navigation | {this} requires manual review |
| `requiresManualReview` | Action | make it {requires manual review} that {this} requires manual review |
| `escalateToCompliance` | Navigation | {this} should escalate to compliance |
| `escalateToCompliance` | Action | make it {escalate to compliance} that {this} should escalate to compliance |
| `overallRiskLevel` | Navigation | {overall risk level} of {this} |
| `overallRiskLevel` | Action | set the overall risk level of {this} to {overall risk level} |
| `totalRecentTransactionAmount` | Navigation | {total recent transaction amount} of {this} |
| `recentTransactionCount` | Navigation | {recent transaction count} of {this} |
| `multipleRulesTriggered` | Navigation | {this} has multiple rules triggered |


---

*Report generated by ODM Report Generator*
