// Webview script for the operation (.dop) and deployment (.dep) views.
(function () {
  const vscode = acquireVsCodeApi();
  const app = document.getElementById('app');

  const PROPERTY_LABELS = {
    'ruleset.debug.enabled': 'Debug information',
    'ruleset.trace.enabled': 'Execution trace',
    'ruleset.status': 'Status',
    'ruleset.bom.enabled': 'BOM-based execution',
    'com.ibm.rules.engine.bytecode.generation': 'Bytecode generation',
    'ruleset.sequential.trace.enabled': 'Sequential trace'
  };

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text !== undefined) e.textContent = text;
    return e;
  }

  /** "AMLDetectionOperation" → "AML detection operation", "loan-compliance-ruleflow" → "Loan compliance ruleflow" */
  function humanize(name) {
    const words = String(name)
      .replace(/[-_]+/g, ' ')
      .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      .replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
      .split(/\s+/)
      .filter(Boolean)
      .map((w, i) => (/^[A-Z0-9]{2,}$/.test(w) ? w : i ? w.toLowerCase() : w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()));
    return words.join(' ');
  }

  function link(text, path, cls) {
    if (!path) return el('span', cls || '', text);
    const a = el('a', 'link' + (cls ? ' ' + cls : ''), text);
    a.href = '#';
    a.onclick = e => { e.preventDefault(); vscode.postMessage({ type: 'open', path }); };
    return a;
  }

  function header(view, kindLabel, title, buttonLabel) {
    const top = el('header', 'top');
    const crumbs = el('div', 'crumbs');
    if (view.project) crumbs.appendChild(el('span', '', view.project));
    crumbs.appendChild(el('span', '', kindLabel));
    top.appendChild(crumbs);
    const row = el('div', 'title-row');
    row.appendChild(el('h1', '', title));
    const open = el('button', 'btn btn-secondary', buttonLabel);
    open.onclick = () => vscode.postMessage({ type: 'openText' });
    row.appendChild(open);
    top.appendChild(row);
    if (view.documentation) top.appendChild(el('p', 'description', view.documentation));
    return top;
  }

  function section(title, hint) {
    const s = el('section', 'card');
    const h = el('header', 'card-header');
    h.appendChild(el('span', 'card-label', title));
    if (hint) h.appendChild(el('span', 'card-hint', hint));
    s.appendChild(h);
    return s;
  }

  function facts(rows) {
    const dl = el('dl', 'facts');
    for (const [label, value] of rows) {
      if (value === undefined || value === null || value === '') continue;
      dl.appendChild(el('dt', '', label));
      const dd = el('dd');
      if (value instanceof Node) dd.appendChild(value);
      else dd.textContent = value;
      dl.appendChild(dd);
    }
    return dl;
  }

  function code(text) {
    return el('code', 'path', text);
  }

  function renderChecks(checks) {
    if (!checks.length) return null;
    const bad = checks.filter(c => !c.ok).length;
    const s = section('Links', bad ? bad + ' broken link' + (bad === 1 ? '' : 's') : 'All links are valid');
    s.classList.add(bad ? 'card-bad' : 'card-ok');
    const list = el('ul', 'checks');
    for (const c of checks) {
      const li = el('li', c.ok ? 'ok' : 'bad');
      li.appendChild(el('span', 'check-icon', c.ok ? '✓' : '✗'));
      li.appendChild(el('span', 'check-label', c.label));
      li.appendChild(c.ok && c.target ? link(c.detail, c.target, 'check-detail') : el('span', 'check-detail', c.detail));
      list.appendChild(li);
    }
    s.appendChild(list);
    return s;
  }

  // ── Operation ─────────────────────────────────────────────────────────

  function renderOperation(view) {
    app.appendChild(header(view, 'Decision operation', humanize(view.name), 'Open as XML'));

    const what = section('What it runs');
    what.classList.add('card-flow');
    what.appendChild(facts([
      ['Rules', view.usingRuleflow && view.ruleflow
        ? link(humanize(view.ruleflow.name), view.ruleflow.path)
        : 'All rules of the project (no ruleflow)'],
      ['Ruleset', view.rulesetName ? code(view.rulesetName) : '']
    ]));
    app.appendChild(what);

    const params = section('Data exchanged', 'What the caller sends and gets back');
    params.classList.add('card-params');
    if (!view.parameters.length) {
      params.appendChild(el('p', 'empty', 'This operation declares no parameters.'));
    } else {
      const table = el('table', 'params');
      const head = el('tr');
      for (const h of ['In rules', 'Direction', 'Business object', 'JSON field']) head.appendChild(el('th', '', h));
      table.appendChild(el('thead')).appendChild(head);
      const body = el('tbody');
      for (const p of view.parameters) {
        const tr = el('tr');
        tr.appendChild(el('td')).appendChild(el('span', 'chip-var', p.verbalization));
        tr.appendChild(el('td')).appendChild(el('span', 'dir dir-' + p.direction.replace(/\W+/g, '-').toLowerCase(), p.direction));
        const type = el('td');
        type.appendChild(el('span', 'type-label', p.typeLabel || '—'));
        if (p.type) {
          type.appendChild(el('br'));
          type.appendChild(el('code', 'java', p.type));
        }
        tr.appendChild(type);
        tr.appendChild(el('td')).appendChild(code(p.name));
        body.appendChild(tr);
      }
      table.appendChild(body);
      params.appendChild(table);
    }
    app.appendChild(params);

    const deps = section('Published by');
    if (!view.deployments.length) {
      deps.appendChild(el('p', 'empty', 'No deployment configuration in this folder publishes this operation.'));
    } else {
      const list = el('ul', 'plain');
      for (const d of view.deployments) list.appendChild(el('li')).appendChild(link(d.name, d.path));
      deps.appendChild(list);
    }
    app.appendChild(deps);

    const checks = renderChecks(view.checks);
    if (checks) app.appendChild(checks);
  }

  // ── Deployment ────────────────────────────────────────────────────────

  function renderDeployment(view) {
    app.appendChild(header(view, 'Deployment configuration', view.ruleAppName || humanize(view.name), 'Open as XML'));

    const app1 = section('RuleApp');
    app1.classList.add('card-flow');
    app1.appendChild(facts([
      ['Name', code(view.ruleAppName || view.name)],
      ['Deploys to', view.targets.length ? view.targets.join(', ') : 'No target configured'],
      ['Java model (XOM)', view.managingXom ? 'Deployed with the RuleApp' : 'Must already be on the server'],
      ['Allowed groups', view.groups]
    ]));
    app.appendChild(app1);

    const ops = section('Published decisions', view.operations.length + ' operation' + (view.operations.length === 1 ? '' : 's'));
    ops.classList.add('card-params');
    if (!view.operations.length) ops.appendChild(el('p', 'empty', 'This configuration publishes no operation.'));
    for (const op of view.operations) {
      const block = el('div', 'op');
      block.appendChild(el('div', 'op-name')).appendChild(link(humanize(op.name), op.path));
      const ruleset = op.rulesetName || op.name;
      const rows = [
        ['Ruleset', code(ruleset)],
        ['Ruleset version', op.version],
        ['Called at', code('/' + (view.ruleAppName || view.name) + '/1.0/' + ruleset + '/' + op.version)]
      ];
      for (const p of op.properties) rows.push([PROPERTY_LABELS[p.key] || p.key, p.value === 'true' ? 'On' : p.value === 'false' ? 'Off' : p.value]);
      block.appendChild(facts(rows));
      ops.appendChild(block);
    }
    if (view.operations.length) {
      ops.appendChild(el('p', 'card-hint note', 'Paths assume version 1.0 of the RuleApp; the version policy below decides the numbers of later deployments.'));
    }
    app.appendChild(ops);

    if (view.policies.length) {
      const pol = section('Versioning', 'How each new deployment is numbered');
      const list = el('ul', 'policies');
      for (const p of view.policies) {
        const li = el('li', p.isDefault ? 'default' : '');
        const title = el('div', 'policy-title', p.label);
        if (p.isDefault) title.appendChild(el('span', 'badge', 'default'));
        li.appendChild(title);
        if (p.description) li.appendChild(el('p', 'policy-desc', p.description));
        list.appendChild(li);
      }
      pol.appendChild(list);
      app.appendChild(pol);
    }

    const checks = renderChecks(view.checks);
    if (checks) app.appendChild(checks);
  }

  window.addEventListener('message', e => {
    if (e.data.type !== 'update') return;
    app.replaceChildren();
    const view = e.data.view;
    if (view.kind === 'operation') renderOperation(view);
    else renderDeployment(view);
  });

  vscode.postMessage({ type: 'ready' });
})();
