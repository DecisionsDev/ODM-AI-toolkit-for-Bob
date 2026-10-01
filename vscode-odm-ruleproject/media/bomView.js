// Webview script for the business-friendly BOM (vocabulary) view.
(function () {
  const vscode = acquireVsCodeApi();
  const app = document.getElementById('app');
  let bom = null;
  let filter = '';

  const PARAM_TYPES = {
    string: 'text', 'java.lang.String': 'text', int: 'whole number', long: 'whole number',
    double: 'number', float: 'number', boolean: 'true / false', 'java.util.Date': 'date'
  };

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text !== undefined) e.textContent = text;
    return e;
  }

  function conceptByQualified(name) {
    return bom.concepts.find(c => c.qualifiedName === name);
  }

  function goTo(className) {
    const target = document.getElementById('concept-' + className);
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      target.classList.add('flash');
      setTimeout(() => target.classList.remove('flash'), 900);
    }
  }

  /**
   * Renders a vocabulary phrase ("{amount} of {this}", "set the amount of {this} to {amount}")
   * as readable text: {this} → concept chip, {n} → parameter chip, {label} → term or value chip.
   */
  function renderPhrase(phrase, concept, opts) {
    const span = el('span', 'phrase');
    // Rules write "the ..." in front of a phrase that starts with a label token.
    let text = phrase;
    if (opts.navigation && /^\{(?!this\})[^}]+\}/.test(text)) text = 'the ' + text;
    const re = /\{([^}]+)\}/g;
    let last = 0;
    let m;
    while ((m = re.exec(text))) {
      if (m.index > last) span.appendChild(document.createTextNode(text.slice(last, m.index)));
      const token = m[1];
      if (token === 'this') {
        span.appendChild(el('span', 'chip chip-concept', concept.label));
      } else if (/^\d+$/.test(token)) {
        const p = (opts.params || [])[Number(token)] || '';
        const ref = conceptByQualified(p);
        span.appendChild(el('span', 'chip chip-value', ref ? ref.label : PARAM_TYPES[p] || p.split('.').pop() || 'value'));
      } else if (opts.navigation) {
        span.appendChild(el('strong', 'term', token));
      } else {
        span.appendChild(el('span', 'chip chip-value', opts.valueType || 'value'));
      }
      last = m.index + m[0].length;
    }
    if (last < text.length) span.appendChild(document.createTextNode(text.slice(last)));
    return span;
  }

  function matches(concept) {
    if (!filter) return true;
    const hay = [concept.label, concept.className]
      .concat(concept.attributes.map(a => (a.navigation || '') + ' ' + a.name))
      .concat(concept.methods.map(m => (m.action || '') + ' ' + (m.navigation || '')))
      .concat(concept.values.map(v => v.label))
      .join(' ')
      .toLowerCase();
    return hay.includes(filter);
  }

  function typeCell(attr) {
    const td = el('td', 'type');
    if (attr.ref) {
      const a = el('a', 'type-link', attr.type);
      a.href = '#';
      a.onclick = e => { e.preventDefault(); goTo(attr.ref); };
      td.appendChild(a);
    } else {
      td.textContent = attr.type;
    }
    return td;
  }

  function renderConcept(c) {
    const card = el('section', 'card' + (c.isEnum ? ' card-enum' : ''));
    card.id = 'concept-' + c.className;

    const header = el('header', 'card-header');
    header.appendChild(el('h2', '', c.label));
    header.appendChild(el('span', 'kind', c.isEnum ? 'List of values' : 'Business object'));
    header.appendChild(el('code', 'java', c.className));
    card.appendChild(header);

    if (c.values.length) {
      const values = el('div', 'values');
      for (const v of c.values) values.appendChild(el('span', 'chip chip-enum', v.label));
      card.appendChild(values);
    }

    const verbalized = c.attributes.filter(a => a.navigation);
    if (verbalized.length) {
      const table = el('table', 'attrs');
      const head = el('tr');
      for (const h of ['How it reads in a rule', 'Type', '']) head.appendChild(el('th', '', h));
      table.appendChild(el('thead')).appendChild(head);
      const body = el('tbody');
      for (const a of verbalized) {
        const tr = el('tr');
        const term = el('td', 'term-cell');
        term.appendChild(renderPhrase(a.navigation, c, { navigation: true }));
        if (a.action) {
          const set = el('div', 'action-line');
          set.appendChild(el('span', 'action-label', 'change'));
          set.appendChild(renderPhrase(a.action, c, { valueType: a.type === 'Yes / No' ? 'true / false' : a.type.toLowerCase() }));
          term.appendChild(set);
        }
        tr.appendChild(term);
        tr.appendChild(typeCell(a));
        const badges = el('td', 'badges');
        if (a.readonly) badges.appendChild(el('span', 'badge', a.type.startsWith('List') ? 'list' : 'calculated'));
        tr.appendChild(badges);
        body.appendChild(tr);
      }
      table.appendChild(body);
      card.appendChild(table);
    }

    const actions = c.methods.filter(m => m.action);
    const questions = c.methods.filter(m => m.navigation);
    if (actions.length || questions.length) {
      const list = el('ul', 'methods');
      for (const m of questions) {
        const li = el('li');
        li.appendChild(el('span', 'action-label', 'value'));
        li.appendChild(renderPhrase(m.navigation, c, { navigation: true, params: m.params }));
        list.appendChild(li);
      }
      for (const m of actions) {
        const li = el('li');
        li.appendChild(el('span', 'action-label', 'action'));
        li.appendChild(renderPhrase(m.action, c, { params: m.params }));
        list.appendChild(li);
      }
      card.appendChild(el('h3', '', 'Actions and calculations'));
      card.appendChild(list);
    }

    const hidden = c.attributes.filter(a => !a.navigation).map(a => a.name)
      .concat(c.methods.filter(m => !m.action && !m.navigation).map(m => m.signature));
    if (hidden.length) {
      const details = el('details', 'hidden-members');
      details.appendChild(el('summary', '', hidden.length + ' member(s) not available in rules (no vocabulary)'));
      details.appendChild(el('p', '', hidden.join(', ')));
      card.appendChild(details);
    }

    if (!c.values.length && !verbalized.length && !actions.length && !questions.length && !hidden.length) {
      card.appendChild(el('p', 'empty', 'No members.'));
    }
    return card;
  }

  function render() {
    if (!bom) return;
    app.replaceChildren();

    const top = el('header', 'top');
    const crumbs = el('div', 'crumbs');
    if (bom.project) crumbs.appendChild(el('span', '', bom.project));
    crumbs.appendChild(el('span', '', 'Business Object Model'));
    top.appendChild(crumbs);

    const titleRow = el('div', 'title-row');
    titleRow.appendChild(el('h1', '', 'Business vocabulary'));
    const open = el('button', 'btn btn-secondary', 'Open as text');
    open.onclick = () => vscode.postMessage({ type: 'openText' });
    titleRow.appendChild(open);
    top.appendChild(titleRow);

    const objects = bom.concepts.filter(c => !c.isEnum).length;
    const enums = bom.concepts.length - objects;
    top.appendChild(el('p', 'summary',
      objects + ' business object' + (objects === 1 ? '' : 's') +
      (enums ? ' and ' + enums + ' list' + (enums === 1 ? '' : 's') + ' of values' : '') +
      (bom.vocabularyFile ? ' · wording from ' + bom.vocabularyFile : ' · no vocabulary file found')));

    const search = el('input', 'search');
    search.type = 'search';
    search.placeholder = 'Find a term, e.g. risk score';
    search.value = filter;
    search.oninput = () => { filter = search.value.trim().toLowerCase(); renderList(); };
    top.appendChild(search);

    const index = el('nav', 'index');
    for (const c of bom.concepts) {
      const chip = el('button', 'chip chip-index' + (c.isEnum ? ' chip-index-enum' : ''), c.label);
      chip.onclick = () => goTo(c.className);
      index.appendChild(chip);
    }
    top.appendChild(index);
    app.appendChild(top);

    const list = el('div', 'concepts');
    list.id = 'concepts';
    app.appendChild(list);
    renderList();
  }

  function renderList() {
    const list = document.getElementById('concepts');
    list.replaceChildren();
    const shown = bom.concepts.filter(matches);
    if (!shown.length) list.appendChild(el('p', 'empty', 'No term matches “' + filter + '”.'));
    for (const c of shown) list.appendChild(renderConcept(c));
  }

  window.addEventListener('message', e => {
    if (e.data.type === 'update') {
      bom = e.data.bom;
      render();
    }
  });

  vscode.postMessage({ type: 'ready' });
})();
