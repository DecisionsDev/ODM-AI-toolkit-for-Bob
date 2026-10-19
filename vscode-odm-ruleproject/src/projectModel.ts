import * as vscode from 'vscode';
import { BomConcept, parseBom } from './bomEditorProvider';

/** Helpers shared by the vocabulary (.voc) and variable set (.var) editors. */

export function decodeXml(text: string): string {
  return text
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&');
}

export function escapeXml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

export function attr(tag: string, name: string): string {
  const m = new RegExp(`\\s${name}="([^"]*)"`).exec(tag);
  return m ? decodeXml(m[1]) : '';
}

export function element(xml: string, name: string): string {
  const m = new RegExp(`<${name}>\\s*(?:<!\\[CDATA\\[([\\s\\S]*?)\\]\\]>|([\\s\\S]*?))\\s*</${name}>`).exec(xml);
  return m ? (m[1] ?? decodeXml(m[2] ?? '')).trim() : '';
}

/** Text of a file, preferring the unsaved content of an open document. */
export async function readText(uri: vscode.Uri): Promise<string | undefined> {
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

/** Rule project folder of a file that lives in `<project>/<folder>/…` (folder: bom, rules, deployment). */
export function projectDirOf(uri: vscode.Uri, folder: string): { name: string; dir: vscode.Uri } {
  const segments = uri.path.split('/');
  const idx = segments.lastIndexOf(folder);
  const dir = idx > 0 ? uri.with({ path: segments.slice(0, idx).join('/') }) : vscode.Uri.joinPath(uri, '..', '..');
  return { name: idx > 0 ? decodeURIComponent(segments[idx - 1]) : '', dir };
}

export interface LoadedBom {
  file: string;
  concepts: BomConcept[];
}

/**
 * Parses the .bom files of a bom/ folder (with their vocabulary for the labels).
 * `onlyBase` restricts the result to `<onlyBase>.bom`.
 */
export async function loadBoms(bomDir: vscode.Uri, onlyBase?: string): Promise<LoadedBom[]> {
  let names: string[];
  try {
    names = (await vscode.workspace.fs.readDirectory(bomDir)).map(([n]) => n);
  } catch {
    return [];
  }
  const result: LoadedBom[] = [];
  for (const file of names.filter(n => n.endsWith('.bom') && (!onlyBase || n === `${onlyBase}.bom`))) {
    const base = file.replace(/\.bom$/, '');
    const voc = names.find(n => n === `${base}_en_US.voc`) ?? names.find(n => n.startsWith(`${base}_`) && n.endsWith('.voc'));
    const bomText = (await readText(vscode.Uri.joinPath(bomDir, file))) ?? '';
    const vocText = voc ? (await readText(vscode.Uri.joinPath(bomDir, voc))) ?? '' : '';
    result.push({ file, concepts: parseBom(bomText, vocText, file).concepts });
  }
  return result;
}
