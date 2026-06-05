# ODM File Templates

Complete file templates for all ODM artifacts. Use these as reference when creating files.

## CRITICAL: No XML Comments in Generated Files

**DO NOT add XML comments** (e.g., `<!-- Made with Bob -->`) to any generated ODM XML files. The ODM XML parser can fail on comments in certain contexts, particularly in:
- Rule files (.brl)
- Ruleflow files (.rfl)
- Deployment files (.dop, .dep)
- BOM mapping files (.b2xa)

If comments are accidentally added, they must be removed before running the build command. See Step 3 in [`build-validation.md`](build-validation.md) for the cleanup command.

## UUID Generation Strategy (MANDATORY)

CRITICAL: Every ODM artifact file MUST have a unique UUID. ALWAYS generate a fresh UUID for each new file. NEVER reuse UUIDs from templates or copy-paste UUIDs between files.

### How to Generate UUIDs

**Method 1 - Using uuidgen command (macOS/Linux - PREFERRED):**

Generate a single UUID:
```bash
uuidgen | tr '[:upper:]' '[:lower:]'
```

Generate multiple UUIDs at once (e.g., for 5 files):
```bash
for i in {1..5}; do uuidgen | tr '[:upper:]' '[:lower:]'; done
```

**Method 2 - Using Python (cross-platform):**
```bash
python3 -c "import uuid; print(str(uuid.uuid4()))"
```

**Method 3 - Using Node.js (if available):**
```bash
node -e "console.log(require('crypto').randomUUID())"
```

### UUID Assignment

Before creating any ODM file, generate the required number of UUIDs:
- .ruleproject: 1 UUID
- Each .rulepackage: 1 UUID per package
- Each .brl rule file: 1 UUID per rule
- .var variable set: 1 UUID
- .rfl ruleflow: 1 UUID
- .dop operation: 1 UUID
- .dep deployment: 1 UUID
- .bom file: 1 UUID
- .b2xa file: 1 UUID
- .voc vocabulary: 1 UUID (optional)

## Rule File Template (.brl)

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

**CRITICAL NOTE:** Multiple actions in 'then' clause use SEMICOLONS after each statement.

## Variables File Template (.var)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ilog.rules.studio.model.base:VariableSet xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:ilog.rules.studio.model.base="http://ilog.rules.studio/model/base.ecore">
  <name>ProjectNameParameters</name>
  <uuid>unique-var-uuid-here</uuid>
  <documentation><![CDATA[Variables for ProjectName]]></documentation>
  <variables name="request" type="package.ClassName" initialValue="" verbalization="the request"/>
</ilog.rules.studio.model.base:VariableSet>
```

**Key Points:**
- name attribute: NO spaces (e.g., "request", "transaction", "loan")
- verbalization attribute: Natural language with "the" prefix (e.g., "the request")
- Rules reference using verbalization in single quotes: `'the request'`
- Deployment operations use the name attribute (without spaces)

## Deployment Operation Template (.dop)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<com.ibm.rules.studio.model.decisionservice:Operation xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:com.ibm.rules.studio.model.decisionservice="http://com.ibm.rules.studio/model/decisionservice.ecore" xmlns:ilog.rules.studio.model.query.extractor="http://ilog.rules.studio/model/query/extractor.ecore" rulesetName="Project_Name_Ruleset" usingRuleflow="true" ruleflowName="main-ruleflow" targetRuleProjectName="ProjectName">
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

**Required Attributes:**
- rulesetName: Use camelCase, not spaces (e.g., "ProjectNameRuleset")
- targetRuleProjectName: Exact project name without spaces (e.g., "ProjectName")
- usingRuleflow: Set to "true" when using ruleflow
- ruleflowName: Name of .rfl file without extension
- Must include `<extractor>` element
- **CRITICAL:** Must include `<targetRuleProject>` href with EXACT UUID from .ruleproject file

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

**Key Annotations:**
- `property "factory.ignore" "true"`: Prevents instantiation (for computed/status properties)
- `property "ilog.rules.engine.dataio.forConversion" "true"`: Marks constructors for JSON/XML serialization
- `readonly`: Properties that shouldn't be modified in rules
- Collections: `public readonly java.util.Collection name domain 0,* class string;`

## Vocabulary File Template (Properties-Based)

**File naming:** `{project-name}_en_US.voc` (or other locale if specified)

```properties
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

**Format Rules:**
- Start with `# Vocabulary Properties` and `uuid = ...`
- Use pattern: `package.Class.property#phrase.type = natural language`
- Navigation phrases: `{property} of {this}`
- Action phrases: `set the {property} of {this} to {property}`
- Boolean navigation: `{this} is [concept]`
- Boolean action: `make it {property} that {this} is [concept]`
- Method actions: Use `{0}`, `{1}`, etc. for parameters

## Ruleflow Template (.rfl)

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

**Execution Modes:**
- Fastpath: Sequential rule execution (order matters) - use for validation
- RetePlus: Rete algorithm (pattern matching, order independent) - use for complex logic

## Rule Package Template (.rulepackage)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ilog.rules.studio.model.base:RulePackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:ilog.rules.studio.model.base="http://ilog.rules.studio/model/base.ecore">
  <name>validation</name>
  <uuid>unique-package-uuid-here</uuid>
  <documentation><![CDATA[Rules for validation logic]]></documentation>
</ilog.rules.studio.model.base:RulePackage>
```

## XOM Project .project Template

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

## XOM Project .classpath Template

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

## Rule Project .project Template

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

**Critical:** Must reference XOM project in `<projects>` section for automatic linking

## Rule Project .ruleproject Template

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

**Critical Attributes:**
- buildMode="DecisionEngine" (NOT "DecisionService")
- isADecisionService="true" (marks as Decision Service)
- migrationFlag="3"
- MigratedToOperationId links to deployment operation UUID
- Must include all namespace declarations
- Must define all modelFolders
- XOM path uses platform:/ reference
- BOM path entry must include origin="xom:/ProjectName/project-xom"

## B2XA File Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<b2x:translation xmlns:b2x="http://schemas.ilog.com/JRules/1.3/Translation" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://schemas.ilog.com/JRules/1.3/Translation ilog/rules/schemas/1_3/b2x.xsd">
    <id>unique-b2xa-uuid-here</id>
    <lang>ARL</lang>
</b2x:translation>
```

## Deployment Configuration Template (.dep)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<com.ibm.rules.studio.model.decisionservice:Deployment xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:com.ibm.rules.studio.model.decisionservice="http://com.ibm.rules.studio/model/decisionservice.ecore" ruleAppName="ProjectName" managingXom="true">
  <name>ProjectName</name>
  <uuid>deployment-uuid-here</uuid>
  <operations operationName="ProjectNameOperation">
    <operation href="ProjectNameOperation.dop#operation-uuid-here"/>
  </operations>
  <versionPolicyName>Increment minor version numbers</versionPolicyName>
  <defaultVersion>1.0</defaultVersion>
</com.ibm.rules.studio.model.decisionservice:Deployment>
```

**CRITICAL:** Must include `ruleAppName` attribute - without it, Build Command fails with "A RuleApp name cannot be empty"