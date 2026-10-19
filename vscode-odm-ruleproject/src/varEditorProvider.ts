import * as vscode from 'vscode';
import { attr, decodeXml, element, escapeXml, loadBoms, projectDirOf, readText } from './projectModel';
import { webviewHtml } from './webviewHtml';

/**
 * Editable table for an ODM variable set (.var, XML).
 *
 * Each <variables/> element is one row (name, type, initial value, verbalization). A row edit
 * rewrites only that element, keeping its other attributes. The view also checks the link
 * with the operations (.dop) of the project: the variableSet href and its UUID, and the
 * variableName each operation uses.
 */

export interface VarRow {
  index: number;
  name: string;
  type: string;
  initialValue: string;
  verbalization: string;
  issues: string[];
  /** Operations that read or write this variable. */
  usedBy: string[];
}

export interface VarLink {
  operation: string;
  path: string;
  ok: boolean;
  detail: string;
}

export interface VarView {
  name: string;
  project: string;
  uuid: string;
  documentation: string;
  rows: VarRow[];
  /** Qualified BOM class names for the type picker. */
  types: { qualified: string; label: string }[];
  links: VarLink[];
  errors: number;
}

const VARIABLE_RE = /<variables\b[^>]*?\/>|<variables\b[^>]*?>[\s\S]*?<\/variables>/g;
const SIMPLE_TYPES = ['java.lang.String', 'int', 'long', 'double', 'boolean', 'java.math.BigDecimal', 'java.util.Date'];

function attributes(tag: string): Map<string, string> {
  const result = new Map<string, string>();
  for (const m of /<variables\b[^>]*/.exec(tag)?.[0].matchAll(/\s([\w:.-]+)="([^"]*)"/g) ?? []) {
    result.set(m[1], decodeXml(m[2]));
  }
  return result;
}

function serializeVariable(attrs: Map<string, string>): string {
  const parts = Array.from(attrs, ([k, v]) => `${k}="${escapeXml(v)}"`);
  return `<variables ${parts.join(' ')}/>`;
}

function variableMatches(xml: string): { index: number; start: number; end: number; tag: string }[] {
  return Array.from(xml.matchAll(VARIABLE_RE)).map((m, index) => ({ index, start: m.index!, end: m.index! + m[0].length, tag: m[0] }));
}

export async function buildVarView(uri: vscode.Uri, xml: string): Promise<VarView> {
  const project = projectDirOf(uri, 'rules');
  const name = element(xml, 'name') || decodeURIComponent(uri.path.split('/').pop()!.replace(/\.var$/, ''));
  const uuid = element(xml, 'uuid');

  const rows: VarRow[] = variableMatches(xml).map(v => {
    const a = attributes(v.tag);
    return {
      index: v.index,
      name: a.get('name') ?? '',
      type: a.get('type') ?? '',
      initialValue: a.get('initialValue') ?? '',
      verbalization: a.get('verbalization') ?? '',
      issues: [],
      usedBy: []
    };
  });

  const boms = await loadBoms(vscode.Uri.joinPath(project.dir, 'bom'));
  const types = boms.flatMap(b => b.concepts).map(c => ({ qualified: c.qualifiedName, label: c.label }));
  const known = new Set([...types.map(t => t.qualified), ...SIMPLE_TYPES]);

  const names = new Set<string>();
  for (const r of rows) {
    if (!r.name) {
      r.issues.push('The name is empty');
    } else if (/\s/.test(r.name)) {
      r.issues.push('The name must not contain spaces: the operation refers to it');
    }
    if (r.name && names.has(r.name)) {
      r.issues.push('Another variable already has this name');
    }
    names.add(r.name);
    if (!r.type) {
      r.issues.push('The type is empty');
    } else if (types.length && !known.has(r.type)) {
      r.issues.push('This class is not in the BOM of the project');
    }
    if (!r.verbalization) {
      r.issues.push('The verbalization is empty: rules quote it as \'the …\'');
    } else if (!/^the\s/.test(r.verbalization)) {
      r.issues.push('Rules usually quote a variable as \'the …\'; the verbalization should start with "the"');
    }
  }

  // Operations of the project that use this variable set.
  const links: VarLink[] = [];
  const depDir = vscode.Uri.joinPath(project.dir, 'deployment');
  let files: string[] = [];
  try {
    files = (await vscode.workspace.fs.readDirectory(depDir)).map(([n]) => n).filter(n => n.endsWith('.dop'));
  } catch {
    // no deployment folder
  }
  const rowByName = new Map(rows.map(r => [r.name, r]));
  for (const file of files) {
    const dopUri = vscode.Uri.joinPath(depDir, file);
    const dop = (await readText(dopUri)) ?? '';
    for (const m of dop.matchAll(/<referencedVariables\b([^>]*)>([\s\S]*?)<\/referencedVariables>/g)) {
      const href = /<variableSet\s+href="([^"]*)"/.exec(m[2])?.[1];
      const [path, hrefUuid = ''] = decodeXml(href ?? '').split('#');
      const target = path ? vscode.Uri.joinPath(dopUri, '..', ...decodeURIComponent(path).split('/')) : undefined;
      const sameFile = target?.path === uri.path;
      const setName = attr(m[1], 'variableSetName');
      if (!sameFile && setName !== name) {
        continue;
      }
      const variableName = attr(m[1], 'variableName');
      const problems: string[] = [];
      if (!sameFile) {
        problems.push(`the variableSet href points to ${path ? decodeURIComponent(path) : 'nothing'}, not to this file`);
      } else if (hrefUuid.toLowerCase() !== uuid.toLowerCase()) {
        problems.push(`the href UUID ${hrefUuid || '(none)'} differs from this file's UUID ${uuid || '(none)'}`);
      }
      if (setName !== name) {
        problems.push(`variableSetName is "${setName}", the set is named "${name}"`);
      }
      const row = rowByName.get(variableName);
      if (!row) {
        problems.push(`the variable "${variableName}" does not exist in this set`);
      } else {
        row.usedBy.push(file.replace(/\.dop$/, ''));
      }
      links.push({
        operation: file.replace(/\.dop$/, ''),
        path: dopUri.toString(),
        ok: problems.length === 0,
        detail: problems.length ? problems.join('; ') : `uses "${variableName}", UUID matches`
      });
    }
  }

  return {
    name,
    project: project.name,
    uuid,
    documentation: element(xml, 'documentation'),
    rows,
    types,
    links,
    errors: rows.reduce((n, r) => n + r.issues.length, 0) + links.filter(l => !l.ok).length
  };
}

export class VarEditorProvider implements vscode.CustomTextEditorProvider {
  public static readonly viewType = 'odm-ruleproject.varView';

  constructor(private readonly context: vscode.ExtensionContext) {}

  public static register(context: vscode.ExtensionContext): vscode.Disposable {
    return vscode.window.registerCustomEditorProvider(VarEditorProvider.viewType, new VarEditorProvider(context));
  }

  public resolveCustomTextEditor(document: vscode.TextDocument, panel: vscode.WebviewPanel): void {
    const mediaRoot = vscode.Uri.joinPath(this.context.extensionUri, 'media');
    panel.webview.options = { enableScripts: true, localResourceRoots: [mediaRoot] };
    panel.webview.html = webviewHtml(panel.webview, mediaRoot, 'varView', 'Variables');

    const update = async () => {
      panel.webview.postMessage({ type: 'update', view: await buildVarView(document.uri, document.getText()) });
    };

    const subs = [
      vscode.workspace.onDidChangeTextDocument(e => {
        const p = e.document.uri.path;
        if (e.document.uri.toString() === document.uri.toString() || p.endsWith('.bom') || p.endsWith('.dop')) {
          update();
        }
      }),
      vscode.workspace.onDidSaveTextDocument(() => update())
    ];
    panel.onDidDispose(() => subs.forEach(s => s.dispose()));

    const eol = document.eol === vscode.EndOfLine.CRLF ? '\r\n' : '\n';
    const at = (offset: number) => document.positionAt(offset);

    panel.webview.onDidReceiveMessage(async msg => {
      const edit = new vscode.WorkspaceEdit();
      const text = document.getText();
      switch (msg.type) {
        case 'ready':
          break;
        case 'openText':
          await vscode.commands.executeCommand('vscode.openWith', document.uri, 'default');
          return;
        case 'open':
          await vscode.commands.executeCommand('vscode.openWith', vscode.Uri.parse(msg.path), 'odm-ruleproject.operationView');
          return;
        case 'edit': {
          const v = variableMatches(text)[msg.index];
          // The name guards against a stale row after the file changed under the view.
          if (v && attributes(v.tag).get('name') === msg.previousName) {
            const attrs = attributes(v.tag);
            for (const field of ['name', 'type', 'initialValue', 'verbalization']) {
              if (typeof msg[field] === 'string') {
                attrs.set(field, msg[field].replace(/[\r\n]+/g, ' ').trim());
              }
            }
            edit.replace(document.uri, new vscode.Range(at(v.start), at(v.end)), serializeVariable(attrs));
          }
          break;
        }
        case 'delete': {
          const v = variableMatches(text)[msg.index];
          if (v && attributes(v.tag).get('name') === msg.previousName) {
            // Take the whole line when the element is alone on it.
            const line = document.lineAt(at(v.start).line);
            const alone = line.text.trim() === v.tag.trim() && !v.tag.includes('\n');
            edit.delete(document.uri, alone ? line.rangeIncludingLineBreak : new vscode.Range(at(v.start), at(v.end)));
          }
          break;
        }
        case 'add': {
          const close = text.lastIndexOf('</');
          if (close >= 0) {
            const attrs = new Map<string, string>([
              ['name', String(msg.name ?? 'newVariable').trim() || 'newVariable'],
              ['type', String(msg.type ?? 'java.lang.String').trim()],
              ['initialValue', ''],
              ['verbalization', String(msg.verbalization ?? '').trim()]
            ]);
            const startOfClose = document.lineAt(at(close).line).range.start;
            const onOwnLine = text.slice(document.offsetAt(startOfClose), close).trim() === '';
            edit.insert(document.uri, onOwnLine ? startOfClose : at(close), `  ${serializeVariable(attrs)}${eol}`);
          }
          break;
        }
      }
      if (edit.size) {
        await vscode.workspace.applyEdit(edit);
      }
      await update();
    });
  }
}
