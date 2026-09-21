# Ruleflow & Deployment

## Ruleflow execution modes

- **Fastpath** — sequential execution; order matters. Use for validation.
- **RetePlus** — Rete pattern matching; order-independent. Use for complex logic.
- Mixing modes in one ruleflow is intentional for performance.
- The ruleflow orchestrates rule execution (not individual rules). Transitions between tasks can carry BAL conditions.

## Deployment operation (`.dop`)

Required attributes:

- `rulesetName="[ProjectName]Ruleset"` — camelCase, no spaces or underscores.
- `usingRuleflow="true"` (when using a ruleflow)
- `ruleflowName="[ruleflow-name]"` (the `.rfl` filename without extension)
- `targetRuleProjectName="[ProjectName]"` — exact project name, camelCase, no spaces (see the project naming convention in `rule-project-config.md` — this must match the actual `.ruleproject` name byte-for-byte).

Include a QueryExtractor:

```xml
<extractor xsi:type="ilog.rules.studio.model.query.extractor:QueryExtractor"
           name="[ProjectName]Operation_extractor" validator="Default Validator"/>
```

## UUID consistency (critical)

The deployment operation must link to the EXACT project UUID from `.ruleproject`:

```xml
<targetRuleProject href="../../[ProjectName]#[project-uuid]"/>
```

- `[project-uuid]` MUST equal the `<uuid>` in `.ruleproject` exactly.
- Always read `.ruleproject` to get the UUID before creating the deployment operation.
- A mismatch causes "Cannot load operation" errors.

## Decision operation parameters

- `direction="IN_OUT"` — parameter is modified by rules and returned.
- Missing `direction` defaults to `IN` (read-only in rule execution).
- This defines the service interface exposed by the Decision Service.

## `.dep` file — `ruleAppName` is mandatory (CRITICAL)

The `.dep` deployment configuration MUST set `ruleAppName` on the root element, or the Build Command fails with `A RuleApp name cannot be empty`:

```xml
<!-- WRONG -->
<com.ibm.rules.studio.model.decisionservice:Deployment xmi:version="2.0" ...>

<!-- CORRECT -->
<com.ibm.rules.studio.model.decisionservice:Deployment xmi:version="2.0" ... ruleAppName="ProjectName" managingXom="true">
```

`ruleAppName` should match the project name without spaces (camelCase).

**The ruleset version is also mandatory.** Each `<operations>` entry in the `.dep` must carry a `ruleset.version` property, or the deployment operation is invalid:

```xml
<operations operationName="ProjectNameOperation">
  <operation href="ProjectNameOperation.dop#operation-uuid-here"/>
  <properties key="ruleset.version">
    <value><![CDATA[1.0]]></value>
  </properties>
</operations>
```

The `href` UUID must equal the `.dop`'s `<uuid>`, and `operationName` must equal its `<name>`. Do not add a target server (`<targets .../>`) to the `.dep`. The full `.dep` template (operations, version policies) is in `assets/templates/odm-file-templates.md`.

## Build properties file

`buildcommand/samples/config-files/[ProjectName].properties`:
```
project = ../../../[ProjectName]
output = ../../../[ProjectName]/output
dep = [ProjectName]
xom-classpath = ../../../[xom-project-name]/[xom-project-name]-1.0.0.jar
ruleapp-name = ProjectName
```
- `project` path is relative to the config-files directory.
- `dep` is the RuleApp name (no spaces) — used as the JAR filename prefix.
- `ruleapp-name` is optional but recommended for clarity; keep it consistent with `.dep`'s `ruleAppName`.

## Structural reference

`../schemas/ruleflow.ecore` defines `.rfl` structure and `../schemas/query.ecore` defines the `QueryExtractor` used in `.dop` — consult them if a generated file is rejected and the templates in `assets/templates/` don't cover the case.
