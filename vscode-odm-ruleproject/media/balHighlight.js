// Shared BAL syntax colouring for the business views (rule, decision table).
// Exposes window.Bal; styles (.tok-*, .statements, .conj) live in ruleView.css.
(function () {
  // Longest phrases first so "is more than" wins over "is".
  const OPERATORS = [
    'it is not true that', 'make it true that', 'make it false that',
    'is at least', 'is at most', 'is more than', 'is less than', 'is equal to',
    'is not equal to', 'is not empty', 'is empty', 'is not null', 'is null',
    'is not one of', 'is one of', 'is between', 'is after', 'is before',
    'at least', 'at most', 'more than', 'less than',
    'is not', 'is', 'contains', 'does not contain'
  ];
  const KEYWORDS = ['set', 'to', 'add', 'remove', 'print', 'there is no', 'there is', 'there are', 'the number of', 'where', 'and', 'or'];

  const escapeRe = s => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const phrase = list => list.map(p => escapeRe(p).replace(/ /g, '\\s+')).join('|');
  const TOKEN_RE = new RegExp(
    [
      '("(?:[^"\\\\]|\\\\.)*")',                 // 1 string
      "('[^']*')",                               // 2 variable
      '(<[^<>/]+>)',                             // 3 placeholder (decision table templates)
      '\\b(true|false|null)\\b',                 // 4 literal
      '(-?\\b\\d+(?:\\.\\d+)?\\b)',              // 5 number
      '\\b(' + phrase(OPERATORS) + ')\\b',       // 6 operator
      '\\b(' + phrase(KEYWORDS) + ')\\b'         // 7 keyword
    ].join('|'),
    'gi'
  );
  const CLASSES = [null, 'tok-string', 'tok-var', 'tok-ph', 'tok-literal', 'tok-number', 'tok-op', 'tok-kw'];

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
      const g = CLASSES.findIndex((_, i) => i > 0 && m[i] !== undefined);
      // Variables and placeholders render as chips without their delimiters.
      const shown = g === 2 || g === 3 ? m[0].slice(1, -1) : m[0];
      const span = el('span', CLASSES[g], shown);
      if (g === 3) span.title = 'Parameter: ' + shown;
      parent.appendChild(span);
      last = m.index + m[0].length;
    }
    if (last < text.length) parent.appendChild(document.createTextNode(text.slice(last)));
    return parent;
  }

  /**
   * Builds an <ol class="statements"> from BAL lines, pulling a leading
   * and/or into the left gutter. `gutter` keeps an empty slot on plain lines.
   */
  function renderStatements(lines, gutter) {
    const list = el('ol', 'statements');
    for (const line of lines) {
      const item = el('li', 'statement');
      let body = line.replace(/\s*;\s*$/, '');
      const conj = /^(and|or)\s+(.*)$/i.exec(body);
      if (conj) {
        item.appendChild(el('span', 'conj', conj[1].toLowerCase()));
        body = conj[2];
      } else if (gutter) {
        item.appendChild(el('span', 'conj conj-empty'));
      }
      item.appendChild(renderPhrase(el('span', 'statement-text'), body));
      list.appendChild(item);
    }
    return list;
  }

  /** Splits a condition block ("if\n  a\n  and b") into statement lines, dropping the leading "if". */
  function conditionLines(text) {
    return text
      .replace(/^\s*if\b/i, '')
      .split(/\r?\n/)
      .map(l => l.trim())
      .filter(l => l && !l.startsWith('//'));
  }

  window.Bal = { renderPhrase, renderStatements, conditionLines };
})();
