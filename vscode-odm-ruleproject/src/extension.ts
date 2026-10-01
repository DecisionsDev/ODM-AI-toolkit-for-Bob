import * as vscode from 'vscode';
import { BrlHoverProvider } from './hoverProvider';
import { BrlDocumentSymbolProvider } from './symbolProvider';
import { BrlRuleEditorProvider } from './ruleEditorProvider';
import { BomEditorProvider } from './bomEditorProvider';
import { RuleflowEditorProvider } from './ruleflowEditorProvider';
import { DeploymentEditorProvider } from './deploymentEditorProvider';

const BRL_LANG_ID = 'brl';

export function activate(context: vscode.ExtensionContext): void {
  // ── Business-friendly rule view (default editor for .brl) ────────────
  context.subscriptions.push(
    BrlRuleEditorProvider.register(context),
    BomEditorProvider.register(context),
    RuleflowEditorProvider.register(context),
    DeploymentEditorProvider.register(context),
    vscode.commands.registerCommand('odm-ruleproject.openRuleView', (uri?: vscode.Uri) => {
      const target = uri ?? vscode.window.activeTextEditor?.document.uri;
      if (target) {
        const viewTypes: Record<string, string> = {
          bom: BomEditorProvider.viewType,
          rfl: RuleflowEditorProvider.viewType,
          dop: DeploymentEditorProvider.operationViewType,
          dep: DeploymentEditorProvider.deploymentViewType
        };
        const viewType = viewTypes[target.path.split('.').pop() ?? ''] ?? BrlRuleEditorProvider.viewType;
        return vscode.commands.executeCommand('vscode.openWith', target, viewType);
      }
    }),
    vscode.commands.registerCommand('odm-ruleproject.openAsXml', (uri?: vscode.Uri) => {
      const target = uri ?? activeCustomEditorUri();
      if (target) {
        return vscode.commands.executeCommand('vscode.openWith', target, 'default');
      }
    })
  );

  // ── Hover documentation ───────────────────────────────────────────────
  context.subscriptions.push(
    vscode.languages.registerHoverProvider(
      { language: BRL_LANG_ID, scheme: 'file' },
      new BrlHoverProvider()
    )
  );

  // ── Outline / symbol provider ─────────────────────────────────────────
  context.subscriptions.push(
    vscode.languages.registerDocumentSymbolProvider(
      { language: BRL_LANG_ID, scheme: 'file' },
      new BrlDocumentSymbolProvider()
    )
  );
}

function activeCustomEditorUri(): vscode.Uri | undefined {
  const input = vscode.window.tabGroups.activeTabGroup.activeTab?.input;
  return input instanceof vscode.TabInputCustom ? input.uri : undefined;
}

export function deactivate(): void {
  // nothing to clean up
}
