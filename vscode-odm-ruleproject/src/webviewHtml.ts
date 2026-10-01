import * as vscode from 'vscode';

/** Page shell shared by the business views: one stylesheet and one script from media/. */
export function webviewHtml(webview: vscode.Webview, mediaRoot: vscode.Uri, name: string, title: string): string {
  const nonce = Array.from({ length: 32 }, () => Math.floor(Math.random() * 36).toString(36)).join('');
  const css = webview.asWebviewUri(vscode.Uri.joinPath(mediaRoot, `${name}.css`));
  const js = webview.asWebviewUri(vscode.Uri.joinPath(mediaRoot, `${name}.js`));
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src ${webview.cspSource}; script-src 'nonce-${nonce}';">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <link href="${css}" rel="stylesheet">
  <title>${title}</title>
</head>
<body>
  <main id="app"></main>
  <script nonce="${nonce}" src="${js}"></script>
</body>
</html>`;
}
