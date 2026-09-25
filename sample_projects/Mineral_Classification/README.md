Generated with the prompt : v1.0.1

Create an ODM rule project based on https://github.com/DecisionsDev/policy-corpus/blob/main/nature/mineral-classification.txt

# ODM Rule Project Documentation

**Project:** Mineral Classification

**Generated:** 2026-03-06 09:54:31

---

## 1. Project Overview

- **Project Name:** Mineral Classification
- **Project UUID:** 1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d
- **Project Type:** Decision Service


## 2. Quality Assessment Summary

### Quality Metrics

- **Rule Documentation:** 0.0% (0/14) - Score: 0.0/20
- **Rule Complexity:** 50.0% simple rules (7/14) - Score: 10.0/20
- **Project Organization:** 2 packages - Score: 20/20
- **BOM Coverage:** 1 classes defined - Score: 5/20
- **Vocabulary Coverage:** Score: 0/20

### Overall Quality Score

**35.0%** (35.0/100) - Grade: **F**

**Quality Interpretation:**

❌ **Critical** - Project requires major refactoring and documentation.

### Issues Found (7)

#### 🟡 Medium Priority Issues (7)

- **Rule12-Phyllosilicate**: Complex rule with 6 conditions and 2 actions (Type: complexity)
- **Rule13-Inosilicate**: Complex rule with 7 conditions and 2 actions (Type: complexity)
- **Rule14-Tectosilicate**: Complex rule with 7 conditions and 2 actions (Type: complexity)
- **Rule4-Halide**: Complex rule with 7 conditions and 2 actions (Type: complexity)
- **Rule5-Borate**: Complex rule with 7 conditions and 2 actions (Type: complexity)
- **Rule7-Sulfate**: Complex rule with 8 conditions and 2 actions (Type: complexity)
- **Rule8-Phosphate**: Complex rule with 6 conditions and 2 actions (Type: complexity)

### Recommendations

- 📝 **Add Documentation**: Less than 50% of rules have documentation. Add meaningful descriptions to help users understand rule purpose and business logic.
- 🔧 **Simplify Complex Rules**: 7 rule(s) have high complexity. Consider breaking them into smaller, more maintainable rules.
- 🏗️ **Expand BOM**: Consider adding more business object classes to better represent your domain model.
- 📊 **Consider Decision Tables**: For rules with similar structure, consider using decision tables for better readability and maintenance.
- ✅ **Regular Reviews**: Conduct periodic code reviews to maintain quality standards.
- 🧪 **Add Test Cases**: Ensure comprehensive test coverage for all rules and decision tables.
- 📖 **Update Vocabulary**: Keep business vocabulary aligned with domain expert terminology.

---

## 3. Deployment Configuration

### Operation: `MineralClassificationOperation`

- **Ruleset Name:** `Mineral_Classification_Ruleset`
- **Using Ruleflow:** true
- **Ruleflow Name:** `mineral-classification-ruleflow`

**Input/Output Parameters:**

| Variable | Type | Direction |
|----------|------|-----------|
| `specimen` | `MineralClassificationParameters` | IN_OUT |


## 4. Ruleflow

### mineral-classification-ruleflow

**Execution Flow:**

| Step | Task | Execution Mode | Rule Package |
|------|------|----------------|--------------|
| 1 | General Classification | `Fastpath` | `general-classification` |
| 2 | Silicate Subclassification | `Fastpath` | `silicate-subclassification` |

**Execution Modes:**

- **Fastpath**: Sequential rule execution where order matters. Rules are evaluated in the order they appear.
- **RetePlus**: Rete algorithm-based execution with pattern matching. Order-independent evaluation.


## 5. Business Rules

### Package: `general-classification`

#### Rule: `Rule1-NativeElement`

```
if
    'the specimen' consists of a single element
then
    set the mineral class of 'the specimen' to "Native Element" ;
    add "Rule 1: Classified as Native Element - consists of a single chemical element" to the messages of 'the specimen' ;
```

**Conditions:**

- 'the specimen' consists of a single element

**Actions:**

- set the mineral class of 'the specimen' to "Native Element"
- add "Rule 1: Classified as Native Element - consists of a single chemical element" to the messages of 'the specimen'

---

#### Rule: `Rule10-OrganicMineral`

```
if
    'the specimen' contains organic molecules
then
    set the mineral class of 'the specimen' to "Organic Mineral" ;
    add "Rule 10: Classified as Organic Mineral - contains organic molecules and originates from biological or biochemical processes" to the messages of 'the specimen' ;
```

**Conditions:**

- 'the specimen' contains organic molecules

**Actions:**

- set the mineral class of 'the specimen' to "Organic Mineral"
- add "Rule 10: Classified as Organic Mineral - contains organic molecules and originates from biological or biochemical processes" to the messages of 'the specimen'

---

#### Rule: `Rule2-Sulfide`

```
if
    the luster of 'the specimen' is "metallic"
    and 'the specimen' produces hydrogen sulfide odor
then
    set the mineral class of 'the specimen' to "Sulfide" ;
    add "Rule 2: Classified as Sulfide - exhibits metallic luster and produces hydrogen sulfide odor with acid" to the messages of 'the specimen' ;
```

**Conditions:**

- the luster of 'the specimen' is "metallic"
- 'the specimen' produces hydrogen sulfide odor

**Actions:**

- set the mineral class of 'the specimen' to "Sulfide"
- add "Rule 2: Classified as Sulfide - exhibits metallic luster and produces hydrogen sulfide odor with acid" to the messages of 'the specimen'

---

#### Rule: `Rule3-Carbonate`

```
if
    'the specimen' reacts with acid
    and 'the specimen' produces effervescence
then
    set the mineral class of 'the specimen' to "Carbonate" ;
    add "Rule 3: Classified as Carbonate - reacts with dilute hydrochloric acid producing effervescence" to the messages of 'the specimen' ;
```

**Conditions:**

- 'the specimen' reacts with acid
- 'the specimen' produces effervescence

**Actions:**

- set the mineral class of 'the specimen' to "Carbonate"
- add "Rule 3: Classified as Carbonate - reacts with dilute hydrochloric acid producing effervescence" to the messages of 'the specimen'

---

#### Rule: `Rule4-Halide`

```
if
    the taste of 'the specimen' is one of { "salty", "bitter" }
    and 'the specimen' dissolves in water
    and the environment of 'the specimen' is "evaporite"
then
    set the mineral class of 'the specimen' to "Halide" ;
    add "Rule 4: Classified as Halide - has salty or bitter taste, dissolves in water, and forms in evaporite environments" to the messages of 'the specimen' ;
```

**Conditions:**

- the taste of 'the specimen' is one of { "salty", "bitter" }
- 'the specimen' dissolves in water
- the environment of 'the specimen' is "evaporite"

**Actions:**

- set the mineral class of 'the specimen' to "Halide"
- add "Rule 4: Classified as Halide - has salty or bitter taste, dissolves in water, and forms in evaporite environments" to the messages of 'the specimen'

---

#### Rule: `Rule5-Borate`

```
if
    the Mohs hardness of 'the specimen' is less than 3.0
    and the color of 'the specimen' is one of { "white", "pale" }
    and the environment of 'the specimen' is "arid"
then
    set the mineral class of 'the specimen' to "Borate" ;
    add "Rule 5: Classified as Borate - soft (Mohs < 3), white to pale-colored, and occurs in arid environments" to the messages of 'the specimen' ;
```

**Conditions:**

- the Mohs hardness of 'the specimen' is less than 3.0
- the color of 'the specimen' is one of { "white", "pale" }
- the environment of 'the specimen' is "arid"

**Actions:**

- set the mineral class of 'the specimen' to "Borate"
- add "Rule 5: Classified as Borate - soft (Mohs < 3), white to pale-colored, and occurs in arid environments" to the messages of 'the specimen'

---

#### Rule: `Rule6-Oxide`

```
if
    it is not true that 'the specimen' reacts with acid
    and the specific gravity of 'the specimen' is more than 4.0
    and 'the specimen' has metal bonded with oxygen
then
    set the mineral class of 'the specimen' to "Oxide" ;
    add "Rule 6: Classified as Oxide - does not react with acid, has high specific gravity, and consists of metal bonded with oxygen" to the messages of 'the specimen' ;
```

**Conditions:**

- it is not true that 'the specimen' reacts with acid
- the specic gravity of 'the specimen' is more than 4.0
- 'the specimen' has metal bonded with oxygen

**Actions:**

- set the mineral class of 'the specimen' to "Oxide"
- add "Rule 6: Classified as Oxide - does not react with acid, has high specific gravity, and consists of metal bonded with oxygen" to the messages of 'the specimen'

---

#### Rule: `Rule7-Sulfate`

```
if
    'the specimen' contains sulfur
    and 'the specimen' contains oxygen
    and the environment of 'the specimen' is one of { "evaporite", "sedimentary" }
then
    set the mineral class of 'the specimen' to "Sulfate" ;
    add "Rule 7: Classified as Sulfate - contains sulfur and oxygen (SO4 group) and forms in evaporitic or sedimentary environments" to the messages of 'the specimen' ;
```

**Conditions:**

- 'the specimen' contains sulfur
- 'the specimen' contains oxygen
- the environment of 'the specimen' is one of { "evaporite", "sedimentary" }

**Actions:**

- set the mineral class of 'the specimen' to "Sulfate"
- add "Rule 7: Classified as Sulfate - contains sulfur and oxygen (SO4 group) and forms in evaporitic or sedimentary environments" to the messages of 'the specimen'

---

#### Rule: `Rule8-Phosphate`

```
if
    'the specimen' contains phosphorus
    and the crystal habit of 'the specimen' is one of { "hexagonal", "prismatic" }
    and it is not true that 'the specimen' reacts with acid
then
    set the mineral class of 'the specimen' to "Phosphate" ;
    add "Rule 8: Classified as Phosphate - contains phosphorus (PO4 group), exhibits hexagonal or prismatic crystal habit, and does not react with acid" to the messages of 'the specimen' ;
```

**Conditions:**

- 'the specimen' contains phosphorus
- the crystal habit of 'the specimen' is one of { "hexagonal", "prismatic" }
- it is not true that 'the specimen' reacts with acid

**Actions:**

- set the mineral class of 'the specimen' to "Phosphate"
- add "Rule 8: Classified as Phosphate - contains phosphorus (PO4 group), exhibits hexagonal or prismatic crystal habit, and does not react with acid" to the messages of 'the specimen'

---

#### Rule: `Rule9-Silicate`

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

**Conditions:**

- the Mohs hardness of 'the specimen' is more than 5.0
- the luster of 'the specimen' is "vitreous"
- it is not true that 'the specimen' reacts with acid
- 'the specimen' contains silicon oxygen tetrahedra

**Actions:**

- set the mineral class of 'the specimen' to "Silicate"
- add "Rule 9: Classified as Silicate - hard (Mohs > 5), displays vitreous luster, shows no acid reactivity, and is composed of silicon-oxygen tetrahedra" to the messages of 'the specimen'

---

### Package: `silicate-subclassification`

#### Rule: `Rule11-Nesosilicate`

```
if
    the mineral class of 'the specimen' is "Silicate"
    and 'the specimen' has isolated tetrahedra
then
    set the silicate subclass of 'the specimen' to "Nesosilicate" ;
    add "Rule 11: Classified as Nesosilicate - contains isolated SiO4 groups without shared oxygen atoms between tetrahedra" to the messages of 'the specimen' ;
```

**Conditions:**

- the mineral class of 'the specimen' is "Silicate"
- 'the specimen' has isolated tetrahedra

**Actions:**

- set the silicate subclass of 'the specimen' to "Nesosilicate"
- add "Rule 11: Classified as Nesosilicate - contains isolated SiO4 groups without shared oxygen atoms between tetrahedra" to the messages of 'the specimen'

---

#### Rule: `Rule12-Phyllosilicate`

```
if
    the mineral class of 'the specimen' is "Silicate"
    and 'the specimen' has sheet structure
    and the cleavage of 'the specimen' is "basal"
    and the crystal habit of 'the specimen' is one of { "platy", "flaky" }
then
    set the silicate subclass of 'the specimen' to "Phyllosilicate" ;
    add "Rule 12: Classified as Phyllosilicate - forms sheets of tetrahedra with basal cleavage and platy or flaky habit" to the messages of 'the specimen' ;
```

**Conditions:**

- the mineral class of 'the specimen' is "Silicate"
- 'the specimen' has sheet structure
- the cleavage of 'the specimen' is "basal"
- the crystal habit of 'the specimen' is one of { "platy", "flaky" }

**Actions:**

- set the silicate subclass of 'the specimen' to "Phyllosilicate"
- add "Rule 12: Classified as Phyllosilicate - forms sheets of tetrahedra with basal cleavage and platy or flaky habit" to the messages of 'the specimen'

---

#### Rule: `Rule13-Inosilicate`

```
if
    the mineral class of 'the specimen' is "Silicate"
    and 'the specimen' has chain structure
    and the crystal habit of 'the specimen' is one of { "prismatic", "fibrous" }
    and the cleavage of 'the specimen' is "distinct"
then
    set the silicate subclass of 'the specimen' to "Inosilicate" ;
    add "Rule 13: Classified as Inosilicate - forms single or double chains of tetrahedra with prismatic or fibrous habit and distinct cleavage" to the messages of 'the specimen' ;
```

**Conditions:**

- the mineral class of 'the specimen' is "Silicate"
- 'the specimen' has chain structure
- the crystal habit of 'the specimen' is one of { "prismatic", "fibrous" }
- the cleavage of 'the specimen' is "distinct"

**Actions:**

- set the silicate subclass of 'the specimen' to "Inosilicate"
- add "Rule 13: Classified as Inosilicate - forms single or double chains of tetrahedra with prismatic or fibrous habit and distinct cleavage" to the messages of 'the specimen'

---

#### Rule: `Rule14-Tectosilicate`

```
if
    the mineral class of 'the specimen' is "Silicate"
    and 'the specimen' has framework structure
    and the cleavage of 'the specimen' is "none"
    and the fracture of 'the specimen' is "conchoidal"
then
    set the silicate subclass of 'the specimen' to "Tectosilicate" ;
    add "Rule 14: Classified as Tectosilicate - forms three-dimensional network of SiO4 tetrahedra with no cleavage and conchoidal fracture" to the messages of 'the specimen' ;
```

**Conditions:**

- the mineral class of 'the specimen' is "Silicate"
- 'the specimen' has framework structure
- the cleavage of 'the specimen' is "none"
- the fracture of 'the specimen' is "conchoidal"

**Actions:**

- set the silicate subclass of 'the specimen' to "Tectosilicate"
- add "Rule 14: Classified as Tectosilicate - forms three-dimensional network of SiO4 tetrahedra with no cleavage and conchoidal fracture" to the messages of 'the specimen'

---


## 6. Decision Tables

*No decision tables found in this project.*


## 7. Rule Variables

### MineralClassificationParameters

| Variable Name | Type | Verbalization |
|---------------|------|---------------|
| `specimen` | `com.mineral.classification.MineralSpecimen` | the specimen |


## 8. Business Object Model (BOM)

### mineral-classification

**Package:** `com.mineral.classification`

#### Class: `MineralSpecimen`

**Properties:**

- `public string specimenId`
- `public string specimenName`
- `public boolean singleElement`
- `public boolean containsSulfur`
- `public boolean containsOxygen`
- `public boolean containsPhosphorus`
- `public boolean containsSiliconOxygenTetrahedra`
- `public boolean containsOrganicMolecules`
- `public boolean metalBondedWithOxygen`
- `public string luster`
- `public double mohsHardness`
- `public double specificGravity`
- `public string taste`
- `public boolean dissolvesInWater`
- `public string color`
- `public string crystalHabit`
- `public string cleavage`
- `public string fracture`
- `public boolean reactsWithAcid`
- `public boolean producesEffervescence`
- `public boolean producesHydrogenSulfideOdor`
- `public string environment`
- `public string silicateStructure`
- `public boolean isolatedTetrahedra`
- `public boolean sheetStructure`
- `public boolean chainStructure`
- `public boolean frameworkStructure`
- `public string mineralClass`
- `public string silicateSubclass`

**Methods:**

- `void addMessage(string arg)`


## 9. Business Vocabulary

### mineral-classification_en_US

#### MineralSpecimen - *mineral specimen*

| Property | Type | Phrase |
|----------|------|--------|
| `specimenId` | Action | set the specimen ID of {this} to {specimen ID} |
| `specimenId` | Navigation | {specimen ID} of {this} |
| `specimenName` | Action | set the specimen name of {this} to {specimen name} |
| `specimenName` | Navigation | {specimen name} of {this} |
| `singleElement` | Action | make it {single element} that {this} consists of a single element |
| `singleElement` | Navigation | {this} consists of a single element |
| `containsSulfur` | Action | make it {contains sulfur} that {this} contains sulfur |
| `containsSulfur` | Navigation | {this} contains sulfur |
| `containsOxygen` | Action | make it {contains oxygen} that {this} contains oxygen |
| `containsOxygen` | Navigation | {this} contains oxygen |
| `containsPhosphorus` | Action | make it {contains phosphorus} that {this} contains phosphorus |
| `containsPhosphorus` | Navigation | {this} contains phosphorus |
| `containsSiliconOxygenTetrahedra` | Action | make it {contains silicon oxygen tetrahedra} that {this} contains silicon oxygen tetrahedra |
| `containsSiliconOxygenTetrahedra` | Navigation | {this} contains silicon oxygen tetrahedra |
| `containsOrganicMolecules` | Action | make it {contains organic molecules} that {this} contains organic molecules |
| `containsOrganicMolecules` | Navigation | {this} contains organic molecules |
| `metalBondedWithOxygen` | Action | make it {metal bonded with oxygen} that {this} has metal bonded with oxygen |
| `metalBondedWithOxygen` | Navigation | {this} has metal bonded with oxygen |
| `luster` | Action | set the luster of {this} to {luster} |
| `luster` | Navigation | {luster} of {this} |
| `mohsHardness` | Action | set the Mohs hardness of {this} to {Mohs hardness} |
| `mohsHardness` | Navigation | {Mohs hardness} of {this} |
| `specificGravity` | Action | set the specific gravity of {this} to {specific gravity} |
| `specificGravity` | Navigation | {specific gravity} of {this} |
| `taste` | Action | set the taste of {this} to {taste} |
| `taste` | Navigation | {taste} of {this} |
| `dissolvesInWater` | Action | make it {dissolves in water} that {this} dissolves in water |
| `dissolvesInWater` | Navigation | {this} dissolves in water |
| `color` | Action | set the color of {this} to {color} |
| `color` | Navigation | {color} of {this} |
| `crystalHabit` | Action | set the crystal habit of {this} to {crystal habit} |
| `crystalHabit` | Navigation | {crystal habit} of {this} |
| `cleavage` | Action | set the cleavage of {this} to {cleavage} |
| `cleavage` | Navigation | {cleavage} of {this} |
| `fracture` | Action | set the fracture of {this} to {fracture} |
| `fracture` | Navigation | {fracture} of {this} |
| `reactsWithAcid` | Action | make it {reacts with acid} that {this} reacts with acid |
| `reactsWithAcid` | Navigation | {this} reacts with acid |
| `producesEffervescence` | Action | make it {produces effervescence} that {this} produces effervescence |
| `producesEffervescence` | Navigation | {this} produces effervescence |
| `producesHydrogenSulfideOdor` | Action | make it {produces hydrogen sulfide odor} that {this} produces hydrogen sulfide odor |
| `producesHydrogenSulfideOdor` | Navigation | {this} produces hydrogen sulfide odor |
| `environment` | Action | set the environment of {this} to {environment} |
| `environment` | Navigation | {environment} of {this} |
| `silicateStructure` | Action | set the silicate structure of {this} to {silicate structure} |
| `silicateStructure` | Navigation | {silicate structure} of {this} |
| `isolatedTetrahedra` | Action | make it {isolated tetrahedra} that {this} has isolated tetrahedra |
| `isolatedTetrahedra` | Navigation | {this} has isolated tetrahedra |
| `sheetStructure` | Action | make it {sheet structure} that {this} has sheet structure |
| `sheetStructure` | Navigation | {this} has sheet structure |
| `chainStructure` | Action | make it {chain structure} that {this} has chain structure |
| `chainStructure` | Navigation | {this} has chain structure |
| `frameworkStructure` | Action | make it {framework structure} that {this} has framework structure |
| `frameworkStructure` | Navigation | {this} has framework structure |
| `mineralClass` | Action | set the mineral class of {this} to {mineral class} |
| `mineralClass` | Navigation | {mineral class} of {this} |
| `silicateSubclass` | Action | set the silicate subclass of {this} to {silicate subclass} |
| `silicateSubclass` | Navigation | {silicate subclass} of {this} |
| `messages` | Navigation | {messages} of {this} |
| `softMineral` | Navigation | {this} is a soft mineral |
| `hardMineral` | Navigation | {this} is a hard mineral |
| `highSpecificGravity` | Navigation | {this} has high specific gravity |
| `saltyOrBitterTaste` | Navigation | {this} has salty or bitter taste |
| `whiteOrPaleColored` | Navigation | {this} is white or pale colored |
| `inEvaporiteEnvironment` | Navigation | {this} is in evaporite environment |
| `inAridEnvironment` | Navigation | {this} is in arid environment |
| `inSedimentaryEnvironment` | Navigation | {this} is in sedimentary environment |
| `hexagonalOrPrismaticHabit` | Navigation | {this} has hexagonal or prismatic habit |
| `plateOrFlakyHabit` | Navigation | {this} has plate or flaky habit |
| `prismaticOrFibrousHabit` | Navigation | {this} has prismatic or fibrous habit |
| `basalCleavage` | Navigation | {this} has basal cleavage |
| `distinctCleavage` | Navigation | {this} has distinct cleavage |
| `noCleavage` | Navigation | {this} has no cleavage |
| `conchoidalFracture` | Navigation | {this} has conchoidal fracture |
| `metallicLuster` | Navigation | {this} has metallic luster |
| `vitreousLuster` | Navigation | {this} has vitreous luster |
| `originatesFromBiologicalProcesses` | Navigation | {this} is biological |


---

*Report generated by ODM Report Generator*
