# Rule Project Configuration

## Project naming convention (CRITICAL — apply before creating anything)

ALL project names use **camelCase, no spaces**, everywhere: `.project`, `.ruleproject`, deployment operations, folder names.

- Rule project: camelCase (e.g. `SimpleLoanApproval`, `FraudDetection`, `CardiovascularRisk`).
- XOM project: camelCase + `-xom` suffix (e.g. `simple-loan-xom`, `fraud-detection-xom`).
- Never use spaces (`Simple Loan Approval` is WRONG; `SimpleLoanApproval` is correct).
- If the user gives a name with spaces, convert it yourself: "Loan Approval" → rule project `LoanApproval`, XOM project `loan-approval-xom`.

This name must be used consistently across `.ruleproject`'s `<name>`, `.project`'s `<name>`, `targetRuleProjectName` in the `.dop`, `ruleAppName` in the `.dep`, and the BOM's `origin` property — a mismatch anywhere breaks the build or produces "Cannot load operation" errors.

## `.ruleproject`

Configure as a Decision Service with these attributes:

- `buildMode="DecisionEngine"` (NOT `"DecisionService"` — common mistake)
- `isADecisionService="true"`
- `migrationFlag="3"`
- `MigratedToOperationId` linking to the deployment operation
- All required namespace declarations
- `modelFolders` for: rules, bom, templates, queries, deployment, resources
- XOM path as an Eclipse platform reference: `url="platform:/[xom-project-name]"`

## `.project`

1. Reference the XOM project so Eclipse links and compiles it before BOM generation. The XOM project name is case-sensitive and must match exactly:

   ```xml
   <projects>
     <project>xom-project-name</project>
   </projects>
   ```

2. Include the IBM ODM build command and natures:
   - Build command: `ilog.rules.studio.model.ruleBuilder`
   - Natures: `ilog.rules.studio.model.decisionProject`, `ilog.rules.studio.model.operationProject`, `ilog.rules.studio.model.ruleNature`

## Standard folders

`rules/`, `bom/`, `templates/`, `queries/`, `deployment/`, `resources/`

## `.gitignore` for a generated project

Include: `output/`, `reports/`, `.syncEntries`

## Notes on portability

- `.ruleproject` uses `platform:/` URIs for workspace references — this is Eclipse-centric, not standard Maven/Gradle, and not portable to non-Eclipse environments without modification.
- Build automation requires IBM ODM tooling, not standard Java tools.

## Three-layer architecture, propagation direction

XOM (Java POJOs) → BOM (business vocabulary bridging Java and business language) → Rules (BAL business logic). Changes propagate **upward only**. The vocabulary is effectively an API contract: changing it breaks existing rules that reference the old phrases.

## Structural reference

`../schemas/model.ecore` defines the `base.ecore` metamodel behind `.ruleproject`/`.rulepackage`/`.var` — consult it if a generated file is rejected and the cause isn't obvious from `assets/templates/`.
