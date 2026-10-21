// Webview script for the ODM Decision Table (.dta) viewer — split-cell layout.
(function () {
  const vscode = acquireVsCodeApi();
  const app = document.getElementById('app');
  let view = null;
  // Header mode: 'titles' (column HeaderText) or 'expressions' (BAL templates).
  let headerMode = (vscode.getState() || {}).headerMode || 'titles';

  // ── DOM helpers ──────────────────────────────────────────────────────────

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) { e.className = cls; }
    if (text !== undefined) { e.textContent = text; }
    return e;
  }

  function section(title, hint) {
    const s = el('section', 'card card-params');
    const h = el('header', 'card-header');
    h.appendChild(el('span', 'card-label', title));
    if (hint !== undefined) { h.appendChild(el('span', 'card-hint', hint)); }
    s.appendChild(h);
    return s;
  }

  // ── Page header ──────────────────────────────────────────────────────────

  function header() {
    const top = el('header', 'top');

    const crumbs = el('div', 'crumbs');
    if (view.project) { crumbs.appendChild(el('span', '', view.project)); }
    if (view.packagePath) { crumbs.appendChild(el('span', '', view.packagePath)); }
    crumbs.appendChild(el('span', '', 'Decision table'));
    top.appendChild(crumbs);

    const row = el('div', 'title-row');
    row.appendChild(el('h1', '', view.name));
    const openBtn = el('button', 'btn btn-secondary', 'Open as XML');
    openBtn.onclick = () => vscode.postMessage({ type: 'openText' });
    row.appendChild(openBtn);
    top.appendChild(row);

    if (view.documentation) { top.appendChild(el('p', 'description', view.documentation)); }
    return top;
  }

  // ── Decision table grid ──────────────────────────────────────────────────
  //
  // Layout per column with N placeholders:
  //
  //  ┌──────────────────────────────────────────────────┐
  //  │  CONDITIONS                │  ACTIONS            │  ← group row (colspan)
  //  ├──────────────┬─────────────┼─────────────────────┤
  //  │ cond col A                 │ act col B           │  ← column header (colspan = params.length)
  //  ├──────┬───────┼─────────────┼─────────────────────┤
  //  │ <min>│ <max> │             │ <a number>          │  ← param sub-header row
  //  ├──────┼───────┼─────────────┼─────────────────────┤
  //  │  600 │  650  │             │  14.5               │  ← data row
  //
  // A column with 0 params (template has no placeholders) gets exactly 1 sub-col.

  function subColCount(col) {
    return col.params.length || 1;
  }

  function tableSection() {
    const colCount = view.columns.length;
    const rowCount = view.rows.length;

    const hint = rowCount + ' row' + (rowCount === 1 ? '' : 's') +
      ', ' + colCount + ' column' + (colCount === 1 ? '' : 's');
    const s = section('Decision Table', hint);
    s.querySelector('.card-header').appendChild(headerToggle());

    if (!colCount) {
      s.appendChild(el('p', 'dt-empty-state', 'No columns found in this decision table.'));
      return s;
    }

    const condCols = view.columns.filter(c => c.kind === 'condition');
    const actCols  = view.columns.filter(c => c.kind === 'action');

    const condSubTotal = condCols.reduce((n, c) => n + subColCount(c), 0);
    const actSubTotal  = actCols.reduce((n, c) => n + subColCount(c), 0);

    const wrap  = el('div', 'dt-wrap');
    const table = el('table', 'dt');
    const thead = el('thead');

    // ── Row 1: group headers (Conditions / Actions) ───────────────────────
    const groupRow = el('tr', 'group-row');
    const gutterTh = el('th', 'row-num');
    gutterTh.rowSpan = 3;  // spans all three header rows
    groupRow.appendChild(gutterTh);

    if (condCols.length) {
      const th = el('th', 'col-cond');
      th.colSpan = condSubTotal;
      th.textContent = 'Conditions';
      groupRow.appendChild(th);
    }
    if (actCols.length) {
      const th = el('th', 'col-act');
      th.colSpan = actSubTotal;
      th.textContent = 'Actions';
      groupRow.appendChild(th);
    }
    thead.appendChild(groupRow);

    // ── Row 2: column headers (one th per column, spans its sub-cols) ─────
    const colRow = el('tr', 'col-header-row');
    const showExpr = headerMode === 'expressions';
    for (const col of view.columns) {
      const th = el('th', col.kind === 'condition' ? 'col-cond col-name' : 'col-act col-name');
      th.colSpan = subColCount(col);
      if (showExpr) {
        th.classList.add('col-expr');
        if (col.template) { Bal.renderPhrase(th, col.template); } else { th.textContent = '—'; }
      } else {
        th.textContent = col.headerText || shortLabel(col.template);
      }
      colRow.appendChild(th);
    }
    thead.appendChild(colRow);

    // ── Row 3: param sub-headers (one th per placeholder) ─────────────────
    const paramRow = el('tr', 'param-header-row');
    for (const col of view.columns) {
      const cls = col.kind === 'condition' ? 'col-cond param-hdr' : 'col-act param-hdr';
      if (col.params.length === 0) {
        // No placeholders: single sub-col shows the full template
        const th = el('th', cls + ' param-hdr-only');
        if (col.template) { Bal.renderPhrase(th, col.template); } else { th.textContent = '—'; }
        paramRow.appendChild(th);
      } else {
        col.params.forEach((p, k) => {
          const th = el('th', cls);
          const title = (col.paramHeaders || [])[k];
          if (showExpr) { Bal.renderPhrase(th, '<' + p + '>'); } else { th.textContent = title || p; }
          paramRow.appendChild(th);
        });
      }
    }
    thead.appendChild(paramRow);
    table.appendChild(thead);

    // ── Body ──────────────────────────────────────────────────────────────
    const tbody = el('tbody');

    if (!rowCount) {
      const tr = el('tr');
      // +1 for gutter
      const totalSubs = view.columns.reduce((n, c) => n + subColCount(c), 0) + 1;
      const td = el('td');
      td.colSpan = totalSubs;
      td.appendChild(el('span', 'dt-empty', 'No rows defined.'));
      tr.appendChild(td);
      tbody.appendChild(tr);
    } else {
      for (let i = 0; i < view.rows.length; i++) {
        const row = view.rows[i];
        const tr  = el('tr');
        tr.appendChild(el('td', 'row-num', String(i + 1)));

        for (let j = 0; j < view.columns.length; j++) {
          const col    = view.columns[j];
          const values = row.cells[j] ?? [];   // raw param values for this column
          const tdCls  = col.kind === 'condition' ? 'col-cond' : 'col-act';

          if (col.params.length === 0) {
            // No placeholders: render single merged cell with whatever value there is
            const td = el('td', tdCls + ' cell-only');
            const v  = values[0] ?? '';
            if (v) {
              td.textContent = v;
            } else {
              td.appendChild(el('span', 'dt-empty', '—'));
            }
            tr.appendChild(td);
          } else {
            // One <td> per placeholder
            for (let k = 0; k < col.params.length; k++) {
              const isLast = k === col.params.length - 1;
              const td = el('td', tdCls + ' cell-param' + (isLast ? ' cell-param-last' : ''));
              const v  = values[k] ?? '';
              if (v) {
                td.textContent = v;
              } else {
                td.appendChild(el('span', 'dt-empty', '—'));
              }
              tr.appendChild(td);
            }
          }
        }
        tbody.appendChild(tr);
      }
    }

    table.appendChild(tbody);
    wrap.appendChild(table);
    s.appendChild(wrap);
    return s;
  }

  /** Segmented control switching column headers between titles and BAL expressions. */
  function headerToggle() {
    const group = el('div', 'seg-toggle');
    group.setAttribute('role', 'group');
    group.setAttribute('aria-label', 'Column headers');
    for (const [mode, label] of [['titles', 'Titles'], ['expressions', 'Expressions']]) {
      const b = el('button', 'seg-btn' + (headerMode === mode ? ' active' : ''), label);
      b.setAttribute('aria-pressed', String(headerMode === mode));
      b.onclick = () => {
        if (headerMode === mode) { return; }
        headerMode = mode;
        vscode.setState({ ...(vscode.getState() || {}), headerMode });
        render();
      };
      group.appendChild(b);
    }
    return group;
  }

  /** Shorten a BAL template to ≤ 48 chars, replacing <ph> with •. */
  function shortLabel(template) {
    const clean = template
      .replace(/<[^>/]+>/g, '•')
      .replace(/\s+/g, ' ')
      .trim();
    if (clean.length <= 48) { return clean; }
    return clean.slice(0, 46).trimEnd() + '…';
  }

  // ── Precondition card ────────────────────────────────────────────────────

  function preconditionSection() {
    const s = section('Precondition', 'Applies to every row');
    s.classList.add('card-if');
    s.appendChild(Bal.renderStatements(Bal.conditionLines(view.precondition), true));
    return s;
  }

  // ── Meta card ────────────────────────────────────────────────────────────

  function metaSection() {
    const s = section('Properties');
    const dl = el('dl', 'facts');

    function row(k, v) {
      if (!v) { return; }
      dl.appendChild(el('dt', '', k));
      dl.appendChild(el('dd', '', v));
    }

    row('File', view.name + '.dta');
    row('UUID', view.uuid);
    if (view.project)     { row('Project', view.project); }
    if (view.packagePath) { row('Package', view.packagePath); }

    s.appendChild(dl);
    return s;
  }

  // ── Render ───────────────────────────────────────────────────────────────

  function render() {
    const scroll = window.scrollY;
    const parts = [header()];
    if (view.precondition) { parts.push(preconditionSection()); }
    parts.push(tableSection(), metaSection());
    app.replaceChildren(...parts);
    window.scrollTo(0, scroll);
  }

  window.addEventListener('message', e => {
    if (e.data.type !== 'update') { return; }
    view = e.data.view;
    render();
  });

  vscode.postMessage({ type: 'ready' });
})();
