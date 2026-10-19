import * as vscode from 'vscode';
import { element, readText } from './projectModel';
import { webviewHtml } from './webviewHtml';

/** One rule or decision table entry shown in the package view. */
export interface PackageRule {
  name: string;
  kind: 'action' | 'decision-table' | 'decision-tree' | 'unknown';
  definition: string;   // BAL text for BRL, empty for DT
  documentation: string;
  uri: string;
}

/** Data model sent to the webview. */
export interface RulePackageView {
  name: string;
  documentation: string;
  uuid: string;
  rulesCount: number;
  rules: PackageRule[];
}

/** Extract the BAL definition from a .brl file. */
function parseRuleKind(xml: string): PackageRule['kind'] {
  if (/ActionRule/.test(xml)) { return 'action'; }
  if (/DecisionTable/.test(xml)) { return 'decision-table'; }
  if (/DecisionTree/.test(xml)) { return 'decision-tree'; }
  return 'unknown';
}

function stripCdata(text: string): string {
  return text.replace(/^<!\[CDATA\[/, '').replace(/\]\]>$/, '').trim();
}

function parseDefinition(xml: string): string {
  const m = /<definition>([\s\S]*?)<\/definition>/.exec(xml);
  return m ? stripCdata(m[1]) : '';
}

async function buildView(uri: vscode.Uri, xml: string): Promise<RulePackageView> {
  const name = element(xml, 'name') || (uri.path.split('/').pop() ?? '.rulepackage');
  const documentation = element(xml, 'documentation');
  const uuid = element(xml, 'uuid');

  // The .rulepackage file lives in the package directory; siblings are the rules.
  const packageDir = vscode.Uri.joinPath(uri, '..');
  let siblings: [string, vscode.FileType][] = [];
  try {
    siblings = await vscode.workspace.fs.readDirectory(packageDir);
  } catch {
    // directory not readable
  }

  const ruleFiles = siblings
    .filter(([n, t]) => t === vscode.FileType.File && (n.endsWith('.brl') || n.endsWith('.dtr') || n.endsWith('.dtx')))
    .map(([n]) => n)
    .sort();

  const rules: PackageRule[] = [];
  for (const file of ruleFiles) {
    const ruleUri = vscode.Uri.joinPath(packageDir, file);
    const ruleXml = (await readText(ruleUri)) ?? '';
    const ruleName = element(ruleXml, 'name') || file.replace(/\.(brl|dtr|dtx)$/, '');
    rules.push({
      name: ruleName,
      kind: file.endsWith('.brl') ? parseRuleKind(ruleXml) : 'decision-table',
      definition: file.endsWith('.brl') ? parseDefinition(ruleXml) : '',
      documentation: element(ruleXml, 'documentation'),
      uri: ruleUri.toString()
    });
  }

  return { name, documentation, uuid, rulesCount: rules.length, rules };
}

export class RulepackageEditorProvider implements vscode.CustomTextEditorProvider {
  public static readonly viewType = 'odm-ruleproject.rulepackageView';

  constructor(private readonly context: vscode.ExtensionContext) {}

  public static register(context: vscode.ExtensionContext): vscode.Disposable {
    return vscode.window.registerCustomEditorProvider(
      RulepackageEditorProvider.viewType,
      new RulepackageEditorProvider(context)
    );
  }

  public resolveCustomTextEditor(document: vscode.TextDocument, panel: vscode.WebviewPanel): void {
    const mediaRoot = vscode.Uri.joinPath(this.context.extensionUri, 'media');
    panel.webview.options = { enableScripts: true, localResourceRoots: [mediaRoot] };
    panel.webview.html = webviewHtml(panel.webview, mediaRoot, 'rulepackageView', 'Rule Package');

    const packageDir = vscode.Uri.joinPath(document.uri, '..');

    const update = async () => {
      const view = await buildView(document.uri, document.getText());
      panel.webview.postMessage({ type: 'update', view });
    };

    const subs = [
      vscode.workspace.onDidChangeTextDocument(e => {
        const p = e.document.uri.path;
        // Refresh when the .rulepackage itself or any sibling rule changes.
        if (
          e.document.uri.toString() === document.uri.toString() ||
          ((p.endsWith('.brl') || p.endsWith('.dtr') || p.endsWith('.dtx')) &&
          vscode.Uri.joinPath(e.document.uri, '..').path === packageDir.path)
        ) {
          update();
        }
      }),
      vscode.workspace.onDidSaveTextDocument(() => update())
    ];
    panel.onDidDispose(() => subs.forEach(s => s.dispose()));

    panel.webview.onDidReceiveMessage(async msg => {
      switch (msg.type) {
        case 'ready':
          await update();
          break;
        case 'openText':
          await vscode.commands.executeCommand('vscode.openWith', document.uri, 'default');
          break;
        case 'openRule':
          await vscode.commands.executeCommand(
            'vscode.openWith',
            vscode.Uri.parse(msg.uri),
            'odm-ruleproject.ruleView'
          );
          break;
      }
    });
  }
}
