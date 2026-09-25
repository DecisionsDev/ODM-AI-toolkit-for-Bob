Generated with the prompt :

Create an ODM rule project based on https://raw.githubusercontent.com/DecisionsDev/policy-corpus/refs/heads/main/healthcare/cardiovascular_risk/cardiovascular_risk.txt

Version v1.0.0 of the bob mode
# ODM Rule Project Documentation

**Project:** Cardiovascular Risk Assessment

**Generated:** 2026-03-06 09:53:35

---

## 1. Project Overview

- **Project Name:** Cardiovascular Risk Assessment
- **Project UUID:** a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d
- **Project Type:** Decision Service


## 2. Quality Assessment Summary

### Quality Metrics

- **Rule Documentation:** 0.0% (0/12) - Score: 0.0/20
- **Rule Complexity:** 100.0% simple rules (12/12) - Score: 20.0/20
- **Project Organization:** 4 packages - Score: 20/20
- **BOM Coverage:** 1 classes defined - Score: 5/20
- **Vocabulary Coverage:** Score: 0/20

### Overall Quality Score

**45.0%** (45.0/100) - Grade: **F**

**Quality Interpretation:**

⚠️ **Poor** - Project needs significant improvements in documentation and organization.

### Recommendations

- 📝 **Add Documentation**: Less than 50% of rules have documentation. Add meaningful descriptions to help users understand rule purpose and business logic.
- 🏗️ **Expand BOM**: Consider adding more business object classes to better represent your domain model.
- 📊 **Consider Decision Tables**: For rules with similar structure, consider using decision tables for better readability and maintenance.
- ✅ **Regular Reviews**: Conduct periodic code reviews to maintain quality standards.
- 🧪 **Add Test Cases**: Ensure comprehensive test coverage for all rules and decision tables.
- 📖 **Update Vocabulary**: Keep business vocabulary aligned with domain expert terminology.

---

## 3. Deployment Configuration

### Operation: `CardiovascularRiskAssessmentOperation`

- **Ruleset Name:** `Cardiovascular_Risk_Assessment_Ruleset`
- **Using Ruleflow:** true
- **Ruleflow Name:** `cardiovascular-risk-ruleflow`

**Input/Output Parameters:**

| Variable | Type | Direction |
|----------|------|-----------|
| `patient` | `CardiovascularRiskAssessmentParameters` | IN_OUT |


## 4. Ruleflow

### cardiovascular-risk-ruleflow

**Execution Flow:**

| Step | Task | Execution Mode | Rule Package |
|------|------|----------------|--------------|
| 1 | Validation | `Fastpath` | `validation` |
| 2 | High Risk Classification | `RetePlus` | `high-risk` |
| 3 | Medium Risk Classification | `Fastpath` | `medium-risk` |
| 4 | Low Risk Classification | `Fastpath` | `low-risk` |

**Execution Modes:**

- **Fastpath**: Sequential rule execution where order matters. Rules are evaluated in the order they appear.
- **RetePlus**: Rete algorithm-based execution with pattern matching. Order-independent evaluation.


## 5. Business Rules

### Package: `high-risk`

#### Rule: `high-risk-age-bp`

```
if
    the age of 'the patient' is at least 60
    and the systolic blood pressure of 'the patient' is at least 140
then
    set the risk level of 'the patient' to "High" ;
    add "High risk: Age 60+ with systolic BP >= 140 mmHg" to the messages of 'the patient' ;
```

**Conditions:**

- the age of 'the patient' is at least 60
- the systolic blood pressure of 'the patient' is at least 140

**Actions:**

- set the risk level of 'the patient' to "High"
- add "High risk: Age 60+ with systolic BP >= 140 mmHg" to the messages of 'the patient'

---

#### Rule: `high-risk-current-smoker`

```
if
    the smoking history of 'the patient' is "current"
then
    set the risk level of 'the patient' to "High" ;
    add "High risk: Current smoker" to the messages of 'the patient' ;
```

**Conditions:**

- the smoking history of 'the patient' is "current"

**Actions:**

- set the risk level of 'the patient' to "High"
- add "High risk: Current smoker" to the messages of 'the patient'

---

#### Rule: `high-risk-diabetes`

```
if
    'the patient' has diabetes
then
    set the risk level of 'the patient' to "High" ;
    add "High risk: Confirmed diabetes diagnosis" to the messages of 'the patient' ;
```

**Conditions:**

- 'the patient' has diabetes

**Actions:**

- set the risk level of 'the patient' to "High"
- add "High risk: Confirmed diabetes diagnosis" to the messages of 'the patient'

---

#### Rule: `high-risk-family-history`

```
if
    'the patient' has family history of CVD
then
    set the risk level of 'the patient' to "High" ;
    add "High risk: Family history of cardiovascular disease" to the messages of 'the patient' ;
```

**Conditions:**

- 'the patient' has family history of CVD

**Actions:**

- set the risk level of 'the patient' to "High"
- add "High risk: Family history of cardiovascular disease" to the messages of 'the patient'

---

#### Rule: `high-risk-ldl`

```
if
    the LDL cholesterol of 'the patient' is at least 160
then
    set the risk level of 'the patient' to "High" ;
    add "High risk: LDL cholesterol >= 160 mg/dL" to the messages of 'the patient' ;
```

**Conditions:**

- the LDL cholesterol of 'the patient' is at least 160

**Actions:**

- set the risk level of 'the patient' to "High"
- add "High risk: LDL cholesterol >= 160 mg/dL" to the messages of 'the patient'

---

### Package: `low-risk`

#### Rule: `default-low-risk`

```
if
    the risk level of 'the patient' is ""
then
    set the risk level of 'the patient' to "Low" ;
    add "Low risk: No high or medium risk factors identified" to the messages of 'the patient' ;
```

**Conditions:**

- the risk level of 'the patient' is ""

**Actions:**

- set the risk level of 'the patient' to "Low"
- add "Low risk: No high or medium risk factors identified" to the messages of 'the patient'

---

### Package: `medium-risk`

#### Rule: `medium-risk-age`

```
if
    the risk level of 'the patient' is ""
    and the age of 'the patient' is at least 45
    and the age of 'the patient' is at most 59
then
    set the risk level of 'the patient' to "Medium" ;
    add "Medium risk: Age between 45 and 59" to the messages of 'the patient' ;
```

**Conditions:**

- the risk level of 'the patient' is ""
- the age of 'the patient' is at least 45
- the age of 'the patient' is at most 59

**Actions:**

- set the risk level of 'the patient' to "Medium"
- add "Medium risk: Age between 45 and 59" to the messages of 'the patient'

---

#### Rule: `medium-risk-bp`

```
if
    the risk level of 'the patient' is ""
    and the systolic blood pressure of 'the patient' is at least 130
    and the systolic blood pressure of 'the patient' is at most 139
then
    set the risk level of 'the patient' to "Medium" ;
    add "Medium risk: Systolic BP between 130 and 139 mmHg" to the messages of 'the patient' ;
```

**Conditions:**

- the risk level of 'the patient' is ""
- the systolic blood pressure of 'the patient' is at least 130
- the systolic blood pressure of 'the patient' is at most 139

**Actions:**

- set the risk level of 'the patient' to "Medium"
- add "Medium risk: Systolic BP between 130 and 139 mmHg" to the messages of 'the patient'

---

#### Rule: `medium-risk-former-smoker`

```
if
    the risk level of 'the patient' is ""
    and the smoking history of 'the patient' is "former"
then
    set the risk level of 'the patient' to "Medium" ;
    add "Medium risk: Former smoker" to the messages of 'the patient' ;
```

**Conditions:**

- the risk level of 'the patient' is ""
- the smoking history of 'the patient' is "former"

**Actions:**

- set the risk level of 'the patient' to "Medium"
- add "Medium risk: Former smoker" to the messages of 'the patient'

---

#### Rule: `medium-risk-ldl`

```
if
    the risk level of 'the patient' is ""
    and the LDL cholesterol of 'the patient' is at least 130
    and the LDL cholesterol of 'the patient' is at most 159
then
    set the risk level of 'the patient' to "Medium" ;
    add "Medium risk: LDL cholesterol between 130 and 159 mg/dL" to the messages of 'the patient' ;
```

**Conditions:**

- the risk level of 'the patient' is ""
- the LDL cholesterol of 'the patient' is at least 130
- the LDL cholesterol of 'the patient' is at most 159

**Actions:**

- set the risk level of 'the patient' to "Medium"
- add "Medium risk: LDL cholesterol between 130 and 159 mg/dL" to the messages of 'the patient'

---

#### Rule: `medium-risk-low-hdl`

```
if
    the risk level of 'the patient' is ""
    and the HDL cholesterol of 'the patient' is less than 40
then
    set the risk level of 'the patient' to "Medium" ;
    add "Medium risk: HDL cholesterol < 40 mg/dL" to the messages of 'the patient' ;
```

**Conditions:**

- the risk level of 'the patient' is ""
- the HDL cholesterol of 'the patient' is less than 40

**Actions:**

- set the risk level of 'the patient' to "Medium"
- add "Medium risk: HDL cholesterol < 40 mg/dL" to the messages of 'the patient'

---

### Package: `validation`

#### Rule: `check-critical-fields`

```
if
    the age of 'the patient' is 0
    or the systolic blood pressure of 'the patient' is 0
then
    flag 'the patient' for manual review with reason "Critical fields missing: age or systolic blood pressure" ;
```

**Conditions:**

- the age of 'the patient' is 0
    or the systolic blood pressure of 'the patient' is 0

**Actions:**

- flag 'the patient' for manual review with reason "Critical fields missing: age or systolic blood pressure"

---


## 6. Decision Tables

*No decision tables found in this project.*


## 7. Rule Variables

### CardiovascularRiskAssessmentParameters

| Variable Name | Type | Verbalization |
|---------------|------|---------------|
| `patient` | `com.healthcare.cvd.Patient` | the patient |


## 8. Business Object Model (BOM)

### cardiovascular-risk

**Package:** `com.healthcare.cvd`

#### Class: `Patient`

**Properties:**

- `public int age`
- `public string sex`
- `public int systolicBP`
- `public int diastolicBP`
- `public int ldlCholesterol`
- `public int hdlCholesterol`
- `public int triglycerides`
- `public string smokingHistory`
- `public boolean hasDiabetes`
- `public boolean hasFamilyHistory`
- `public boolean onBPMedication`
- `public boolean onCholesterolMedication`
- `public string riskLevel`
- `public boolean requiresManualReview`

**Methods:**

- `void addMessage(string arg)`
- `void flagForManualReview(string reason)`


## 9. Business Vocabulary

### cardiovascular-risk_en_US

#### Patient - *patient*

| Property | Type | Phrase |
|----------|------|--------|
| `age` | Navigation | {age} of {this} |
| `age` | Action | set the age of {this} to {age} |
| `sex` | Navigation | {sex} of {this} |
| `sex` | Action | set the sex of {this} to {sex} |
| `systolicBP` | Navigation | {systolic blood pressure} of {this} |
| `systolicBP` | Action | set the systolic blood pressure of {this} to {systolic blood pressure} |
| `diastolicBP` | Navigation | {diastolic blood pressure} of {this} |
| `diastolicBP` | Action | set the diastolic blood pressure of {this} to {diastolic blood pressure} |
| `ldlCholesterol` | Navigation | {LDL cholesterol} of {this} |
| `ldlCholesterol` | Action | set the LDL cholesterol of {this} to {LDL cholesterol} |
| `hdlCholesterol` | Navigation | {HDL cholesterol} of {this} |
| `hdlCholesterol` | Action | set the HDL cholesterol of {this} to {HDL cholesterol} |
| `triglycerides` | Navigation | {triglycerides} of {this} |
| `triglycerides` | Action | set the triglycerides of {this} to {triglycerides} |
| `smokingHistory` | Navigation | {smoking history} of {this} |
| `smokingHistory` | Action | set the smoking history of {this} to {smoking history} |
| `hasDiabetes` | Navigation | {this} has diabetes |
| `hasDiabetes` | Action | make it {has diabetes} that {this} has diabetes |
| `hasFamilyHistory` | Navigation | {this} has family history of CVD |
| `hasFamilyHistory` | Action | make it {has family history} that {this} has family history of CVD |
| `onBPMedication` | Navigation | {this} is on blood pressure medication |
| `onBPMedication` | Action | make it {on BP medication} that {this} is on blood pressure medication |
| `onCholesterolMedication` | Navigation | {this} is on cholesterol medication |
| `onCholesterolMedication` | Action | make it {on cholesterol medication} that {this} is on cholesterol medication |
| `riskLevel` | Navigation | {risk level} of {this} |
| `riskLevel` | Action | set the risk level of {this} to {risk level} |
| `messages` | Navigation | {messages} of {this} |
| `requiresManualReview` | Navigation | {this} requires manual review |
| `requiresManualReview` | Action | make it {requires manual review} that {this} requires manual review |


---

*Report generated by ODM Report Generator*
