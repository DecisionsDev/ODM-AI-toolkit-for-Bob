import * as vscode from 'vscode';
import { BrlRuleEditorProvider, parseRule } from './ruleEditorProvider';
import { webviewHtml } from './webviewHtml';

/**
 * Business-friendly, read-only view of an ODM ruleflow (.rfl).
 *
 * The extension extracts the <rfModel> XML and a catalog of the project's rule
 * packages (documentation + rule titles); the webview parses the model with
 * DOMParser and draws the flow.
 */

export interface CatalogRule {
  title: string;
  name: string;
  uuid: string;
  path: string;
}

export interface CatalogPackage {
  /** ODM package name: folder path relative to rules/, joined with "." */
  name: string;
  documentation: string;
  rules: CatalogRule[];
}

export interface ParsedRuleflow {
  name: string;
  project: string;
  documentation: string;
  model: string;
  packages: CatalogPackage[];
}

function decodeXml(text: string): string {
  return text
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&');
}

/** Returns the inner <Ruleflow> XML, whether it is stored inline, in CDATA or escaped. */
export function extractModel(rfl: string): string {
  const inner = /<rfModel>([\s\S]*?)<\/rfModel>/.exec(rfl)?.[1] ?? '';
  const cdata = /^\s*<!\[CDATA\[([\s\S]*?)\]\]>\s*$/.exec(inner);
  if (cdata) {
    return cdata[1];
  }
  return inner.trimStart().startsWith('&lt;') ? decodeXml(inner) : inner;
}

const DOC_RE = /<documentation>\s*(?:<!\[CDATA\[([\s\S]*?)\]\]>|([\s\S]*?))\s*<\/documentation>/;

async function readText(uri: vscode.Uri): Promise<string> {
  const open = vscode.workspace.textDocuments.find(d => d.uri.toString() === uri.toString());
  return open ? open.getText() : new TextDecoder().decode(await vscode.workspace.fs.readFile(uri));
}

/** Walks rules/ and collects every package folder with its documentation and rules. */
export async function buildCatalog(rulesRoot: vscode.Uri): Promise<CatalogPackage[]> {
  const packages: CatalogPackage[] = [];

  const walk = async (dir: vscode.Uri, segments: string[]) => {
    let entries: [string, vscode.FileType][];
    try {
      entries = await vscode.workspace.fs.readDirectory(dir);
    } catch {
      return;
    }
    const pkg: CatalogPackage = { name: segments.join('.'), documentation: '', rules: [] };
    for (const [entry, type] of entries.sort(([a], [b]) => a.localeCompare(b))) {
      const uri = vscode.Uri.joinPath(dir, entry);
      if (type === vscode.FileType.Directory) {
        await walk(uri, [...segments, entry]);
      } else if (entry === '.rulepackage') {
        const m = DOC_RE.exec(await readText(uri));
        pkg.documentation = (m?.[1] ?? decodeXml(m?.[2] ?? '')).trim();
      } else if (entry.endsWith('.brl')) {
        const text = await readText(uri);
        const rule = parseRule(text, uri);
        pkg.rules.push({
          title: rule.title,
          name: rule.name,
          uuid: /<uuid>([^<]*)<\/uuid>/.exec(text)?.[1] ?? '',
          path: uri.toString()
        });
      }
    }
    if (segments.length && (pkg.rules.length || pkg.documentation)) {
      packages.push(pkg);
    }
  };

  await walk(rulesRoot, []);
  return packages;
}

export class RuleflowEditorProvider implements vscode.CustomTextEditorProvider {
  public static readonly viewType = 'odm-ruleproject.ruleflowView';

  constructor(private readonly context: vscode.ExtensionContext) {}

  public static register(context: vscode.ExtensionContext): vscode.Disposable {
    return vscode.window.registerCustomEditorProvider(RuleflowEditorProvider.viewType, new RuleflowEditorProvider(context));
  }

  public resolveCustomTextEditor(document: vscode.TextDocument, panel: vscode.WebviewPanel): void {
    const mediaRoot = vscode.Uri.joinPath(this.context.extensionUri, 'media');
    panel.webview.options = { enableScripts: true, localResourceRoots: [mediaRoot] };
    panel.webview.html = webviewHtml(panel.webview, mediaRoot, 'ruleflowView', 'Ruleflow');

    const segments = document.uri.path.split('/');
    const rulesIdx = segments.lastIndexOf('rules');
    const rulesRoot = rulesIdx >= 0
      ? document.uri.with({ path: segments.slice(0, rulesIdx + 1).join('/') })
      : vscode.Uri.joinPath(document.uri, '..');

    const update = async () => {
      const text = document.getText();
      const flow: ParsedRuleflow = {
        name: decodeXml(/<name>([^<]*)<\/name>/.exec(text)?.[1] ?? ''),
        project: rulesIdx > 0 ? decodeURIComponent(segments[rulesIdx - 1]) : '',
        documentation: (() => {
          const m = DOC_RE.exec(text.split('<rfModel>')[0]);
          return (m?.[1] ?? decodeXml(m?.[2] ?? '')).trim();
        })(),
        model: extractModel(text),
        packages: await buildCatalog(rulesRoot)
      };
      panel.webview.postMessage({ type: 'update', flow });
    };

    // Refresh when the flow changes, or when a rule or package of this project is saved.
    const rulesPrefix = rulesRoot.toString();
    const subs = [
      vscode.workspace.onDidChangeTextDocument(e => {
        if (e.document.uri.toString() === document.uri.toString()) {
          update();
        }
      }),
      vscode.workspace.onDidSaveTextDocument(d => {
        if (d.uri.toString().startsWith(rulesPrefix)) {
          update();
        }
      })
    ];
    panel.onDidDispose(() => subs.forEach(s => s.dispose()));

    panel.webview.onDidReceiveMessage(async msg => {
      switch (msg.type) {
        case 'ready':
          await update();
          break;
        case 'openRule':
          await vscode.commands.executeCommand('vscode.openWith', vscode.Uri.parse(msg.path), BrlRuleEditorProvider.viewType);
          break;
        case 'openText':
          await vscode.commands.executeCommand('vscode.openWith', document.uri, 'default');
          break;
      }
    });
  }
}
