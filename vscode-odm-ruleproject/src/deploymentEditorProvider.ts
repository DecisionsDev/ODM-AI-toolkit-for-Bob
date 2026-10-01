import * as vscode from 'vscode';
import { parseBom } from './bomEditorProvider';
import { RuleflowEditorProvider } from './ruleflowEditorProvider';
import { webviewHtml } from './webviewHtml';

/**
 * Business-friendly, read-only views of the decision service files in deployment/:
 * operations (.dop) and deployment configurations (.dep).
 *
 * Both views resolve the hrefs the files contain (ruleflow, variable set, rule project,
 * operations) and check that the UUID after "#" matches the target file, since a stale
 * UUID breaks the build and the Decision Center import.
 */

export interface LinkCheck {
  label: string;
  ok: boolean;
  detail: string;
  /** File to open when the user clicks the check. */
  target?: string;
}

export interface OperationParameter {
  name: string;
  direction: string;
  verbalization: string;
  type: string;
  typeLabel: string;
}

export interface OperationView {
  kind: 'operation';
  name: string;
  project: string;
  documentation: string;
  rulesetName: string;
  usingRuleflow: boolean;
  ruleflow?: { name: string; path?: string };
  parameters: OperationParameter[];
  deployments: { name: string; path: string }[];
  checks: LinkCheck[];
}

export interface DeploymentOperation {
  name: string;
  rulesetName: string;
  version: string;
  properties: { key: string; value: string }[];
  path?: string;
}

export interface DeploymentView {
  kind: 'deployment';
  name: string;
  project: string;
  documentation: string;
  ruleAppName: string;
  managingXom: boolean;
  targets: string[];
  operations: DeploymentOperation[];
  policies: { label: string; description: string; isDefault: boolean }[];
  groups: string;
  checks: LinkCheck[];
}

function decodeXml(text: string): string {
  return text
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&');
}

function attr(tag: string, name: string): string {
  const m = new RegExp(`\\s${name}="([^"]*)"`).exec(tag);
  return m ? decodeXml(m[1]) : '';
}

function element(xml: string, name: string): string {
  const m = new RegExp(`<${name}>\\s*(?:<!\\[CDATA\\[([\\s\\S]*?)\\]\\]>|([\\s\\S]*?))\\s*</${name}>`).exec(xml);
  return m ? (m[1] ?? decodeXml(m[2] ?? '')).trim() : '';
}

/** Root element's opening tag (after the XML declaration). */
function rootTag(xml: string): string {
  return /<(?!\?)[\w.:]+\s[^>]*>/.exec(xml)?.[0] ?? '';
}

async function readText(uri: vscode.Uri): Promise<string | undefined> {
  const open = vscode.workspace.textDocuments.find(d => d.uri.toString() === uri.toString());
  if (open) {
    return open.getText();
  }
  try {
    return new TextDecoder().decode(await vscode.workspace.fs.readFile(uri));
  } catch {
    return undefined;
  }
}

/** Resolves "../rules/x.rfl#uuid" against the folder of `from`. */
function resolveHref(from: vscode.Uri, href: string): { uri: vscode.Uri; uuid: string } {
  const [path, uuid = ''] = href.split('#');
  return { uri: vscode.Uri.joinPath(from, '..', ...decodeURIComponent(path).split('/')), uuid };
}

/** Checks that the file behind an href exists and carries the expected <uuid>. */
async function checkHref(from: vscode.Uri, href: string, label: string, uuidFile?: string): Promise<LinkCheck & { text?: string; uri: vscode.Uri }> {
  const { uri, uuid } = resolveHref(from, href);
  const file = uuidFile ? vscode.Uri.joinPath(uri, uuidFile) : uri;
  const text = await readText(file);
  const name = decodeURIComponent(uri.path.split('/').pop() ?? '');
  if (text === undefined) {
    return { label, ok: false, detail: `${name} not found`, uri: file };
  }
  const actual = /<uuid>([^<]*)<\/uuid>/.exec(text)?.[1] ?? '';
  const ok = !uuid || actual.toLowerCase() === uuid.toLowerCase();
  return {
    label,
    ok,
    detail: ok ? name : `${name}: link points to UUID ${uuid}, but the file has ${actual || 'no UUID'}`,
    target: file.toString(),
    text,
    uri: file
  };
}

/** Business label of a BOM class, read from the rule project's BOM and vocabulary. */
async function conceptLabels(projectDir: vscode.Uri): Promise<Map<string, string>> {
  const labels = new Map<string, string>();
  const bomDir = vscode.Uri.joinPath(projectDir, 'bom');
  let entries: [string, vscode.FileType][] = [];
  try {
    entries = await vscode.workspace.fs.readDirectory(bomDir);
  } catch {
    return labels;
  }
  for (const [name] of entries.filter(([n]) => n.endsWith('.bom'))) {
    const base = name.replace(/\.bom$/, '');
    const voc = entries.map(([n]) => n).find(n => n === `${base}_en_US.voc`) ?? entries.map(([n]) => n).find(n => n.startsWith(`${base}_`) && n.endsWith('.voc'));
    const bomText = (await readText(vscode.Uri.joinPath(bomDir, name))) ?? '';
    const vocText = voc ? (await readText(vscode.Uri.joinPath(bomDir, voc))) ?? '' : '';
    for (const c of parseBom(bomText, vocText, name).concepts) {
      labels.set(c.qualifiedName, c.label);
    }
  }
  return labels;
}

function projectInfo(uri: vscode.Uri): { name: string; dir: vscode.Uri } {
  const segments = uri.path.split('/');
  const idx = segments.lastIndexOf('deployment');
  const dir = idx > 0 ? uri.with({ path: segments.slice(0, idx).join('/') }) : vscode.Uri.joinPath(uri, '..', '..');
  return { name: idx > 0 ? decodeURIComponent(segments[idx - 1]) : '', dir };
}

const DIRECTIONS: Record<string, string> = { IN: 'Input', OUT: 'Output', IN_OUT: 'Input & output' };

export async function buildOperationView(uri: vscode.Uri, xml: string): Promise<OperationView> {
  const root = rootTag(xml);
  const project = projectInfo(uri);
  const checks: LinkCheck[] = [];
  const labels = await conceptLabels(project.dir);

  const projectHref = /<targetRuleProject\s+href="([^"]*)"/.exec(xml)?.[1];
  if (projectHref) {
    const c = await checkHref(uri, decodeXml(projectHref), 'Rule project', '.ruleproject');
    checks.push({ label: c.label, ok: c.ok, detail: c.detail.replace(/^\.ruleproject/, attr(root, 'targetRuleProjectName') || 'rule project'), target: c.target });
  }

  let ruleflow: OperationView['ruleflow'];
  const usingRuleflow = attr(root, 'usingRuleflow') === 'true';
  const flowHref = /<ruleflow\s+href="([^"]*)"/.exec(xml)?.[1];
  if (flowHref) {
    const c = await checkHref(uri, decodeXml(flowHref), 'Ruleflow');
    checks.push(c);
    ruleflow = { name: attr(root, 'ruleflowName') || c.detail, path: c.text !== undefined ? c.uri.toString() : undefined };
  } else if (usingRuleflow) {
    ruleflow = { name: attr(root, 'ruleflowName') };
  }

  const parameters: OperationParameter[] = [];
  const varSets = new Map<string, string>();
  for (const m of xml.matchAll(/<referencedVariables\b([^>]*)>([\s\S]*?)<\/referencedVariables>/g)) {
    const name = attr(m[1], 'variableName');
    const setHref = /<variableSet\s+href="([^"]*)"/.exec(m[2])?.[1];
    let text: string | undefined;
    if (setHref) {
      const key = decodeXml(setHref);
      if (!varSets.has(key)) {
        const c = await checkHref(uri, key, `Parameters (${attr(m[1], 'variableSetName')})`);
        checks.push(c);
        varSets.set(key, c.text ?? '');
      }
      text = varSets.get(key);
    }
    const variable = new RegExp(`<variables\\b[^>]*\\sname="${name}"[^>]*>`).exec(text ?? '')?.[0] ?? '';
    const type = attr(variable, 'type');
    parameters.push({
      name,
      direction: DIRECTIONS[attr(m[1], 'direction')] ?? attr(m[1], 'direction'),
      verbalization: attr(variable, 'verbalization') || name,
      type,
      typeLabel: labels.get(type) ?? type.split('.').pop() ?? ''
    });
  }

  // Deployments in the same folder that publish this operation.
  const deployments: OperationView['deployments'] = [];
  const dopName = uri.path.split('/').pop()!;
  const dir = vscode.Uri.joinPath(uri, '..');
  try {
    for (const [name] of await vscode.workspace.fs.readDirectory(dir)) {
      if (!name.endsWith('.dep')) {
        continue;
      }
      const depUri = vscode.Uri.joinPath(dir, name);
      const dep = (await readText(depUri)) ?? '';
      if (dep.includes(`href="${dopName}#`) || dep.includes(`href="${encodeURI(dopName)}#`)) {
        deployments.push({ name: attr(rootTag(dep), 'ruleAppName') || name.replace(/\.dep$/, ''), path: depUri.toString() });
      }
    }
  } catch {
    // no deployment folder listing
  }

  return {
    kind: 'operation',
    name: element(xml, 'name') || dopName.replace(/\.dop$/, ''),
    project: project.name,
    documentation: element(xml, 'documentation'),
    rulesetName: attr(root, 'rulesetName'),
    usingRuleflow,
    ruleflow,
    parameters,
    deployments,
    checks
  };
}

export async function buildDeploymentView(uri: vscode.Uri, xml: string): Promise<DeploymentView> {
  const root = rootTag(xml);
  const checks: LinkCheck[] = [];
  const operations: DeploymentOperation[] = [];

  for (const m of xml.matchAll(/<operations\b([^>]*)>([\s\S]*?)<\/operations>/g)) {
    const properties: { key: string; value: string }[] = [];
    for (const p of m[2].matchAll(/<properties\s+key="([^"]*)"\s*>\s*<value>\s*(?:<!\[CDATA\[([\s\S]*?)\]\]>|([\s\S]*?))\s*<\/value>/g)) {
      properties.push({ key: p[1], value: (p[2] ?? decodeXml(p[3] ?? '')).trim() });
    }
    const op: DeploymentOperation = {
      name: attr(m[1], 'operationName'),
      rulesetName: '',
      version: properties.find(p => p.key === 'ruleset.version')?.value ?? '1.0',
      properties: properties.filter(p => p.key !== 'ruleset.version')
    };
    const href = /<operation\s+href="([^"]*)"/.exec(m[2])?.[1];
    if (href) {
      const c = await checkHref(uri, decodeXml(href), `Operation ${op.name}`);
      checks.push(c);
      if (c.text !== undefined) {
        op.path = c.uri.toString();
        op.rulesetName = attr(rootTag(c.text), 'rulesetName');
      }
    }
    operations.push(op);
  }

  const policies = Array.from(xml.matchAll(/<versionPolicies\b([^>]*)>([\s\S]*?)<\/versionPolicies>/g)).map(m => ({
    label: attr(m[1], 'label'),
    description: element(m[2], 'description'),
    isDefault: attr(m[1], 'default') === 'true'
  }));

  return {
    kind: 'deployment',
    name: element(xml, 'name'),
    project: projectInfo(uri).name,
    documentation: element(xml, 'documentation'),
    ruleAppName: attr(root, 'ruleAppName'),
    managingXom: attr(root, 'managingXom') === 'true',
    targets: Array.from(xml.matchAll(/<targets\b([^>]*)\/?>/g)).map(m => attr(m[1], 'label')).filter(Boolean),
    operations,
    policies,
    groups: /<details\s+key="Dep_Groups"\s+value="([^"]*)"/.exec(xml)?.[1] ?? '',
    checks
  };
}

/** One provider class serves both file types; the view type decides which model is built. */
export class DeploymentEditorProvider implements vscode.CustomTextEditorProvider {
  public static readonly operationViewType = 'odm-ruleproject.operationView';
  public static readonly deploymentViewType = 'odm-ruleproject.deploymentView';

  constructor(private readonly context: vscode.ExtensionContext, private readonly kind: 'operation' | 'deployment') {}

  public static register(context: vscode.ExtensionContext): vscode.Disposable {
    return vscode.Disposable.from(
      vscode.window.registerCustomEditorProvider(DeploymentEditorProvider.operationViewType, new DeploymentEditorProvider(context, 'operation')),
      vscode.window.registerCustomEditorProvider(DeploymentEditorProvider.deploymentViewType, new DeploymentEditorProvider(context, 'deployment'))
    );
  }

  public resolveCustomTextEditor(document: vscode.TextDocument, panel: vscode.WebviewPanel): void {
    const mediaRoot = vscode.Uri.joinPath(this.context.extensionUri, 'media');
    panel.webview.options = { enableScripts: true, localResourceRoots: [mediaRoot] };
    panel.webview.html = webviewHtml(panel.webview, mediaRoot, 'deploymentView', this.kind === 'operation' ? 'Operation' : 'Deployment');

    const update = async () => {
      const view = this.kind === 'operation'
        ? await buildOperationView(document.uri, document.getText())
        : await buildDeploymentView(document.uri, document.getText());
      panel.webview.postMessage({ type: 'update', view });
    };

    const subs = [
      vscode.workspace.onDidChangeTextDocument(e => {
        if (e.document.uri.toString() === document.uri.toString()) {
          update();
        }
      }),
      // Linked files (ruleflow, .var, .dop, .ruleproject) may change the checks.
      vscode.workspace.onDidSaveTextDocument(() => update())
    ];
    panel.onDidDispose(() => subs.forEach(s => s.dispose()));

    panel.webview.onDidReceiveMessage(async msg => {
      switch (msg.type) {
        case 'ready':
          await update();
          break;
        case 'open': {
          const target = vscode.Uri.parse(msg.path);
          const viewType = target.path.endsWith('.rfl')
            ? RuleflowEditorProvider.viewType
            : target.path.endsWith('.dop')
              ? DeploymentEditorProvider.operationViewType
              : target.path.endsWith('.dep')
                ? DeploymentEditorProvider.deploymentViewType
                : 'default';
          await vscode.commands.executeCommand('vscode.openWith', target, viewType);
          break;
        }
        case 'openText':
          await vscode.commands.executeCommand('vscode.openWith', document.uri, 'default');
          break;
      }
    });
  }
}
