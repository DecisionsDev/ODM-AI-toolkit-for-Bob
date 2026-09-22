# AI_Context.md — Cardiovascular Risk Assessment

Part of the ODM examples in this repository. For format-level explanations (what each file type is, the syntax rules, the error table) read the root [AI_Context.md](../../AI_Context.md); this file describes only this project.

## Purpose

- Rule project folder: `Cardiovascular Risk Assessment/` (name `Cardiovascular Risk Assessment`); XOM Java project: `cardiovascular-risk-xom/`.
- Source policy used to generate it: <https://raw.githubusercontent.com/DecisionsDev/policy-corpus/refs/heads/main/healthcare/cardiovascular_risk/cardiovascular_risk.txt>

## Build status

Builds successfully (`BUILD SUCCESS`) with IBM Semeru JDK 21 and `rules-compiler.jar`, XOM compiled from `src/`.

## Decision interface

- Variable `patient` of type `com.healthcare.cvd.Patient`, verbalized `the patient` (rules write `'the patient'`), direction `IN_OUT`.
- Operation `CardiovascularRiskAssessmentOperation`: ruleset `Cardiovascular_Risk_Assessment_Ruleset`, ruleflow `cardiovascular-risk-ruleflow`.
- Deployment config: `CardiovascularRiskAssessment.dep`, `dep` name `CardiovascularRiskAssessment`, RuleApp `Cardiovascular_Risk_Assessment`.

## XOM and BOM

Package `com.healthcare.cvd`. Business classes and their members (from `bom/cardiovascular-risk.bom`):

- **Patient**: age, sex, systolicBP, diastolicBP, ldlCholesterol, hdlCholesterol, triglycerides, smokingHistory, hasDiabetes, hasFamilyHistory, onBPMedication, onCholesterolMedication, riskLevel, messages, requiresManualReview; methods: addMessage, flagForManualReview

XOM sources: `src/com/healthcare/cvd/Patient.java`.

## Ruleflow (execution order)

| # | Rule package | Mode |
|---|---|---|
| 1 | validation | Fastpath |
| 2 | high-risk | RetePlus |
| 3 | medium-risk | Fastpath |
| 4 | low-risk | Fastpath |

Fastpath = sequential execution, no inference. RetePlus = pattern-matching inference where one rule's effect may trigger others.

## Rules by package

### `rules/high-risk/` — Rules for high risk classification
- `high-risk-age-bp.brl` (rule): high-risk-age-bp
- `high-risk-current-smoker.brl` (rule): high-risk-current-smoker
- `high-risk-diabetes.brl` (rule): high-risk-diabetes
- `high-risk-family-history.brl` (rule): high-risk-family-history
- `high-risk-ldl.brl` (rule): high-risk-ldl

### `rules/low-risk/` — Rules for low risk classification (default)
- `default-low-risk.brl` (rule): default-low-risk

### `rules/medium-risk/` — Rules for medium risk classification
- `medium-risk-age.brl` (rule): medium-risk-age
- `medium-risk-bp.brl` (rule): medium-risk-bp
- `medium-risk-former-smoker.brl` (rule): medium-risk-former-smoker
- `medium-risk-ldl.brl` (rule): medium-risk-ldl
- `medium-risk-low-hdl.brl` (rule): medium-risk-low-hdl

### `rules/validation/` — Rules for data validation and missing data handling
- `check-critical-fields.brl` (rule): check-critical-fields

## Representative rule

`rules/medium-risk/medium-risk-age.brl` — the BAL inside `<definition><![CDATA[ … ]]>`:

```
if
    the risk level of 'the patient' is ""
    and the age of 'the patient' is at least 45
    and the age of 'the patient' is at most 59
then
    set the risk level of 'the patient' to "Medium" ;
    add "Medium risk: Age between 45 and 59" to the messages of 'the patient' ;
```

## Design notes

- Severity tiers: `validation` → `high-risk` (RetePlus) → `medium-risk` → `low-risk`. `low-risk/default-low-risk.brl` is the fallback that fires last.
- Single business class `Patient`: `smokingHistory` is a string; clinical flags are booleans (`hasDiabetes`, `hasFamilyHistory`, `onBPMedication`, `onCholesterolMedication`, `requiresManualReview`), auto-mapped from getters/setters.
- The fallback works by state, not by a special construct: `default-low-risk.brl` tests `the risk level of 'the patient' is ""` (nothing assigned yet) and then sets `"Low"`. That is why the low-risk package must run last.

## File inventory

```
Cardiovascular Risk Assessment/.ruleproject
Cardiovascular Risk Assessment/bom/cardiovascular-risk.b2xa
Cardiovascular Risk Assessment/bom/cardiovascular-risk.bom
Cardiovascular Risk Assessment/bom/cardiovascular-risk_en_US.voc
Cardiovascular Risk Assessment/deployment/CardiovascularRiskAssessment.dep
Cardiovascular Risk Assessment/deployment/CardiovascularRiskAssessmentOperation.dop
Cardiovascular Risk Assessment/rules/CardiovascularRiskAssessmentParameters.var
Cardiovascular Risk Assessment/rules/cardiovascular-risk-ruleflow.rfl
Cardiovascular Risk Assessment/rules/high-risk/.rulepackage
Cardiovascular Risk Assessment/rules/high-risk/high-risk-age-bp.brl
Cardiovascular Risk Assessment/rules/high-risk/high-risk-current-smoker.brl
Cardiovascular Risk Assessment/rules/high-risk/high-risk-diabetes.brl
Cardiovascular Risk Assessment/rules/high-risk/high-risk-family-history.brl
Cardiovascular Risk Assessment/rules/high-risk/high-risk-ldl.brl
Cardiovascular Risk Assessment/rules/low-risk/.rulepackage
Cardiovascular Risk Assessment/rules/low-risk/default-low-risk.brl
Cardiovascular Risk Assessment/rules/medium-risk/.rulepackage
Cardiovascular Risk Assessment/rules/medium-risk/medium-risk-age.brl
Cardiovascular Risk Assessment/rules/medium-risk/medium-risk-bp.brl
Cardiovascular Risk Assessment/rules/medium-risk/medium-risk-former-smoker.brl
Cardiovascular Risk Assessment/rules/medium-risk/medium-risk-ldl.brl
Cardiovascular Risk Assessment/rules/medium-risk/medium-risk-low-hdl.brl
Cardiovascular Risk Assessment/rules/validation/.rulepackage
Cardiovascular Risk Assessment/rules/validation/check-critical-fields.brl
```
