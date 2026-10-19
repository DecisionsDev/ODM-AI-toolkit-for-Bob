import * as vscode from 'vscode';
import { element } from './projectModel';
import { webviewHtml } from './webviewHtml';

/**
 * Read-only viewer for ODM Decision Table (.dta) files.
 *
 * The .dta file is an XML envelope around an inner <DT> document.
 * We extract ConditionDefinition / ActionDefinition templates and
 * then for each Condition row we zip Param values into the template
 * to reconstruct a human-readable row.
 */

export interface DtColumn {
  id: string;
  kind: 'condition' | 'action';
  /** BAL template with <placeholder> markers. */
  template: string;
  /** Placeholder names, in order of appearance. */
  params: string[];
}

export interface DtRow {
  /**
   * One entry per column.  Each entry is an array of raw param values
   * (one per placeholder in that column's template).  Empty array means
   * the cell was not present in the row (e.g. an action not fired).
   */
  cells: string[][];
}

export interface DtableView {
  name: string;
  project: string;
  packagePath: string;
  documentation: string;
  uuid: string;
  columns: DtColumn[];
  rows: DtRow[];
}

// ── XML helpers ────────────────────────────────────────────────────────────

function stripCdata(text: string): string {
  return text.replace(/^<!\[CDATA\[/, '').replace(/\]\]>$/, '').trim();
}

function innerText(xml: string, tag: string): string {
  const m = new RegExp(`<${tag}[^>]*>([\\s\\S]*?)<\\/${tag}>`).exec(xml);
  if (!m) { return ''; }
  return stripCdata(m[1].trim());
}

function attrVal(tag: string, attr: string): string {
  const m = new RegExp(`\\s${attr}="([^"]*)"`).exec(tag);
  return m ? m[1] : '';
}

/** Extract all <placeholder> names from a BAL template. */
function placeholders(template: string): string[] {
  const names: string[] = [];
  for (const m of template.matchAll(/<([^>/]+)>/g)) {
    names.push(m[1]);
  }
  return names;
}

/** Fill a template with values, replacing <placeholder> in order. */
export function fillTemplate(template: string, values: string[]): string {
  let i = 0;
  return template.replace(/<[^>/]+>/g, () => values[i++] ?? '?');
}

/** Extract raw Param values from an <Expression> block. */
function extractParams(exprBody: string): string[] {
  return Array.from(exprBody.matchAll(/<Param[^>]*>([\s\S]*?)<\/Param>/g))
    .map(p => stripCdata(p[1].trim()));
}

// ── Parser ─────────────────────────────────────────────────────────────────

export function parseDtable(xml: string, uri: vscode.Uri): DtableView {
  const name = element(xml, 'name') || uri.path.split('/').pop()!.replace(/\.dta$/, '');
  const uuid = element(xml, 'uuid');
  const docText = element(xml, 'documentation');

  const segments = uri.path.split('/');
  const rulesIdx = segments.lastIndexOf('rules');
  const project = rulesIdx > 0 ? decodeURIComponent(segments[rulesIdx - 1]) : '';
  const packagePath = rulesIdx >= 0
    ? segments.slice(rulesIdx + 1, -1).map(decodeURIComponent).join(' › ')
    : '';

  // Extract the inner <DT>…</DT> block
  const dtMatch = /<DT\b[\s\S]*<\/DT>/.exec(xml);
  const dt = dtMatch?.[0] ?? '';

  // ── Columns ────────────────────────────────────────────────────────────
  const columns: DtColumn[] = [];

  for (const m of dt.matchAll(/<ConditionDefinition\s+Id="([^"]+)"[^>]*>([\s\S]*?)<\/ConditionDefinition>/g)) {
    const template = innerText(m[2], 'Text');
    columns.push({ id: m[1], kind: 'condition', template, params: placeholders(template) });
  }
  for (const m of dt.matchAll(/<ActionDefinition\s+Id="([^"]+)"[^>]*>([\s\S]*?)<\/ActionDefinition>/g)) {
    const template = innerText(m[2], 'Text');
    columns.push({ id: m[1], kind: 'action', template, params: placeholders(template) });
  }

  const colIndexById = new Map(columns.map((c, i) => [c.id, i]));

  // ── Rows ───────────────────────────────────────────────────────────────
  const rows: DtRow[] = [];

  // Each <Partition DefId="C0"> wraps one or more <Condition> rows.
  for (const partMatch of dt.matchAll(/<Partition\s+DefId="([^"]+)"[^>]*>([\s\S]*?)<\/Partition>/g)) {
    const condColId = partMatch[1];
    const condColIdx = colIndexById.get(condColId) ?? -1;

    for (const condMatch of partMatch[2].matchAll(/<Condition[^>]*>([\s\S]*?)<\/Condition>/g)) {
      const condXml = condMatch[1];
      // Each cell is now an array of raw param values (one per placeholder).
      const cells: string[][] = columns.map(() => []);

      // Condition expression params
      if (condColIdx >= 0) {
        const exprMatch = /<Expression[^>]*>([\s\S]*?)<\/Expression>/.exec(condXml);
        cells[condColIdx] = exprMatch ? extractParams(exprMatch[1]) : [];
      }

      // Action params
      const actionSetMatch = /<ActionSet[^>]*>([\s\S]*?)<\/ActionSet>/.exec(condXml);
      if (actionSetMatch) {
        for (const actMatch of actionSetMatch[1].matchAll(/<Action\s+DefId="([^"]+)"[^>]*>([\s\S]*?)<\/Action>/g)) {
          const actIdx = colIndexById.get(actMatch[1]) ?? -1;
          if (actIdx < 0) { continue; }
          const exprMatch = /<Expression[^>]*>([\s\S]*?)<\/Expression>/.exec(actMatch[2]);
          cells[actIdx] = exprMatch ? extractParams(exprMatch[1]) : [];
        }
      }

      rows.push({ cells });
    }
  }

  return { name, project, packagePath, documentation: docText, uuid, columns, rows };
}

// ── Editor Provider ────────────────────────────────────────────────────────

export class DtableEditorProvider implements vscode.CustomTextEditorProvider {
  public static readonly viewType = 'odm-ruleproject.dtableView';

  constructor(private readonly context: vscode.ExtensionContext) {}

  public static register(context: vscode.ExtensionContext): vscode.Disposable {
    return vscode.window.registerCustomEditorProvider(
      DtableEditorProvider.viewType,
      new DtableEditorProvider(context),
      { webviewOptions: { retainContextWhenHidden: false } }
    );
  }

  public resolveCustomTextEditor(document: vscode.TextDocument, panel: vscode.WebviewPanel): void {
    const mediaRoot = vscode.Uri.joinPath(this.context.extensionUri, 'media');
    panel.webview.options = { enableScripts: true, localResourceRoots: [mediaRoot] };
    panel.webview.html = webviewHtml(panel.webview, mediaRoot, 'dtableView', 'Decision Table');

    const update = () =>
      panel.webview.postMessage({ type: 'update', view: parseDtable(document.getText(), document.uri) });

    const changeSub = vscode.workspace.onDidChangeTextDocument(e => {
      if (e.document.uri.toString() === document.uri.toString()) { update(); }
    });
    panel.onDidDispose(() => changeSub.dispose());

    panel.webview.onDidReceiveMessage(async msg => {
      switch (msg.type) {
        case 'ready':
          update();
          break;
        case 'openText':
          await vscode.commands.executeCommand('vscode.openWith', document.uri, 'default');
          break;
      }
    });
  }
}
