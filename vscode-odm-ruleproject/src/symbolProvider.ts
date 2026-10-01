import * as vscode from 'vscode';

/**
 * Provides the Outline view (document symbols) for BRL files.
 *
 * Top-level symbols  →  each rule block  (SymbolKind.Function)
 * Children           →  if / then / definitions sections (SymbolKind.Namespace)
 */
export class BrlDocumentSymbolProvider implements vscode.DocumentSymbolProvider {
  provideDocumentSymbols(
    document: vscode.TextDocument
  ): vscode.ProviderResult<vscode.DocumentSymbol[]> {
    const symbols: vscode.DocumentSymbol[] = [];
    // Plain-text form: rule "name"   |   Rule Designer XML form: <name>name</name>
    const ruleHeaderRe = /^\s*(?:rule\s+"([^"]+)"|<name>([^<]+)<\/name>)/;
    // In XML files the first section follows <definition><![CDATA[ on the same line
    const sectionRe = /^\s*(?:<definition><!\[CDATA\[)?(definitions|if|then|else)\b/;

    let currentRule: vscode.DocumentSymbol | null = null;
    let currentRuleStartLine = 0;
    let currentSection: vscode.DocumentSymbol | null = null;
    let currentSectionStartLine = 0;

    const finalizeSection = (endLine: number) => {
      if (currentSection && currentRule) {
        const endPos = new vscode.Position(endLine, 0);
        const fullRange = new vscode.Range(
          new vscode.Position(currentSectionStartLine, 0),
          endPos
        );
        currentSection.range = fullRange;
        currentSection.selectionRange = fullRange;
        currentRule.children.push(currentSection);
        currentSection = null;
      }
    };

    const finalizeRule = (endLine: number) => {
      finalizeSection(endLine);
      if (currentRule) {
        const endPos = new vscode.Position(endLine, 0);
        currentRule.range = new vscode.Range(
          new vscode.Position(currentRuleStartLine, 0),
          endPos
        );
        currentRule.selectionRange = currentRule.range;
        symbols.push(currentRule);
        currentRule = null;
      }
    };

    for (let i = 0; i < document.lineCount; i++) {
      const lineText = document.lineAt(i).text;

      // New rule header
      const ruleMatch = ruleHeaderRe.exec(lineText);
      if (ruleMatch) {
        finalizeRule(i);
        currentRuleStartLine = i;
        const ruleName = ruleMatch[1] ?? ruleMatch[2];
        const nameStart = lineText.indexOf(ruleName);
        const nameRange = new vscode.Range(
          new vscode.Position(i, nameStart),
          new vscode.Position(i, nameStart + ruleName.length)
        );
        currentRule = new vscode.DocumentSymbol(
          ruleName,
          '',
          vscode.SymbolKind.Function,
          nameRange,
          nameRange
        );
        continue;
      }

      if (!currentRule) {
        continue;
      }

      // Section keyword
      const sectionMatch = sectionRe.exec(lineText);
      if (sectionMatch) {
        finalizeSection(i);
        currentSectionStartLine = i;
        const sectionName = sectionMatch[1];
        const kindMap: Record<string, vscode.SymbolKind> = {
          definitions: vscode.SymbolKind.Namespace,
          if: vscode.SymbolKind.Event,
          then: vscode.SymbolKind.Operator,
          else: vscode.SymbolKind.Operator
        };
        const sectionRange = new vscode.Range(
          new vscode.Position(i, 0),
          new vscode.Position(i, lineText.length)
        );
        currentSection = new vscode.DocumentSymbol(
          sectionName,
          '',
          kindMap[sectionName] ?? vscode.SymbolKind.Namespace,
          sectionRange,
          sectionRange
        );
      }
    }

    // Finalize last rule
    finalizeRule(document.lineCount);

    return symbols;
  }
}
