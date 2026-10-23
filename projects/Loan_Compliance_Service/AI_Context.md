# AI_Context.md — Loan Compliance Service

Part of the ODM examples in this repository. For format-level explanations (what each file type is, the syntax rules, the error table) read the root [AI_Context.md](../../AI_Context.md); this file describes only this project.

## Purpose

- Rule project folder: `Loan Compliance Service/` (name `Loan Compliance Service`); XOM Java project: `loan-compliance-xom/`.
- Source policy used to generate it: <https://raw.githubusercontent.com/DecisionsDev/policy-corpus/refs/heads/main/loan/loan_compliance/loan_policy_test_dataset_100.csv>

## Build status

**FAILS** — no `.dep` file in `deployment/`, so the build reports `The deployment configuration named "…" was not found in rule project`. Rules, BOM and XOM were not otherwise validated. Its XOM still implements `Serializable` (older convention) and its `.voc` lists phrases (`hasCoSigner`, `combinedCreditScore`, `combinedAnnualIncome`, `loanToIncomeRatio`) whose members are not declared in the `.bom`.

## Decision interface

- Variable `request` of type `com.loan.compliance.LoanRequest`, verbalized `the request` (rules write `'the request'`), direction `IN_OUT`.
- Operation `LoanComplianceOperation`: ruleset `Loan_Compliance_Service_Ruleset`, ruleflow `loan-compliance-ruleflow`.
- Deployment config: none (missing).

## XOM and BOM

Package `com.loan.compliance`. Business classes and their members (from `bom/loan-compliance.bom`):

- **Address**: country, state, city, zipCode
- **Applicant**: birthDate, address, creditScore, annualIncome, incomeDocument, employmentStatus, financialRecordPresent, monthlyDebtAmount, monthlyGrossIncome; computed (`readonly`, `factory.ignore`): age, debtToIncomeRatio
- **LoanRequest**: applicant, coSigner, loanAmount, eligible, interestRate, reason, messages, violations; methods: addMessage, addViolation

XOM sources: `src/com/loan/compliance/Address.java`, `src/com/loan/compliance/Applicant.java`, `src/com/loan/compliance/LoanRequest.java`.

## Ruleflow (execution order)

| # | Rule package | Mode |
|---|---|---|
| 1 | validation | Fastpath |
| 2 | pricing | RetePlus |

Fastpath = sequential execution, no inference. RetePlus = pattern-matching inference where one rule's effect may trigger others.

## Rules by package

### `rules/pricing/` — Interest rate pricing rules based on credit score
- `interest-rate-by-credit-score.dta` (decision table): interest-rate-by-credit-score

### `rules/validation/` — Validation rules for loan eligibility
- `approve-eligible-loan.brl` (rule): approve-eligible-loan
- `check-age.brl` (rule): check-age
- `check-debt-to-income.brl` (rule): check-debt-to-income
- `initialize-eligibility.brl` (rule): initialize-eligibility

## Representative rule

`rules/validation/check-age.brl` — the BAL inside `<definition><![CDATA[ … ]]>`:

```
if
    the applicant of 'the request' is not null
    and the age of the applicant of 'the request' is less than 18
then
    make it false that 'the request' is eligible ;
    set the reason of 'the request' to "Applicant must be at least 18 years old" ;
    add "Age requirement not met - applicant is under 18" to the violations of 'the request' ;
```

## Design notes

- Generated from a decision-table prompt: pricing is a `.dta` decision table (`interest-rate-by-credit-score.dta`) with open-ended first/last rows and interval rows in between, setting `interest rate`.
- `validation` (Fastpath) initialises `eligible`, rejects on age < 18 and on debt-to-income, then approves; `pricing` (RetePlus) applies the table.
- Use it to learn the decision-table XML, but do not copy its missing `.dep` or its `Serializable` XOM.

## File inventory

```
Loan Compliance Service/.ruleproject
Loan Compliance Service/bom/loan-compliance.b2xa
Loan Compliance Service/bom/loan-compliance.bom
Loan Compliance Service/bom/loan-compliance_en_US.voc
Loan Compliance Service/deployment/LoanComplianceOperation.dop
Loan Compliance Service/rules/LoanComplianceParameters.var
Loan Compliance Service/rules/loan-compliance-ruleflow.rfl
Loan Compliance Service/rules/pricing/.rulepackage
Loan Compliance Service/rules/pricing/interest-rate-by-credit-score.dta
Loan Compliance Service/rules/validation/.rulepackage
Loan Compliance Service/rules/validation/approve-eligible-loan.brl
Loan Compliance Service/rules/validation/check-age.brl
Loan Compliance Service/rules/validation/check-debt-to-income.brl
Loan Compliance Service/rules/validation/initialize-eligibility.brl
```
