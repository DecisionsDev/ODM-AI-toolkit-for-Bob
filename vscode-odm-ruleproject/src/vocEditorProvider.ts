import * as vscode from 'vscode';
import { BomConcept } from './bomEditorProvider';
import { LoadedBom, loadBoms, projectDirOf } from './projectModel';
import { webviewHtml } from './webviewHtml';

/**
 * Editable table for an ODM vocabulary (.voc, Java-properties text).
 *
 * Every `key = phrase` line becomes a row. Editing a phrase rewrites only the text after
 * the "=" of that line, so comments, blank lines and the uuid header are untouched.
 * Keys are checked against the sibling .bom: a class or member that is not there is flagged.
 */

export interface VocIssue {
  level: 'error' | 'warning';
  message: string;
}

export interface VocEntry {
  line: number;
  key: string;
  className: string;
  member: string;
  kind: string;
  value: string;
  issues: VocIssue[];
}

export interface VocView {
  name: string;
  project: string;
  uuid: string;
  bomFile: string;
  /** Set when no .bom could be found, so entries cannot be validated. */
  noBom: boolean;
  entries: VocEntry[];
  suggestions: string[];
  errors: number;
  warnings: number;
}

const ENTRY_RE = /^(\s*[^#=\s][^=]*?\s*=\s*)(.*)$/;
const KNOWN_KINDS = new Set(['concept.label', 'phrase.navigation', 'phrase.action', 'instance.label']);

/** Splits "pkg.Class.member(java.lang.String)#phrase.action" into its parts. */
export function splitVocKey(key: string): { className: string; member: string; kind: string } {
  const hash = key.indexOf('#');
  const owner = hash < 0 ? key : key.slice(0, hash);
  const kind = hash < 0 ? '' : key.slice(hash + 1);
  if (kind === 'concept.label' || hash < 0) {
    return { className: owner, member: '', kind: kind || 'property' };
  }
  const base = owner.replace(/\(.*$/, '');
  const dot = base.lastIndexOf('.');
  return {
    className: dot < 0 ? '' : base.slice(0, dot),
    member: dot < 0 ? owner : owner.slice(dot + 1),
    kind
  };
}

function normalizeMember(member: string): string {
  return member.replace(/\s+/g, '');
}

function methodKey(name: string, params: string[]): string {
  return `${name}(${params.map(p => (p === 'string' ? 'java.lang.String' : p)).join(',')})`;
}

/** Names the BOM defines for one class: attributes, enum values and method keys like "add(java.lang.String)". */
function bomMembers(concept: BomConcept): Set<string> {
  const names = new Set<string>();
  concept.attributes.forEach(a => names.add(a.name));
  concept.values.forEach(v => names.add(v.name));
  concept.methods.forEach(m => names.add(methodKey(m.signature.slice(0, m.signature.indexOf('(')), m.params)));
  return names;
}

function keyOf(lineMatchPrefix: string): string {
  return lineMatchPrefix.replace(/\s*=\s*$/, '').trim();
}

export function buildVocView(uri: vscode.Uri, text: string, boms: LoadedBom[]): VocView {
  const entries: VocEntry[] = [];
  let uuid = '';
  text.split(/\r?\n/).forEach((lineText, line) => {
    const m = ENTRY_RE.exec(lineText);
    if (!m) {
      return;
    }
    const key = keyOf(m[1]);
    if (key === 'uuid') {
      uuid = m[2].trim();
      return;
    }
    entries.push({ line, key, ...splitVocKey(key), value: m[2].trim(), issues: [] });
  });

  const concepts = new Map<string, BomConcept>();
  boms.forEach(b => b.concepts.forEach(c => concepts.set(c.qualifiedName, c)));
  const members = new Map<string, Set<string>>();
  const noBom = concepts.size === 0;

  const seen = new Set<string>();
  for (const e of entries) {
    if (seen.has(e.key)) {
      e.issues.push({ level: 'warning', message: 'Duplicate key: only one of the lines is used' });
    }
    seen.add(e.key);
    if (e.kind !== 'property' && !KNOWN_KINDS.has(e.kind)) {
      e.issues.push({ level: 'warning', message: `Unknown phrase type "${e.kind}"` });
    }
    if (!e.value) {
      e.issues.push({ level: 'warning', message: 'Empty phrase' });
    }
    if (noBom || e.kind === 'property') {
      continue;
    }
    const concept = concepts.get(e.className);
    if (!concept) {
      e.issues.push({ level: 'error', message: `Class ${e.className || '(none)'} is not in the BOM` });
      continue;
    }
    if (e.member) {
      if (!members.has(e.className)) {
        members.set(e.className, bomMembers(concept));
      }
      if (!members.get(e.className)!.has(normalizeMember(e.member))) {
        e.issues.push({ level: 'error', message: `${concept.className} has no member ${e.member} in the BOM` });
      }
    }
  }

  // Keys the BOM could have a phrase for but the vocabulary does not define yet.
  const suggestions: string[] = [];
  for (const c of concepts.values()) {
    const candidates = [`${c.qualifiedName}#concept.label`];
    c.attributes.forEach(a => {
      candidates.push(`${c.qualifiedName}.${a.name}#phrase.navigation`);
      if (!a.readonly) {
        candidates.push(`${c.qualifiedName}.${a.name}#phrase.action`);
      }
    });
    c.values.forEach(v => candidates.push(`${c.qualifiedName}.${v.name}#instance.label`));
    c.methods.forEach(m => {
      const key = `${c.qualifiedName}.${methodKey(m.signature.slice(0, m.signature.indexOf('(')), m.params)}`;
      candidates.push(`${key}#phrase.navigation`, `${key}#phrase.action`);
    });
    candidates.filter(k => !seen.has(k)).forEach(k => suggestions.push(k));
  }

  const count = (level: VocIssue['level']) => entries.reduce((n, e) => n + e.issues.filter(i => i.level === level).length, 0);
  return {
    name: decodeURIComponent(uri.path.split('/').pop() ?? ''),
    project: projectDirOf(uri, 'bom').name,
    uuid,
    bomFile: boms.map(b => b.file).join(', '),
    noBom,
    entries,
    suggestions,
    errors: count('error'),
    warnings: count('warning')
  };
}

export class VocEditorProvider implements vscode.CustomTextEditorProvider {
  public static readonly viewType = 'odm-ruleproject.vocView';

  constructor(private readonly context: vscode.ExtensionContext) {}

  public static register(context: vscode.ExtensionContext): vscode.Disposable {
    return vscode.window.registerCustomEditorProvider(VocEditorProvider.viewType, new VocEditorProvider(context));
  }

  public resolveCustomTextEditor(document: vscode.TextDocument, panel: vscode.WebviewPanel): void {
    const mediaRoot = vscode.Uri.joinPath(this.context.extensionUri, 'media');
    panel.webview.options = { enableScripts: true, localResourceRoots: [mediaRoot] };
    panel.webview.html = webviewHtml(panel.webview, mediaRoot, 'vocView', 'Vocabulary');

    const update = async () => {
      const bomDir = vscode.Uri.joinPath(document.uri, '..');
      // <base>_<LOCALE>.voc describes <base>.bom; fall back to every BOM of the folder.
      const base = document.uri.path.split('/').pop()!.replace(/_[a-z]{2}(?:_[A-Z]{2})?\.voc$/, '');
      let boms = await loadBoms(bomDir, base);
      if (!boms.length) {
        boms = await loadBoms(bomDir);
      }
      panel.webview.postMessage({ type: 'update', view: buildVocView(document.uri, document.getText(), boms) });
    };

    const subs = [
      vscode.workspace.onDidChangeTextDocument(e => {
        if (e.document.uri.toString() === document.uri.toString() || e.document.uri.path.endsWith('.bom')) {
          update();
        }
      }),
      vscode.workspace.onDidSaveTextDocument(() => update())
    ];
    panel.onDidDispose(() => subs.forEach(s => s.dispose()));

    const eol = document.eol === vscode.EndOfLine.CRLF ? '\r\n' : '\n';

    /** Matches the line the row was built from; false when the file changed under the view. */
    const entryAt = (line: unknown, key: unknown): RegExpExecArray | null => {
      if (typeof line !== 'number' || line < 0 || line >= document.lineCount) {
        return null;
      }
      const m = ENTRY_RE.exec(document.lineAt(line).text);
      return m && keyOf(m[1]) === key ? m : null;
    };

    panel.webview.onDidReceiveMessage(async msg => {
      const edit = new vscode.WorkspaceEdit();
      switch (msg.type) {
        case 'ready':
          break;
        case 'openText':
          await vscode.commands.executeCommand('vscode.openWith', document.uri, 'default');
          return;
        case 'edit': {
          const m = entryAt(msg.line, msg.key);
          if (m) {
            const value = String(msg.value ?? '').replace(/[\r\n]+/g, ' ').trim();
            edit.replace(document.uri, document.lineAt(msg.line).range, m[1] + value);
          }
          break;
        }
        case 'delete':
          if (entryAt(msg.line, msg.key)) {
            edit.delete(document.uri, document.lineAt(msg.line).rangeIncludingLineBreak);
          }
          break;
        case 'add': {
          const key = String(msg.key ?? '').replace(/[\r\n=]+/g, '').trim();
          const value = String(msg.value ?? '').replace(/[\r\n]+/g, ' ').trim();
          if (key) {
            const end = document.lineAt(document.lineCount - 1);
            const lead = end.text.length ? eol : '';
            edit.insert(document.uri, end.range.end, `${lead}${key} = ${value}${eol}`);
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
