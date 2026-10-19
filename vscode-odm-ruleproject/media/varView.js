// Webview script for the editable variable set (.var) view.
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
    if (hint) h.appendChild(el('span', 'card-hint', hint));
    s.appendChild(h);
    return s;
  }

  function header() {
    const top = el('header', 'top');
    const crumbs = el('div', 'crumbs');
    if (view.project) crumbs.appendChild(el('span', '', view.project));
    crumbs.appendChild(el('span', '', 'Variable set'));
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

  /** Type picker: BOM classes by business label, the common Java types, and the current value if unknown. */
  function typeSelect(current, onChange) {
    const select = el('select', 'cell-input');
    select.setAttribute('aria-label', 'Type');
    const known = new Set();
    const group = (label, items) => {
      if (!items.length) return;
      const g = el('optgroup');
      g.label = label;
      for (const [value, text] of items) {
        const o = el('option', '', text);
        o.value = value;
        known.add(value);
        g.appendChild(o);
      }
      select.appendChild(g);
    };
    group('Business objects (BOM)', view.types.map(t => [t.qualified, t.label + '  —  ' + t.qualified]));
    group('Basic types', [
      ['java.lang.String', 'Text (java.lang.String)'],
      ['int', 'Whole number (int)'],
      ['long', 'Whole number (long)'],
      ['double', 'Number (double)'],
      ['java.math.BigDecimal', 'Number (java.math.BigDecimal)'],
      ['boolean', 'Yes / No (boolean)'],
      ['java.util.Date', 'Date (java.util.Date)']
    ]);
    if (current && !known.has(current)) {
      group('Not in the BOM', [[current, current]]);
    }
    select.value = current;
    select.onchange = () => onChange(select.value);
    return select;
  }

  function textInput(value, label, onCommit) {
    const input = el('input', 'cell-input');
    input.type = 'text';
    input.value = value;
    input.setAttribute('aria-label', label);
    input.addEventListener('change', () => { if (input.value.trim() !== value) onCommit(input.value); });
    input.addEventListener('keydown', e => {
      if (e.key === 'Enter') input.blur();
      if (e.key === 'Escape') { input.value = value; input.blur(); }
    });
    return input;
  }

  function variables() {
    const s = section('Variables', view.rows.length + ' variable' + (view.rows.length === 1 ? '' : 's'));
    s.classList.add('card-params');
    if (!view.rows.length) {
      s.appendChild(el('p', 'empty', 'This set declares no variable.'));
      return s;
    }
    const t = el('table', 'params var-table');
    const head = el('tr');
    for (const h of ['Name', 'Type', 'Verbalization (as quoted in rules)', 'Initial value', 'Used by', 'Issues', '']) head.appendChild(el('th', '', h));
    t.appendChild(el('thead')).appendChild(head);
    const body = el('tbody');
    for (const r of view.rows) {
      const change = patch => vscode.postMessage(Object.assign({ type: 'edit', index: r.index, previousName: r.name }, patch));
      const tr = el('tr', r.issues.length ? 'row-error' : '');
      tr.appendChild(el('td')).appendChild(textInput(r.name, 'Name', v => change({ name: v })));
      tr.appendChild(el('td')).appendChild(typeSelect(r.type, v => change({ type: v })));
      tr.appendChild(el('td')).appendChild(textInput(r.verbalization, 'Verbalization', v => change({ verbalization: v })));
      tr.appendChild(el('td')).appendChild(textInput(r.initialValue, 'Initial value', v => change({ initialValue: v })));
      const used = el('td');
      if (r.usedBy.length) used.textContent = r.usedBy.join(', ');
      else used.appendChild(el('span', 'card-hint', 'No operation'));
      tr.appendChild(used);
      const issues = el('td', 'issues');
      for (const i of r.issues) issues.appendChild(el('div', 'issue issue-error', i));
      tr.appendChild(issues);
      const del = el('td');
      const btn = el('button', 'btn btn-secondary btn-small', 'Delete');
      btn.setAttribute('aria-label', 'Delete variable ' + r.name);
      btn.onclick = () => vscode.postMessage({ type: 'delete', index: r.index, previousName: r.name });
      del.appendChild(btn);
      tr.appendChild(del);
      body.appendChild(tr);
    }
    t.appendChild(body);
    s.appendChild(t);
    return s;
  }

  function addForm() {
    const s = section('Add a variable');
    const row = el('div', 'editor-bar add-row');
    const name = el('input', 'cell-input');
    name.type = 'text';
    name.placeholder = 'name, e.g. request';
    name.setAttribute('aria-label', 'New variable name');
    let type = view.types.length ? view.types[0].qualified : 'java.lang.String';
    const select = typeSelect(type, v => { type = v; });
    const verbalization = el('input', 'cell-input');
    verbalization.type = 'text';
    verbalization.placeholder = 'the request';
    verbalization.setAttribute('aria-label', 'New variable verbalization');
    const add = el('button', 'btn btn-primary', 'Add');
    const submit = () => {
      const n = name.value.trim().replace(/\s+/g, '');
      if (!n) return;
      vscode.postMessage({ type: 'add', name: n, type, verbalization: verbalization.value || 'the ' + n });
    };
    add.onclick = submit;
    row.append(name, select, verbalization, add);
    s.appendChild(row);
    return s;
  }

  function links() {
    const bad = view.links.filter(l => !l.ok).length;
    const s = section('Operations', !view.links.length ? 'No operation uses this set' : bad ? bad + ' broken link' + (bad === 1 ? '' : 's') : 'All links are valid');
    if (!view.links.length) {
      s.appendChild(el('p', 'empty', 'No .dop file of the project references this variable set.'));
      return s;
    }
    s.classList.add(bad ? 'card-bad' : 'card-ok');
    const list = el('ul', 'checks');
    for (const l of view.links) {
      const li = el('li', l.ok ? 'ok' : 'bad');
      li.appendChild(el('span', 'check-icon', l.ok ? '✓' : '✗'));
      const a = el('a', 'link check-label', l.operation);
      a.href = '#';
      a.onclick = e => { e.preventDefault(); vscode.postMessage({ type: 'open', path: l.path }); };
      li.appendChild(a);
      li.appendChild(el('span', 'check-detail', l.detail));
      list.appendChild(li);
    }
    s.appendChild(list);
    return s;
  }

  function render() {
    const scroll = window.scrollY;
    app.replaceChildren(header(), variables(), addForm(), links());
    window.scrollTo(0, scroll);
  }

  window.addEventListener('message', e => {
    if (e.data.type !== 'update') return;
    view = e.data.view;
    render();
  });

  vscode.postMessage({ type: 'ready' });
})();
