// Webview script for the business-friendly ruleflow view.
(function () {
  const vscode = acquireVsCodeApi();
  const app = document.getElementById('app');
  const SVG_NS = 'http://www.w3.org/2000/svg';
  let flow = null;
  let resizeObserver = null;

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text !== undefined) e.textContent = text;
    return e;
  }

  /** "high-value-transactions" → "High value transactions" */
  function humanize(name) {
    const words = String(name).split('.').pop().replace(/[-_]+/g, ' ').trim();
    return words.charAt(0).toUpperCase() + words.slice(1);
  }

  const children = (parent, localName) =>
    parent ? Array.from(parent.children).filter(c => !localName || c.localName === localName) : [];
  const first = (root, localName) => root.getElementsByTagNameNS('*', localName)[0] || null;

  // ── Model ──────────────────────────────────────────────────────────────

  function parseModel(xml) {
    const doc = new DOMParser().parseFromString(xml, 'application/xml');
    if (doc.getElementsByTagName('parsererror').length || !first(doc, 'Ruleflow')) {
      return { error: 'The ruleflow model could not be read.' };
    }

    const labels = {};
    const resourceSet = first(doc, 'ResourceSet');
    for (const d of children(resourceSet, 'Data')) labels[d.getAttribute('Name')] = d.textContent.trim();

    const tasks = {};
    for (const t of children(first(doc, 'TaskList'))) {
      const task = { id: t.getAttribute('Identifier'), kind: t.localName, entries: [], mode: t.getAttribute('ExecutionMode') || 'RetePlus' };
      for (const entry of children(first(t, 'RuleList'))) {
        task.entries.push({ kind: entry.localName, name: entry.getAttribute('Name'), uuid: entry.getAttribute('Uuid') || entry.getAttribute('UUID') });
      }
      if (t.localName === 'ActionTask') {
        task.actions = t.textContent.trim();
      }
      if (t.localName === 'FlowTask') {
        task.subflow = t.getAttribute('FileName') || t.getAttribute('Ruleflow') || t.getAttribute('Name') || '';
      }
      tasks[task.id] = task;
    }

    const nodes = children(first(doc, 'NodeList')).map(n => {
      const id = n.getAttribute('Identifier');
      const task = tasks[n.getAttribute('Task')];
      return {
        id,
        kind: task ? task.kind : n.localName,
        task,
        label: labels[id + '#label'] || (task && labels[task.id + '#name']) || ''
      };
    });

    const transitions = children(first(doc, 'TransitionList')).map(t => {
      const cond = Array.from(t.getElementsByTagNameNS('*', '*')).find(c => c.children.length === 0 && c.textContent.trim());
      return { id: t.getAttribute('Identifier'), from: t.getAttribute('Source'), to: t.getAttribute('Target'), condition: cond ? cond.textContent.trim() : '' };
    });

    return { nodes, transitions };
  }

  /** Layers nodes by longest path from the start, ignoring edges that loop back. */
  function layer(model) {
    const out = {};
    const incoming = {};
    for (const n of model.nodes) { out[n.id] = []; incoming[n.id] = 0; }
    for (const t of model.transitions) {
      if (out[t.from] && t.to in incoming) { out[t.from].push(t.to); incoming[t.to]++; }
    }
    const back = new Set();
    const state = {};
    const visit = id => {
      state[id] = 1;
      for (const next of out[id]) {
        if (state[next] === 1) back.add(id + '>' + next);
        else if (!state[next]) visit(next);
      }
      state[id] = 2;
    };
    const roots = model.nodes.filter(n => n.kind === 'StartTask' || incoming[n.id] === 0);
    for (const r of roots) if (!state[r.id]) visit(r.id);
    for (const n of model.nodes) if (!state[n.id]) visit(n.id);

    const depth = {};
    const order = [];
    const seen = {};
    const topo = id => {
      if (seen[id]) return;
      seen[id] = true;
      for (const next of out[id]) if (!back.has(id + '>' + next)) topo(next);
      order.unshift(id);
    };
    for (const n of model.nodes) topo(n.id);
    for (const id of order) depth[id] = depth[id] || 0;
    for (const id of order) {
      for (const next of out[id]) {
        if (!back.has(id + '>' + next)) depth[next] = Math.max(depth[next] || 0, depth[id] + 1);
      }
    }
    const layers = [];
    for (const n of model.nodes) (layers[depth[n.id]] = layers[depth[n.id]] || []).push(n);
    return { layers: layers.filter(Boolean), back };
  }

  // ── Catalog lookups ───────────────────────────────────────────────────

  function findPackage(name) {
    return flow.packages.find(p => p.name === name || p.name.replace(/\./g, '/') === name);
  }

  function findRule(entry) {
    for (const p of flow.packages) {
      const r = p.rules.find(r => (entry.uuid && r.uuid === entry.uuid) || r.name === entry.name);
      if (r) return r;
    }
    return null;
  }

  function ruleButton(rule) {
    const b = el('button', 'rule-link', rule.title);
    b.title = 'Open this rule';
    b.onclick = () => vscode.postMessage({ type: 'openRule', path: rule.path });
    return b;
  }

  // ── Rendering ─────────────────────────────────────────────────────────

  function renderNode(node, stepNumber) {
    const kind = node.kind;
    if (kind === 'StartTask' || kind === 'StopTask') {
      return el('div', 'node node-terminal', kind === 'StartTask' ? 'Start' : 'End');
    }
    if (/Branch/.test(kind)) {
      const n = el('div', 'node node-branch');
      n.appendChild(el('span', '', node.label || 'Decision'));
      return n;
    }
    if (/Fork/.test(kind)) return el('div', 'node node-bar', node.label || 'Run in parallel');
    if (/Join/.test(kind)) return el('div', 'node node-bar', node.label || 'Wait for all branches');

    const card = el('div', 'node node-task');
    const head = el('div', 'task-head');
    if (stepNumber) head.appendChild(el('span', 'step', 'Step ' + stepNumber));
    const task = node.task || { entries: [] };
    const fallback = task.kind === 'ActionTask' ? 'Set values' : task.kind === 'FlowTask' ? 'Subflow' : humanize(task.id || node.id);
    const title = node.label || task.entries.map(e => humanize(e.name || '')).join(', ') || fallback;
    head.appendChild(el('h2', '', title));
    card.appendChild(head);

    if (task.kind === 'ActionTask') {
      card.appendChild(el('p', 'task-meta', 'Sets values directly (no rules)'));
      if (task.actions) card.appendChild(el('pre', 'actions', task.actions));
      return card;
    }
    if (task.kind === 'FlowTask') {
      card.appendChild(el('p', 'task-meta', 'Runs another ruleflow' + (task.subflow ? ': ' + humanize(task.subflow.replace(/\.rfl$/, '')) : '')));
      return card;
    }

    let total = 0;
    const body = el('div', 'task-body');
    for (const entry of task.entries) {
      if (entry.kind === 'Package') {
        const pkg = findPackage(entry.name);
        const block = el('div', 'pkg');
        if (!pkg) {
          block.appendChild(el('div', 'pkg-name', humanize(entry.name)));
          block.appendChild(el('p', 'warn', 'Package “' + entry.name + '” not found in this project.'));
          body.appendChild(block);
          continue;
        }
        total += pkg.rules.length;
        if (task.entries.length > 1 || humanize(pkg.name) !== title) block.appendChild(el('div', 'pkg-name', humanize(pkg.name)));
        if (pkg.documentation) block.appendChild(el('p', 'pkg-doc', pkg.documentation));
        const details = el('details', 'rules');
        details.appendChild(el('summary', '', pkg.rules.length + ' rule' + (pkg.rules.length === 1 ? '' : 's')));
        const list = el('ul');
        for (const r of pkg.rules) list.appendChild(el('li')).appendChild(ruleButton(r));
        details.appendChild(list);
        block.appendChild(details);
        body.appendChild(block);
      } else {
        const rule = findRule(entry);
        total += 1;
        const block = el('div', 'pkg');
        if (rule) block.appendChild(ruleButton(rule));
        else block.appendChild(el('p', 'warn', 'Rule “' + (entry.name || entry.uuid) + '” not found.'));
        body.appendChild(block);
      }
    }
    if (!task.entries.length) body.appendChild(el('p', 'warn', 'This step runs no rules.'));

    const meta = el('p', 'task-meta', total + ' rule' + (total === 1 ? '' : 's'));
    const mode = el('span', 'mode', task.mode === 'Fastpath' ? 'Fastpath' : task.mode === 'Sequential' ? 'Sequential' : 'RetePlus');
    mode.title = task.mode === 'Fastpath' || task.mode === 'Sequential'
      ? 'Each rule is evaluated once, in order'
      : 'Rules are re-evaluated when other rules of this step change data';
    meta.appendChild(mode);
    card.appendChild(meta);
    card.appendChild(body);
    return card;
  }

  function render() {
    if (!flow) return;
    if (resizeObserver) resizeObserver.disconnect();
    app.replaceChildren();

    const model = parseModel(flow.model);

    const top = el('header', 'top');
    const crumbs = el('div', 'crumbs');
    if (flow.project) crumbs.appendChild(el('span', '', flow.project));
    crumbs.appendChild(el('span', '', 'Ruleflow'));
    top.appendChild(crumbs);
    const titleRow = el('div', 'title-row');
    titleRow.appendChild(el('h1', '', humanize(flow.name || 'Ruleflow')));
    const open = el('button', 'btn btn-secondary', 'Open as XML');
    open.onclick = () => vscode.postMessage({ type: 'openText' });
    titleRow.appendChild(open);
    top.appendChild(titleRow);
    if (flow.documentation) top.appendChild(el('p', 'description', flow.documentation));
    app.appendChild(top);

    if (model.error) {
      app.appendChild(el('p', 'warn', model.error));
      return;
    }

    // Summary and packages that never run.
    const ruleTasks = model.nodes.filter(n => n.task && n.task.kind === 'RuleTask');
    const used = new Set();
    for (const n of ruleTasks) for (const e of n.task.entries) {
      const p = e.kind === 'Package' ? findPackage(e.name) : null;
      if (p) used.add(p.name);
    }
    const ruleCount = flow.packages.filter(p => used.has(p.name)).reduce((s, p) => s + p.rules.length, 0);
    const branches = model.nodes.filter(n => /Branch/.test(n.kind)).length;
    top.appendChild(el('p', 'summary',
      ruleTasks.length + ' step' + (ruleTasks.length === 1 ? '' : 's') + ' · ' + ruleCount + ' rule' + (ruleCount === 1 ? '' : 's') +
      (branches ? ' · ' + branches + ' decision point' + (branches === 1 ? '' : 's') : '')));
    const unused = flow.packages.filter(p => p.rules.length && !used.has(p.name));
    if (unused.length) {
      const warn = el('div', 'callout');
      warn.appendChild(el('strong', '', 'Not in this flow: '));
      warn.appendChild(document.createTextNode(unused.map(p => humanize(p.name) + ' (' + p.rules.length + ')').join(', ') + '. These rules never run.'));
      top.appendChild(warn);
    }

    // Diagram: one row per layer, arrows drawn on an SVG overlay.
    const { layers, back } = layer(model);
    const diagram = el('div', 'diagram');
    const svg = document.createElementNS(SVG_NS, 'svg');
    svg.classList.add('edges');
    diagram.appendChild(svg);
    const nodeEls = {};
    let step = 0;
    for (const row of layers) {
      const rowEl = el('div', 'layer');
      for (const n of row) {
        const numbered = n.task && n.task.kind === 'RuleTask' ? ++step : 0;
        const nodeEl = renderNode(n, numbered);
        nodeEls[n.id] = nodeEl;
        rowEl.appendChild(nodeEl);
      }
      diagram.appendChild(rowEl);
    }
    app.appendChild(diagram);

    const labelEls = [];
    const drawEdges = () => {
      svg.replaceChildren();
      labelEls.forEach(l => l.remove());
      labelEls.length = 0;
      const box = diagram.getBoundingClientRect();
      svg.setAttribute('width', box.width);
      svg.setAttribute('height', box.height);

      const defs = document.createElementNS(SVG_NS, 'defs');
      const marker = document.createElementNS(SVG_NS, 'marker');
      marker.setAttribute('id', 'arrow');
      marker.setAttribute('viewBox', '0 0 10 10');
      marker.setAttribute('refX', '9');
      marker.setAttribute('refY', '5');
      marker.setAttribute('markerWidth', '7');
      marker.setAttribute('markerHeight', '7');
      marker.setAttribute('orient', 'auto-start-reverse');
      const tip = document.createElementNS(SVG_NS, 'path');
      tip.setAttribute('d', 'M0,0 L10,5 L0,10 z');
      tip.setAttribute('class', 'arrow-tip');
      marker.appendChild(tip);
      defs.appendChild(marker);
      svg.appendChild(defs);

      const outgoing = {};
      for (const t of model.transitions) (outgoing[t.from] = outgoing[t.from] || []).push(t);

      for (const t of model.transitions) {
        const a = nodeEls[t.from];
        const b = nodeEls[t.to];
        if (!a || !b) continue;
        const ra = a.getBoundingClientRect();
        const rb = b.getBoundingClientRect();
        const path = document.createElementNS(SVG_NS, 'path');
        let d;
        let mid;
        if (back.has(t.from + '>' + t.to)) {
          // Loop back: leave on the right, travel up, enter on the right.
          const x1 = ra.right - box.left, y1 = ra.top + ra.height / 2 - box.top;
          const x2 = rb.right - box.left, y2 = rb.top + rb.height / 2 - box.top;
          const xr = Math.max(x1, x2) + 40;
          d = `M${x1},${y1} C${xr},${y1} ${xr},${y2} ${x2},${y2}`;
          mid = { x: xr - 10, y: (y1 + y2) / 2 };
        } else {
          const x1 = ra.left + ra.width / 2 - box.left, y1 = ra.bottom - box.top;
          const x2 = rb.left + rb.width / 2 - box.left, y2 = rb.top - box.top;
          const dy = Math.max(24, (y2 - y1) / 2);
          d = `M${x1},${y1} C${x1},${y1 + dy} ${x2},${y2 - dy} ${x2},${y2}`;
          mid = { x: (x1 + x2) / 2, y: (y1 + y2) / 2 };
        }
        path.setAttribute('d', d);
        path.setAttribute('class', 'edge');
        path.setAttribute('marker-end', 'url(#arrow)');
        svg.appendChild(path);

        const siblings = outgoing[t.from] || [];
        const label = t.condition ? 'when ' + t.condition : siblings.length > 1 && siblings.some(s => s.condition) ? 'otherwise' : '';
        if (label) {
          const tag = el('div', 'edge-label', label);
          tag.title = label;
          tag.style.left = mid.x + 'px';
          tag.style.top = mid.y + 'px';
          diagram.appendChild(tag);
          labelEls.push(tag);
        }
      }
    };

    requestAnimationFrame(drawEdges);
    resizeObserver = new ResizeObserver(() => requestAnimationFrame(drawEdges));
    resizeObserver.observe(diagram);
    for (const d of diagram.querySelectorAll('details')) d.addEventListener('toggle', () => requestAnimationFrame(drawEdges));
  }

  window.addEventListener('message', e => {
    if (e.data.type === 'update') {
      flow = e.data.flow;
      render();
    }
  });

  vscode.postMessage({ type: 'ready' });
})();
