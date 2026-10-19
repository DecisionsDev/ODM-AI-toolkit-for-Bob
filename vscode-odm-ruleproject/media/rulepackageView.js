// Webview script for the Rule Package (.rulepackage) view.
(function () {
  const vscode = acquireVsCodeApi();
  const app = document.getElementById('app');
  let view = null;

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text !== undefined) e.textContent = text;
    return e;
  }

  function section(title, hint) {
    const s = el('section', 'card');
    const h = el('header', 'card-header');
    h.appendChild(el('span', 'card-label', title));
    if (hint !== undefined) h.appendChild(el('span', 'card-hint', hint));
    s.appendChild(h);
    return s;
  }

  function kindLabel(kind) {
    switch (kind) {
      case 'action': return 'Action rule';
      case 'decision-table': return 'Decision table';
      case 'decision-tree': return 'Decision tree';
      default: return 'Rule';
    }
  }

  function kindClass(kind) {
    switch (kind) {
      case 'action': return 'kind-action';
      case 'decision-table': return 'kind-dt';
      case 'decision-tree': return 'kind-dt';
      default: return 'kind-action';
    }
  }

  function header() {
    const top = el('header', 'top');
    const crumbs = el('div', 'crumbs');
    crumbs.appendChild(el('span', '', 'Rule package'));
    top.appendChild(crumbs);
    const row = el('div', 'title-row');
    row.appendChild(el('h1', '', view.name));
    const open = el('button', 'btn btn-secondary', 'Open as XML');
    open.onclick = () => vscode.postMessage({ type: 'openText' });
    row.appendChild(open);
    top.appendChild(row);
    if (view.documentation) top.appendChild(el('p', 'description', view.documentation));
    return top;
  }

  function rulesList() {
    const count = view.rules.length;
    const s = section('Rules', count + ' rule' + (count === 1 ? '' : 's'));
    s.classList.add('card-params');

    if (!count) {
      s.appendChild(el('p', 'empty', 'This package contains no rules.'));
      return s;
    }

    const table = el('table', 'params pkg-table');
    const head = el('tr');
    for (const h of ['Type', 'Name', 'Definition', 'Documentation', '']) {
      head.appendChild(el('th', '', h));
    }
    table.appendChild(el('thead')).appendChild(head);

    const tbody = el('tbody');
    for (const rule of view.rules) {
      const tr = el('tr');

      // Type badge
      const tdKind = el('td');
      tdKind.appendChild(el('span', 'kind-badge ' + kindClass(rule.kind), kindLabel(rule.kind)));
      tr.appendChild(tdKind);

      // Name
      const tdName = el('td', 'rule-name', rule.name);
      tr.appendChild(tdName);

      // Definition (BAL preview, hidden for DT)
      const tdDef = el('td', 'rule-def');
      if (rule.definition) {
        const pre = el('pre', 'bal-preview');
        pre.textContent = rule.definition;
        tdDef.appendChild(pre);
      } else {
        tdDef.appendChild(el('span', 'card-hint', '—'));
      }
      tr.appendChild(tdDef);

      // Documentation
      const tdDoc = el('td', 'rule-doc', rule.documentation || '');
      tr.appendChild(tdDoc);

      // Open button
      const tdOpen = el('td');
      const btn = el('button', 'btn btn-secondary btn-small', 'Open');
      btn.setAttribute('aria-label', 'Open rule ' + rule.name);
      btn.onclick = () => vscode.postMessage({ type: 'openRule', uri: rule.uri });
      tdOpen.appendChild(btn);
      tr.appendChild(tdOpen);

      tbody.appendChild(tr);
    }
    table.appendChild(tbody);
    s.appendChild(table);
    return s;
  }

  function render() {
    const scroll = window.scrollY;
    app.replaceChildren(header(), rulesList());
    window.scrollTo(0, scroll);
  }

  window.addEventListener('message', e => {
    if (e.data.type !== 'update') return;
    view = e.data.view;
    render();
  });

  vscode.postMessage({ type: 'ready' });
})();
