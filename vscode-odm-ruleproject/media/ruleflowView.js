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
      const id = t.getAttribute('Identifier');
      return { id, from: t.getAttribute('Source'), to: t.getAttribute('Target'), condition: cond ? cond.textContent.trim() : '', label: labels[id + '#label'] || '' };
    });

    return { nodes, transitions };
  }

  /**
   * Places nodes on a grid. Rows follow the longest path from the start. Each node hands
   * its column to the successor with the longest continuation; other branches open a new
   * column beside it (left if declared before the main branch, right otherwise). Edges that
   * cannot run down a free column get a narrow lane next to their source.
   */
  function layout(model) {
    const out = {}, inc = {}, index = {};
    model.nodes.forEach((n, i) => { out[n.id] = []; inc[n.id] = []; index[n.id] = i; });
    const transitions = model.transitions.filter(t => out[t.from] && inc[t.to]);
    transitions.forEach((t, i) => { t.order = i; out[t.from].push(t); inc[t.to].push(t); });

    // Edges that loop back to an earlier node.
    const back = new Set();
    const state = {};
    const visit = id => {
      state[id] = 1;
      for (const t of out[id]) {
        if (state[t.to] === 1) back.add(t);
        else if (!state[t.to]) visit(t.to);
      }
      state[id] = 2;
    };
    const roots = model.nodes.filter(n => n.kind === 'StartTask' || !inc[n.id].length);
    for (const r of roots) if (!state[r.id]) visit(r.id);
    for (const n of model.nodes) if (!state[n.id]) visit(n.id);
    const fwdOut = id => out[id].filter(t => !back.has(t));
    const fwdIn = id => inc[id].filter(t => !back.has(t));

    const order = [];
    const seen = {};
    const topo = id => {
      if (seen[id]) return;
      seen[id] = true;
      for (const t of fwdOut(id)) topo(t.to);
      order.unshift(id);
    };
    for (const n of model.nodes) topo(n.id);
    const row = {}, rest = {};
    for (const id of order) row[id] = 0;
    for (const id of order) for (const t of fwdOut(id)) row[t.to] = Math.max(row[t.to], row[id] + 1);
    for (const id of [...order].reverse()) rest[id] = Math.max(0, ...fwdOut(id).map(t => rest[t.to] + 1));

    // Main branch of each node: longest continuation, then unconditional, then file order.
    const heir = {};
    for (const n of model.nodes) {
      const tight = fwdOut(n.id).filter(t => row[t.to] === row[n.id] + 1);
      tight.sort((a, b) => rest[b.to] - rest[a.to] || (a.condition ? 1 : 0) - (b.condition ? 1 : 0) || a.order - b.order);
      if (tight.length) heir[n.id] = tight[0];
    }

    const cols = [];
    let seq = 0;
    const addColumn = (kind, beside, side) => {
      const c = { kind, seq: seq++, rows: new Set(), segs: [], beside, side };
      if (!beside) cols.push(c);
      else {
        let i = cols.indexOf(beside);
        if (side > 0) {
          i++;
          if (kind === 'node') while (i < cols.length && cols[i].beside === beside && cols[i].side > 0) i++;
        }
        cols.splice(i, 0, c);
      }
      return c;
    };

    const colOf = {};
    const byRow = [...order].sort((a, b) => row[a] - row[b] || index[a] - index[b]);
    const bySeq = (a, b) => colOf[a.from].seq - colOf[b.from].seq;
    for (const id of byRow) {
      const preds = fwdIn(id).filter(t => row[t.from] === row[id] - 1 && colOf[t.from]).sort(bySeq);
      const inherit = preds.find(t => heir[t.from] === t);
      let c;
      if (inherit) c = colOf[inherit.from];
      else if (preds.length) {
        const h = heir[preds[0].from];
        c = addColumn('node', colOf[preds[0].from], h && preds[0].order < h.order ? -1 : 1);
      } else c = addColumn('node');
      colOf[id] = c;
      c.rows.add(row[id]);
    }

    // Routing. Vertical segments sharing a target may overlap (they merge into one line).
    const free = (c, r1, r2, target) => {
      for (let r = r1; r <= r2; r++) if (c.rows.has(r)) return false;
      return !c.segs.some(s => s.r1 <= r2 && r1 <= s.r2 && s.target !== target);
    };
    const use = (c, r1, r2, target) => { if (r1 <= r2) c.segs.push({ r1, r2, target }); };
    const routes = new Map();
    for (const t of transitions) {
      if (back.has(t)) { routes.set(t, { kind: 'back' }); continue; }
      const cs = colOf[t.from], cd = colOf[t.to], rs = row[t.from], rd = row[t.to];
      if (cs === cd && free(cs, rs + 1, rd - 1, t.to)) {
        use(cs, rs + 1, rd - 1, t.to);
        routes.set(t, { kind: 'down' });
      } else if (cs !== cd && free(cd, rs, rd - 1, t.to)) {
        use(cd, rs, rd - 1, t.to);
        routes.set(t, { kind: 'across', via: cd });
      } else if (cs !== cd && free(cs, rs + 1, rd, t.to)) {
        use(cs, rs + 1, rd, t.to);
        routes.set(t, { kind: 'drop' });
      } else {
        const h = heir[t.from];
        const side = cs !== cd ? (cols.indexOf(cd) < cols.indexOf(cs) ? -1 : 1) : h && t.order < h.order ? -1 : 1;
        const lane = cols.find(c => c.kind === 'lane' && c.beside === cs && c.side === side && free(c, rs, rd, t.to)) || addColumn('lane', cs, side);
        use(lane, rs, rd, t.to);
        routes.set(t, { kind: 'lane', via: lane });
      }
    }

    return { row, cols, colOf, routes, transitions, back, fwdIn, fwdOut, rows: Math.max(0, ...Object.values(row)) + 1 };
  }

  /** Every node and edge on a path that goes through the given node or transition. */
  function pathsThrough(L, fromId, toId, via) {
    const reach = (id, step, end) => {
      const s = new Set([id]);
      const stack = [id];
      while (stack.length) for (const t of step(stack.pop())) if (!s.has(t[end])) { s.add(t[end]); stack.push(t[end]); }
      return s;
    };
    const up = reach(fromId, L.fwdIn, 'from');
    const down = reach(toId, L.fwdOut, 'to');
    const edges = new Set(L.transitions.filter(t => !L.back.has(t) && (up.has(t.to) || down.has(t.from))));
    if (via) edges.add(via);
    return { nodes: new Set([...up, ...down]), edges };
  }

  /** Orthogonal polyline with rounded corners. */
  function orthPath(points, radius) {
    const pts = points.filter((p, i) => i === 0 || p[0] !== points[i - 1][0] || p[1] !== points[i - 1][1]);
    let d = `M${pts[0][0]},${pts[0][1]}`;
    for (let i = 1; i < pts.length - 1; i++) {
      const [px, py] = pts[i - 1], [x, y] = pts[i], [nx, ny] = pts[i + 1];
      const r = Math.min(radius, Math.hypot(x - px, y - py) / 2, Math.hypot(nx - x, ny - y) / 2);
      d += ` L${x - Math.sign(x - px) * r},${y - Math.sign(y - py) * r} Q${x},${y} ${x + Math.sign(nx - x) * r},${y + Math.sign(ny - y) * r}`;
    }
    const last = pts[pts.length - 1];
    return d + ` L${last[0]},${last[1]}`;
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

    // Diagram: nodes on a grid (one column per branch), arrows drawn on an SVG overlay.
    const L = layout(model);
    const scroller = el('div', 'diagram-scroll');
    const diagram = el('div', 'diagram');
    if (L.back.size) diagram.classList.add('has-loops');
    diagram.style.gridTemplateColumns = L.cols.map(c => c.kind === 'lane' ? 'var(--lane-width)' : 'minmax(180px, max-content)').join(' ');
    diagram.style.gridTemplateRows = `repeat(${L.rows}, auto)`;
    const svg = document.createElementNS(SVG_NS, 'svg');
    svg.classList.add('edges');
    diagram.appendChild(svg);
    const guides = new Map();
    L.cols.forEach((c, i) => {
      const g = el('div', 'col-guide');
      g.style.gridColumn = String(i + 1);
      g.style.gridRow = '1 / -1';
      guides.set(c, g);
      diagram.appendChild(g);
    });

    let trace = null;
    const cellEls = {};
    const nodeEls = {};
    const setTrace = t => {
      trace = t;
      diagram.classList.toggle('tracing', !!trace);
      for (const id in cellEls) cellEls[id].classList.toggle('on', !!trace && trace.nodes.has(id));
      for (const e of diagram.querySelectorAll('[data-edge]')) {
        const on = !!trace && trace.edges.has(L.transitions[e.dataset.edge]);
        e.classList.toggle('on', on);
        if (e.localName === 'path') e.setAttribute('marker-end', on ? 'url(#arrow-on)' : 'url(#arrow)');
      }
    };

    let step = 0;
    const placed = model.nodes.filter(n => L.colOf[n.id])
      .sort((a, b) => L.row[a.id] - L.row[b.id] || L.cols.indexOf(L.colOf[a.id]) - L.cols.indexOf(L.colOf[b.id]));
    for (const n of placed) {
      const numbered = n.task && n.task.kind === 'RuleTask' ? ++step : 0;
      const nodeEl = renderNode(n, numbered);
      const cell = el('div', 'cell');
      cell.style.gridRow = String(L.row[n.id] + 1);
      cell.style.gridColumn = String(L.cols.indexOf(L.colOf[n.id]) + 1);
      cell.appendChild(nodeEl);
      if (/Branch/.test(n.kind)) {
        nodeEl.title = 'Highlight every path through this decision';
        nodeEl.addEventListener('mouseenter', () => setTrace(pathsThrough(L, n.id, n.id)));
        nodeEl.addEventListener('mouseleave', () => setTrace(null));
      }
      nodeEls[n.id] = nodeEl;
      cellEls[n.id] = cell;
      diagram.appendChild(cell);
    }
    scroller.appendChild(diagram);
    app.appendChild(scroller);

    const marker = (id, cls) => {
      const m = document.createElementNS(SVG_NS, 'marker');
      m.setAttribute('id', id);
      m.setAttribute('viewBox', '0 0 10 10');
      m.setAttribute('refX', '9');
      m.setAttribute('refY', '5');
      m.setAttribute('markerWidth', '7');
      m.setAttribute('markerHeight', '7');
      m.setAttribute('orient', 'auto-start-reverse');
      const tip = document.createElementNS(SVG_NS, 'path');
      tip.setAttribute('d', 'M0,0 L10,5 L0,10 z');
      tip.setAttribute('class', cls);
      m.appendChild(tip);
      return m;
    };

    const labelEls = [];
    const drawEdges = () => {
      svg.replaceChildren();
      labelEls.forEach(l => l.remove());
      labelEls.length = 0;
      const box = diagram.getBoundingClientRect();
      svg.setAttribute('width', box.width);
      svg.setAttribute('height', box.height);
      const rect = e => {
        const r = e.getBoundingClientRect();
        return { l: r.left - box.left, r: r.right - box.left, t: r.top - box.top, b: r.bottom - box.top, x: (r.left + r.right) / 2 - box.left, y: (r.top + r.bottom) / 2 - box.top };
      };
      const colX = c => rect(guides.get(c)).x;

      const defs = document.createElementNS(SVG_NS, 'defs');
      defs.appendChild(marker('arrow', 'arrow-tip'));
      defs.appendChild(marker('arrow-on', 'arrow-tip on'));
      svg.appendChild(defs);

      L.transitions.forEach((t, i) => {
        const a = nodeEls[t.from] && rect(nodeEls[t.from]);
        const b = nodeEls[t.to] && rect(nodeEls[t.to]);
        if (!a || !b) return;
        const route = L.routes.get(t);
        const sideTo = (n, x) => x < n.x ? n.l : n.r;
        let pts = null;
        let d;
        if (route.kind === 'back') {
          // Loop back: leave on the right, travel up, enter on the right.
          const xr = box.width - 12;
          d = `M${a.r},${a.y} C${xr},${a.y} ${xr},${b.y} ${b.r},${b.y}`;
        } else if (route.kind === 'down') {
          pts = [[a.x, a.b], [a.x, b.t]];
        } else if (route.kind === 'across') {
          const vx = colX(route.via);
          pts = [[sideTo(a, vx), a.y], [vx, a.y], [vx, b.t]];
        } else if (route.kind === 'drop') {
          pts = [[a.x, a.b], [a.x, b.y], [sideTo(b, a.x), b.y]];
        } else {
          const lx = colX(route.via);
          pts = [[sideTo(a, lx), a.y], [lx, a.y], [lx, b.y], [sideTo(b, lx), b.y]];
        }
        const path = document.createElementNS(SVG_NS, 'path');
        path.setAttribute('d', pts ? orthPath(pts, 8) : d);
        path.setAttribute('class', 'edge');
        path.setAttribute('marker-end', 'url(#arrow)');
        path.dataset.edge = String(i);
        svg.appendChild(path);

        const siblings = L.fwdOut(t.from);
        const name = t.label || (!t.condition && siblings.length > 1 && siblings.some(s => s.condition) ? 'otherwise' : '');
        if (!name && !t.condition) return;
        const tag = el('div', 'edge-label');
        if (name) tag.appendChild(el('span', 'edge-name', name));
        if (t.condition) tag.appendChild(el('span', 'edge-cond', (name ? '' : 'when ') + t.condition));
        tag.title = (name ? name + '\n' : '') + (t.condition ? 'when ' + t.condition : '');
        // Center on the first segment: along a horizontal run, or just below the source.
        let x, y, width = 240;
        if (!pts) { x = box.width - 12; y = (a.y + b.y) / 2; }
        else {
          const [[x1, y1], [x2, y2]] = pts;
          if (y1 === y2) { x = (x1 + x2) / 2; y = y1; width = Math.max(110, Math.abs(x2 - x1) - 16); }
          else { x = x1; y = Math.min((y1 + y2) / 2, y1 + 36); }
        }
        tag.style.left = x + 'px';
        tag.style.top = y + 'px';
        tag.style.maxWidth = Math.min(width, 260) + 'px';
        tag.dataset.edge = String(i);
        tag.addEventListener('mouseenter', () => setTrace(pathsThrough(L, t.from, t.to, t)));
        tag.addEventListener('mouseleave', () => setTrace(null));
        diagram.appendChild(tag);
        labelEls.push(tag);
      });
      setTrace(trace);
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
