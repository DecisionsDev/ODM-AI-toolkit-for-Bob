Bob Mode version : v1.0.0
Generated with the prompt :
generate a fraud validation ODM rule project based on geo location transaction

that has been enhanced with the Bob **Enhance prompt with additional context** button

Create a complete and production-ready IBM Operational Decision Manager (ODM) rule project designed to validate financial transactions based on geolocation data. The project should include the following components:

Business Object Model (BOM): Define a comprehensive data model that includes Transaction, Customer, Location, DeviceInfo, and RiskScore objects with relevant attributes such as transaction ID, amount, currency, timestamp, merchant location coordinates (latitude/longitude), customer home country, IP address geolocation, device fingerprint, previous transaction location, and velocity metrics.

Rule Artifacts: Develop a full set of decision tables, business rules, and rule flows covering the following fraud detection scenarios:

Impossible travel detection: flag transactions where the geographic distance between two consecutive transactions cannot be physically traveled within the elapsed time
High-risk country validation: cross-reference transaction origin against a dynamic list of high-risk or sanctioned countries
IP address and physical location mismatch: detect discrepancies between the IP geolocation and the point-of-sale or merchant location
Unusual transaction corridor: identify transactions occurring in geographic regions where the customer has no prior transaction history
Velocity checks: detect multiple transactions originating from different countries within a configurable time window
Card-present versus card-not-present location anomaly: validate whether the physical card location aligns with the transaction origin
Rule Flow: Design a sequential and conditional rule flow that first performs data enrichment, then applies geolocation pre-screening rules, executes risk scoring logic, and finally triggers an approval, review, or decline decision with a detailed reason code.

Decision Service: Expose the rule project as a deployable RuleApp with a RESTful decision service endpoint that accepts a JSON transaction payload and returns a structured response containing the fraud decision, risk score, confidence level, triggered rule names, and recommended action.

Verbalization and Governance: Write all rules using natural language verbalization in the ODM Rule Designer format so that business analysts can read, modify, and govern the rules without developer intervention. Include rule documentation, metadata tags, and version control annotations.

Testing: Provide a complete set of test scenarios and test suites within the ODM Decision Center covering true positive fraud cases, true negative legitimate transactions, edge cases such as border-crossing transactions, and regression test cases for each geolocation rule category.


# ODM Rule Project Documentation

**Project:** Geolocation Fraud Detection

**Generated:** 2026-03-06 09:54:14

---

## 1. Project Overview

- **Project Name:** Geolocation Fraud Detection
- **Project UUID:** a3f1e2d4-7b8c-4a9e-b012-3c4d5e6f7a8b
- **Project Type:** Decision Service


## 2. Quality Assessment Summary

### Quality Metrics

- **Rule Documentation:** 100.0% (22/22) - Score: 20.0/20
- **Rule Complexity:** 31.8% simple rules (7/22) - Score: 6.4/20
- **Project Organization:** 4 packages - Score: 20/20
- **BOM Coverage:** 5 classes defined - Score: 20/20
- **Vocabulary Coverage:** Score: 0/20

### Overall Quality Score

**66.4%** (66.4/100) - Grade: **D**

**Quality Interpretation:**

⚠️ **Fair** - Project is functional but has several areas that need attention.

### Issues Found (15)

#### 🟡 Medium Priority Issues (15)

- **approve-low-risk**: Complex rule with 6 conditions and 4 actions (Type: complexity)
- **review-high-risk-country-single**: Complex rule with 8 conditions and 4 actions (Type: complexity)
- **review-moderate-risk**: Complex rule with 7 conditions and 4 actions (Type: complexity)
- **score-card-presence-mismatch**: Complex rule with 7 conditions and 3 actions (Type: complexity)
- **score-high-risk-country**: Complex rule with 8 conditions and 3 actions (Type: complexity)
- **score-impossible-travel**: Complex rule with 7 conditions and 3 actions (Type: complexity)
- **score-ip-location-mismatch**: Complex rule with 8 conditions and 3 actions (Type: complexity)
- **score-unusual-corridor**: Complex rule with 11 conditions and 3 actions (Type: complexity)
- **score-velocity-anomaly**: Complex rule with 7 conditions and 3 actions (Type: complexity)
- **detect-card-presence-location-anomaly**: Complex rule with 9 conditions and 3 actions (Type: complexity)
- **detect-high-risk-country**: Complex rule with 6 conditions and 3 actions (Type: complexity)
- **detect-impossible-travel**: Complex rule with 6 conditions and 3 actions (Type: complexity)
- **detect-ip-location-mismatch**: Complex rule with 8 conditions and 3 actions (Type: complexity)
- **detect-unusual-transaction-corridor**: Complex rule with 11 conditions and 3 actions (Type: complexity)
- **detect-vpn-proxy-tor**: Complex rule with 9 conditions and 3 actions (Type: complexity)

### Recommendations

- 🔧 **Simplify Complex Rules**: 15 rule(s) have high complexity. Consider breaking them into smaller, more maintainable rules.
- 📊 **Consider Decision Tables**: For rules with similar structure, consider using decision tables for better readability and maintenance.
- ✅ **Regular Reviews**: Conduct periodic code reviews to maintain quality standards.
- 🧪 **Add Test Cases**: Ensure comprehensive test coverage for all rules and decision tables.
- 📖 **Update Vocabulary**: Keep business vocabulary aligned with domain expert terminology.

---

## 3. Deployment Configuration

### Operation: `Geolocation Fraud DetectionOperation`

- **Ruleset Name:** `Geolocation_Fraud_Detection_Ruleset`
- **Using Ruleflow:** true
- **Ruleflow Name:** `geolocation-fraud-flow`

**Input/Output Parameters:**

| Variable | Type | Direction |
|----------|------|-----------|
| `transaction` | `Geolocation Fraud DetectionParameters` | IN_OUT |


## 4. Ruleflow

### geolocation-fraud-flow

**Execution Flow:**

| Step | Task | Execution Mode | Rule Package |
|------|------|----------------|--------------|
| 1 | Data Enrichment | `Fastpath` | `data-enrichment` |
| 2 | Geolocation Pre-Screening | `RetePlus` | `geolocation-prescreening` |
| 3 | Risk Scoring | `RetePlus` | `risk-scoring` |
| 4 | Fraud Decision | `Fastpath` | `fraud-decision` |

**Execution Modes:**

- **Fastpath**: Sequential rule execution where order matters. Rules are evaluated in the order they appear.
- **RetePlus**: Rete algorithm-based execution with pattern matching. Order-independent evaluation.


## 5. Business Rules

### Package: `data-enrichment`

#### Rule: `initialize-risk-score`

```
if
    the fraud decision of 'the transaction' is "PENDING"

  then
    add audit message "Geolocation fraud analysis started - risk score initialized" to 'the transaction' ;
```

**Conditions:**

- the fraud decision of 'the transaction' is "PENDING"

**Actions:**

- add audit message "Geolocation fraud analysis started - risk score initialized" to 'the transaction'

---

#### Rule: `set-default-fraud-decision`

```
if
    the fraud decision of 'the transaction' is "PENDING"

  then
    set the fraud decision of 'the transaction' to "APPROVE" ;
    set the recommended action of 'the transaction' to "AUTHORIZE" ;
    add audit message "Default decision set to APPROVE - awaiting geolocation analysis" to 'the transaction' ;
```

**Conditions:**

- the fraud decision of 'the transaction' is "PENDING"

**Actions:**

- set the fraud decision of 'the transaction' to "APPROVE"
- set the recommended action of 'the transaction' to "AUTHORIZE"
- add audit message "Default decision set to APPROVE - awaiting geolocation analysis" to 'the transaction'

---

#### Rule: `validate-required-fields`

```
if
    the merchant location of 'the transaction' is null

  then
    set the fraud decision of 'the transaction' to "REVIEW" ;
    set the decision reason code of 'the transaction' to "GEO-001-MISSING-MERCHANT-LOCATION" ;
    set the recommended action of 'the transaction' to "MANUAL_REVIEW" ;
    add audit message "Merchant location is missing - cannot perform geolocation analysis" to 'the transaction' ;
```

**Conditions:**

- the merchant location of 'the transaction' is null

**Actions:**

- set the fraud decision of 'the transaction' to "REVIEW"
- set the decision reason code of 'the transaction' to "GEO-001-MISSING-MERCHANT-LOCATION"
- set the recommended action of 'the transaction' to "MANUAL_REVIEW"
- add audit message "Merchant location is missing - cannot perform geolocation analysis" to 'the transaction'

---

### Package: `fraud-decision`

#### Rule: `approve-low-risk`

```
if
    the fraud decision of 'the transaction' is "APPROVE"
    and the total score of the risk score of 'the transaction' is less than 25

  then
    set the decision reason code of 'the transaction' to "GEO-301-LOW-RISK-APPROVED" ;
    set the recommended action of 'the transaction' to "AUTHORIZE" ;
    set the confidence level of the risk score of 'the transaction' to "HIGH" ;
    add audit message "DECISION: APPROVE - Low geolocation risk score, transaction authorized" to 'the transaction' ;
```

**Conditions:**

- the fraud decision of 'the transaction' is "APPROVE"
- the total score of the risk score of 'the transaction' is less than 25

**Actions:**

- set the decision reason code of 'the transaction' to "GEO-301-LOW-RISK-APPROVED"
- set the recommended action of 'the transaction' to "AUTHORIZE"
- set the confidence level of the risk score of 'the transaction' to "HIGH"
- add audit message "DECISION: APPROVE - Low geolocation risk score, transaction authorized" to 'the transaction'

---

#### Rule: `decline-high-risk-country`

```
if
    the risk score of 'the transaction' has high risk country detected
    and the total score of the risk score of 'the transaction' is at least 50

  then
    set the fraud decision of 'the transaction' to "DECLINE" ;
    set the decision reason code of 'the transaction' to "GEO-102-HIGH-RISK-COUNTRY-COMBINED" ;
    set the recommended action of 'the transaction' to "BLOCK_AND_ALERT" ;
    add audit message "DECISION: DECLINE - High-risk country with combined fraud signals" to 'the transaction' ;
```

**Conditions:**

- the risk score of 'the transaction' has high risk country detected
- the total score of the risk score of 'the transaction' is at least 50

**Actions:**

- set the fraud decision of 'the transaction' to "DECLINE"
- set the decision reason code of 'the transaction' to "GEO-102-HIGH-RISK-COUNTRY-COMBINED"
- set the recommended action of 'the transaction' to "BLOCK_AND_ALERT"
- add audit message "DECISION: DECLINE - High-risk country with combined fraud signals" to 'the transaction'

---

#### Rule: `decline-impossible-travel`

```
if
    the risk score of 'the transaction' has impossible travel detected
    and the total score of the risk score of 'the transaction' is at least 40

  then
    set the fraud decision of 'the transaction' to "DECLINE" ;
    set the decision reason code of 'the transaction' to "GEO-101-IMPOSSIBLE-TRAVEL" ;
    set the recommended action of 'the transaction' to "BLOCK_AND_ALERT" ;
    add audit message "DECISION: DECLINE - Impossible travel detected with high confidence score" to 'the transaction' ;
```

**Conditions:**

- the risk score of 'the transaction' has impossible travel detected
- the total score of the risk score of 'the transaction' is at least 40

**Actions:**

- set the fraud decision of 'the transaction' to "DECLINE"
- set the decision reason code of 'the transaction' to "GEO-101-IMPOSSIBLE-TRAVEL"
- set the recommended action of 'the transaction' to "BLOCK_AND_ALERT"
- add audit message "DECISION: DECLINE - Impossible travel detected with high confidence score" to 'the transaction'

---

#### Rule: `review-high-risk-country-single`

```
if
    the fraud decision of 'the transaction' is "APPROVE"
    and the risk score of 'the transaction' has high risk country detected
    and the total score of the risk score of 'the transaction' is at least 30
    and the total score of the risk score of 'the transaction' is less than 50

  then
    set the fraud decision of 'the transaction' to "REVIEW" ;
    set the decision reason code of 'the transaction' to "GEO-202-HIGH-RISK-COUNTRY-SINGLE" ;
    set the recommended action of 'the transaction' to "CONTACT_CUSTOMER" ;
    add audit message "DECISION: REVIEW - High-risk country detected as single signal" to 'the transaction' ;
```

**Conditions:**

- the fraud decision of 'the transaction' is "APPROVE"
- the risk score of 'the transaction' has high risk country detected
- the total score of the risk score of 'the transaction' is at least 30
- the total score of the risk score of 'the transaction' is less than 50

**Actions:**

- set the fraud decision of 'the transaction' to "REVIEW"
- set the decision reason code of 'the transaction' to "GEO-202-HIGH-RISK-COUNTRY-SINGLE"
- set the recommended action of 'the transaction' to "CONTACT_CUSTOMER"
- add audit message "DECISION: REVIEW - High-risk country detected as single signal" to 'the transaction'

---

#### Rule: `review-moderate-risk`

```
if
    the fraud decision of 'the transaction' is "APPROVE"
    and the total score of the risk score of 'the transaction' is at least 25
    and the total score of the risk score of 'the transaction' is less than 50

  then
    set the fraud decision of 'the transaction' to "REVIEW" ;
    set the decision reason code of 'the transaction' to "GEO-201-MODERATE-RISK" ;
    set the recommended action of 'the transaction' to "MANUAL_REVIEW" ;
    add audit message "DECISION: REVIEW - Moderate geolocation risk score requires analyst review" to 'the transaction' ;
```

**Conditions:**

- the fraud decision of 'the transaction' is "APPROVE"
- the total score of the risk score of 'the transaction' is at least 25
- the total score of the risk score of 'the transaction' is less than 50

**Actions:**

- set the fraud decision of 'the transaction' to "REVIEW"
- set the decision reason code of 'the transaction' to "GEO-201-MODERATE-RISK"
- set the recommended action of 'the transaction' to "MANUAL_REVIEW"
- add audit message "DECISION: REVIEW - Moderate geolocation risk score requires analyst review" to 'the transaction'

---

### Package: `geolocation-prescreening`

#### Rule: `detect-card-presence-location-anomaly`

```
if
    the card type of 'the transaction' is "PRESENT"
    and the device info of 'the transaction' is not null
    and the merchant location of 'the transaction' is not null
    and the IP geolocation of the device info of 'the transaction' is not null
    and the IP distance in km from device (the device info of 'the transaction') to location (the merchant location of 'the transaction') is more than 200

  then
    make it true that the risk score of 'the transaction' has card presence mismatch detected ;
    add risk factor "Card-present transaction but IP geolocation is more than 200 km from merchant" to the risk score of 'the transaction' ;
    add audit message "CARD_PRESENCE_MISMATCH detected - card-present transaction with remote IP geolocation" to 'the transaction' ;
```

**Conditions:**

- the card type of 'the transaction' is "PRESENT"
- the device info of 'the transaction' is not null
- the merchant location of 'the transaction' is not null
- the IP geolocation of the device info of 'the transaction' is not null
- the IP distance in km from device (the device info of 'the transaction') to location (the merchant location of 'the transaction') is more than 200

**Actions:**

- make it true that the risk score of 'the transaction' has card presence mismatch detected
- add risk factor "Card-present transaction but IP geolocation is more than 200 km from merchant" to the risk score of 'the transaction'
- add audit message "CARD_PRESENCE_MISMATCH detected - card-present transaction with remote IP geolocation" to 'the transaction'

---

#### Rule: `detect-high-risk-country`

```
if
    the merchant location of 'the transaction' is not null
    and the country code of the merchant location of 'the transaction' is one of { "KP" , "IR" , "MM" , "RU" , "BY" , "SY" , "CU" , "SD" , "SS" , "YE" , "LY" , "SO" , "ZW" , "VE" , "HT" , "NI" , "ML" , "BF" , "GN" , "GW" }

  then
    make it true that the risk score of 'the transaction' has high risk country detected ;
    add risk factor "Transaction originates from a high-risk or sanctioned country" to the risk score of 'the transaction' ;
    add audit message "HIGH_RISK_COUNTRY detected - merchant country is on the high-risk list" to 'the transaction' ;
```

**Conditions:**

- the merchant location of 'the transaction' is not null
- the country code of the merchant location of 'the transaction' is one of { "KP" , "IR" , "MM" , "RU" , "BY" , "SY" , "CU" , "SD" , "SS" , "YE" , "LY" , "SO" , "ZW" , "VE" , "HT" , "NI" , "ML" , "BF" , "GN" , "GW" }

**Actions:**

- make it true that the risk score of 'the transaction' has high risk country detected
- add risk factor "Transaction originates from a high-risk or sanctioned country" to the risk score of 'the transaction'
- add audit message "HIGH_RISK_COUNTRY detected - merchant country is on the high-risk list" to 'the transaction'

---

#### Rule: `detect-impossible-travel`

```
if
    the previous transaction location of 'the transaction' is not null
    and the merchant location of 'the transaction' is not null
    and distance in km from (the previous transaction location of 'the transaction') to (the merchant location of 'the transaction') is more than 900

  then
    make it true that the risk score of 'the transaction' has impossible travel detected ;
    add risk factor "Impossible travel: required speed exceeds 900 km/h between consecutive transactions" to the risk score of 'the transaction' ;
    add audit message "IMPOSSIBLE_TRAVEL detected - required travel speed exceeds maximum aircraft speed" to 'the transaction' ;
```

**Conditions:**

- the previous transaction location of 'the transaction' is not null
- the merchant location of 'the transaction' is not null
- distance in km from (the previous transaction location of 'the transaction') to (the merchant location of 'the transaction') is more than 900

**Actions:**

- make it true that the risk score of 'the transaction' has impossible travel detected
- add risk factor "Impossible travel: required speed exceeds 900 km/h between consecutive transactions" to the risk score of 'the transaction'
- add audit message "IMPOSSIBLE_TRAVEL detected - required travel speed exceeds maximum aircraft speed" to 'the transaction'

---

#### Rule: `detect-ip-location-mismatch`

```
if
    the device info of 'the transaction' is not null
    and the merchant location of 'the transaction' is not null
    and the IP geolocation of the device info of 'the transaction' is not null
    and the IP distance in km from device (the device info of 'the transaction') to location (the merchant location of 'the transaction') is more than 500

  then
    make it true that the risk score of 'the transaction' has IP location mismatch detected ;
    add risk factor "IP geolocation is more than 500 km from merchant location" to the risk score of 'the transaction' ;
    add audit message "IP_LOCATION_MISMATCH detected - IP geolocation significantly differs from merchant location" to 'the transaction' ;
```

**Conditions:**

- the device info of 'the transaction' is not null
- the merchant location of 'the transaction' is not null
- the IP geolocation of the device info of 'the transaction' is not null
- the IP distance in km from device (the device info of 'the transaction') to location (the merchant location of 'the transaction') is more than 500

**Actions:**

- make it true that the risk score of 'the transaction' has IP location mismatch detected
- add risk factor "IP geolocation is more than 500 km from merchant location" to the risk score of 'the transaction'
- add audit message "IP_LOCATION_MISMATCH detected - IP geolocation significantly differs from merchant location" to 'the transaction'

---

#### Rule: `detect-multi-country-velocity`

```
if
    the distinct countries in last 24 hours of 'the transaction' is at least 3

  then
    make it true that the risk score of 'the transaction' has velocity anomaly detected ;
    add risk factor "Transactions detected in 3 or more distinct countries within 24 hours" to the risk score of 'the transaction' ;
    add audit message "MULTI_COUNTRY_VELOCITY detected - transactions in multiple countries within 24 hours" to 'the transaction' ;
```

**Conditions:**

- the distinct countries in last 24 hours of 'the transaction' is at least 3

**Actions:**

- make it true that the risk score of 'the transaction' has velocity anomaly detected
- add risk factor "Transactions detected in 3 or more distinct countries within 24 hours" to the risk score of 'the transaction'
- add audit message "MULTI_COUNTRY_VELOCITY detected - transactions in multiple countries within 24 hours" to 'the transaction'

---

#### Rule: `detect-unusual-transaction-corridor`

```
if
    the customer of 'the transaction' is not null
    and the merchant location of 'the transaction' is not null
    and it is not true that 'the transaction' is whitelisted

  then
    make it true that the risk score of 'the transaction' has unusual corridor detected ;
    add risk factor "Transaction in country with no prior transaction history and no travel notification" to the risk score of 'the transaction' ;
    add audit message "UNUSUAL_CORRIDOR detected - transaction country not in customer history" to 'the transaction' ;
```

**Conditions:**

- the customer of 'the transaction' is not null
- the merchant location of 'the transaction' is not null
- it is not true that 'the transaction' is whitelisted

**Actions:**

- make it true that the risk score of 'the transaction' has unusual corridor detected
- add risk factor "Transaction in country with no prior transaction history and no travel notification" to the risk score of 'the transaction'
- add audit message "UNUSUAL_CORRIDOR detected - transaction country not in customer history" to 'the transaction'

---

#### Rule: `detect-vpn-proxy-tor`

```
if
    the device info of 'the transaction' is not null
    and (the device info of 'the transaction' has VPN detected
         or the device info of 'the transaction' has proxy detected
         or the device info of 'the transaction' has Tor detected)

  then
    make it true that the risk score of 'the transaction' has IP location mismatch detected ;
    add risk factor "Transaction uses VPN, proxy, or Tor - IP geolocation cannot be trusted" to the risk score of 'the transaction' ;
    add audit message "VPN_PROXY_TOR detected - IP anonymization tool in use" to 'the transaction' ;
```

**Conditions:**

- the device info of 'the transaction' is not null
- (the device info of 'the transaction' has VPN detected
         or the device info of 'the transaction' has proxy detected
         or the device info of 'the transaction' has Tor detected)

**Actions:**

- make it true that the risk score of 'the transaction' has IP location mismatch detected
- add risk factor "Transaction uses VPN, proxy, or Tor - IP geolocation cannot be trusted" to the risk score of 'the transaction'
- add audit message "VPN_PROXY_TOR detected - IP anonymization tool in use" to 'the transaction'

---

### Package: `risk-scoring`

#### Rule: `score-card-presence-mismatch`

```
if
    the risk score of 'the transaction' has card presence mismatch detected

  then
    record score 15 on the risk score of 'the transaction' ;
    record triggered rule "CARD_PRESENCE_MISMATCH" on the risk score of 'the transaction' ;
    add audit message "Risk score +15: Card presence location mismatch detected" to 'the transaction' ;
```

**Conditions:**

- the risk score of 'the transaction' has card presence mismatch detected

**Actions:**

- record score 15 on the risk score of 'the transaction'
- record triggered rule "CARD_PRESENCE_MISMATCH" on the risk score of 'the transaction'
- add audit message "Risk score +15: Card presence location mismatch detected" to 'the transaction'

---

#### Rule: `score-high-risk-country`

```
if
    the risk score of 'the transaction' has high risk country detected

  then
    record score 30 on the risk score of 'the transaction' ;
    record triggered rule "HIGH_RISK_COUNTRY" on the risk score of 'the transaction' ;
    add audit message "Risk score +30: High-risk or sanctioned country detected" to 'the transaction' ;
```

**Conditions:**

- the risk score of 'the transaction' has high risk country detected

**Actions:**

- record score 30 on the risk score of 'the transaction'
- record triggered rule "HIGH_RISK_COUNTRY" on the risk score of 'the transaction'
- add audit message "Risk score +30: High-risk or sanctioned country detected" to 'the transaction'

---

#### Rule: `score-impossible-travel`

```
if
    the risk score of 'the transaction' has impossible travel detected

  then
    record score 40 on the risk score of 'the transaction' ;
    record triggered rule "IMPOSSIBLE_TRAVEL" on the risk score of 'the transaction' ;
    add audit message "Risk score +40: Impossible travel detected" to 'the transaction' ;
```

**Conditions:**

- the risk score of 'the transaction' has impossible travel detected

**Actions:**

- record score 40 on the risk score of 'the transaction'
- record triggered rule "IMPOSSIBLE_TRAVEL" on the risk score of 'the transaction'
- add audit message "Risk score +40: Impossible travel detected" to 'the transaction'

---

#### Rule: `score-ip-location-mismatch`

```
if
    the risk score of 'the transaction' has IP location mismatch detected

  then
    record score 25 on the risk score of 'the transaction' ;
    record triggered rule "IP_LOCATION_MISMATCH" on the risk score of 'the transaction' ;
    add audit message "Risk score +25: IP location mismatch or anonymization detected" to 'the transaction' ;
```

**Conditions:**

- the risk score of 'the transaction' has IP location mismatch detected

**Actions:**

- record score 25 on the risk score of 'the transaction'
- record triggered rule "IP_LOCATION_MISMATCH" on the risk score of 'the transaction'
- add audit message "Risk score +25: IP location mismatch or anonymization detected" to 'the transaction'

---

#### Rule: `score-unusual-corridor`

```
if
    the risk score of 'the transaction' has unusual corridor detected

  then
    record score 20 on the risk score of 'the transaction' ;
    record triggered rule "UNUSUAL_CORRIDOR" on the risk score of 'the transaction' ;
    add audit message "Risk score +20: Unusual transaction corridor detected" to 'the transaction' ;
```

**Conditions:**

- the risk score of 'the transaction' has unusual corridor detected

**Actions:**

- record score 20 on the risk score of 'the transaction'
- record triggered rule "UNUSUAL_CORRIDOR" on the risk score of 'the transaction'
- add audit message "Risk score +20: Unusual transaction corridor detected" to 'the transaction'

---

#### Rule: `score-velocity-anomaly`

```
if
    the risk score of 'the transaction' has velocity anomaly detected

  then
    record score 20 on the risk score of 'the transaction' ;
    record triggered rule "VELOCITY_ANOMALY" on the risk score of 'the transaction' ;
    add audit message "Risk score +20: Multi-country velocity anomaly detected" to 'the transaction' ;
```

**Conditions:**

- the risk score of 'the transaction' has velocity anomaly detected

**Actions:**

- record score 20 on the risk score of 'the transaction'
- record triggered rule "VELOCITY_ANOMALY" on the risk score of 'the transaction'
- add audit message "Risk score +20: Multi-country velocity anomaly detected" to 'the transaction'

---

#### Rule: `set-confidence-level`

```
if
    the total score of the risk score of 'the transaction' is at least 60

  then
    set the confidence level of the risk score of 'the transaction' to "HIGH" ;
    add audit message "Confidence level set to HIGH - total risk score >= 60" to 'the transaction' ;
```

**Conditions:**

- the total score of the risk score of 'the transaction' is at least 60

**Actions:**

- set the confidence level of the risk score of 'the transaction' to "HIGH"
- add audit message "Confidence level set to HIGH - total risk score >= 60" to 'the transaction'

---


## 6. Decision Tables

*No decision tables found in this project.*


## 7. Rule Variables

### Geolocation Fraud DetectionParameters

| Variable Name | Type | Verbalization |
|---------------|------|---------------|
| `transaction` | `com.fraud.geolocation.Transaction` | the transaction |


## 8. Business Object Model (BOM)

### geolocation-fraud

**Package:** `com.fraud.geolocation`

#### Class: `Customer`

**Properties:**

- `public int accountAgeDays`
- `public double averageTransactionAmount`
- `public string customerId`
- `public string firstName`
- `public boolean highValueCustomer`
- `public string homeCountry`
- `public string homeCountryCode`
- `public com.fraud.geolocation.Location homeLocation`
- `public string lastName`
- `public int totalTransactionCount`

**Methods:**

- `void addKnownTransactionCountry(string countryCode)`
- `void addTravelNotification(string countryCode)`
- `boolean hasTransactionHistoryIn(string countryCode)`
- `boolean hasTravelNotificationFor(string countryCode)`

#### Class: `DeviceInfo`

**Properties:**

- `public string deviceFingerprint`
- `public string deviceType`
- `public string ipAddress`
- `public com.fraud.geolocation.Location ipGeolocation`
- `public boolean knownDevice`
- `public boolean proxyDetected`
- `public boolean torDetected`
- `public string userAgent`
- `public boolean vpnDetected`

**Methods:**

- `double ipToMerchantDistance(com.fraud.geolocation.Location merchantLocation)`

#### Class: `Location`

**Properties:**

- `public string city`
- `public string country`
- `public string countryCode`
- `public double latitude`
- `public double longitude`
- `public string postalCode`
- `public string region`

**Methods:**

- `double distanceTo(com.fraud.geolocation.Location other)`

#### Class: `RiskScore`

**Properties:**

- `public boolean cardPresenceMismatchDetected`
- `public string confidenceLevel`
- `public boolean highRiskCountryDetected`
- `public boolean impossibleTravelDetected`
- `public boolean ipLocationMismatchDetected`
- `public int maxScore`
- `public int totalScore`
- `public boolean unusualCorridorDetected`
- `public boolean velocityAnomalyDetected`

**Methods:**

- `void addRiskFactor(string factor)`
- `void addScore(int points, string ruleName)`
- `void addScore(int points)`
- `void addTriggeredRule(string ruleName)`
- `boolean exceedsThreshold(int threshold)`

#### Class: `Transaction`

**Properties:**

- `public double amount`
- `public string cardNumber`
- `public boolean cardPresent`
- `public string cardType`
- `public string currency`
- `public com.fraud.geolocation.Customer customer`
- `public string decisionReasonCode`
- `public com.fraud.geolocation.DeviceInfo deviceInfo`
- `public int distinctCountriesLast24Hours`
- `public int distinctCountriesLast7Days`
- `public string fraudDecision`
- `public string merchantCategory`
- `public string merchantId`
- `public com.fraud.geolocation.Location merchantLocation`
- `public string merchantName`
- `public com.fraud.geolocation.Location previousTransactionLocation`
- `public long previousTransactionTimestampMs`
- `public string recommendedAction`
- `public com.fraud.geolocation.RiskScore riskScore`
- `public double totalAmountLast1Hour`
- `public double totalAmountLast24Hours`
- `public int transactionCountLast1Hour`
- `public int transactionCountLast24Hours`
- `public string transactionId`
- `public long transactionTimestampMs`

**Methods:**

- `void addAuditMessage(string message)`


## 9. Business Vocabulary

### geolocation-fraud_en_US

#### Customer - *customer*

| Property | Type | Phrase |
|----------|------|--------|
| `accountAgeDays` | Action | set the account age in days of {this} to {account age in days} |
| `accountAgeDays` | Navigation | {account age in days} of {this} |
| `averageTransactionAmount` | Action | set the average transaction amount of {this} to {average transaction amount} |
| `averageTransactionAmount` | Navigation | {average transaction amount} of {this} |
| `customerId` | Action | set the customer ID of {this} to {customer ID} |
| `customerId` | Navigation | {customer ID} of {this} |
| `firstName` | Action | set the first name of {this} to {first name} |
| `firstName` | Navigation | {first name} of {this} |
| `highValueCustomer` | Action | make it {high value customer} that {this} is a high value customer |
| `highValueCustomer` | Navigation | {this} is a high value customer |
| `homeCountry` | Action | set the home country of {this} to {home country} |
| `homeCountry` | Navigation | {home country} of {this} |
| `homeCountryCode` | Action | set the home country code of {this} to {home country code} |
| `homeCountryCode` | Navigation | {home country code} of {this} |
| `homeLocation` | Action | set the home location of {this} to {home location} |
| `homeLocation` | Navigation | {home location} of {this} |
| `knownTransactionCountries` | Navigation | {known transaction countries} of {this} |
| `lastName` | Action | set the last name of {this} to {last name} |
| `lastName` | Navigation | {last name} of {this} |
| `totalTransactionCount` | Action | set the total transaction count of {this} to {total transaction count} |
| `totalTransactionCount` | Navigation | {total transaction count} of {this} |
| `travelNotifications` | Navigation | {travel notifications} of {this} |

#### DeviceInfo - *device info*

| Property | Type | Phrase |
|----------|------|--------|
| `deviceFingerprint` | Action | set the device fingerprint of {this} to {device fingerprint} |
| `deviceFingerprint` | Navigation | {device fingerprint} of {this} |
| `deviceType` | Action | set the device type of {this} to {device type} |
| `deviceType` | Navigation | {device type} of {this} |
| `ipAddress` | Action | set the IP address of {this} to {IP address} |
| `ipAddress` | Navigation | {IP address} of {this} |
| `ipGeolocation` | Action | set the IP geolocation of {this} to {IP geolocation} |
| `ipGeolocation` | Navigation | {IP geolocation} of {this} |
| `knownDevice` | Action | make it {known device} that {this} is a known device |
| `knownDevice` | Navigation | {this} is a known device |
| `proxyDetected` | Action | make it {proxy detected} that {this} has proxy detected |
| `proxyDetected` | Navigation | {this} has proxy detected |
| `torDetected` | Action | make it {tor detected} that {this} has Tor detected |
| `torDetected` | Navigation | {this} has Tor detected |
| `userAgent` | Action | set the user agent of {this} to {user agent} |
| `userAgent` | Navigation | {user agent} of {this} |
| `vpnDetected` | Action | make it {vpn detected} that {this} has VPN detected |
| `vpnDetected` | Navigation | {this} has VPN detected |

#### Location - *location*

| Property | Type | Phrase |
|----------|------|--------|
| `city` | Action | set the city of {this} to {city} |
| `city` | Navigation | {city} of {this} |
| `country` | Action | set the country of {this} to {country} |
| `country` | Navigation | {country} of {this} |
| `countryCode` | Action | set the country code of {this} to {country code} |
| `countryCode` | Navigation | {country code} of {this} |
| `latitude` | Action | set the latitude of {this} to {latitude} |
| `latitude` | Navigation | {latitude} of {this} |
| `longitude` | Action | set the longitude of {this} to {longitude} |
| `longitude` | Navigation | {longitude} of {this} |
| `postalCode` | Action | set the postal code of {this} to {postal code} |
| `postalCode` | Navigation | {postal code} of {this} |
| `region` | Action | set the region of {this} to {region} |
| `region` | Navigation | {region} of {this} |

#### RiskScore - *risk score*

| Property | Type | Phrase |
|----------|------|--------|
| `cardPresenceMismatchDetected` | Action | make it {card presence mismatch detected} that {this} has card presence mismatch detected |
| `cardPresenceMismatchDetected` | Navigation | {this} has card presence mismatch detected |
| `confidenceLevel` | Action | set the confidence level of {this} to {confidence level} |
| `confidenceLevel` | Navigation | {confidence level} of {this} |
| `highRiskCountryDetected` | Action | make it {high risk country detected} that {this} has high risk country detected |
| `highRiskCountryDetected` | Navigation | {this} has high risk country detected |
| `impossibleTravelDetected` | Action | make it {impossible travel detected} that {this} has impossible travel detected |
| `impossibleTravelDetected` | Navigation | {this} has impossible travel detected |
| `ipLocationMismatchDetected` | Action | make it {ip location mismatch detected} that {this} has IP location mismatch detected |
| `ipLocationMismatchDetected` | Navigation | {this} has IP location mismatch detected |
| `maxScore` | Action | set the max score of {this} to {max score} |
| `maxScore` | Navigation | {max score} of {this} |
| `normalizedScore` | Navigation | {normalized score} of {this} |
| `riskFactors` | Navigation | {risk factors} of {this} |
| `totalScore` | Action | set the total score of {this} to {total score} |
| `totalScore` | Navigation | {total score} of {this} |
| `triggeredRules` | Navigation | {triggered rules} of {this} |
| `unusualCorridorDetected` | Action | make it {unusual corridor detected} that {this} has unusual corridor detected |
| `unusualCorridorDetected` | Navigation | {this} has unusual corridor detected |
| `velocityAnomalyDetected` | Action | make it {velocity anomaly detected} that {this} has velocity anomaly detected |
| `velocityAnomalyDetected` | Navigation | {this} has velocity anomaly detected |

#### Transaction - *transaction*

| Property | Type | Phrase |
|----------|------|--------|
| `amount` | Action | set the amount of {this} to {amount} |
| `amount` | Navigation | {amount} of {this} |
| `auditMessages` | Navigation | {audit messages} of {this} |
| `cardNumber` | Action | set the card number of {this} to {card number} |
| `cardNumber` | Navigation | {card number} of {this} |
| `cardPresent` | Action | set the card present of {this} to {card present} |
| `cardPresent` | Navigation | {card present} of {this} |
| `cardType` | Action | set the card type of {this} to {card type} |
| `cardType` | Navigation | {card type} of {this} |
| `currency` | Action | set the currency of {this} to {currency} |
| `currency` | Navigation | {currency} of {this} |
| `customer` | Action | set the customer of {this} to {customer} |
| `customer` | Navigation | {customer} of {this} |
| `decisionReasonCode` | Action | set the decision reason code of {this} to {decision reason code} |
| `decisionReasonCode` | Navigation | {decision reason code} of {this} |
| `deviceInfo` | Action | set the device info of {this} to {device info} |
| `deviceInfo` | Navigation | {device info} of {this} |
| `distinctCountriesLast24Hours` | Action | set the distinct countries in last 24 hours of {this} to {distinct countries in last 24 hours} |
| `distinctCountriesLast24Hours` | Navigation | {distinct countries in last 24 hours} of {this} |
| `distinctCountriesLast7Days` | Action | set the distinct countries in last 7 days of {this} to {distinct countries in last 7 days} |
| `distinctCountriesLast7Days` | Navigation | {distinct countries in last 7 days} of {this} |
| `distanceFromPreviousTransactionKm` | Navigation | {prior distance} of {this} |
| `fraudDecision` | Action | set the fraud decision of {this} to {fraud decision} |
| `fraudDecision` | Navigation | {fraud decision} of {this} |
| `inKnownTransactionCorridor` | Navigation | {this} is whitelisted |
| `merchantCategory` | Action | set the merchant category of {this} to {merchant category} |
| `merchantCategory` | Navigation | {merchant category} of {this} |
| `merchantId` | Action | set the merchant ID of {this} to {merchant ID} |
| `merchantId` | Navigation | {merchant ID} of {this} |
| `merchantLocation` | Action | set the merchant location of {this} to {merchant location} |
| `merchantLocation` | Navigation | {merchant location} of {this} |
| `merchantName` | Action | set the merchant name of {this} to {merchant name} |
| `merchantName` | Navigation | {merchant name} of {this} |
| `previousTransactionLocation` | Action | set the previous transaction location of {this} to {previous transaction location} |
| `previousTransactionLocation` | Navigation | {previous transaction location} of {this} |
| `previousTransactionTimestampMs` | Action | set the previous transaction timestamp of {this} to {previous transaction timestamp} |
| `previousTransactionTimestampMs` | Navigation | {previous transaction timestamp} of {this} |
| `recommendedAction` | Action | set the recommended action of {this} to {recommended action} |
| `recommendedAction` | Navigation | {recommended action} of {this} |
| `riskScore` | Action | set the risk score of {this} to {risk score} |
| `riskScore` | Navigation | {risk score} of {this} |
| `totalAmountLast1Hour` | Action | set the total amount in last 1 hour of {this} to {total amount in last 1 hour} |
| `totalAmountLast1Hour` | Navigation | {total amount in last 1 hour} of {this} |
| `totalAmountLast24Hours` | Action | set the total amount in last 24 hours of {this} to {total amount in last 24 hours} |
| `totalAmountLast24Hours` | Navigation | {total amount in last 24 hours} of {this} |
| `transactionCountLast1Hour` | Action | set the transaction count in last 1 hour of {this} to {transaction count in last 1 hour} |
| `transactionCountLast1Hour` | Navigation | {transaction count in last 1 hour} of {this} |
| `transactionCountLast24Hours` | Action | set the transaction count in last 24 hours of {this} to {transaction count in last 24 hours} |
| `transactionCountLast24Hours` | Navigation | {transaction count in last 24 hours} of {this} |
| `transactionId` | Action | set the transaction ID of {this} to {transaction ID} |
| `transactionId` | Navigation | {transaction ID} of {this} |
| `transactionTimestampMs` | Action | set the transaction timestamp of {this} to {transaction timestamp} |
| `transactionTimestampMs` | Navigation | {transaction timestamp} of {this} |


---

*Report generated by ODM Report Generator*
