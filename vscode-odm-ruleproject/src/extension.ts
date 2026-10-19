import * as vscode from 'vscode';
import { BrlHoverProvider } from './hoverProvider';
import { BrlDocumentSymbolProvider } from './symbolProvider';
import { BrlRuleEditorProvider } from './ruleEditorProvider';
import { BomEditorProvider } from './bomEditorProvider';
import { RuleflowEditorProvider } from './ruleflowEditorProvider';
import { DeploymentEditorProvider } from './deploymentEditorProvider';
import { VocEditorProvider } from './vocEditorProvider';
import { VarEditorProvider } from './varEditorProvider';
import { RulepackageEditorProvider } from './rulepackageEditorProvider';
import { DtableEditorProvider } from './dtableEditorProvider';
import { B2xaSymbolProvider, RulePackageSymbolProvider, RuleProjectSymbolProvider, VarDocumentSymbolProvider, VocDocumentSymbolProvider } from './fileSymbolProviders';

const BRL_LANG_ID = 'brl';

export function activate(context: vscode.ExtensionContext): void {
  // ── Business-friendly rule view (default editor for .brl) ────────────
  context.subscriptions.push(
    BrlRuleEditorProvider.register(context),
    BomEditorProvider.register(context),
    RuleflowEditorProvider.register(context),
    DeploymentEditorProvider.register(context),
    VocEditorProvider.register(context),
    VarEditorProvider.register(context),
    RulepackageEditorProvider.register(context),
    DtableEditorProvider.register(context),
    vscode.commands.registerCommand('odm-ruleproject.openRuleView', (uri?: vscode.Uri) => {
      const target = uri ?? vscode.window.activeTextEditor?.document.uri;
      if (target) {
        const viewTypes: Record<string, string> = {
          bom: BomEditorProvider.viewType,
          rfl: RuleflowEditorProvider.viewType,
          dop: DeploymentEditorProvider.operationViewType,
          dep: DeploymentEditorProvider.deploymentViewType,
          voc: VocEditorProvider.viewType,
          var: VarEditorProvider.viewType,
          rulepackage: RulepackageEditorProvider.viewType,
          dta: DtableEditorProvider.viewType
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
    ),
    vscode.languages.registerDocumentSymbolProvider({ language: 'odm-voc', scheme: 'file' }, new VocDocumentSymbolProvider()),
    vscode.languages.registerDocumentSymbolProvider({ language: 'odm-var', scheme: 'file' }, new VarDocumentSymbolProvider()),
    vscode.languages.registerDocumentSymbolProvider({ language: 'odm-rulepackage', scheme: 'file' }, new RulePackageSymbolProvider()),
    vscode.languages.registerDocumentSymbolProvider({ language: 'odm-ruleproject', scheme: 'file' }, new RuleProjectSymbolProvider()),
    vscode.languages.registerDocumentSymbolProvider({ language: 'odm-b2xa', scheme: 'file' }, new B2xaSymbolProvider())
  );
}

function activeCustomEditorUri(): vscode.Uri | undefined {
  const input = vscode.window.tabGroups.activeTabGroup.activeTab?.input;
  return input instanceof vscode.TabInputCustom ? input.uri : undefined;
}

export function deactivate(): void {
  // nothing to clean up
}
