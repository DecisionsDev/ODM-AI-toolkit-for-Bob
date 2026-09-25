# Rule dependencies and ruleflow design

Use this when the user asks which rules depend on which, why a rule never fires, or what the ruleflow should be ("quelles sont les dépendances entre les règles ?", "propose un ruleflow adapté").

## 1. Analyse

```bash
java -jar <skill>/scripts/odm.jar deps <RuleProjectDir>            # add --verbose for each rule's reads/writes
```

How it works. Every `.voc` phrase is tied to a BOM member. `deps` finds those phrases in each `.brl` and `.dta`:

- A phrase in `if`/`definitions` is a **condition read**. A phrase in an action argument is an **action read**.
- An action phrase is a **write** (`set …`, `make it … that …`, `add … to …`).
- For a method or computed getter, `deps` reads the XOM source to find the fields behind it. `record score {0} on {this}` → `addScore()` → writes `RiskScore.totalScore`. `offset deficit` → `getOffsetDeficit()` → reads `safCreditsEarned`.

Rule A → rule B means B reads something A writes, so B must run after A.

Limits: `deps` only follows a method into fields of its own class (and the class's own methods). It doesn't track side effects on other objects or inherited fields. A rule reported with `no vocabulary phrase recognized` has unknown dependencies, so read it yourself. Always open the rules behind an ERROR before you change anything.

## 2. Read the report

| Section / line | Meaning | Usual fix |
|---|---|---|
| `package dependencies` | Writer package → reader package. The ruleflow must run the writer first. | Order the tasks that way. |
| `dependencies inside a package` | One rule reads what another rule of the same package writes. | Split the package in two (the `note` gives the layers), or make that task RetePlus. |
| `ERROR … reads X before it is written by …` | The reader runs in an earlier task, so it only sees the input value. The rule usually never fires, or fires on stale data. | Reorder the tasks, or move the reading rule to a later package. |
| `WARN task n (…, Fastpath): rules read what other rules of the same task write` | Same as the inside-a-package case, in a non-RetePlus task. | Split the package, or use RetePlus. |
| `WARN task n: X is set by k rules` | Several rules overwrite X. If more than one fires, rule order decides the result. | Make the conditions mutually exclusive, or guard on a status (`if the decision is "APPROVE"` …). |
| `set by several packages` | A status that moves along the flow (`PENDING` → `APPROVE` → `REVIEW`). The last task to set it wins. | Defaults go in the first task, the final decision in the last. |
| `package … is not in the ruleflow` | Its rules never run. | Add it to the ruleflow. |

A value that the reader's own package also writes is treated as shared state (a guard such as `if X is null, set X`, or a status machine). The current order decides it, so it appears as an overwrite, not as a dependency.

## 3. Design the ruleflow

1. Build a linear flow in dependency order. The usual shape is: validation / defaults → detection → scoring → decision → finalization (messages, summaries).
2. Give each task one mode:
   - **Fastpath** when no rule of the task depends on another rule of the task.
   - **RetePlus** only when rules in the task really chain (one rule's action enables another rule's condition). Prefer splitting into two sequential packages: the order is then explicit and easier to test.
3. Packages that depend on each other in both directions come out as one task (`a+b:RetePlus`). That's a design smell. Move the rules behind the backward edges into a later package so that each dependency runs one way.
4. To move a rule to another package, recreate it with `odm rule <RuleProjectDir> <new-package> "<name>" --file body.bal`, then delete the old `.brl`. `odm rule` creates the `.rulepackage` a new package needs; a plain directory is not enough.

## 4. Apply and verify

```bash
java -jar <skill>/scripts/odm.jar ruleflow <RuleProjectDir> --packages validation:Fastpath,scoring:RetePlus,decision:Fastpath
java -jar <skill>/scripts/odm.jar deps <RuleProjectDir>     # expect: "the current ruleflow already follows the dependencies."
java -jar <skill>/scripts/odm.jar check <RuleProjectDir>
java -jar <skill>/scripts/odm.jar build <Name>.properties
```

`ruleflow` rewrites the `.rfl` used by the `.dop` and keeps its name and UUID, so the `.dop` link stays valid. `a+b` puts two packages in one task.

It refuses in two cases unless you pass `--force`:
- The flow has branches, conditional transitions, or task actions, because the rewrite would lose them. Change those in Rule Designer.
- `--packages` leaves out a package that has rules.

## 5. Report to the user

Answer in the user's language. Give:
- The dependency table (writer → reader, and on which data).
- The problems found, each with the rule and why it matters (for example: "`approve-eligible-loan` tests the interest rate, but `pricing` runs after it, so the loan is never approved").
- The proposed ruleflow, with one line of justification per task.

Apply the change only if the user asked for it, or once they agree.
