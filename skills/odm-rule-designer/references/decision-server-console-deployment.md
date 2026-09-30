# Deploying a RuleApp to the Rule Execution Server console

Two steps: `odm build` has the rules compiler build a RuleApp archive with the XOM inside, then the **ODM Management MCP Server** ([DecisionsDev/ibm-odm-management-mcp-server](https://github.com/DecisionsDev/ibm-odm-management-mcp-server)) deploys that archive to the Decision Server console (RES console). Decision Center is not involved. To go through Decision Center instead, see `decision-center-deployment.md`.

## 1. Build the RuleApp archive

```bash
java -jar <skill>/scripts/odm.jar xom <parent>/<xom-dir>
java -jar <skill>/scripts/odm.jar check <parent>/<Name>
java -jar <skill>/scripts/odm.jar build <parent>/<Name>.properties
```

`build` always compiles with `embedded-xom = true`. `odm init` writes the key in the `.properties`. For an older `.properties` without it, `build` uses a temporary copy with the key and deletes it afterwards. After `BUILD SUCCESS` it prints:

```
ruleapp: /abs/path/<Name>/output/<RuleAppName>.jar
RuleApp <RuleAppName>/1.0, embedded XOM <xom>-1.0.0_1.0.jar
  ruleset /<RuleAppName>/1.0/<RulesetName>/1.0  managed XOM reslib://<RuleAppName>_1.0/1.0
RES console: deployRuleAppArchive with file=/abs/path/...
```

Why the XOM is always embedded: without it, the compiler gives a RuleApp with only `ruleset.dsar`, which doesn't reference the XOM. On RES, that ruleset fails at execution because the XOM classes aren't found. With the XOM embedded, RES creates the managed XOM resource and library (`reslib://<RuleAppName>_<version>`) from the archive, so one upload is enough.

`build` refuses to run when the XOM jar named by `xom-classpath` is missing or older than the `.java` files in `<xom>/src`. Run `odm xom` again. The RuleApp name comes from `ruleAppName` in the `.dep`, and the ruleset path comes from the archive's `META-INF/archive.xml`. Give the user the ruleset path, because clients call it: `/<RuleAppName>/<version>/<RulesetName>/<version>`.

Only the XOM jar is embedded. The jars in `<xom>/lib` (Jackson) are not. Jackson annotations are only metadata, so the XOM loads without them. If XOM code calls another library at run time, deploy that library with `deployResource` and `addLibrary`, or ask the RES administrator to deploy it.

## 2. Deploy with the MCP server

**Ask the user first.** A deployment changes a shared server and can replace a ruleset that is already in use. Say which RES console you'll use (the server's `--res-url`), the RuleApp and ruleset paths, and whether the deployment replaces an existing version or adds a new one. Then wait for approval.

The RES tools are only published when the server was started with `--res-url` (or `ODM_RES_URL`). Their names and parameters come from the RES console's REST API description (WADL). Read each tool's input schema before you call it, and take the `merging` and `versioning` values from its enum. Don't guess them.

| Step | Tool | Notes |
|---|---|---|
| 1. Is it already deployed? | `getRuleApps` (or `getRuleApp`) | Look for `<RuleAppName>`. Note the versions that exist. |
| 2. Deploy | `deployRuleAppArchive` | Set `file` to the **absolute** path that `odm build` printed after `ruleapp:`. Pick `merging`/`versioning` with the user: replace the existing version (fine in development), or add a new version (keeps the old one available to its clients). |
| 3. Check | `getRuleset` / `getRulesets` | Confirm the ruleset path and version that RES now holds, and report them. |

After a deployment, the Decision MCP Server (`ibm-odm-decision-mcp-server`), if it is connected, exposes the new ruleset as a tool once it has been refreshed. Use it to run a smoke test.

**The server reads `file` from its own disk.** It opens the path locally and sends the bytes as `application/octet-stream`. So it must run on the same machine as the archive (the usual stdio setup in Bob or Claude). If it runs remotely or in a container, the path won't resolve. Copy the archive to a volume the server can see, or use the manual route below.

### When a tool is missing

- **No management server connected**, or it failed to connect: say so. Give the user the archive path and the manual route: in the RES console, **Explorer → Deploy RuleApp Archive**, and choose the jar. The same REST call is `POST <res-console>/apiauth/v1/ruleapps` with the jar as the `application/octet-stream` body. Don't claim the RuleApp is deployed.
- **The server is connected but lists no RES tools** (`getRuleApps`, `deployRuleAppArchive`): it was started without `--res-url`, or with `--tags`/`--tools` options that exclude them. Ask the user to add `--res-url <res-console-url>`, and if needed `--tags Ruleapps` or `--tools getRuleApps getRuleApp deployRuleAppArchive getRulesets getRuleset`.
- **`getRuleApps` works but `deployRuleAppArchive` is missing or returns `403`**: the credentials only have the `resMonitor` role. Deploying needs `resDeployer`.
- **`401`**: the credentials are wrong (`ODM_USERNAME`/`ODM_PASSWORD`, `ZENAPIKEY`, or OpenID client settings).
- **The ruleset is deployed but execution fails with `ClassNotFoundException`**: the archive has no embedded XOM (`build` printed `WARN  the XOM is not embedded`, or the jar came from another tool such as the `build_ruleset` MCP tool), or a library the XOM needs isn't on RES. Rebuild with `odm build` and deploy again.
