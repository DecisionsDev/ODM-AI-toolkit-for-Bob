import * as vscode from 'vscode';
import { webviewHtml } from './webviewHtml';

/**
 * Business-friendly view of a Rule Designer .brl file.
 *
 * The file on disk stays the ODM XML envelope; the webview only shows the rule
 * name, its documentation and the BAL text found in <definition><![CDATA[ ... ]]>.
 * Edits made in the view are written back into that CDATA section.
 */

export interface DocField {
  key: string;
  value: string;
}

export interface ParsedRule {
  name: string;
  title: string;
  project: string;
  packagePath: string;
  docFields: DocField[];
  docText: string;
  definition: string;
}

const DEFINITION_RE = /(<definition>\s*<!\[CDATA\[)([\s\S]*?)(\]\]>\s*<\/definition>)/;
const DOCUMENTATION_RE = /<documentation>\s*(?:<!\[CDATA\[([\s\S]*?)\]\]>|([\s\S]*?))\s*<\/documentation>/;
const NAME_RE = /<name>([^<]*)<\/name>/;
const DOC_FIELD_RE = /^([A-Z][A-Za-z ]{0,30}):\s*(.*)$/;

function decodeXml(text: string): string {
  return text
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&');
}

/** "check-first-checked-count" → "Check first checked count" */
function humanize(name: string): string {
  const words = name.replace(/[-_]+/g, ' ').trim();
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/** Splits "Key: value" documentation lines into fields; other lines continue the previous field. */
function parseDocumentation(raw: string): { fields: DocField[]; text: string } {
  const fields: DocField[] = [];
  const free: string[] = [];
  for (const line of raw.split(/\r?\n/).map(l => l.trim())) {
    if (!line) {
      continue;
    }
    const m = DOC_FIELD_RE.exec(line);
    if (m) {
      fields.push({ key: m[1], value: m[2] });
    } else if (fields.length > 0) {
      const last = fields[fields.length - 1];
      last.value = last.value ? `${last.value} ${line}` : line;
    } else {
      free.push(line);
    }
  }
  return { fields, text: free.join(' ') };
}

/** Removes the common leading indentation so the rule reads cleanly. */
function dedent(text: string): string {
  const lines = text.replace(/\s+$/, '').split(/\r?\n/);
  const indents = lines
    .slice(1)
    .filter(l => l.trim())
    .map(l => /^\s*/.exec(l)![0].length);
  const min = indents.length ? Math.min(...indents) : 0;
  return lines.map((l, i) => (i === 0 ? l.trimStart() : l.slice(Math.min(min, /^\s*/.exec(l)![0].length)))).join('\n');
}

export function parseRule(xml: string, uri: vscode.Uri): ParsedRule {
  const name = decodeXml(NAME_RE.exec(xml)?.[1] ?? '');
  const docMatch = DOCUMENTATION_RE.exec(xml);
  const doc = parseDocumentation(docMatch ? (docMatch[1] ?? decodeXml(docMatch[2] ?? '')) : '');
  const definition = DEFINITION_RE.exec(xml)?.[2] ?? '';

  // .../<Project>/rules/<package>/<sub>/rule.brl
  const segments = uri.path.split('/');
  const rulesIdx = segments.lastIndexOf('rules');
  const project = rulesIdx > 0 ? decodeURIComponent(segments[rulesIdx - 1]) : '';
  const packagePath = rulesIdx >= 0 ? segments.slice(rulesIdx + 1, -1).map(decodeURIComponent).join(' › ') : '';

  const titleField = doc.fields.find(f => f.key === 'Rule');
  const fields = doc.fields.filter(f => f.key !== 'Rule' && f.key !== 'Package');

  return {
    name,
    title: titleField?.value || humanize(name || segments[segments.length - 1].replace(/\.brl$/, '')),
    project,
    packagePath,
    docFields: fields,
    docText: doc.text,
    definition: dedent(definition)
  };
}

/** Re-indents the BAL text the way Rule Designer writes it (sections flush, lines indented). */
function formatDefinition(text: string): string {
  return text
    .replace(/\r\n/g, '\n')
    .split('\n')
    .map(l => l.trim())
    .filter((l, i, all) => l || (i > 0 && all[i - 1]))
    .map(l => (/^(definitions|if|then|else)$/.test(l) || !l ? l : `    ${l}`))
    .join('\n')
    .trim();
}

export class BrlRuleEditorProvider implements vscode.CustomTextEditorProvider {
  public static readonly viewType = 'odm-ruleproject.ruleView';

  constructor(private readonly context: vscode.ExtensionContext) {}

  public static register(context: vscode.ExtensionContext): vscode.Disposable {
    return vscode.window.registerCustomEditorProvider(
      BrlRuleEditorProvider.viewType,
      new BrlRuleEditorProvider(context),
      { webviewOptions: { retainContextWhenHidden: false } }
    );
  }

  public resolveCustomTextEditor(
    document: vscode.TextDocument,
    panel: vscode.WebviewPanel
  ): void {
    const mediaRoot = vscode.Uri.joinPath(this.context.extensionUri, 'media');
    panel.webview.options = { enableScripts: true, localResourceRoots: [mediaRoot] };
    panel.webview.html = webviewHtml(panel.webview, mediaRoot, 'ruleView', 'Rule');

    const update = () =>
      panel.webview.postMessage({ type: 'update', rule: parseRule(document.getText(), document.uri) });

    const changeSub = vscode.workspace.onDidChangeTextDocument(e => {
      if (e.document.uri.toString() === document.uri.toString()) {
        update();
      }
    });
    panel.onDidDispose(() => changeSub.dispose());

    panel.webview.onDidReceiveMessage(async msg => {
      switch (msg.type) {
        case 'ready':
          update();
          break;
        case 'saveDefinition':
          await this.replaceDefinition(document, String(msg.text));
          break;
        case 'openXml':
          await vscode.commands.executeCommand('vscode.openWith', document.uri, 'default');
          break;
      }
    });
  }

  private async replaceDefinition(document: vscode.TextDocument, text: string): Promise<void> {
    const xml = document.getText();
    const m = DEFINITION_RE.exec(xml);
    if (!m) {
      vscode.window.showErrorMessage('This file has no <definition> section to update.');
      return;
    }
    // "]]>" cannot appear inside CDATA: split it across two sections.
    const body = formatDefinition(text).replace(/\]\]>/g, ']]]]><![CDATA[>');
    const start = document.positionAt(m.index + m[1].length);
    const end = document.positionAt(m.index + m[1].length + m[2].length);
    const edit = new vscode.WorkspaceEdit();
    edit.replace(document.uri, new vscode.Range(start, end), body);
    await vscode.workspace.applyEdit(edit);
  }
}
