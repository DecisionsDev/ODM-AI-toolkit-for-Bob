// Webview script for the ODM Decision Table (.dta) viewer — split-cell layout.
(function () {
  const vscode = acquireVsCodeApi();
  const app = document.getElementById('app');
  let view = null;

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
    for (const col of view.columns) {
      const th = el('th', col.kind === 'condition' ? 'col-cond col-name' : 'col-act col-name');
      th.colSpan = subColCount(col);
      th.title = col.template;   // full template on hover
      th.textContent = shortLabel(col.template);
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
        th.textContent = col.template || '—';
        paramRow.appendChild(th);
      } else {
        for (const p of col.params) {
          const th = el('th', cls);
          th.textContent = p;
          paramRow.appendChild(th);
        }
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

  /** Shorten a BAL template to ≤ 48 chars, replacing <ph> with •. */
  function shortLabel(template) {
    const clean = template
      .replace(/<[^>/]+>/g, '•')
      .replace(/\s+/g, ' ')
      .trim();
    if (clean.length <= 48) { return clean; }
    return clean.slice(0, 46).trimEnd() + '…';
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
    app.replaceChildren(header(), tableSection(), metaSection());
    window.scrollTo(0, scroll);
  }

  window.addEventListener('message', e => {
    if (e.data.type !== 'update') { return; }
    view = e.data.view;
    render();
  });

  vscode.postMessage({ type: 'ready' });
})();
