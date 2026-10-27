# Deploying a rule project to Decision Center

Two steps: `odm export` zips the project locally, then the **ODM Management MCP Server** ([DecisionsDev/ibm-odm-management-mcp-server](https://github.com/DecisionsDev/ibm-odm-management-mcp-server)) imports it into Decision Center and deploys it to a Rule Execution Server. Don't confuse it with the *Decision* MCP Server (`ibm-odm-decision-mcp-server`), which only runs rulesets already deployed to RES. To deploy straight to RES without Decision Center, see `decision-server-console-deployment.md`.

## 1. Export the archive

Build first. Decision Center imports a project that doesn't compile and only fails later, when it builds the RuleApp:

```bash
java -jar <skill>/scripts/odm.jar xom <parent>/<xom-dir>
java -jar <skill>/scripts/odm.jar build <parent>/<Name>.properties      # must print BUILD SUCCESS
java -jar <skill>/scripts/odm.jar export <parent>/<Name>                # --out <file.zip> to choose the path
```

`export` runs `odm check` (errors only) and refuses to write the archive when:
- the XOM has no compiled classes, or its `src/` is newer than `bin/`. Run `odm xom` again.
- the folder name and the `<name>` in `.ruleproject` differ. Decision Center names the decision service after `<name>`.

It prints the absolute archive path, which the MCP tool needs. The archive uses the same layout as a Rule Designer publish:

```
<Name>/.project, .ruleproject, bom/, rules/, deployment/, templates/, queries/, resources/
<Name>/resources/xom-libraries/<xom>.zip     # managed XOM: the compiled classes from <xom>/bin
<xom>/.project, .classpath, pom.xml, src/    # XOM sources, so Rule Designer can import the zip too
```

`output/`, `reports/`, `.gitignore`, `.DS_Store` and `.syncEntries` are left out. The jars in `<xom>/lib` (Jackson) are not included. Jackson annotations are only metadata, so the XOM loads without them on RES. If XOM code calls another library at runtime, deploy that library to RES separately (RES console tool `deployResource`).

The `.dep` that `odm init` generates has `managingXom="true"`, so the XOM is deployed with the RuleApp. `export` warns when a `.dep` doesn't have it.

## 2. Import and deploy with the MCP server

**Ask the user first.** Importing and deploying change a shared server. Say which Decision Center, which decision service, and whether it is a new import or an update of an existing one, then wait for approval.

The tool names below come from the Decision Center REST API `operationId`s. Their parameter names come from the server's OpenAPI, so read each tool's input schema before you call it and don't guess the parameter names.

| Step | Tool | Notes |
|---|---|---|
| 1. Does the service already exist? | `decisionServices` | Look for the `<name>` from `.ruleproject`. |
| 2a. New service | `decisionServicesImport` | Set `file` to the **absolute** archive path from `odm export`. The response has `decisionService.id`. |
| 2b. Existing service | `branches`, then `branchImport` | Get the decision service's main branch id, then import the archive on top of that branch with `file`. Don't use `decisionServicesImport` for this: it doesn't update an existing service. |
| 3. Find the deployment | `deploymentConfigurations` | Use the decision service id. Pick the configuration named like the `.dep` (`<Name>`). |
| 4. Choose the target | `servers` | Only if `deploy` needs a server and the configuration doesn't set one. |
| 5. Deploy | `deploy` | Use the deployment configuration id. Report the result: `DeploymentReport` / `DeploymentReports` give the details. `build` only builds the RuleApp, and `download` returns the archive without deploying it. |

After a deployment, the Decision MCP Server (`ibm-odm-decision-mcp-server`), if it is connected, exposes the new ruleset version as a tool once it has been refreshed. Use it to run a smoke test.

**The server reads `file` from its own disk.** It opens the path locally, so it must run on the same machine as the archive (the usual stdio setup in Bob or Claude). If it runs remotely or in a container, the path won't resolve. Copy the archive to a volume the server can see, or use the manual route below.

### When a tool is missing

- **No management server connected**, or it failed to connect: say so. Give the user the archive path and the manual route: in Decision Center, **Library → Import Decision Service**, then **Deploy** from the deployment configuration. The same REST call is `POST <dc-api>/v1/decisionservices/import` with multipart `file=@<archive>;type=application/zip`. Don't claim the rules are deployed.
- **The server is connected but doesn't list `decisionServicesImport` / `branchImport`.** These tools have the `Admin` tag, and `deploy` has the `Build` tag. The server was started with `--tags` or `--tools` options that exclude them, or the user's Decision Center role doesn't grant them. Ask the user to add them, for example `--tools decisionServices branches decisionServicesImport branchImport deploymentConfigurations servers deploy DeploymentReport`.
- **`401`**: the credentials are wrong (`ODM_USERNAME`/`ODM_PASSWORD`, `ZENAPIKEY`, or OpenID client settings).
- **The upload is rejected when the archive is larger than 20 MB.** Decision Center's default upload limit is 20 MB. The archive holds no jars, so this means something large was left in the rule project (look under `resources/`).
