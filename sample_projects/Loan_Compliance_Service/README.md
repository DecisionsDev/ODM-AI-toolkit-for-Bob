Generated with the prompt :

create an ODM rule project based on a decision table using the https://raw.githubusercontent.com/DecisionsDev/policy-corpus/refs/heads/main/loan/loan_compliance/loan_policy_test_dataset_100.csv file

Version v1.0.0 of the bob mode
# ODM Rule Project Documentation

**Project:** Loan Compliance Service

**Generated:** 2026-03-06 09:53:45

---

## 1. Project Overview

- **Project Name:** Loan Compliance Service
- **Project UUID:** a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d
- **Project Type:** Decision Service


## 2. Quality Assessment Summary

### Quality Metrics

- **Rule Documentation:** 0.0% (0/4) - Score: 0.0/20
- **Rule Complexity:** 100.0% simple rules (4/4) - Score: 20.0/20
- **Project Organization:** 1 packages - Score: 20/20
- **BOM Coverage:** 3 classes defined - Score: 15/20
- **Vocabulary Coverage:** Score: 0/20

### Overall Quality Score

**55.0%** (55.0/100) - Grade: **F**

**Quality Interpretation:**

⚠️ **Poor** - Project needs significant improvements in documentation and organization.

### Recommendations

- 📝 **Add Documentation**: Less than 50% of rules have documentation. Add meaningful descriptions to help users understand rule purpose and business logic.
- 📁 **Consider More Packages**: Only one rule package found. Consider organizing rules into multiple packages by business domain or functionality.
- ✅ **Regular Reviews**: Conduct periodic code reviews to maintain quality standards.
- 🧪 **Add Test Cases**: Ensure comprehensive test coverage for all rules and decision tables.
- 📖 **Update Vocabulary**: Keep business vocabulary aligned with domain expert terminology.

---

## 3. Deployment Configuration

### Operation: `LoanComplianceOperation`

- **Ruleset Name:** `Loan_Compliance_Service_Ruleset`
- **Using Ruleflow:** true
- **Ruleflow Name:** `loan-compliance-ruleflow`

**Input/Output Parameters:**

| Variable | Type | Direction |
|----------|------|-----------|
| `request` | `LoanComplianceParameters` | IN_OUT |


## 4. Ruleflow

### loan-compliance-ruleflow

**Execution Flow:**

| Step | Task | Execution Mode | Rule Package |
|------|------|----------------|--------------|
| 1 | Validation | `Fastpath` | `validation` |
| 2 | Pricing | `RetePlus` | `pricing` |

**Execution Modes:**

- **Fastpath**: Sequential rule execution where order matters. Rules are evaluated in the order they appear.
- **RetePlus**: Rete algorithm-based execution with pattern matching. Order-independent evaluation.


## 5. Business Rules

### Package: `pricing`

*No rules found in this package.*

### Package: `validation`

#### Rule: `approve-eligible-loan`

```
if
    'the request' is eligible
    and the interest rate of 'the request' is more than 0
then
    set the reason of 'the request' to "Loan approved with " + the interest rate of 'the request' + "% APR." ;
    add "Loan application approved" to the messages of 'the request' ;
```

**Conditions:**

- 'the request' is eligible
- the interest rate of 'the request' is more than 0

**Actions:**

- set the reason of 'the request' to "Loan approved with " + the interest rate of 'the request' + "% APR."
- add "Loan application approved" to the messages of 'the request'

---

#### Rule: `check-age`

```
if
    the applicant of 'the request' is not null
    and the age of the applicant of 'the request' is less than 18
then
    make it false that 'the request' is eligible ;
    set the reason of 'the request' to "Applicant must be at least 18 years old" ;
    add "Age requirement not met - applicant is under 18" to the violations of 'the request' ;
```

**Conditions:**

- the applicant of 'the request' is not null
- the age of the applicant of 'the request' is less than 18

**Actions:**

- make it false that 'the request' is eligible
- set the reason of 'the request' to "Applicant must be at least 18 years old"
- add "Age requirement not met - applicant is under 18" to the violations of 'the request'

---

#### Rule: `check-debt-to-income`

```
if
    the applicant of 'the request' is not null
    and the debt to income ratio of the applicant of 'the request' is more than 40
then
    make it false that 'the request' is eligible ;
    set the reason of 'the request' to "Applicant's debt-to-income ratio must not exceed 40%" ;
    add "Debt-to-income ratio exceeds 40% threshold" to the violations of 'the request' ;
```

**Conditions:**

- the applicant of 'the request' is not null
- the debt to income ratio of the applicant of 'the request' is more than 40

**Actions:**

- make it false that 'the request' is eligible
- set the reason of 'the request' to "Applicant's debt-to-income ratio must not exceed 40%"
- add "Debt-to-income ratio exceeds 40% threshold" to the violations of 'the request'

---

#### Rule: `initialize-eligibility`

```
if
    the applicant of 'the request' is not null
then
    make it true that 'the request' is eligible ;
```

**Conditions:**

- the applicant of 'the request' is not null

**Actions:**

- make it true that 'the request' is eligible

---


## 6. Decision Tables

### Decision Table: `interest-rate-by-credit-score`


## 7. Rule Variables

### LoanComplianceParameters

| Variable Name | Type | Verbalization |
|---------------|------|---------------|
| `request` | `com.loan.compliance.LoanRequest` | the request |


## 8. Business Object Model (BOM)

### loan-compliance

**Package:** `com.loan.compliance`

#### Class: `Address`

**Properties:**

- `public string country`
- `public string state`
- `public string city`
- `public string zipCode`

#### Class: `Applicant`

**Properties:**

- `public java.util.Date birthDate`
- `public com.loan.compliance.Address address`
- `public int creditScore`
- `public double annualIncome`
- `public string incomeDocument`
- `public string employmentStatus`
- `public boolean financialRecordPresent`
- `public double monthlyDebtAmount`
- `public double monthlyGrossIncome`

#### Class: `LoanRequest`

**Properties:**

- `public com.loan.compliance.Applicant applicant`
- `public com.loan.compliance.Applicant coSigner`
- `public double loanAmount`
- `public boolean eligible`
- `public double interestRate`
- `public string reason`

**Methods:**

- `void addMessage(string arg)`
- `void addViolation(string arg)`


## 9. Business Vocabulary

### loan-compliance_en_US

#### Address - *address*

| Property | Type | Phrase |
|----------|------|--------|
| `country` | Navigation | {country} of {this} |
| `country` | Action | set the country of {this} to {country} |
| `state` | Navigation | {state} of {this} |
| `state` | Action | set the state of {this} to {state} |
| `city` | Navigation | {city} of {this} |
| `city` | Action | set the city of {this} to {city} |
| `zipCode` | Navigation | {zip code} of {this} |
| `zipCode` | Action | set the zip code of {this} to {zip code} |

#### Applicant - *applicant*

| Property | Type | Phrase |
|----------|------|--------|
| `birthDate` | Navigation | {birth date} of {this} |
| `birthDate` | Action | set the birth date of {this} to {birth date} |
| `address` | Navigation | {address} of {this} |
| `address` | Action | set the address of {this} to {address} |
| `creditScore` | Navigation | {credit score} of {this} |
| `creditScore` | Action | set the credit score of {this} to {credit score} |
| `annualIncome` | Navigation | {annual income} of {this} |
| `annualIncome` | Action | set the annual income of {this} to {annual income} |
| `incomeDocument` | Navigation | {income document} of {this} |
| `incomeDocument` | Action | set the income document of {this} to {income document} |
| `employmentStatus` | Navigation | {employment status} of {this} |
| `employmentStatus` | Action | set the employment status of {this} to {employment status} |
| `financialRecordPresent` | Navigation | {this} has financial record |
| `financialRecordPresent` | Action | make it {financial record present} that {this} has financial record |
| `monthlyDebtAmount` | Navigation | {monthly debt amount} of {this} |
| `monthlyDebtAmount` | Action | set the monthly debt amount of {this} to {monthly debt amount} |
| `monthlyGrossIncome` | Navigation | {monthly gross income} of {this} |
| `monthlyGrossIncome` | Action | set the monthly gross income of {this} to {monthly gross income} |
| `age` | Navigation | {age} of {this} |
| `debtToIncomeRatio` | Navigation | {debt to income ratio} of {this} |

#### LoanRequest - *loan request*

| Property | Type | Phrase |
|----------|------|--------|
| `applicant` | Navigation | {applicant} of {this} |
| `applicant` | Action | set the applicant of {this} to {applicant} |
| `coSigner` | Navigation | {co-signer} of {this} |
| `coSigner` | Action | set the co-signer of {this} to {co-signer} |
| `loanAmount` | Navigation | {loan amount} of {this} |
| `loanAmount` | Action | set the loan amount of {this} to {loan amount} |
| `eligible` | Navigation | {this} is eligible |
| `eligible` | Action | make it {eligible} that {this} is eligible |
| `interestRate` | Navigation | {interest rate} of {this} |
| `interestRate` | Action | set the interest rate of {this} to {interest rate} |
| `reason` | Navigation | {reason} of {this} |
| `reason` | Action | set the reason of {this} to {reason} |
| `messages` | Navigation | {messages} of {this} |
| `violations` | Navigation | {violations} of {this} |
| `hasCoSigner` | Navigation | {this} has co-signer |
| `combinedCreditScore` | Navigation | {combined credit score} of {this} |
| `combinedAnnualIncome` | Navigation | {combined annual income} of {this} |
| `loanToIncomeRatio` | Navigation | {loan to income ratio} of {this} |


---

*Report generated by ODM Report Generator*
