# Project Documentation Rules (Non-Obvious Only)

- **Source of truth**: answer ODM questions from the `odm-rule-designer` skill (`skills/odm-rule-designer/SKILL.md` and `references/`). If anything here seems to disagree with it, the skill wins.
- **Vocabulary vs BRL Sourcing**: BAL syntax is dictated by phrase patterns in `bom/<name>_<LOCALE>.voc` (properties file) and `bom/<name>.bom` (text BRL), not Java method names directly.
- **Rule Documentation Tracking**: Rule documentation is parsed from `<documentation>` tags inside `.brl` XML descriptors; `tools/odm-report-generator.py` tracks this for quality scoring.
- **Mode Definition Reference**: The canonical reference for ODM rule design patterns is embedded inside [`files/custom_modes.yaml`](../../files/custom_modes.yaml).

