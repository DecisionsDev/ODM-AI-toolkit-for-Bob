import * as vscode from 'vscode';

interface HoverEntry {
  pattern: RegExp;
  documentation: vscode.MarkdownString;
}

function md(text: string): vscode.MarkdownString {
  const m = new vscode.MarkdownString(text);
  m.isTrusted = true;
  return m;
}

/** All BAL keyword hover entries, matched left-to-right (longest first). */
const HOVER_ENTRIES: HoverEntry[] = [
  // ── Structural keywords ────────────────────────────────────────────────
  {
    pattern: /\bif\b/,
    documentation: md(
      '**`if`** — Opens the *condition* section of a BAL rule.\n\n' +
      'All conditions are combined with `and` (no `or`). Each condition is on its own line.\n\n' +
      '```brl\nif\n  the amount of \'the request\' is more than 1000\n  and the status of \'the request\' is "ACTIVE"\n```'
    )
  },
  {
    pattern: /\bthen\b/,
    documentation: md(
      '**`then`** — Opens the *action* section of a BAL rule.\n\n' +
      'Best practice: **one action phrase per rule**. Each statement ends with ` ;`.\n\n' +
      '```brl\nthen\n  add "Amount exceeds limit" to the messages of \'the request\' ;\n```'
    )
  },
  {
    pattern: /\bdefinitions\b/,
    documentation: md(
      '**`definitions`** — Optional block before `if` for binding local variables.\n\n' +
      'Use `set \'v\' to <expr> ;` to avoid repeating long navigation chains, or to bind an intermediate ' +
      'object that you then compare with `it is not true that`.\n\n' +
      '```brl\ndefinitions\n  set \'score\' to the credit score of the borrower of \'the request\' ;\nif\n  \'score\' is less than 500\n```'
    )
  },

  // ── Condition operators ────────────────────────────────────────────────
  {
    pattern: /\bis more than\b/,
    documentation: md(
      '**`is more than`** — Strict greater-than numeric comparison.\n\n' +
      '> ✅ `the amount of \'the request\' is more than 1000`  \n' +
      '> ❌ `> 1000` — symbols are not valid BAL'
    )
  },
  {
    pattern: /\bis less than\b/,
    documentation: md(
      '**`is less than`** — Strict less-than numeric comparison.\n\n' +
      '> ✅ `the score of \'the applicant\' is less than 500`  \n' +
      '> ❌ `< 500` — symbols are not valid BAL'
    )
  },
  {
    pattern: /\bis at least\b/,
    documentation: md(
      '**`is at least`** — Greater-than-or-equal numeric comparison (`>=`).\n\n' +
      '> ✅ `the age of \'the applicant\' is at least 18`  \n' +
      '> ❌ `>= 18` or `is equal to or more than`'
    )
  },
  {
    pattern: /\bis at most\b/,
    documentation: md(
      '**`is at most`** — Less-than-or-equal numeric comparison (`<=`).\n\n' +
      '> ✅ `the debt ratio of \'the applicant\' is at most 0.5`  \n' +
      '> ❌ `<= 0.5` or `is equal to or less than`'
    )
  },
  {
    pattern: /\bis one of\b/,
    documentation: md(
      '**`is one of { … }`** — Set membership test.\n\n' +
      '> ✅ `the country of \'the request\' is one of { "US", "CA", "MX" }`  \n' +
      '> ❌ `is in { ... }` — `in` is not valid BAL for sets'
    )
  },
  {
    pattern: /\bit is not true that\b/,
    documentation: md(
      '**`it is not true that`** — The correct negation form in BAL.\n\n' +
      'Use this instead of `not (...)` or `is not (expression)`.\n\n' +
      '```brl\nit is not true that \'the request\' is approved\n```\n\n' +
      'To compare two navigated values, bind one first in `definitions`:\n' +
      '```brl\ndefinitions\n  set \'prev\' to the previous status of \'the request\' ;\nif\n  it is not true that the current status of \'the request\' is \'prev\'\n```'
    )
  },
  {
    pattern: /\bis not null\b/,
    documentation: md(
      '**`is not null`** — Null check before navigating through an object.\n\n' +
      'Always check intermediate objects in their own `and` clause:\n' +
      '```brl\nthe borrower of \'the request\' is not null\nand the credit score of the borrower of \'the request\' is less than 500\n```'
    )
  },

  // ── Connectors ─────────────────────────────────────────────────────────
  {
    pattern: /\band\b/,
    documentation: md(
      '**`and`** — The only allowed condition connector in BAL.\n\n' +
      'Each `and` clause goes on its own line. To express an `or`, write separate rules instead.'
    )
  },

  // ── Action keywords ────────────────────────────────────────────────────
  {
    pattern: /\bset the\b/,
    documentation: md(
      '**`set the`** — Action: assign a value to a property.\n\n' +
      '```brl\nthen\n  set the status of \'the request\' to "APPROVED" ;\n```\n\n' +
      '> Every action statement ends with ` ;`'
    )
  },
  {
    pattern: /\bmake it\b/,
    documentation: md(
      '**`make it`** — Action: set a boolean property via its adjective phrase.\n\n' +
      '```brl\nthen\n  make it true that \'the request\' is approved ;\n```'
    )
  },
  {
    pattern: /\badd\b/,
    documentation: md(
      '**`add … to the … of`** — Action: add an element to a collection.\n\n' +
      '```brl\nthen\n  add "Amount exceeds limit" to the messages of \'the request\' ;\n```\n\n' +
      'The collection must be declared `readonly` in the BOM with an `addX(...)` XOM method.'
    )
  },

  // ── BOM header ─────────────────────────────────────────────────────────
  {
    pattern: /\bproperty\b/,
    documentation: md(
      '**`property`** — BOM file header directive.\n\n' +
      'Common values:\n' +
      '- `property loadGetterSetterAsProperties "true"` — binds BOM to XOM getters/setters\n' +
      '- `property "factory.ignore" "true"` — marks a BOM property as computed (no setter)\n' +
      '- `property "ilog.rules.engine.dataio.forConversion" "true"` — marks the full-args constructor'
    )
  }
];

export class BrlHoverProvider implements vscode.HoverProvider {
  provideHover(
    document: vscode.TextDocument,
    position: vscode.Position
  ): vscode.ProviderResult<vscode.Hover> {
    // Build context: the line and a small window around the cursor
    const line = document.lineAt(position).text;
    const prefix = line.substring(0, position.character + 20);

    for (const entry of HOVER_ENTRIES) {
      const match = prefix.match(entry.pattern);
      if (match && match.index !== undefined) {
        const start = new vscode.Position(position.line, match.index);
        const end = new vscode.Position(position.line, match.index + match[0].length);
        return new vscode.Hover(entry.documentation, new vscode.Range(start, end));
      }
    }
    return undefined;
  }
}
