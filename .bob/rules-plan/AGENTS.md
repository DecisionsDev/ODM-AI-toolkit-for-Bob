# Architecture & Constraint Rules (Non-Obvious Only)

- **Single source of truth**: ODM conventions, build gotchas and design best practices live in the `odm-rule-designer` skill (`skills/odm-rule-designer/SKILL.md` and its `references/`). Read it before planning ODM work; if anything here seems to disagree with it, the skill wins.
- **Ruleflow design**: plan rule tasks with the Fastpath algorithm only, never RetePlus. When rules chain inside a package, plan sequential packages instead (`skills/odm-rule-designer/references/ruleflow-design.md`).
- **Generated files**: plan for `odm init` / `odm rule` / `odm ruleflow` to generate `.ruleproject`, `.dop`, `.dep`, `.var`, `.rfl` and their UUID links; don't plan to hand-write them.
