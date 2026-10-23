# AI_Context.md — IBM ODM Decision Service projects: file formats and worked examples

This document is written to be **self-contained**. It explains how an IBM Operational Decision Manager
(ODM) Decision Service project is laid out on disk, what every file type contains, how the files
reference each other, and which mistakes break the build. Every code excerpt below is copied from a
project committed under [projects/](projects/) that was compiled successfully with IBM's Build Command
(`rules-compiler.jar`). Companion files:

- [AGENTS.md](AGENTS.md): short do/don't rules and build commands for coding agents.
- `projects/<Project>/AI_Context.md`: a file-by-file walkthrough of one project.

## 1. Mental model

A Decision Service is a stack of five layers. A name must stay identical across all five:

```
XOM  Java classes (execution object model)       projects/<X>/<x>-xom/src/**/*.java
 ↓ mapped by
BOM  Business Object Model, plain text            <Project>/bom/<base>.bom
 ↓ verbalized by
VOC  Vocabulary, Java-properties text             <Project>/bom/<base>_en_US.voc
 ↓ used by
BAL  Business Action Language rules               <Project>/rules/<package>/<rule>.brl  (XML wrapper, BAL in CDATA)
 ↓ orchestrated by
RFL  Ruleflow + deployment operation              <Project>/rules/*.rfl, <Project>/deployment/*.dop, *.dep
```

Example of one concept traversing all layers (from Aviation Pollution Compliance):

```
XOM field/getter/setter : private Operator airlineOperator;  getAirlineOperator() / setAirlineOperator(...)
BOM property            : public com.aviation.compliance.Operator airlineOperator;   (auto-mapped)
Vocabulary              : ...ComplianceRequest.airlineOperator#phrase.navigation = {airline operator} of {this}
Rule                    : the airline operator of 'the request'
```

## 2. Project layout on disk

Each directory under `projects/` holds one Decision Service. Two sibling folders matter: the Rule
Project (name may contain spaces) and the XOM Java project.

```
projects/Loan_Compliance_Service/
├── Loan Compliance Service/            <- the Rule Project (contains .ruleproject)
│   ├── .ruleproject                    XML: project identity, XOM/BOM paths, folders
│   ├── bom/
│   │   ├── loan-compliance.bom         plain text BOM
│   │   ├── loan-compliance_en_US.voc   vocabulary, locale suffix required
│   │   └── loan-compliance.b2xa        tiny XML: BOM→ARL translation id
│   ├── rules/
│   │   ├── LoanComplianceParameters.var    XML: ruleset variables (input/output objects)
│   │   ├── loan-compliance-ruleflow.rfl    XML: execution order of rule packages
│   │   ├── validation/                     one folder = one rule package
│   │   │   ├── .rulepackage                XML: package name/uuid/documentation
│   │   │   └── check-age.brl               XML: one rule (BAL text in CDATA)
│   │   └── pricing/interest-rate-by-credit-score.dta   XML: decision table
│   ├── deployment/
│   │   ├── LoanComplianceOperation.dop     XML: operation (ruleset name, variables, ruleflow)
│   │   └── <name>.dep                      XML: deployment configuration (RuleApp name, versioning)
│   ├── queries/  resources/  templates/    usually only .placeholder files
└── loan-compliance-xom/
    └── src/com/loan/compliance/*.java      execution classes
```

## 3. Format map

| Extension | Encoding | Purpose | Key facts |
|---|---|---|---|
| `.ruleproject` | XML (EMF/XMI) | Project descriptor | `<name>`, `<uuid>`, `buildMode="DecisionEngine"`, `isADecisionService="true"`, XOM path entry, BOM path entry with `origin="xom:/<Project>/<xom-project>"` |
| `.bom` | **Plain text** (never XML) | Business classes over XOM | Starts with `property` header lines, then `package x.y;`, then Java-like classes |
| `_en_US.voc` | Java-properties text | Natural-language phrases | `uuid = …` header; `<class>#concept.label`, `<member>#phrase.navigation` / `#phrase.action` |
| `.b2xa` | XML | BOM↔ARL translation | Only `<id>` and `<lang>ARL</lang>`; same base name as `.bom` |
| `.var` | XML | Ruleset variables | `name` has no spaces; `verbalization="the request"` |
| `.brl` | XML wrapper | One BAL rule | `<definition><![CDATA[ if … then … ]]></definition>` |
| `.dta` | XML wrapper | Decision table | `<DT>` payload inside `<definition>`; BAL fragments in `<Text>` |
| `.rulepackage` | XML | Rule package descriptor | Folder name == `<name>`; edit rarely |
| `.rfl` | XML wrapper | Ruleflow | `<Package Name="…"/>` must equal a rule package folder name |
| `.dop` | XML | Operation | `rulesetName`, `ruleflowName`, `referencedVariables`, `targetRuleProject href` |
| `.dep` | XML | Deployment config | `ruleAppName` attribute required (empty → build error) |
| `.java` | Java | XOM | Jackson POJOs, no `Serializable` |

Every artifact carries its own UUID (`<uuid>` in XML, `property uuid` in `.bom`, `uuid =` in `.voc`,
`<id>` in `.b2xa`). Never copy a UUID between files.

## 4. Anatomy of each file (verified excerpts)

### 4.1 `.ruleproject`

Excerpt from `Loan Compliance Service/.ruleproject`:

```xml
<ilog.rules.studio.model.base:RuleProject xmi:version="2.0" ... buildMode="DecisionEngine" isADecisionService="true" migrationFlag="3" MigratedToOperationId="b8f3c2d1-9a7e-4f5b-8c6d-1e2f3a4b5c6d">
  <name>Loan Compliance Service</name>
  <uuid>a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d</uuid>
  <outputLocation>output</outputLocation>
  <categories>any</categories>
  <paths xsi:type="ilog.rules.studio.model.xom:XOMPath" pathID="XOM">
    <entries xsi:type="ilog.rules.studio.model.xom:LibraryXOMPathEntry" name="org.eclipse.jdt.launching.JRE_CONTAINER" url="file:org.eclipse.jdt.launching.JRE_CONTAINER" kind="LIBRARY"/>
    <entries xsi:type="ilog.rules.studio.model.xom:SystemXOMPathEntry" name="loan-compliance-xom" url="platform:/loan-compliance-xom" kind="JAVA_PROJECT"/>
  </paths>
  <paths xsi:type="ilog.rules.studio.model.bom:BOMPath" pathID="BOM">
    <entries xsi:type="ilog.rules.studio.model.bom:BOMEntry" name="loan-compliance" url="platform:/Loan Compliance Service/bom/loan-compliance.bom" origin="xom:/Loan Compliance Service/loan-compliance-xom"/>
  </paths>
  <modelFolders xsi:type="ilog.rules.studio.model.base:SourceFolder"><name>rules</name></modelFolders>
  <modelFolders xsi:type="ilog.rules.studio.model.bom:BOMFolder"><name>bom</name></modelFolders>
  <modelFolders xsi:type="com.ibm.rules.studio.model.decisionservice:OperationFolder"><name>deployment</name></modelFolders>
  <!-- plus TemplateFolder "templates", QueryFolder "queries", ResourceFolder "resources" -->
</ilog.rules.studio.model.base:RuleProject>
```

The message "Classic rule projects are not supported" means `isADecisionService="true"` or the
`OperationFolder` named `deployment` is missing.

### 4.2 `.bom` (plain text)

Excerpt from `bom/loan-compliance.bom`:

```
property loadGetterSetterAsProperties "true"
property origin "xom:/Loan Compliance Service/loan-compliance-xom"
property uuid "c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f"
package com.loan.compliance;

public class Applicant
{
    public java.util.Date birthDate;
    public com.loan.compliance.Address address;
    public int creditScore;
    public boolean financialRecordPresent;
    public readonly int age
                property "factory.ignore" "true";
    public Applicant(java.util.Date birthDate, com.loan.compliance.Address address, int creditScore, double annualIncome)
                property "ilog.rules.engine.dataio.forConversion" "true";
    public Applicant();
}

public class LoanRequest
{
    public readonly java.util.Collection messages domain 0,* class string;
    public readonly java.util.Collection violations domain 0,* class string;
    public void addMessage(string arg);
    public void addViolation(string arg);
}
```

Rules:

- With `loadGetterSetterAsProperties "true"`, plain getter/setter pairs auto-map. Declare a property
  explicitly only for special annotations.
- **Computed getters with no setter** (`getAge()`, `isInKnownCorridor()`) must be declared
  `public readonly <type> name property "factory.ignore" "true";`, or rules cannot see them.
- **Boolean naming**: `isPregnant()/setPregnant()` → `public boolean pregnant;`. `hasAllergies()/setHasAllergies()`
  → `public boolean hasAllergies;`. Wrong: `isPregnant` (error `GBREX0021E Cannot find attribute 'isPregnant' in execution class`).
- Collections are `readonly … domain 0,* class <type>`; rules mutate them through add-methods only.
- Use the type keyword `string` for `java.lang.String`.
- Constructors used for JSON conversion carry `property "ilog.rules.engine.dataio.forConversion" "true"`.
- Reserved words that cannot be property names: `operator`, `function`, `rule`, `package`, `import`.
  Rename with a qualifier (`airlineOperator`).

### 4.3 `.voc` (vocabulary)

Excerpt from `bom/loan-compliance_en_US.voc`:

```
# Vocabulary Properties
uuid = d1e2f3a4-b5c6-7d8e-9f0a-1b2c3d4e5f6a

com.loan.compliance.Applicant#concept.label = applicant
com.loan.compliance.Applicant.creditScore#phrase.navigation = {credit score} of {this}
com.loan.compliance.Applicant.creditScore#phrase.action = set the credit score of {this} to {credit score}
com.loan.compliance.Applicant.financialRecordPresent#phrase.navigation = {this} has financial record
com.loan.compliance.Applicant.financialRecordPresent#phrase.action = make it {financial record present} that {this} has financial record
com.loan.compliance.Applicant.age#phrase.navigation = {age} of {this}
com.loan.compliance.LoanRequest.addViolation(java.lang.String)#phrase.action = add {0} to the violations of {this}
```

- File name: `<base>_<LOCALE>.voc`, default `en_US`. Locale suffix is mandatory.
- `#phrase.navigation` creates read expressions; `#phrase.action` creates write expressions.
- Method phrases use `{0}`, `{1}` for arguments, and their key includes the Java signature.
- Two phrases that begin with the same token sequence produce `Ambiguous sentence`.

### 4.4 `.b2xa`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<b2x:translation xmlns:b2x="http://schemas.ilog.com/JRules/1.3/Translation" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://schemas.ilog.com/JRules/1.3/Translation ilog/rules/schemas/1_3/b2x.xsd">
    <id>e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b</id>
    <lang>ARL</lang>
</b2x:translation>
```

### 4.5 `.var` (ruleset variables)

```xml
<ilog.rules.studio.model.base:VariableSet xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:ilog.rules.studio.model.base="http://ilog.rules.studio/model/base.ecore">
  <name>LoanComplianceParameters</name>
  <uuid>f1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c</uuid>
  <variables name="request" type="com.loan.compliance.LoanRequest" initialValue="" verbalization="the request"/>
</ilog.rules.studio.model.base:VariableSet>
```

`name` has no spaces (the `.dop` refers to it). `verbalization` is what rules quote: `'the request'`.

### 4.6 `.brl` (one rule, XML wrapper around BAL)

`rules/validation/check-age.brl`:

```xml
<ilog.rules.studio.model.brl:ActionRule xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:ilog.rules.studio.model.brl="http://ilog.rules.studio/model/brl.ecore">
  <name>check-age</name>
  <uuid>d5e6f7a8-b9c0-1d2e-3f4a-5b6c7d8e9f0a</uuid>
  <locale>en_US</locale>
  <definition><![CDATA[if
    the applicant of 'the request' is not null
    and the age of the applicant of 'the request' is less than 18
then
    make it false that 'the request' is eligible ;
    set the reason of 'the request' to "Applicant must be at least 18 years old" ;
    add "Age requirement not met - applicant is under 18" to the violations of 'the request' ;]]></definition>
</ilog.rules.studio.model.brl:ActionRule>
```

Variants seen in committed projects:

- **`definitions` block** (bind a variable from a collection; Luggage `excess-fees/calculate-overweight-fee.brl`):
  ```
  definitions
      set 'bag' to a baggage item in the luggages of 'the request' ;
  if
      'bag' is overweight for economy
      and the weight of 'bag' is at most 32
  then
      add fee 75 to 'the request' ;
  ```
- **Arithmetic** needs parentheses (Aviation `penalties/calculate-offset-penalty.brl`):
  `add ( the offset deficit of the airline operator of 'the request' * 100 ) to the penalty amount of the airline operator of 'the request' ;`
- **String concatenation** uses `+` (Loan `approve-eligible-loan.brl`):
  `set the reason of 'the request' to "Loan approved with " + the interest rate of 'the request' + "% APR." ;`
- **Null-safe navigation**: test each intermediate object in its own `and` clause before reading its property.

### 4.7 `.dta` (decision table)

Structure (Loan `pricing/interest-rate-by-credit-score.dta`, abridged):

```xml
<model.dt:DecisionTable xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:model.dt="http://ilog.rules.studio/model/dt.ecore">
  <name>interest-rate-by-credit-score</name>
  <uuid>c4d5e6f7-a8b9-0c1d-2e3f-4a5b6c7d8e9f</uuid>
  <categories>any</categories>
  <locale>en_US</locale>
  <definition>
<DT xmlns="http://schemas.ilog.com/Rules/7.0/DecisionTable" Version="7.0">
  <Body>
    <Structure>
      <ConditionDefinitions>
        <ConditionDefinition Id="C0"><ExpressionDefinition>
          <Text><![CDATA[the credit score of the applicant of 'the request' is at least <min> and less than <max>]]></Text>
        </ExpressionDefinition></ConditionDefinition>
      </ConditionDefinitions>
      <ActionDefinitions>
        <ActionDefinition Id="A0"><ExpressionDefinition>
          <Text><![CDATA[set the interest rate of 'the request' to <a number>]]></Text>
        </ExpressionDefinition></ActionDefinition>
      </ActionDefinitions>
    </Structure>
    <Contents>
      <Partition DefId="C0">
        <Condition>
          <Expression><Param><![CDATA[600]]></Param><Param><![CDATA[650]]></Param></Expression>
          <ActionSet><Action DefId="A0"><Expression><Param><![CDATA[14.5]]></Param></Expression></Action></ActionSet>
        </Condition>
        <!-- one <Condition> per row; open-ended rows override <Text> ("<a number> is less than <a number>") -->
      </Partition>
    </Contents>
  </Body>
</DT></definition>
</model.dt:DecisionTable>
```

`ConditionDefinition`/`ActionDefinition` hold the BAL template with `<placeholders>`; each `Condition`
row supplies `Param` values in placeholder order. Cross-border projects use the same shape for a
risk-score decision table.

### 4.8 `.rulepackage`

```xml
<ilog.rules.studio.model.base:RulePackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:ilog.rules.studio.model.base="http://ilog.rules.studio/model/base.ecore">
  <name>pricing</name>
  <uuid>b3c4d5e6-f7a8-9b0c-1d2e-3f4a5b6c7d8e</uuid>
  <documentation><![CDATA[Interest rate pricing rules based on credit score]]></documentation>
</ilog.rules.studio.model.base:RulePackage>
```

The folder is `rules/pricing/`, so `<name>` is `pricing`. A nested folder is a nested package.

### 4.9 `.rfl` (ruleflow)

The ruleflow is XML that wraps an inner `<Ruleflow>` document. Each `RuleTask` runs one or more packages:

```xml
<RuleTask ExecutionMode="Fastpath" ExitCriteria="None" Identifier="task_1" Ordering="Default">
  <RuleList><Package Name="validation"/></RuleList>
</RuleTask>
<RuleTask ExecutionMode="RetePlus" ExitCriteria="None" Identifier="task_2" Ordering="Default">
  <RuleList><Package Name="pricing"/></RuleList>
</RuleTask>
```

Tasks are chained by `<Transition Source="node_1" Target="node_2"/>` between `StartTask` and `StopTask`.
`<Properties><imports><![CDATA[use com.loan.compliance;]]></imports></Properties>` imports the BOM package.

Execution modes used: `Fastpath` (sequential, no inference: most projects), `RetePlus` (pattern-matching
inference: used where one rule's effect must trigger others, e.g. AML, Luggage fees, Loan pricing).

### 4.10 `.dop` (operation) and `.dep` (deployment)

`.dop`:

```xml
<com.ibm.rules.studio.model.decisionservice:Operation ... rulesetName="Loan_Compliance_Service_Ruleset" usingRuleflow="true" ruleflowName="loan-compliance-ruleflow" targetRuleProjectName="Loan Compliance Service">
  <name>LoanComplianceOperation</name>
  <uuid>b8f3c2d1-9a7e-4f5b-8c6d-1e2f3a4b5c6d</uuid>
  <referencedVariables variableName="request" variableSetName="LoanComplianceParameters" direction="IN_OUT">
    <variableSet href="../rules/LoanComplianceParameters.var#f1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c"/>
  </referencedVariables>
  <ruleflow href="../rules/loan-compliance-ruleflow.rfl#b9c0d1e2-f3a4-5b6c-7d8e-9f0a1b2c3d4e"/>
  <targetRuleProject href="../../Loan%20Compliance%20Service#a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"/>
</com.ibm.rules.studio.model.decisionservice:Operation>
```

`.dep` (Luggage `deployment/Luggage_Compliance_Service.dep`, abridged):

```xml
<com.ibm.rules.studio.model.decisionservice:Deployment ... ruleAppName="LuggageComplianceService" managingXom="true">
  <name>Luggage_Compliance_Service</name>
  <uuid>0d520d68-3bea-42fe-9e25-0c618fbaf20a</uuid>
  <targets label="RES"/>
  <operations operationName="LuggageComplianceOperation">
    <operation href="LuggageComplianceOperation.dop#d6d23401-d5ee-4dd1-b45a-3473b87cb93b"/>
    <properties key="ruleset.version"><value><![CDATA[1.0]]></value></properties>
  </operations>
  <versionPolicies label="Increment minor version numbers" ruleset="INCREMENT_MINOR" default="true" recurrent="true"/>
</com.ibm.rules.studio.model.decisionservice:Deployment>
```

The `dep` **element `<name>`** is what the build's `dep =` property must equal.

### 4.11 XOM (Java)

Pattern used in newer projects (Cross-Border Fraud `Cardholder.java`):

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Cardholder {
    private String cardholderId;
    private List<String> priorTransactionCountries;

    public Cardholder() { this.priorTransactionCountries = new ArrayList<>(); }
    public void addPriorTransactionCountry(String country) { this.priorTransactionCountries.add(country); }

    @JsonIgnore
    public boolean hasPriorTransactionIn(String country) {
        return country != null && priorTransactionCountries.contains(country);
    }
}
```

- Do not implement `Serializable`. (Loan Compliance is an older project that still does.)
- Annotate computed accessors (`isX()`, `hasX()` with no setter) with `@JsonIgnore` so JSON round trips work.
- Provide a no-argument constructor.
- Domain vocabulary for such a method: `Cardholder.hasPriorTransactionIn(java.lang.String)#phrase.navigation = {this} has previously transacted in {0}`.

## 5. Cross-file linkage checklist

| Link | Must match |
|---|---|
| `.dop` `<targetRuleProject href="../../Name#UUID"/>` | `.ruleproject` `<uuid>` (spaces encoded as `%20`) |
| `.dop` `<ruleflow href="…rfl#UUID"/>` and `ruleflowName` | `.rfl` `<uuid>` and `<name>` |
| `.dop` `<variableSet href="…var#UUID"/>`, `variableName`, `variableSetName` | `.var` `<uuid>`, `variables/@name`, `<name>` |
| `.dep` `<operation href="Op.dop#UUID"/>` and `operationName` | `.dop` `<uuid>` and `<name>` |
| `.rfl` `<Package Name="x"/>` | folder `rules/x/` |
| `.ruleproject` BOM `origin` and `.bom` `property origin` | `xom:/<Project name>/<xom project>` |
| `.bom`, `.voc`, `.b2xa` base name | identical (voc adds `_en_US`) |
| Rule text `'the request'` | `.var` `verbalization` |
| Voc member keys | Java class + member names in the BOM |

## 6. Verified build status

Building is the only proof of correctness. Command (JDK 21):

```
java -jar rules-compiler.jar -config <Project>.properties
```

```
project = <absolute path to Rule Project folder>
output = <output dir>
dep = <the .dep <name> value>
xom-classpath = <path to a single XOM jar>
```

Note: `xom-classpath` accepted one jar in testing. If the XOM needs Jackson, put the dependency classes
in the same jar (a "fat" XOM jar). A `:`-joined list failed with "could not be found or is not a file".

Result of building every committed project with IBM Semeru JDK 21 (XOM compiled from source):

| Project | Build | Notes |
|---|---|---|
| AML_Detection_service | SUCCESS | |
| Aviation_Pollution_Compliance | SUCCESS | |
| Cardiovascular_Risk_Assessment | SUCCESS | |
| CrossBorder_Fraud_Detection | SUCCESS | |
| Geolocation_Fraud_Detection | SUCCESS | |
| Luggage_Compliance_Service | SUCCESS | uses `definitions` |
| Mineral_Classification | SUCCESS | |
| Loan_Compliance_Service | **FAILURE** | has no `.dep`; error: `The deployment configuration named "…" was not found in rule project`. Its XOM still implements `Serializable` and its vocabulary has phrases with no BOM member (`hasCoSigner`, `combinedCreditScore`). Treat as a partial example. |

JDK matters: with JDK 25 every build failed with `GBREX0011E: Cannot find method 'resume()' in execution class 'java.lang.Thread'`
(B2X mapping over removed JDK methods). Use JDK 17 or 21.

## 7. BAL syntax rules and common errors

| Wrong | Right | Compiler symptom |
|---|---|---|
| `x >= 5`, `x <= 5`, `x > 5`, `x < 5` | `is at least 5`, `is at most 5`, `is more than 5`, `is less than 5` | word expected/not required |
| `is equal to` | plain `is` / `is not` (`the decision of 'the request' is "PENDING"`) | `The word 'January' is expected in place of 'equal'` |
| `is in { … }` | `is one of { "A", "B" }` | word expected |
| `'the var' is not active` | `it is not true that 'the var' is active` | word expected |
| `is not defined` | `is null` / `is not null` | word expected |
| `set … ;` missing final `;` | every action ends with ` ;` | parse error |
| `the violations of 'the request' is empty` | expose a computed boolean (`hasViolations`) with `@JsonIgnore` and a vocabulary phrase | invalid expression |
| Boolean BOM property declared `isPregnant` | `pregnant` | `GBREX0021E Cannot find attribute … in execution class` |
| Two phrases starting `add {0} to the …` sharing a prefix | make the full phrase unique | `Ambiguous sentence` |
| Phrase label `speed`, `distance`, `minutes`, `points`, `increase`, `decrease`, `by`, `elapsed`, `location` | choose another word (`prior`, `gap`, `offset`, `tally`) | `The word 'X' is missing` |
| `.dep` without `ruleAppName` | set `ruleAppName="…"` | `A RuleApp name cannot be empty` |
| Missing `isADecisionService` / `deployment` OperationFolder | add them in `.ruleproject` | `Classic rule projects are not supported` |
| Property named `operator` | `airlineOperator` | build breaks at BOM |
| Copied UUIDs | one new UUID per file | `Cannot load operation` |

The compiler stops at the first broken rule package (alphabetical by package name). A clean report for
other packages does not prove they are valid; rebuild until `BUILD SUCCESS`.

## 8. Project catalog

Each project's folder has its own `AI_Context.md` with a file-by-file tour.

| Project | Domain / decision | Root object | Ruleflow (mode) | Features shown |
|---|---|---|---|---|
| [Mineral_Classification](projects/Mineral_Classification/AI_Context.md) | Classify a mineral specimen by chemistry, then silicate subtype | `MineralSpecimen` | 2 packages (Fastpath) | One rule per class, two-stage classification, Jackson XOM |
| [Cardiovascular_Risk_Assessment](projects/Cardiovascular_Risk_Assessment/AI_Context.md) | Patient risk tier: high, medium, low | `Patient` | validation, high-risk (RetePlus), medium, low | Severity-tiered packages, default rule last |
| [Luggage_Compliance_Service](projects/Luggage_Compliance_Service/AI_Context.md) | Airline baggage limits and fees | `LuggageRequest` | 5 packages (Fastpath/RetePlus) | `definitions` iteration over collection, fee calculation |
| [Aviation_Pollution_Compliance](projects/Aviation_Pollution_Compliance/AI_Context.md) | Emissions limits, CORSIA offsets, penalties | `ComplianceRequest` | 7 packages | Arithmetic, null-safe navigation, computed boolean |
| [AML_Detection_service](projects/AML_Detection_service/AI_Context.md) | Anti-money-laundering alerts and escalation | `AMLRequest` | 5 packages (RetePlus + Fastpath) | Alert creation methods, cumulative amounts |
| [CrossBorder_Fraud_Detection](projects/CrossBorder_Fraud_Detection/AI_Context.md) | Cross-border transaction risk scoring | `Transaction` | 4 packages (Fastpath + RetePlus) | Score accumulation, compliance pre-screening, audit trail |
| [Geolocation_Fraud_Detection](projects/Geolocation_Fraud_Detection/AI_Context.md) | Geolocation and velocity-based fraud risk scoring | `Transaction` | 4 packages (Fastpath + RetePlus) | Pre-screening anomaly detection, risk scoring, confidence levels |
| [Loan_Compliance_Service](projects/Loan_Compliance_Service/AI_Context.md) | Loan eligibility and interest pricing | `LoanRequest` | validation (Fastpath), pricing (RetePlus) | Decision table `.dta`; **does not build yet** |

## 9. Recipe: building a new project

1. Write the XOM (Jackson POJOs), compile to a jar.
2. Write the `.bom` (properties header, `package`, classes) and the `_en_US.voc`.
3. Create the `.ruleproject`, `.b2xa`, `.var`, one `.rulepackage` per package, `.brl` files.
4. Create the `.rfl`, `.dop` (with correct hrefs), `.dep` (with `ruleAppName`).
5. Generate a fresh UUID per file (`uuidgen | tr A-Z a-z`).
6. Build with the compiler, fix one error at a time (rebuild after each), repeat until `BUILD SUCCESS`.
