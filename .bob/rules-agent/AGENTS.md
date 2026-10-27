# Coding Rules (Non-Obvious Only)

- **Use the skill**: for any ODM change, follow the `odm-rule-designer` skill (`skills/odm-rule-designer/SKILL.md`). It holds the XOM, BOM, vocabulary and BAL rules, and its `odm` CLI generates the XML files. If anything here seems to disagree with it, the skill wins.
- **Done means built**: an ODM change is finished only when `odm check` has no errors and `odm build` prints `BUILD SUCCESS`.
