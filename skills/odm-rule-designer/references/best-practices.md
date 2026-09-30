# Rule project design best practices

These practices keep a rule project readable for business users. None of them breaks a build. `odm check` reports the ones it can detect as `WARN … best practice: …` (column **Lint** below). Apply them when you design a new project, and when you add rules to an existing project. If a practice conflicts with an explicit user requirement, follow the requirement and say which practice it breaks.

## Project and package layout

| # | Practice | Lint |
|---|---|---|
| 1 | Never put a rule artifact (`.brl`, `.dta`) directly in `rules/`. Put it in a rule package. `odm rule` always takes a package. | root artifact |
| 2 | Don't leave rule packages empty. Delete a package that has no rules left, and remove it from the ruleflow. | empty package |
| 3 | Put rule artifacts only in **leaf** packages (packages with no sub-packages). | non-leaf package |
| 4 | Rules in the same package must be cohesive: they work on the same subset of BOM objects and attributes (for example, all `applicant` checks). `odm deps --verbose` shows each rule's reads and writes, so use it to spot a rule that doesn't belong. | — |
| 5 | Pick a naming convention for rules, tables, packages and the ruleflow, and use it everywhere. Default: kebab-case `verb-object[-qualifier]` for rules (`check-age`, `score-high-risk-country`), kebab-case nouns for packages (`validation`, `risk-scoring`), `<base>-ruleflow` for the flow. | — |
| 6 | Don't reuse a rule name in different packages. | duplicate name |

## Rules (BAL)

| # | Practice | Lint |
|---|---|---|
| 7 | **Atomic action rules:** a conjunction of conditions and **one** action phrase. When several actions always go together (set a status + add a message), add a **virtual BOM method** that does them all, and give it one verbalization (see below). | >1 action |
| 8 | No `else`. Write two rules: one for the positive case and one for the negative case. | `else` |
| 9 | Only conjunctions (`and`) of conditions. For `A or B`, write one rule per alternative. | `or` |
| 10 | No `print`, except temporarily while debugging. Remove it before delivery. | `print` |
| 11 | No rule priorities. Order execution with the ruleflow (separate tasks), or, if the order is intentional, with the rule list of one task. | priority |
| 12 | Use as few ODM functions as possible. Put logic in the XOM and expose it as a BOM method. | — |

### Virtual BOM method (one phrase for several actions)

A virtual method exists in the BOM only, and its body is written in the `.b2xa`. Tested on ODM 9.6 (`BUILD SUCCESS`):

`.bom`, inside the class:
```
    public void reject(string reason);
```
`.b2xa`, inside `<b2x:translation>` after `<lang>`:
```xml
    <class>
        <businessName>com.loan.compliance.LoanRequest</businessName>
        <method>
            <name>reject</name>
            <parameter type="java.lang.String"/>
            <body><![CDATA[
                this.eligible = false;
                this.reason = reason;
                this.addViolation(reason);
            ]]></body>
        </method>
    </class>
```
`.voc`:
```
com.loan.compliance.LoanRequest.reject(java.lang.String)#phrase.action = reject {this} because {0}
```
Rule:
```
if
    the applicant of 'the request' is not null
    and the age of the applicant of 'the request' is less than 18
then
    reject 'the request' because "Applicant must be at least 18 years old" ;
```
- The B2X body must stay small (practice 13). If it grows, move the logic into an XOM method and map the BOM method to it directly, without a B2X body.
- `odm deps` doesn't look inside B2X bodies. Rules that use a virtual method show up as `no vocabulary phrase recognized` for that action, so check their dependencies yourself.
- Make sure the `.voc` ends with a newline before you append a line to it.

## BOM

| # | Practice | Lint |
|---|---|---|
| 13 | Keep a B2X body (`.b2xa`, for an attribute or a method) to a few statements, **5 at most**. Put longer logic in the XOM. | >5 statements |

## Decision tables

| # | Practice | Lint |
|---|---|---|
| 14 | At most **500 rows**, and preferably few enough that users don't have to scroll through several pages. Split a large table by a leading condition (for example, one table per product or region). | >500 rows |
| 15 | No sparse tables. If many cells are empty because a condition column only matters for some rows, split the table into several tables, each with its own set of condition columns. | — |

## Ruleflow and parameters

| # | Practice | Lint |
|---|---|---|
| 16 | A rule task references **rule packages**, not a list of individual rules, unless you are deliberately setting an execution order. | individual rules |
| 17 | Keep a flow simple: few tasks (**10 at most**) and a low cyclomatic complexity (**5 at most**: transitions − nodes + 2). Group the rest into subflows. | size / complexity |
| 18 | Don't make a flow so trivial that users have to drill down through subflows that each hold only one or two tasks. | — |
| 19 | Avoid task properties that users can't see: exit criteria, ordering other than `Default`, firing limits, and initial or final actions. | ExitCriteria, Ordering, actions |
| 20 | Avoid dynamic filters (`<Select>`) on rule tasks. Put the rules in their own package instead. | `<Select>` |
| 21 | Every rule task uses the **Fastpath** algorithm, always. Never RetePlus or Sequential. When rules chain inside a task, split its package into sequential packages (see `ruleflow-design.md`). | non-Fastpath task |
| 22 | Limit ruleset parameters to **one input and one output object** (`--var request:LoanRequest:IN --var response:LoanDecision:OUT`), or a single `IN_OUT` object. | >1 IN / >1 OUT |
| 23 | Keep the number of variables to a minimum. Variables are global to all rules, so they can cause unexpected behavior when a rule matches several objects. | >2 variables |

## Existing projects

On an existing project, `odm check` may report many best-practice warnings. List them to the user grouped by practice. Fix them only when the user asks, because some fixes (splitting rules, adding virtual methods, changing the ruleflow) change the project structure. After each fix, rerun `odm check`, `odm deps` and `odm build`.
