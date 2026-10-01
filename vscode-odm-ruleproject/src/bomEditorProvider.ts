import * as vscode from 'vscode';
import { webviewHtml } from './webviewHtml';

/**
 * Business-friendly, read-only view of an ODM .bom file.
 *
 * The BOM gives the structure (classes, attributes, methods, enum values) and the
 * sibling {bom-name}_{LOCALE}.voc file gives the business wording used in rules.
 */

export interface BomMember {
  name: string;
  /** Friendly type label, e.g. "Number", "List of transaction". */
  type: string;
  /** Class name of the referenced concept, if the type points to another BOM class. */
  ref?: string;
  readonly: boolean;
  /** Verbalized navigation phrase, with {this}/{label} placeholders kept for the webview. */
  navigation?: string;
  action?: string;
}

export interface BomMethod {
  signature: string;
  action?: string;
  navigation?: string;
  params: string[];
}

export interface BomConcept {
  className: string;
  qualifiedName: string;
  label: string;
  isEnum: boolean;
  values: { name: string; label: string }[];
  attributes: BomMember[];
  methods: BomMethod[];
}

export interface ParsedBom {
  packageName: string;
  project: string;
  vocabularyFile: string;
  concepts: BomConcept[];
}

const CLASS_RE = /^\s*public\s+(?:abstract\s+)?(?:final\s+)?class\s+(\w+)/;
const MEMBER_RE = /^\s*public\s+((?:(?:static|final|readonly)\s+)*)([\w.$]+(?:\[\])?)\s+(\w+)(?:\s+domain\s+([^;]*?))?\s*(?:;|$)/;
const METHOD_RE = /^\s*public\s+((?:(?:static|final|abstract)\s+)*)(?:([\w.$]+(?:\[\])?)\s+)?(\w+)\s*\(([^)]*)\)/;

function humanize(className: string): string {
  // AMLRequest → "AML request", BaggageItem → "baggage item"
  return className
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
    .split(' ')
    .map(w => (/^[A-Z]{2,}$/.test(w) ? w : w.toLowerCase()))
    .join(' ');
}

function parseVocabulary(text: string): Map<string, string> {
  const entries = new Map<string, string>();
  for (const line of text.split(/\r?\n/)) {
    if (!line.trim() || line.trimStart().startsWith('#')) {
      continue;
    }
    const eq = line.indexOf(' = ');
    if (eq > 0) {
      // Keys are matched without spaces: Rule Designer writes "addScore(int, java.lang.String)".
      entries.set(line.slice(0, eq).replace(/\s+/g, ''), line.slice(eq + 3).trim());
    }
  }
  return entries;
}

export function parseBom(bomText: string, vocText: string, packagePath: string): ParsedBom {
  const voc = parseVocabulary(vocText);
  const packageName = /^\s*package\s+([\w.]+)\s*;/m.exec(bomText)?.[1] ?? '';
  const concepts: BomConcept[] = [];
  let current: BomConcept | null = null;

  for (const line of bomText.split(/\r?\n/)) {
    const cls = CLASS_RE.exec(line);
    if (cls) {
      const qualifiedName = packageName ? `${packageName}.${cls[1]}` : cls[1];
      current = {
        className: cls[1],
        qualifiedName,
        label: voc.get(`${qualifiedName}#concept.label`) ?? humanize(cls[1]),
        isEnum: false,
        values: [],
        attributes: [],
        methods: []
      };
      concepts.push(current);
      continue;
    }
    if (!current) {
      continue;
    }
    if (/^\s*extends\s+java\.lang\.Enum\b/.test(line)) {
      current.isEnum = true;
      continue;
    }
    if (/^\s*}/.test(line)) {
      current = null;
      continue;
    }

    const method = METHOD_RE.exec(line);
    if (method) {
      const name = method[3];
      if (name === current.className) {
        continue; // constructors are technical
      }
      const params = method[4]
        .split(',')
        .map(p => p.trim().split(/\s+/)[0])
        .filter(Boolean);
      const javaParams = params.map(p => (p === 'string' ? 'java.lang.String' : p)).join(',');
      const key = `${current.qualifiedName}.${name}(${javaParams})`;
      current.methods.push({
        signature: `${name}(${params.join(', ')})`,
        action: voc.get(`${key}#phrase.action`),
        navigation: voc.get(`${key}#phrase.navigation`),
        params
      });
      continue;
    }

    const member = MEMBER_RE.exec(line);
    if (member) {
      const modifiers = member[1];
      const name = member[3];
      const key = `${current.qualifiedName}.${name}`;
      if (/\bstatic\b/.test(modifiers)) {
        current.values.push({ name, label: voc.get(`${key}#instance.label`) ?? name });
        continue;
      }
      current.attributes.push({
        name,
        ...describeType(member[2], member[4]),
        readonly: /\breadonly\b/.test(modifiers),
        navigation: voc.get(`${key}#phrase.navigation`),
        action: voc.get(`${key}#phrase.action`)
      });
    }
  }

  // Resolve references to other concepts into business labels.
  const byQualified = new Map(concepts.map(c => [c.qualifiedName, c]));
  for (const c of concepts) {
    for (const a of c.attributes) {
      if (a.ref) {
        const target = byQualified.get(a.ref);
        a.type = a.type.replace('{ref}', target?.label ?? a.ref.split('.').pop()!);
        a.ref = target?.className;
      }
    }
    if (c.isEnum && c.values.length === 0) {
      c.isEnum = false;
    }
  }

  return { packageName, project: '', vocabularyFile: '', concepts };
}

function describeType(javaType: string, domain?: string): { type: string; ref?: string } {
  const simple: Record<string, string> = {
    string: 'Text', 'java.lang.String': 'Text', char: 'Text',
    int: 'Whole number', long: 'Whole number', short: 'Whole number', byte: 'Whole number',
    'java.lang.Integer': 'Whole number', 'java.lang.Long': 'Whole number', 'java.math.BigInteger': 'Whole number',
    double: 'Number', float: 'Number', 'java.lang.Double': 'Number', 'java.lang.Float': 'Number',
    'java.math.BigDecimal': 'Number',
    boolean: 'Yes / No', 'java.lang.Boolean': 'Yes / No',
    'java.util.Date': 'Date', 'java.time.LocalDate': 'Date', 'java.time.LocalDateTime': 'Date & time',
    'java.time.ZonedDateTime': 'Date & time', 'java.time.Duration': 'Duration'
  };
  if (/^java\.util\.(List|Set|Collection|ArrayList|HashSet)$/.test(javaType) || javaType.endsWith('[]')) {
    const element = /class\s+([\w.$]+)/.exec(domain ?? '')?.[1] ?? javaType.replace(/\[\]$/, '');
    const inner = describeType(element);
    return { type: `List of ${inner.ref ? '{ref}' : inner.type.toLowerCase()}`, ref: inner.ref };
  }
  if (simple[javaType]) {
    return { type: simple[javaType] };
  }
  if (javaType.includes('.') && !javaType.startsWith('java.')) {
    return { type: '{ref}', ref: javaType };
  }
  return { type: javaType.split('.').pop()! };
}

export class BomEditorProvider implements vscode.CustomTextEditorProvider {
  public static readonly viewType = 'odm-ruleproject.bomView';

  constructor(private readonly context: vscode.ExtensionContext) {}

  public static register(context: vscode.ExtensionContext): vscode.Disposable {
    return vscode.window.registerCustomEditorProvider(BomEditorProvider.viewType, new BomEditorProvider(context));
  }

  public resolveCustomTextEditor(document: vscode.TextDocument, panel: vscode.WebviewPanel): void {
    const mediaRoot = vscode.Uri.joinPath(this.context.extensionUri, 'media');
    panel.webview.options = { enableScripts: true, localResourceRoots: [mediaRoot] };
    panel.webview.html = webviewHtml(panel.webview, mediaRoot, 'bomView', 'Business Object Model');

    const update = async () => {
      const voc = await this.findVocabulary(document.uri);
      const bom = parseBom(document.getText(), voc?.text ?? '', document.uri.path);
      const segments = document.uri.path.split('/');
      const bomIdx = segments.lastIndexOf('bom');
      bom.project = bomIdx > 0 ? decodeURIComponent(segments[bomIdx - 1]) : '';
      bom.vocabularyFile = voc ? decodeURIComponent(voc.uri.path.split('/').pop()!) : '';
      panel.webview.postMessage({ type: 'update', bom });
    };

    const subs = [
      vscode.workspace.onDidChangeTextDocument(e => {
        if (e.document.uri.toString() === document.uri.toString() || e.document.uri.path.endsWith('.voc')) {
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
        case 'openText':
          await vscode.commands.executeCommand('vscode.openWith', document.uri, 'default');
          break;
      }
    });
  }

  /** Finds {bom-name}_{LOCALE}.voc next to the BOM, preferring en_US. */
  private async findVocabulary(bomUri: vscode.Uri): Promise<{ uri: vscode.Uri; text: string } | undefined> {
    const dir = vscode.Uri.joinPath(bomUri, '..');
    const base = bomUri.path.split('/').pop()!.replace(/\.bom$/, '');
    let entries: [string, vscode.FileType][];
    try {
      entries = await vscode.workspace.fs.readDirectory(dir);
    } catch {
      return undefined;
    }
    const vocs = entries
      .map(([n]) => n)
      .filter(n => n.startsWith(`${base}_`) && n.endsWith('.voc'))
      .sort((a, b) => Number(b.endsWith('_en_US.voc')) - Number(a.endsWith('_en_US.voc')));
    if (!vocs.length) {
      return undefined;
    }
    const uri = vscode.Uri.joinPath(dir, vocs[0]);
    const open = vscode.workspace.textDocuments.find(d => d.uri.toString() === uri.toString());
    const text = open ? open.getText() : new TextDecoder().decode(await vscode.workspace.fs.readFile(uri));
    return { uri, text };
  }
}
