import * as vscode from 'vscode';

/**
 * Outline (document symbols) for the vocabulary (.voc), variable set (.var)
 * and rule package (.rulepackage) text files.
 */

function lineRange(document: vscode.TextDocument, line: number): vscode.Range {
  return document.lineAt(line).range;
}

/** Vocabulary: one symbol per class, with its attributes and methods as children. */
export class VocDocumentSymbolProvider implements vscode.DocumentSymbolProvider {
  provideDocumentSymbols(document: vscode.TextDocument): vscode.DocumentSymbol[] {
    const classes = new Map<string, vscode.DocumentSymbol>();
    const classSymbol = (qualified: string, line: number): vscode.DocumentSymbol => {
      let symbol = classes.get(qualified);
      if (!symbol) {
        const range = lineRange(document, line);
        symbol = new vscode.DocumentSymbol(qualified.split('.').pop() ?? qualified, qualified, vscode.SymbolKind.Class, range, range);
        classes.set(qualified, symbol);
      }
      return symbol;
    };

    for (let i = 0; i < document.lineCount; i++) {
      const m = /^\s*([^#=\s][^=]*?)\s*=\s*(.*)$/.exec(document.lineAt(i).text);
      if (!m || m[1] === 'uuid') {
        continue;
      }
      const hash = m[1].indexOf('#');
      if (hash < 0) {
        continue;
      }
      const owner = m[1].slice(0, hash);
      const kind = m[1].slice(hash + 1);
      if (kind === 'concept.label') {
        const symbol = classSymbol(owner, i);
        symbol.detail = m[2];
        symbol.range = lineRange(document, i);
        symbol.selectionRange = symbol.range;
        continue;
      }
      const base = owner.replace(/\(.*$/, '');
      const dot = base.lastIndexOf('.');
      if (dot < 0) {
        continue;
      }
      const parent = classSymbol(owner.slice(0, dot), i);
      const member = owner.slice(dot + 1);
      const range = lineRange(document, i);
      parent.children.push(
        new vscode.DocumentSymbol(
          `${member} (${kind.replace(/^(phrase|instance)\./, '')})`,
          m[2],
          member.includes('(') ? vscode.SymbolKind.Method : kind.startsWith('instance') ? vscode.SymbolKind.EnumMember : vscode.SymbolKind.Property,
          range,
          range
        )
      );
    }
    return Array.from(classes.values());
  }
}

/** Variable set: the set itself, with one child per <variables/> element. */
export class VarDocumentSymbolProvider implements vscode.DocumentSymbolProvider {
  provideDocumentSymbols(document: vscode.TextDocument): vscode.DocumentSymbol[] {
    const text = document.getText();
    const setName = /<name>\s*([^<]+?)\s*<\/name>/.exec(text);
    const fullRange = new vscode.Range(0, 0, document.lineCount, 0);
    const nameRange = setName
      ? new vscode.Range(document.positionAt(setName.index), document.positionAt(setName.index + setName[0].length))
      : fullRange;
    const root = new vscode.DocumentSymbol(setName?.[1] ?? 'Variable set', 'variable set', vscode.SymbolKind.Namespace, fullRange, nameRange);

    for (const m of text.matchAll(/<variables\b[^>]*>/g)) {
      const name = /\sname="([^"]*)"/.exec(m[0])?.[1] ?? '?';
      const type = /\stype="([^"]*)"/.exec(m[0])?.[1] ?? '';
      const range = new vscode.Range(document.positionAt(m.index!), document.positionAt(m.index! + m[0].length));
      root.children.push(new vscode.DocumentSymbol(name, type.split('.').pop() ?? type, vscode.SymbolKind.Variable, range, range));
    }
    return [root];
  }
}

/** Rule package: one symbol with the package name. */
export class RulePackageSymbolProvider implements vscode.DocumentSymbolProvider {
  provideDocumentSymbols(document: vscode.TextDocument): vscode.DocumentSymbol[] {
    const text = document.getText();
    const name = /<name>\s*([^<]+?)\s*<\/name>/.exec(text);
    if (!name) {
      return [];
    }
    const range = new vscode.Range(document.positionAt(name.index), document.positionAt(name.index + name[0].length));
    return [new vscode.DocumentSymbol(name[1], 'rule package', vscode.SymbolKind.Package, range, range)];
  }
}

/** Rule project: the project name, with one child per model folder (rules, bom, ...). */
export class RuleProjectSymbolProvider implements vscode.DocumentSymbolProvider {
  provideDocumentSymbols(document: vscode.TextDocument): vscode.DocumentSymbol[] {
    const text = document.getText();
    const name = /<name>\s*([^<]+?)\s*<\/name>/.exec(text);
    if (!name) {
      return [];
    }
    const toRange = (index: number, length: number) =>
      new vscode.Range(document.positionAt(index), document.positionAt(index + length));
    const nameRange = toRange(name.index, name[0].length);
    const root = new vscode.DocumentSymbol(name[1], 'rule project', vscode.SymbolKind.Package, new vscode.Range(0, 0, document.lineCount, 0), nameRange);

    for (const m of text.matchAll(/<modelFolders\b[^>]*>\s*<name>\s*([^<]+?)\s*<\/name>/g)) {
      const range = toRange(m.index!, m[0].length);
      root.children.push(new vscode.DocumentSymbol(m[1], 'folder', vscode.SymbolKind.Module, range, range));
    }
    return [root];
  }
}

/** BOM-to-ARL translation: one symbol with the translation language and id. */
export class B2xaSymbolProvider implements vscode.DocumentSymbolProvider {
  provideDocumentSymbols(document: vscode.TextDocument): vscode.DocumentSymbol[] {
    const text = document.getText();
    const lang = /<lang>\s*([^<]+?)\s*<\/lang>/.exec(text);
    const id = /<id>\s*([^<]+?)\s*<\/id>/.exec(text);
    const found = lang ?? id;
    if (!found) {
      return [];
    }
    const range = new vscode.Range(document.positionAt(found.index), document.positionAt(found.index + found[0].length));
    return [new vscode.DocumentSymbol(`${lang?.[1] ?? '?'} translation`, id?.[1] ?? '', vscode.SymbolKind.Package, range, range)];
  }
}
