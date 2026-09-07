# Project Documentation Rules (Non-Obvious Only)

- **Vocabulary vs BRL Sourcing**: BAL syntax is strictly dictated by phrase patterns in `bom/<name>_<LOCALE>.voc` (properties file) and `bom/<name>.bom` (text BRL), not Java method names directly.
- **Rule Package Folder Names**: Package directory names in `rules/<pkg>/` must match `<Package Name="<pkg>"/>` elements inside `.rfl` ruleflow files exactly.
- **Rule Documentation Tracking**: Rule documentation is parsed from `<documentation>` tags inside `.brl` XML descriptors; `tools/odm-report-generator.py` tracks this for quality scoring.
- **Mode Definition Reference**: The canonical reference for ODM rule design patterns is embedded inside [`files/custom_modes.yaml`](files/custom_modes.yaml:1).
