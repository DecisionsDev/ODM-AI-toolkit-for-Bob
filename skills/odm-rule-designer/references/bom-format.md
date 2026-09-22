# BOM (Business Object Model) Format

## Use text-based BRL format, NOT XML

Start with a properties section (no XML declaration):

```
property loadGetterSetterAsProperties "true"
property origin "xom:/[ProjectName]/[xom-project-name]"
property uuid "[unique-uuid]"
package [package.name];
```

Then Java-like class syntax with annotations.

## Annotations

- `property loadGetterSetterAsProperties "true"` — auto-maps XOM getter/setter to BOM properties. Set this always.
- `property "ilog.rules.engine.dataio.forConversion" "true"` — mark constructors for JSON/XML serialization.
- `property "factory.ignore" "true"` — prevents factory instantiation; use for computed/status properties.
- `readonly` — BOM-only restriction; XOM setters still exist but rules can't write. Collections example:
  `public readonly java.util.Collection name domain 0,* class string;`

## Auto-mapped properties (the #1 source of errors)

With `loadGetterSetterAsProperties="true"`, ODM auto-maps getters/setters. **Do not redeclare** properties that have standard getters/setters — let them auto-map.

**Boolean naming — the BOM property name matches the SETTER name without `set`, NOT the getter:**

| XOM getter / setter | Correct BOM property | Wrong (causes error) |
|---|---|---|
| `isPregnant()` / `setPregnant()` | `public boolean pregnant;` | `isPregnant` |
| `hasAllergies()` / `setHasAllergies()` | `public boolean hasAllergies;` | — |

- `is` prefix: drop `is`, lowercase first letter (`isPregnant` → `pregnant`).
- `has` prefix: keep as-is (`hasAllergies` → `hasAllergies`).
- Wrong naming produces: `[B2X] GBREX0021E: Cannot find attribute 'isPregnant' in execution class`.

**Computed boolean properties** (getter, no setter) that need vocabulary phrases MUST be declared explicitly:

```
public readonly boolean inKnownTransactionCorridor property "factory.ignore" "true";
```

XOM: `public boolean isInKnownTransactionCorridor() { ... }` (no setter, annotated `@JsonIgnore` — see `xom.md`)
Vocabulary: `...inKnownTransactionCorridor#phrase.navigation = {this} is whitelisted`

Without the BOM declaration, ODM can't resolve the property in rules.

**Only declare a property explicitly when:** you need a special annotation (`factory.ignore`, `readonly`), the XOM breaks standard getter/setter patterns, or you want to hide a property from rules.

## Reserved keywords (CRITICAL — discovered from production builds)

These words cannot be used as XOM/BOM property names — they break compilation:

- **`operator`** is the most common offender. `private Operator operator;` fails; rename to something qualified, e.g. `private Operator airlineOperator;`. Propagate the rename through BOM, vocabulary, and rules.
- Other known reserved words: `function`, `rule`, `package`, `import`.
- When you hit one of these as a natural domain term, add a qualifying prefix (`airline operator`, not `operator`) rather than fighting the keyword.

## Property naming consistency across layers

All four layers — XOM field/getter/setter, BOM property, vocabulary phrase, and rule usage — must share the same base name:

```
XOM field:    private Operator airlineOperator;
XOM getter:   public Operator getAirlineOperator()
XOM setter:   public void setAirlineOperator(Operator airlineOperator)
BOM:          public com.example.Operator airlineOperator;
Vocabulary:   ComplianceRequest.airlineOperator#phrase.navigation = {airline operator} of {this}
Rule usage:   the airline operator of 'the request'
```

## Collections — never check "empty" directly

`the violations of 'the request' is empty` is **not valid** BAL against a raw collection property. Instead expose a computed boolean in XOM:

```java
@JsonIgnore
public boolean hasAnyViolations() { return !violations.isEmpty(); }
```

```
Vocabulary: ComplianceRequest.anyViolations#phrase.navigation = {this} has violations
Rule:       it is not true that 'the request' has violations
```

Collections themselves stay `readonly` in the BOM so rules can only `add`/`remove`, never replace wholesale:
```
public readonly java.util.Collection violations domain 0,* class string;
```

**If `add {0} to the <label> of {this}` fails with a cascading, seemingly-unrelated error** (e.g. `The word 'X' is expected in place of 'Y'`, or a later word in the label reported as `Variable 'Y' is not declared`), and the BOM/vocabulary declarations look syntactically correct, don't assume your own syntax is wrong — some specific collection property names have been observed to fail this way for reasons that didn't reproduce consistently across renames (word order, word choice, and declaration position were all ruled out in one investigation). The fast, reliable fix is to rename the property to something else and retest, rather than debugging the parser further — this is cheap compared to the time spent root-causing it. When in doubt, reuse an already-proven collection name pattern like `messages` or `triggeredRules` rather than inventing a new one for a low-value collection.

## Working BOM structure — recommended declaration order

1. Property block: `loadGetterSetterAsProperties`, `origin`, `uuid`.
2. `package` declaration.
3. Per class: simple properties (leave auto-mapped) → computed properties (`factory.ignore`) → collections (`readonly`) → primary constructor (`forConversion`) → default constructor → methods (`add`/`remove`/compute).

## `.b2xa` file (ARL association)

Create `{base-name}.b2xa` in `bom/`. Base name must match the `.bom` and `.voc` (no locale suffix). Validate against `../schemas/b2x.xsd` (JRules 1.3 Translation schema) if the file is rejected:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<b2x:translation xmlns:b2x="http://schemas.ilog.com/JRules/1.3/Translation" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://schemas.ilog.com/JRules/1.3/Translation ilog/rules/schemas/1_3/b2x.xsd">
    <id>unique-uuid-here</id>
    <lang>ARL</lang>
</b2x:translation>
```
