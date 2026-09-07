# Architecture & Constraint Rules (Non-Obvious Only)

- **XOM-BOM Synchronization**: XOM classes must be compiled with `javac` before creating or referencing BOM entries; `.ruleproject` references XOM projects using Eclipse platform URLs (`platform:/<xom-project>`), not direct file paths.
- **Ruleflow Execution Modes**: `Fastpath` tasks execute rules sequentially where order matters (ideal for validation/init); `RetePlus` tasks use pattern matching for order-independent evaluation. Mixing modes across tasks in one `.rfl` is standard.
- **UUID Strict Linkage**: Every `.dop` deployment operation must explicitly link to the exact UUID defined in `.ruleproject` (`<targetRuleProject href="../../<ProjectName>#<uuid>"/>`). Mismatches cause runtime loading failures in Rule Designer.
- **Build Mode Setting**: In `.ruleproject`, `buildMode` must be set to `"DecisionEngine"` (never `"DecisionService"`), with `isADecisionService="true"` and `migrationFlag="3"`.
- **Properties-based BOM & Voc**: Never format `.bom` or `.voc` files as XML; they use proprietary BRL text and Java properties syntax respectively.
