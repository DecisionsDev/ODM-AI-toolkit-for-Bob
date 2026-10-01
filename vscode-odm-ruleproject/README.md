# IBM ODM Rule Project & Decision Service — VS Code Extension

Full language support, visual editors, syntax highlighting, and navigation for IBM Operational Decision Manager (**ODM**) Rule Projects and Decision Services (`.brl`, `.bom`, `.rfl`, `.dop`, `.dep`) inside VS Code.

---

## Features

| Feature | Details |
|---|---|
| **Rule view** (default for `.brl`) | Business-friendly page: rule title, description, tags, and the rule as *Definitions / If / Then / Else* cards — no XML. **Edit rule** changes the rule text in place; **Open as XML** shows the Rule Designer source |
| **Business vocabulary view** (default for `.bom`) | Each business object and list of values, with every attribute and action worded as it reads in a rule (taken from the sibling `_en_US.voc`), friendly types (Text, Number, Yes / No, Date, List of …), links between objects, a search box, and a list of members that have no vocabulary. Read-only; **Open as text** shows the BOM source |
| **Ruleflow view** (default for `.rfl`) | The flow as a diagram, Start → numbered steps → End. Each step shows its packages, their description and their rules (click a rule to open it), and the execution mode. Decision branches show their condition ("when …" / "otherwise"). Warns about packages that are missing or not in the flow. Read-only; **Open as XML** shows the source |
| **Operation view** (default for `.dop`) | What the decision service runs (ruleflow, ruleset name), the data it exchanges as business terms ("the request — Input & output — loan request"), which deployments publish it, and a check of every link (rule project, ruleflow, parameters), including the UUID after `#` |
| **Deployment view** (default for `.dep`) | The RuleApp, its targets and XOM handling, each published operation with its ruleset version, options and the path clients call, the versioning policies (default highlighted), and a check of the operation links |
| **Syntax highlighting** | Full BAL grammar: rule structure, keywords, operators, strings, numbers, variables, comments |
| **Code folding** | Each `rule "..."` block collapses independently |
| **Outline view** | Breadcrumb and Explorer outline show every rule with its `definitions` / `if` / `then` sections |
| **Snippets** | 14 snippets for rules, conditions, actions, and BOM declarations |
| **Hover documentation** | Inline docs for every BAL keyword (`if`, `then`, `is more than`, `it is not true that`, …) |

---

## Getting started

### Install from source

```bash
cd vscode-odm-ruleproject
npm install
npm run compile
```

Press **F5** in VS Code to open an Extension Development Host with the extension loaded, then open any `.brl`, `.bom`, `.rfl`, `.dop`, or `.dep` file.

### Package as `.vsix`

```bash
npm install -g @vscode/vsce
vsce package
# produces odm-ruleproject-0.1.0.vsix
```

Install in VS Code: **Extensions → ⋮ → Install from VSIX…**

---

## Snippets reference

| Prefix | Description |
|---|---|
| `rule` / `brule` | Basic rule skeleton |
| `ruledef` | Rule with `definitions` block |
| `rulenull` | Rule with null-safe navigation pattern |
| `isoneof` | `is one of { … }` set check |
| `morethan` / `lessthan` / `atleast` / `atmost` | Numeric comparisons |
| `negation` | `it is not true that` |
| `setprop` | `set the … to …` action |
| `addto` | `add … to the … of` collection action |
| `makeit` | Boolean property setter |
| `setvar` | `definitions` local variable binding |
| `bomclass` | BOM class declaration |
| `bomcomputed` | BOM readonly computed property |
| `bomcollection` | BOM collection property |
| `bomenum` | BOM enum class |

---

## BAL quick reference

### Comparisons (never use symbols)

```brl
the amount of 'the request' is more than 1000
the score of 'the applicant' is at least 600
the ratio of 'the loan' is at most 0.8
the status of 'the request' is "ACTIVE"
```

### Set membership

```brl
the country of 'the transaction' is one of { "US", "CA", "MX" }
```

### Negation

```brl
it is not true that 'the request' is approved
```

### Null-safe navigation

```brl
if
  the borrower of 'the request' is not null
  and the credit score of the borrower of 'the request' is less than 500
then
  add "Low credit score" to the messages of 'the request' ;
```

### Action statements (end with `;`)

```brl
then
  set the status of 'the request' to "REJECTED" ;
  add "Insufficient score" to the messages of 'the request' ;
```

---

## Project structure

```
vscode-odm-ruleproject/
├── src/
│   ├── extension.ts                # Activation point
│   ├── ruleEditorProvider.ts       # Visual BAL rule editor
│   ├── bomEditorProvider.ts        # Business vocabulary browser
│   ├── ruleflowEditorProvider.ts   # Ruleflow diagram viewer
│   ├── deploymentEditorProvider.ts # Operation & deployment viewer
│   ├── hoverProvider.ts            # Keyword hover documentation
│   └── symbolProvider.ts           # Outline / DocumentSymbol provider
├── media/
│   ├── ruleView.css / .js          # Carbon UI for rules
│   ├── bomView.css / .js           # Carbon UI for BOM vocabulary
│   ├── ruleflowView.css / .js      # Carbon UI for ruleflows
│   └── deploymentView.css / .js    # Carbon UI for deployments
├── syntaxes/
│   └── brl.tmLanguage.json         # TextMate grammar
├── snippets/
│   └── brl.json                    # Code snippets
├── language-configuration.json
├── package.json
└── tsconfig.json
```

---

## License

Apache-2.0
