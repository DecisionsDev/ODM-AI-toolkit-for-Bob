// Webview script for the editable vocabulary (.voc) view.
(function () {
  const vscode = acquireVsCodeApi();
  const app = document.getElementById('app');
  let view = null;
  let filter = '';
  let onlyIssues = false;

  const KIND_LABELS = {
    'concept.label': 'Term',
    'phrase.navigation': 'Reads as',
    'phrase.action': 'Writes as',
    'instance.label': 'Value'
  };

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
    crumbs.appendChild(el('span', '', 'Vocabulary'));
    top.appendChild(crumbs);
    const row = el('div', 'title-row');
    row.appendChild(el('h1', '', view.name));
    const open = el('button', 'btn btn-secondary', 'Open as text');
    open.onclick = () => vscode.postMessage({ type: 'openText' });
    row.appendChild(open);
    top.appendChild(row);
    return top;
  }

  function summary() {
    const s = section('Validation', view.noBom ? 'No BOM found next to this vocabulary' : 'Checked against ' + view.bomFile);
    if (view.noBom) {
      s.classList.add('card-warn');
      s.appendChild(el('p', 'empty', 'Keys cannot be checked because no .bom file was found in this folder.'));
      return s;
    }
    s.classList.add(view.errors ? 'card-bad' : view.warnings ? 'card-warn' : 'card-ok');
    const parts = [];
    parts.push(view.errors + ' error' + (view.errors === 1 ? '' : 's'));
    parts.push(view.warnings + ' warning' + (view.warnings === 1 ? '' : 's'));
    parts.push(view.suggestions.length + ' BOM member' + (view.suggestions.length === 1 ? '' : 's') + ' without a phrase');
    s.appendChild(el('p', 'summary-line', parts.join(' · ')));
    return s;
  }

  function matches(entry) {
    if (onlyIssues && !entry.issues.length) return false;
    if (!filter) return true;
    const hay = (entry.key + ' ' + entry.value).toLowerCase();
    return hay.includes(filter.toLowerCase());
  }

  function phraseCell(entry) {
    const td = el('td');
    const input = el('input', 'cell-input');
    input.type = 'text';
    input.value = entry.value;
    input.setAttribute('aria-label', 'Phrase for ' + entry.key);
    const commit = () => {
      if (input.value.trim() !== entry.value) {
        vscode.postMessage({ type: 'edit', line: entry.line, key: entry.key, value: input.value });
      }
    };
    input.addEventListener('change', commit);
    input.addEventListener('keydown', e => {
      if (e.key === 'Enter') input.blur();
      if (e.key === 'Escape') { input.value = entry.value; input.blur(); }
    });
    td.appendChild(input);
    return td;
  }

  function table() {
    const s = section('Phrases', view.entries.length + ' entr' + (view.entries.length === 1 ? 'y' : 'ies'));

    const bar = el('div', 'editor-bar');
    const search = el('input', 'cell-input search');
    search.type = 'search';
    search.placeholder = 'Filter by class, member or phrase';
    search.value = filter;
    search.setAttribute('aria-label', 'Filter phrases');
    search.oninput = () => { filter = search.value; renderRows(); };
    bar.appendChild(search);
    const label = el('label', 'check-toggle');
    const box = el('input');
    box.type = 'checkbox';
    box.checked = onlyIssues;
    box.onchange = () => { onlyIssues = box.checked; renderRows(); };
    label.appendChild(box);
    label.appendChild(document.createTextNode(' Only entries with issues'));
    bar.appendChild(label);
    s.appendChild(bar);

    const t = el('table', 'params voc-table');
    const head = el('tr');
    for (const h of ['Class', 'Member', 'Type', 'Phrase', 'Issues', '']) head.appendChild(el('th', '', h));
    t.appendChild(el('thead')).appendChild(head);
    const body = el('tbody');
    t.appendChild(body);
    s.appendChild(t);
    const none = el('p', 'empty', 'No entry matches the filter.');
    s.appendChild(none);

    function renderRows() {
      body.replaceChildren();
      let shown = 0;
      for (const e of view.entries) {
        if (!matches(e)) continue;
        shown++;
        const tr = el('tr', e.issues.some(i => i.level === 'error') ? 'row-error' : e.issues.length ? 'row-warn' : '');
        const cls = el('td');
        cls.appendChild(el('span', 'type-label', e.className.split('.').pop() || '—'));
        cls.appendChild(el('br'));
        cls.appendChild(el('code', 'java', e.className));
        tr.appendChild(cls);
        tr.appendChild(el('td')).appendChild(el('code', 'path', e.member || '—'));
        tr.appendChild(el('td')).appendChild(el('span', 'dir dir-input', KIND_LABELS[e.kind] || e.kind));
        tr.appendChild(phraseCell(e));
        const issues = el('td', 'issues');
        for (const i of e.issues) issues.appendChild(el('div', 'issue issue-' + i.level, i.message));
        tr.appendChild(issues);
        const del = el('td');
        const btn = el('button', 'btn btn-secondary btn-small', 'Delete');
        btn.setAttribute('aria-label', 'Delete ' + e.key);
        btn.onclick = () => vscode.postMessage({ type: 'delete', line: e.line, key: e.key });
        del.appendChild(btn);
        tr.appendChild(del);
        body.appendChild(tr);
      }
      none.hidden = shown > 0;
      t.hidden = shown === 0;
    }
    renderRows();
    return s;
  }

  function addForm() {
    const s = section('Add a phrase', 'Pick a BOM member that has no phrase yet, or type a key');
    const row = el('div', 'editor-bar add-row');
    const key = el('input', 'cell-input');
    key.type = 'text';
    key.placeholder = 'com.example.Customer.age#phrase.navigation';
    key.setAttribute('list', 'voc-suggestions');
    key.setAttribute('aria-label', 'Key');
    const list = el('datalist');
    list.id = 'voc-suggestions';
    for (const k of view.suggestions) list.appendChild(el('option')).value = k;
    const phrase = el('input', 'cell-input');
    phrase.type = 'text';
    phrase.placeholder = '{age} of {this}';
    phrase.setAttribute('aria-label', 'Phrase');
    const add = el('button', 'btn btn-primary', 'Add');
    const submit = () => {
      if (!key.value.trim()) return;
      vscode.postMessage({ type: 'add', key: key.value, value: phrase.value });
    };
    add.onclick = submit;
    phrase.addEventListener('keydown', e => { if (e.key === 'Enter') submit(); });
    row.append(key, phrase, add);
    s.append(row, list);
    return s;
  }

  function render() {
    const scroll = window.scrollY;
    app.replaceChildren(header(), summary(), table(), addForm());
    window.scrollTo(0, scroll);
  }

  window.addEventListener('message', e => {
    if (e.data.type !== 'update') return;
    view = e.data.view;
    render();
  });

  vscode.postMessage({ type: 'ready' });
})();
