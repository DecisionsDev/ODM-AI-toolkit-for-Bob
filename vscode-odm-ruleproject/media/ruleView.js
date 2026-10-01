// Webview script for the business-friendly rule view.
(function () {
  const vscode = acquireVsCodeApi();
  const app = document.getElementById('app');
  let rule = null;
  let editing = false;

  const SECTIONS = {
    definitions: { label: 'Definitions', hint: 'Names and variables declared for this rule' },
    if: { label: 'If', hint: 'Preconditions & criteria' },
    then: { label: 'Then', hint: 'Actions to perform' },
    else: { label: 'Else', hint: 'Alternative actions' }
  };

  // Longest phrases first so "is more than" wins over "is".
  const OPERATORS = [
    'it is not true that', 'make it true that', 'make it false that',
    'is at least', 'is at most', 'is more than', 'is less than', 'is equal to',
    'is not equal to', 'is not null', 'is null', 'is not one of', 'is one of',
    'is between', 'is not', 'is', 'contains', 'does not contain'
  ];
  const KEYWORDS = ['set', 'to', 'add', 'remove', 'print', 'there is no', 'there is', 'there are', 'the number of', 'where'];

  const escapeRe = s => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const phrase = list => list.map(p => escapeRe(p).replace(/ /g, '\\s+')).join('|');
  const TOKEN_RE = new RegExp(
    [
      '("(?:[^"\\\\]|\\\\.)*")',                 // 1 string
      "('[^']*')",                               // 2 variable
      '\\b(true|false|null)\\b',                 // 3 literal
      '(-?\\b\\d+(?:\\.\\d+)?\\b)',              // 4 number
      '\\b(' + phrase(OPERATORS) + ')\\b',       // 5 operator
      '\\b(' + phrase(KEYWORDS) + ')\\b'         // 6 keyword
    ].join('|'),
    'gi'
  );

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text !== undefined) e.textContent = text;
    return e;
  }

  /** Appends a BAL phrase to `parent` as styled spans (textContent only, no HTML injection). */
  function renderPhrase(parent, text) {
    let last = 0;
    TOKEN_RE.lastIndex = 0;
    let m;
    while ((m = TOKEN_RE.exec(text))) {
      if (m.index > last) parent.appendChild(document.createTextNode(text.slice(last, m.index)));
      const cls = m[1] ? 'tok-string' : m[2] ? 'tok-var' : m[3] ? 'tok-literal' : m[4] ? 'tok-number' : m[5] ? 'tok-op' : 'tok-kw';
      parent.appendChild(el('span', cls, m[2] ? m[2].slice(1, -1) : m[0]));
      last = m.index + m[0].length;
    }
    if (last < text.length) parent.appendChild(document.createTextNode(text.slice(last)));
  }

  /** Splits the BAL text into sections of statements. */
  function parseSections(text) {
    const sections = [];
    let current = null;
    for (const raw of text.split(/\r?\n/)) {
      let line = raw.trim();
      if (!line || line.startsWith('//')) continue;
      const head = /^(definitions|if|then|else)\b\s*(.*)$/i.exec(line);
      if (head) {
        current = { kind: head[1].toLowerCase(), lines: [] };
        sections.push(current);
        line = head[2];
        if (!line) continue;
      }
      if (!current) {
        current = { kind: 'if', lines: [] };
        sections.push(current);
      }
      current.lines.push(line);
    }
    return sections;
  }

  function renderSection(section) {
    const meta = SECTIONS[section.kind];
    const card = el('section', 'card card-' + section.kind);
    const header = el('header', 'card-header');
    header.appendChild(el('span', 'card-label', meta.label));
    header.appendChild(el('span', 'card-hint', meta.hint));
    card.appendChild(header);

    const list = el('ol', 'statements');
    for (const line of section.lines) {
      const item = el('li', 'statement');
      let body = line.replace(/\s*;\s*$/, '');
      const conj = /^(and|or)\s+(.*)$/i.exec(body);
      if (conj) {
        item.appendChild(el('span', 'conj', conj[1].toLowerCase()));
        body = conj[2];
      } else if (section.kind === 'if' || section.kind === 'then' || section.kind === 'else') {
        item.appendChild(el('span', 'conj conj-empty'));
      }
      const text = el('span', 'statement-text');
      renderPhrase(text, body);
      item.appendChild(text);
      list.appendChild(item);
    }
    card.appendChild(list);
    return card;
  }

  function render() {
    if (!rule) return;
    app.replaceChildren();

    const top = el('header', 'top');
    const crumbs = el('div', 'crumbs');
    if (rule.project) crumbs.appendChild(el('span', '', rule.project));
    if (rule.packagePath) crumbs.appendChild(el('span', '', rule.packagePath));
    top.appendChild(crumbs);

    const titleRow = el('div', 'title-row');
    titleRow.appendChild(el('h1', '', rule.title));
    const actions = el('div', 'actions');
    if (!editing) {
      const edit = el('button', 'btn btn-primary', 'Edit rule');
      edit.onclick = () => { editing = true; render(); };
      actions.appendChild(edit);
    }
    const xml = el('button', 'btn btn-secondary', 'Open as XML');
    xml.title = 'Open the underlying Rule Designer file';
    xml.onclick = () => vscode.postMessage({ type: 'openXml' });
    actions.appendChild(xml);
    titleRow.appendChild(actions);
    top.appendChild(titleRow);
    app.appendChild(top);

    const description = rule.docFields.find(f => f.key === 'Description');
    if (description || rule.docText) {
      app.appendChild(el('p', 'description', description ? description.value : rule.docText));
    }

    const others = rule.docFields.filter(f => f.key !== 'Description');
    if (others.length) {
      const dl = el('dl', 'fields');
      for (const f of others) {
        dl.appendChild(el('dt', '', f.key));
        const dd = el('dd');
        if (f.key === 'Tags') {
          for (const tag of f.value.split(',').map(t => t.trim()).filter(Boolean)) dd.appendChild(el('span', 'tag', tag));
        } else {
          dd.textContent = f.value;
        }
        dl.appendChild(dd);
      }
      app.appendChild(dl);
    }

    if (editing) {
      const box = el('section', 'card editor');
      const area = el('textarea', 'editor-text');
      area.value = rule.definition;
      area.spellcheck = false;
      area.rows = Math.max(8, rule.definition.split('\n').length + 2);
      box.appendChild(area);
      const bar = el('div', 'editor-bar');
      bar.appendChild(el('span', 'card-hint', 'Use is more than, is at least, is one of { … }, and end each action with ;'));
      const cancel = el('button', 'btn btn-secondary', 'Cancel');
      cancel.onclick = () => { editing = false; render(); };
      const apply = el('button', 'btn btn-primary', 'Apply');
      apply.onclick = () => {
        editing = false;
        vscode.postMessage({ type: 'saveDefinition', text: area.value });
      };
      bar.appendChild(cancel);
      bar.appendChild(apply);
      box.appendChild(bar);
      app.appendChild(box);
      area.focus();
      return;
    }

    const sections = parseSections(rule.definition);
    if (!sections.length) {
      app.appendChild(el('p', 'empty', 'This rule has no content yet. Click “Edit rule” to write it.'));
    }
    for (const s of sections) app.appendChild(renderSection(s));
  }

  window.addEventListener('message', e => {
    if (e.data.type === 'update') {
      rule = e.data.rule;
      if (!editing) render();
    }
  });

  vscode.postMessage({ type: 'ready' });
})();
