# AI_Context.md — Mineral Classification

Part of the ODM examples in this repository. For format-level explanations (what each file type is, the syntax rules, the error table) read the root [AI_Context.md](../../AI_Context.md); this file describes only this project.

## Purpose

- Rule project folder: `Mineral Classification/` (name `Mineral Classification`); XOM Java project: `mineral-classification-xom/`.
- Source policy used to generate it: <https://github.com/DecisionsDev/policy-corpus/blob/main/nature/mineral-classification.txt>

## Build status

Builds successfully (`BUILD SUCCESS`) with IBM Semeru JDK 21 and `rules-compiler.jar`, XOM compiled from `src/`.

## Decision interface

- Variable `specimen` of type `com.mineral.classification.MineralSpecimen`, verbalized `the specimen` (rules write `'the specimen'`), direction `IN_OUT`.
- Operation `MineralClassificationOperation`: ruleset `Mineral_Classification_Ruleset`, ruleflow `mineral-classification-ruleflow`.
- Deployment config: `deployment.dep`, `dep` name `deployment`, RuleApp `MineralClassification`.

## XOM and BOM

Package `com.mineral.classification`. Business classes and their members (from `bom/mineral-classification.bom`):

- **MineralSpecimen**: specimenId, specimenName, singleElement, containsSulfur, containsOxygen, containsPhosphorus, containsSiliconOxygenTetrahedra, containsOrganicMolecules, metalBondedWithOxygen, luster, mohsHardness, specificGravity, taste, dissolvesInWater, color, crystalHabit, cleavage, fracture, reactsWithAcid, producesEffervescence, producesHydrogenSulfideOdor, environment, silicateStructure, isolatedTetrahedra, sheetStructure, chainStructure, frameworkStructure, mineralClass, silicateSubclass, messages; methods: addMessage

XOM sources: `src/com/mineral/classification/JsonExample.java`, `src/com/mineral/classification/MineralSpecimen.java`.

## Ruleflow (execution order)

| # | Rule package | Mode |
|---|---|---|
| 1 | general-classification | Fastpath |
| 2 | silicate-subclassification | Fastpath |

Fastpath = sequential execution, no inference. RetePlus = pattern-matching inference where one rule's effect may trigger others.

## Rules by package

### `rules/general-classification/` — Rules for general mineral classification (Section 1 of policy)
- `Rule1-NativeElement.brl` (rule): Rule1-NativeElement
- `Rule10-OrganicMineral.brl` (rule): Rule10-OrganicMineral
- `Rule2-Sulfide.brl` (rule): Rule2-Sulfide
- `Rule3-Carbonate.brl` (rule): Rule3-Carbonate
- `Rule4-Halide.brl` (rule): Rule4-Halide
- `Rule5-Borate.brl` (rule): Rule5-Borate
- `Rule6-Oxide.brl` (rule): Rule6-Oxide
- `Rule7-Sulfate.brl` (rule): Rule7-Sulfate
- `Rule8-Phosphate.brl` (rule): Rule8-Phosphate
- `Rule9-Silicate.brl` (rule): Rule9-Silicate

### `rules/silicate-subclassification/` — Rules for silicate subclassification (Section 2 of policy)
- `Rule11-Nesosilicate.brl` (rule): Rule11-Nesosilicate
- `Rule12-Phyllosilicate.brl` (rule): Rule12-Phyllosilicate
- `Rule13-Inosilicate.brl` (rule): Rule13-Inosilicate
- `Rule14-Tectosilicate.brl` (rule): Rule14-Tectosilicate

## Representative rule

`rules/general-classification/Rule9-Silicate.brl` — the BAL inside `<definition><![CDATA[ … ]]>`:

```
if
    the Mohs hardness of 'the specimen' is more than 5.0
    and the luster of 'the specimen' is "vitreous"
    and it is not true that 'the specimen' reacts with acid
    and 'the specimen' contains silicon oxygen tetrahedra
then
    set the mineral class of 'the specimen' to "Silicate" ;
    add "Rule 9: Classified as Silicate - hard (Mohs > 5), displays vitreous luster, shows no acid reactivity, and is composed of silicon-oxygen tetrahedra" to the messages of 'the specimen' ;
```

## Design notes

- Two-stage decision: `general-classification` assigns a mineral class (rules 1–10: native element, sulfide, carbonate, halide, borate, oxide, sulfate, phosphate, silicate, organic mineral); `silicate-subclassification` then refines silicates (nesosilicate, phyllosilicate, inosilicate, tectosilicate). The ruleflow order encodes that dependency.
- Rule file names follow the source policy numbering (`Rule4-Halide.brl`); the `<name>` inside each `.brl` is what the engine uses.
- The XOM uses Jackson (`@JsonInclude`) and ships a JSON example (`README-JSON.md` in the XOM folder).

## File inventory

```
Mineral Classification/.gitignore
Mineral Classification/.ruleproject
Mineral Classification/bom/mineral-classification.b2xa
Mineral Classification/bom/mineral-classification.bom
Mineral Classification/bom/mineral-classification_en_US.voc
Mineral Classification/deployment/MineralClassificationOperation.dop
Mineral Classification/deployment/deployment.dep
Mineral Classification/rules/MineralClassificationParameters.var
Mineral Classification/rules/mineral-classification-ruleflow.rfl
Mineral Classification/rules/general-classification/.rulepackage
Mineral Classification/rules/general-classification/Rule1-NativeElement.brl
Mineral Classification/rules/general-classification/Rule10-OrganicMineral.brl
Mineral Classification/rules/general-classification/Rule2-Sulfide.brl
Mineral Classification/rules/general-classification/Rule3-Carbonate.brl
Mineral Classification/rules/general-classification/Rule4-Halide.brl
Mineral Classification/rules/general-classification/Rule5-Borate.brl
Mineral Classification/rules/general-classification/Rule6-Oxide.brl
Mineral Classification/rules/general-classification/Rule7-Sulfate.brl
Mineral Classification/rules/general-classification/Rule8-Phosphate.brl
Mineral Classification/rules/general-classification/Rule9-Silicate.brl
Mineral Classification/rules/silicate-subclassification/.rulepackage
Mineral Classification/rules/silicate-subclassification/Rule11-Nesosilicate.brl
Mineral Classification/rules/silicate-subclassification/Rule12-Phyllosilicate.brl
Mineral Classification/rules/silicate-subclassification/Rule13-Inosilicate.brl
Mineral Classification/rules/silicate-subclassification/Rule14-Tectosilicate.brl
```
