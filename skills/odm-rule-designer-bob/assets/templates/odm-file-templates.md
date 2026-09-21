All placeholders below use **camelCase project names, never spaces** — see `references/rule-project-config.md`. Adapt every template to the specific business domain while keeping structure and format exact.

## Complete Rule File Template (`.brl`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ilog.rules.studio.model.brl:ActionRule xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:ilog.rules.studio.model.brl="http://ilog.rules.studio/model/brl.ecore">
  <name>sample rule name</name>
  <uuid>unique-rule-uuid-here</uuid>
  <locale>en_US</locale>
  <definition><![CDATA[if
    the property of 'the variable' is more than 100
    and 'the variable' is active
  then
    set the status of 'the variable' to "APPROVED" ;
    add "Rule executed successfully" to the messages of 'the variable' ;]]></definition>
</ilog.rules.studio.model.brl:ActionRule>
```

**CRITICAL:** every action in a multi-statement `then` clause ends with a semicolon.

## Variables File Template (`.var`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ilog.rules.studio.model.base:VariableSet xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:ilog.rules.studio.model.base="http://ilog.rules.studio/model/base.ecore">
  <name>ProjectNameParameters</name>
  <uuid>unique-var-uuid-here</uuid>
  <documentation><![CDATA[Variables for ProjectName]]></documentation>
  <variables name="request" type="package.ClassName" initialValue="" verbalization="the request"/>
</ilog.rules.studio.model.base:VariableSet>
```

- `name` attribute: NO spaces (`request`, `transaction`, `loan`).
- `verbalization`: natural language with `the` prefix (`the request`).
- Rules reference the verbalization in single quotes: `'the request'`.
- Deployment operations use the `name` attribute (without spaces).

## Deployment Operation Template (`.dop`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<com.ibm.rules.studio.model.decisionservice:Operation xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:com.ibm.rules.studio.model.decisionservice="http://com.ibm.rules.studio/model/decisionservice.ecore" xmlns:ilog.rules.studio.model.query.extractor="http://ilog.rules.studio/model/query/extractor.ecore" rulesetName="ProjectNameRuleset" usingRuleflow="true" ruleflowName="main-ruleflow" targetRuleProjectName="ProjectName">
  <name>ProjectNameOperation</name>
  <uuid>operation-uuid-here</uuid>
  <referencedVariables variableName="request" variableSetName="ProjectNameParameters" direction="IN_OUT">
    <variableSet href="../rules/ProjectNameParameters.var#var-uuid-here"/>
  </referencedVariables>
  <ruleflow href="../rules/main-ruleflow.rfl#ruleflow-uuid-here"/>
  <extractor xsi:type="ilog.rules.studio.model.query.extractor:QueryExtractor" name="ProjectNameOperation_extractor" validator="Default Validator"/>
  <targetRuleProject href="../../ProjectName#project-uuid-here"/>
</com.ibm.rules.studio.model.decisionservice:Operation>
```

**Required attributes:**
- `rulesetName`: camelCase, no spaces/underscores (`ProjectNameRuleset`).
- `targetRuleProjectName`: exact project name, camelCase, no spaces (`ProjectName`).
- `usingRuleflow="true"` when using a ruleflow.
- `ruleflowName`: `.rfl` filename without extension.
- Must include `<extractor>`.
- **CRITICAL:** `<targetRuleProject>` href must carry the EXACT UUID from `.ruleproject`.

## BOM File Template (Text-Based BRL)

```
property loadGetterSetterAsProperties "true"
property origin "xom:/ProjectName/project-xom"
property uuid "unique-bom-uuid-here"
package com.example.domain;

public class BusinessObject
{
    public readonly string id;
    public string name;
    public int value;
    public boolean active;

    public readonly string status
                property "factory.ignore" "true";

    public readonly java.util.Collection messages domain 0,* class string;

    public BusinessObject(string id, string name, int value)
                property "ilog.rules.engine.dataio.forConversion" "true";

    public BusinessObject();

    public void addMessage(string arg);
    public void activate();
}
```

**Key annotations:**
- `property "factory.ignore" "true"` — prevents instantiation (computed/status properties).
- `property "ilog.rules.engine.dataio.forConversion" "true"` — marks constructors for JSON/XML serialization.
- `readonly` — properties that shouldn't be modified in rules.
- Collections: `public readonly java.util.Collection name domain 0,* class string;`

**Correct BOM mapping examples:**

*Boolean with `is` prefix:*
```
XOM:  public boolean isPregnant() { return isPregnant; }
      public void setPregnant(boolean isPregnant) { this.isPregnant = isPregnant; }
BOM (correct): public boolean pregnant;
BOM (WRONG — causes error): public boolean isPregnant;
```

*Computed property (no setter):*
```
XOM: public boolean isBloodTypeCompatible() { return ...; }  // no setter
BOM (correct): // DO NOT declare — let auto-mapping handle it
BOM (WRONG): public readonly boolean isBloodTypeCompatible property "factory.ignore" "true";
Vocabulary: com.example.TransfusionRequest.bloodTypeCompatible#phrase.navigation = {blood type compatible} of {this}
```

*Standard property:*
```
XOM: public String getName() { return name; } / public void setName(String name) { this.name = name; }
BOM: public string name;
```

## Vocabulary File Template (Properties-Based)

File naming: `{project-name}_en_US.voc` (or other locale).

```
# Vocabulary Properties
uuid = unique-vocab-uuid-here

# com.example.domain.BusinessObject
com.example.domain.BusinessObject#concept.label = business object
com.example.domain.BusinessObject.id#phrase.navigation = {id} of {this}
com.example.domain.BusinessObject.name#phrase.action = set the name of {this} to {name}
com.example.domain.BusinessObject.name#phrase.navigation = {name} of {this}
com.example.domain.BusinessObject.value#phrase.action = set the value of {this} to {value}
com.example.domain.BusinessObject.value#phrase.navigation = {value} of {this}
com.example.domain.BusinessObject.active#phrase.action = make it {active} that {this} is active
com.example.domain.BusinessObject.active#phrase.navigation = {this} is active
com.example.domain.BusinessObject.status#phrase.navigation = {status} of {this}
com.example.domain.BusinessObject.messages#phrase.navigation = {messages} of {this}
com.example.domain.BusinessObject.addMessage(java.lang.String)#phrase.action = add {0} to the messages of {this}
com.example.domain.BusinessObject.activate()#phrase.action = activate {this}
```

**Format rules:** start with `# Vocabulary Properties` and `uuid = ...`; pattern `package.Class.property#phrase.type = natural language`; navigation `{property} of {this}`; action `set the {property} of {this} to {property}`; boolean navigation `{this} is [concept]`; boolean action `make it {property} that {this} is [concept]`; method actions use `{0}`, `{1}`, etc.

## Ruleflow Template (`.rfl`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ilog.rules.studio.model.ruleflow:RuleFlow xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:ilog.rules.studio.model.ruleflow="http://ilog.rules.studio/model/ruleflow.ecore">
  <name>main-ruleflow</name>
  <uuid>unique-ruleflow-uuid-here</uuid>
  <locale>en_US</locale>
  <categories>any</categories>
  <rfModel>
<Ruleflow xmlns="http://schemas.ilog.com/Rules/7.0/Ruleflow">
  <Body>
    <TaskList>
      <StartTask Identifier="task_0"/>
      <RuleTask ExecutionMode="Fastpath" Identifier="task_1" Ordering="Default">
        <RuleList>
          <Package Name="validation"/>
        </RuleList>
      </RuleTask>
      <RuleTask ExecutionMode="RetePlus" Identifier="task_2" Ordering="Default">
        <RuleList>
          <Package Name="processing"/>
        </RuleList>
      </RuleTask>
      <StopTask Identifier="task_3"/>
    </TaskList>
    <NodeList>
      <TaskNode Identifier="node_0" Task="task_0"/>
      <TaskNode Identifier="node_1" Task="task_1"/>
      <TaskNode Identifier="node_2" Task="task_2"/>
      <TaskNode Identifier="node_3" Task="task_3"/>
    </NodeList>
    <TransitionList>
      <Transition Identifier="transition_0" Source="node_0" Target="node_1"/>
      <Transition Identifier="transition_1" Source="node_1" Target="node_2"/>
      <Transition Identifier="transition_2" Source="node_2" Target="node_3"/>
    </TransitionList>
  </Body>
  <Resources>
    <ResourceSet Locale="en_US">
      <Data Name="node_1#label">Validation</Data>
      <Data Name="node_2#label">Processing</Data>
    </ResourceSet>
  </Resources>
  <Properties>
    <imports><![CDATA[use com.example.domain;
]]></imports>
  </Properties>
</Ruleflow>
  </rfModel>
</ilog.rules.studio.model.ruleflow:RuleFlow>
```

Execution modes: **Fastpath** = sequential (order matters), use for validation. **RetePlus** = Rete pattern matching (order-independent), use for complex logic.

## Rule Package Template (`.rulepackage`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ilog.rules.studio.model.base:RulePackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:ilog.rules.studio.model.base="http://ilog.rules.studio/model/base.ecore">
  <name>validation</name>
  <uuid>unique-package-uuid-here</uuid>
  <documentation><![CDATA[Rules for validation logic]]></documentation>
</ilog.rules.studio.model.base:RulePackage>
```

## XOM Project `.project` Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
    <name>project-xom</name>
    <comment></comment>
    <projects>
    </projects>
    <buildSpec>
        <buildCommand>
            <name>org.eclipse.jdt.core.javabuilder</name>
            <arguments>
            </arguments>
        </buildCommand>
    </buildSpec>
    <natures>
        <nature>org.eclipse.jdt.core.javanature</nature>
    </natures>
</projectDescription>
```

## XOM Project `.classpath` Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
    <classpathentry kind="src" path="src"/>
    <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
    <classpathentry kind="lib" path="lib/jackson-core-2.15.2.jar"/>
    <classpathentry kind="lib" path="lib/jackson-databind-2.15.2.jar"/>
    <classpathentry kind="lib" path="lib/jackson-annotations-2.15.2.jar"/>
    <classpathentry kind="output" path="classes"/>
</classpath>
```

## Rule Project `.project` Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
    <name>ProjectName</name>
    <comment></comment>
    <projects>
        <project>project-xom</project>
    </projects>
    <buildSpec>
        <buildCommand>
            <name>ilog.rules.studio.model.ruleBuilder</name>
            <arguments>
            </arguments>
        </buildCommand>
    </buildSpec>
    <natures>
        <nature>ilog.rules.studio.model.decisionProject</nature>
        <nature>ilog.rules.studio.model.operationProject</nature>
        <nature>ilog.rules.studio.model.ruleNature</nature>
    </natures>
</projectDescription>
```

**Critical:** must reference the XOM project in `<projects>` for automatic linking.

## Rule Project `.ruleproject` Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ilog.rules.studio.model.base:RuleProject xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:com.ibm.rules.studio.model.decisionservice="http://com.ibm.rules.studio/model/decisionservice.ecore" xmlns:ilog.rules.studio.model.base="http://ilog.rules.studio/model/base.ecore" xmlns:ilog.rules.studio.model.bom="http://ilog.rules.studio/model/bom.ecore" xmlns:ilog.rules.studio.model.query="http://ilog.rules.studio/model/query.ecore" xmlns:ilog.rules.studio.model.rule="http://ilog.rules.studio/model/rule.ecore" xmlns:ilog.rules.studio.model.xom="http://ilog.rules.studio/model/xom.ecore" buildMode="DecisionEngine" isADecisionService="true" migrationFlag="3" MigratedToOperationId="operation-uuid-here">
  <name>ProjectName</name>
  <uuid>project-uuid-here</uuid>
  <outputLocation>output</outputLocation>
  <categories>any</categories>
  <paths xsi:type="ilog.rules.studio.model.xom:XOMPath" pathID="XOM">
    <entries xsi:type="ilog.rules.studio.model.xom:LibraryXOMPathEntry" name="org.eclipse.jdt.launching.JRE_CONTAINER" url="file:org.eclipse.jdt.launching.JRE_CONTAINER" kind="LIBRARY"/>
    <entries xsi:type="ilog.rules.studio.model.xom:SystemXOMPathEntry" name="project-xom" url="platform:/project-xom" kind="JAVA_PROJECT"/>
  </paths>
  <paths xsi:type="ilog.rules.studio.model.bom:BOMPath" pathID="BOM">
    <entries xsi:type="ilog.rules.studio.model.bom:BOMEntry" name="domain" url="platform:/ProjectName/bom/domain.bom" origin="xom:/ProjectName/project-xom"/>
  </paths>
  <modelFolders xsi:type="ilog.rules.studio.model.base:SourceFolder">
    <name>rules</name>
  </modelFolders>
  <modelFolders xsi:type="ilog.rules.studio.model.bom:BOMFolder">
    <name>bom</name>
  </modelFolders>
  <modelFolders xsi:type="ilog.rules.studio.model.rule:TemplateFolder">
    <name>templates</name>
  </modelFolders>
  <modelFolders xsi:type="ilog.rules.studio.model.query:QueryFolder">
    <name>queries</name>
  </modelFolders>
  <modelFolders xsi:type="com.ibm.rules.studio.model.decisionservice:OperationFolder">
    <name>deployment</name>
  </modelFolders>
  <modelFolders xsi:type="ilog.rules.studio.model.base:ResourceFolder">
    <name>resources</name>
  </modelFolders>
</ilog.rules.studio.model.base:RuleProject>
```

**Critical attributes:** `buildMode="DecisionEngine"` (NOT `"DecisionService"`); `isADecisionService="true"`; `migrationFlag="3"`; `MigratedToOperationId` links to the deployment operation UUID; all namespace declarations; all `modelFolders`; XOM path uses `platform:/`; BOM path entry includes `origin="xom:/ProjectName/project-xom"`.

## `.b2xa` File Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<b2x:translation xmlns:b2x="http://schemas.ilog.com/JRules/1.3/Translation" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://schemas.ilog.com/JRules/1.3/Translation ilog/rules/schemas/1_3/b2x.xsd">
    <id>unique-b2xa-uuid-here</id>
    <lang>ARL</lang>
</b2x:translation>
```

## `.dep` Deployment Configuration — `ruleAppName` and `ruleset.version` required

Complete template (file name: `[ProjectName].dep`, in `deployment/`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<com.ibm.rules.studio.model.decisionservice:Deployment xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:com.ibm.rules.studio.model.decisionservice="http://com.ibm.rules.studio/model/decisionservice.ecore" ruleAppName="ProjectName" managingXom="true">
  <name>ProjectName</name>
  <uuid>unique-dep-uuid-here</uuid>
  <operations operationName="ProjectNameOperation">
    <operation href="ProjectNameOperation.dop#operation-uuid-here"/>
    <properties key="ruleset.version">
      <value><![CDATA[1.0]]></value>
    </properties>
  </operations>
  <versionPolicies label="Increment minor version numbers" ruleset="INCREMENT_MINOR" default="true" recurrent="true">
    <description><![CDATA[Updates the minor version for each ruleset. Makes the new version available but retains previous versions.]]></description>
  </versionPolicies>
  <versionPolicies label="Use the base version numbers" recurrent="true">
    <description><![CDATA[Uses the numbers provided in the deployment configuration. Replaces the latest version of each ruleset with this release. Used for hot fixes or development.]]></description>
  </versionPolicies>
  <versionPolicies label="The user can define the version numbers" ruleset="MANUAL" recurrent="true">
    <description><![CDATA[Allows you to enter your own version numbers. Used for hot fixes or updates to an earlier release.]]></description>
  </versionPolicies>
</com.ibm.rules.studio.model.decisionservice:Deployment>
```

**Required:**
- `ruleAppName` on the root element — without it the Build Command fails with `A RuleApp name cannot be empty`.
- Every `<operations>` entry MUST contain `<properties key="ruleset.version"><value><![CDATA[1.0]]></value></properties>` — the ruleset version is required, omitting it breaks the deployment operation.
- Do NOT generate a target server: no `<targets .../>` element in the `.dep` (deployment targets are configured by the user later, not by the skill).
- The `<operation href="…dop#…">` UUID must match the `<uuid>` in the `.dop`; `operationName` must match the `.dop`'s `<name>`.

See `references/ruleflow-deployment.md`.

## Decision Table Template (`.dta`)

See `references/decision-tables.md` for when/why to use one. Full structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<model.dt:DecisionTable xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:model.dt="http://ilog.rules.studio/model/dt.ecore">
  <name>scoring-table-name</name>
  <uuid>unique-decision-table-uuid</uuid>
  <categories>any</categories>
  <locale>en_US</locale>
  <definition>
<DT xmlns="http://schemas.ilog.com/Rules/7.0/DecisionTable" Version="7.0">
  <Body>
    <Properties>
      <Property Name="Check.Overlap.ErrorLevel">Error</Property>
      <Property Name="Lock.ApplyLocking" Type="xs:boolean"><![CDATA[false]]></Property>
      <Property Name="UI.ShowInvisibleColumns" Type="xs:boolean"><![CDATA[true]]></Property>
      <Property Name="UI.ShowRuleView" Type="xs:boolean"><![CDATA[false]]></Property>
    </Properties>
    <Structure>
      <ConditionDefinitions>
        <ConditionDefinition Id="C0">
          <ExpressionDefinition>
            <Properties>
              <Property Name="context" Type="ilog.rules.dt.model.check.IlrDTExpressionChecker$IntervalContext">
                <intervalContext direction="asc" enabled="false"/>
              </Property>
            </Properties>
            <Text><![CDATA[the [property] of 'the [variable]' is at least <min> and less than <max>]]></Text>
          </ExpressionDefinition>
        </ConditionDefinition>
      </ConditionDefinitions>
      <ActionDefinitions>
        <ActionDefinition Id="A0">
          <ExpressionDefinition>
            <Text><![CDATA[set 'the [result variable]' to 'the [result variable]' + <a number>]]></Text>
          </ExpressionDefinition>
        </ActionDefinition>
      </ActionDefinitions>
    </Structure>
    <Contents>
      <Partition DefId="C0">
        <Condition>
          <Expression>
            <Text><![CDATA[<a number> is less than <a number>]]></Text>
            <Param><![CDATA[1000]]></Param>
          </Expression>
          <ActionSet>
            <Action DefId="A0">
              <Expression>
                <Param><![CDATA[10]]></Param>
              </Expression>
            </Action>
          </ActionSet>
        </Condition>
        <Condition>
          <Expression>
            <Param><![CDATA[1000]]></Param>
            <Param><![CDATA[5000]]></Param>
          </Expression>
          <ActionSet>
            <Action DefId="A0">
              <Expression>
                <Param><![CDATA[50]]></Param>
              </Expression>
            </Action>
          </ActionSet>
        </Condition>
        <Condition>
          <Expression>
            <Text><![CDATA[<a number> is at least <a number>]]></Text>
            <Param><![CDATA[5000]]></Param>
          </Expression>
          <ActionSet>
            <Action DefId="A0">
              <Expression>
                <Param><![CDATA[100]]></Param>
              </Expression>
            </Action>
          </ActionSet>
        </Condition>
      </Partition>
    </Contents>
  </Body>
  <Resources DefaultLocale="en_US">
    <ResourceSet Locale="en_US">
      <Data Name="Definitions(A0)#HeaderText"><![CDATA[Add to score]]></Data>
      <Data Name="Definitions(A0)#Width"><![CDATA[400]]></Data>
      <Data Name="Definitions(C0)#HeaderText"><![CDATA[Condition Range]]></Data>
      <Data Name="Definitions(C0)#Width"><![CDATA[400]]></Data>
      <Data Name="Definitions(C0)[0]#HeaderText"><![CDATA[Min]]></Data>
      <Data Name="Definitions(C0)[1]#HeaderText"><![CDATA[Max]]></Data>
    </ResourceSet>
  </Resources>
</DT></definition>
</model.dt:DecisionTable>
```

Real-world example (credit scoring by income): condition = yearly income ranges (`< 10K`, `10K-20K`, `20K-30K`, ...), action = add points to credit score (`21, 50, 80, 120, ...`); first row has an open lower bound (`< 10,000`), last row an open upper bound (`>= 200,000`), middle rows use explicit min/max.

## BOM reserved keywords, proven patterns, naming conventions

See `references/bom-format.md` (reserved keywords, property-naming consistency, collection anti-pattern) and `references/vocabulary-and-bal.md` (BAL parser constraints, proven patterns from production builds) — these are the rules most likely to break a build if skipped.
