# XOM-BOM Guidelines

## Critical XOM-BOM Relationship

1. **XOM (eXecution Object Model)** is Java source that MUST be compiled before BOM changes
2. **BOM (Business Object Model)** references XOM via property origin "xom:/[ProjectName]/[xom-project-name]"
3. Changes to XOM Java classes require BOM regeneration - BOM doesn't auto-sync
4. XOM path in .ruleproject uses `platform:/[xom-project-name]` (Eclipse workspace reference, not filesystem)
5. XOM-BOM synchronization is MANUAL - compile XOM first, then regenerate BOM from Rule Designer

## XOM Project Creation

### Eclipse Project Structure
1. Always create Eclipse-compatible XOM projects with `.project` and `.classpath` files
2. `.project` file must define `org.eclipse.jdt.core.javanature` and `org.eclipse.jdt.core.javabuilder`
3. `.classpath` file must specify source folder (src), JRE container, and output folder (classes)

### Jackson JAR Files (CRITICAL)
Jackson libraries are REQUIRED for JSON serialization/deserialization in ODM Decision Server.

Download Jackson 2.15.2 (or later) JAR files to `xom-project/lib/` directory:

```bash
mkdir -p [xom-project]/lib
cd [xom-project]/lib

# Download Jackson Core
curl -L -o jackson-core-2.15.2.jar \
  https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar

# Download Jackson Databind
curl -L -o jackson-databind-2.15.2.jar \
  https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar

# Download Jackson Annotations
curl -L -o jackson-annotations-2.15.2.jar \
  https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar
```

Verify downloads: `ls -lh [xom-project]/lib/*.jar` (each should be ~300-500KB)

Add to `.classpath`: `<classpathentry kind="lib" path="lib/jackson-core-2.15.2.jar"/>` (repeat for each JAR)

### Compile XOM Classes

```bash
cd [xom-project]
javac -cp "lib/*" -d bin src/[package]/*.java
```

### JSON Serialization Best Practices

- XOM classes should NOT implement Serializable - use standard JavaBean conventions
- ODM Decision Server uses Jackson for REST API JSON serialization/deserialization
- Avoid java.io.Serializable - it's Java-specific and has security vulnerabilities

**Add @JsonIgnore annotations to computed properties (methods without setters):**
- Computed properties are serialized by Jackson but cannot be deserialized (no setter)
- This causes UnrecognizedPropertyException during round-trip JSON processing
- Example: `@JsonIgnore public boolean isEligible() { return ...; }`
- Apply to all methods like `isX()`, `hasX()`, `requiresX()` that have no corresponding setter

**Add @JsonInclude(JsonInclude.Include.NON_NULL) at class level:**
- Excludes null-valued properties from JSON output
- Results in cleaner, more compact JSON payloads
- Standard practice for REST APIs
- Example: `@JsonInclude(JsonInclude.Include.NON_NULL) public class LoanApplication { ... }`

### Example XOM Structure

```
xom-project/
├── .project          # Eclipse project descriptor
├── .classpath        # Eclipse classpath configuration with Jackson JARs
├── lib/              # Jackson library dependencies (REQUIRED for JSON)
│   ├── jackson-databind-2.15.2.jar
│   ├── jackson-core-2.15.2.jar
│   └── jackson-annotations-2.15.2.jar
├── src/              # Java source files
│   └── package/
│       └── ClassName.java (POJO with @JsonIgnore and @JsonInclude annotations)
└── bin/              # Compiled output directory
```

## BOM File Format (CRITICAL)

### Must Use Text-Based BRL Format, NOT XML

Start with properties section (no XML declaration):

```
property loadGetterSetterAsProperties "true"
property origin "xom:/[ProjectName]/[xom-project-name]"
property uuid "[unique-uuid]"
package [package.name];
```

### Java-Like Class Syntax with Annotations

- Always set `property loadGetterSetterAsProperties "true"` for automatic property mapping
- Mark constructors with `property "ilog.rules.engine.dataio.forConversion" "true"` for JSON/XML serialization
- Mark computed/status properties with `property "factory.ignore" "true"` to prevent instantiation
- Use `readonly` for properties that shouldn't be modified in rules (even if XOM has setters)
- Collections: `public readonly java.util.Collection name domain 0,* class string;`

### BOM Property Annotations (Non-Standard)

- `property "factory.ignore" "true"` prevents factory instantiation (use for computed/status properties)
- `property "ilog.rules.engine.dataio.forConversion" "true"` marks constructors for serialization
- `readonly` fields in BOM don't prevent XOM setter methods - BOM restriction only

## Auto-Mapped Properties (CRITICAL)

When `loadGetterSetterAsProperties="true"` is set, ODM automatically maps XOM getter/setter methods to BOM properties.

**DO NOT explicitly declare properties in BOM that have standard getters/setters in XOM - let them auto-map.**

### Boolean Property Naming Mismatch (COMMON ERROR)

Boolean property mapping rules:
- XOM: `isPregnant()` getter + `setPregnant()` setter → BOM: `public boolean pregnant;` (NOT isPregnant!)
- XOM: `hasAllergies()` getter + `setHasAllergies()` setter → BOM: `public boolean hasAllergies;`
- Rule: BOM property name matches the setter method name (without "set" prefix)
- For "is" prefix: remove "is" and lowercase first letter (isPregnant → pregnant)
- For "has" prefix: keep as-is (hasAllergies → hasAllergies)

**ERROR EXAMPLE:** If XOM has `isPregnant()`/`setPregnant()`, using `public boolean isPregnant;` in BOM will cause:
`[B2X] GBREX0021E: Cannot find attribute 'isPregnant' in execution class`

### Computed Properties (No Setter)

XOM: `public boolean isBloodTypeCompatible() { return ...; }` (no setter)

BOM: DO NOT declare - let auto-mapping handle it

Vocabulary: Add navigation phrase for the auto-mapped property name
- Example: `com.hospital.transfusion.TransfusionRequest.bloodTypeCompatible#phrase.navigation = {blood type compatible} of {this}`

### When to Explicitly Declare Properties in BOM

Only explicitly declare properties in BOM when:
- You need special annotations (factory.ignore, readonly, etc.)
- The XOM doesn't follow standard getter/setter patterns
- You want to hide certain XOM properties from rules

## Correct BOM Mapping Examples

### Example 1: Boolean with "is" prefix

**XOM:**
```java
public boolean isPregnant() { return isPregnant; }
public void setPregnant(boolean isPregnant) { this.isPregnant = isPregnant; }
```

**BOM (CORRECT):**
```
public boolean pregnant;
```

**BOM (WRONG - will cause error):**
```
public boolean isPregnant;
```

### Example 2: Computed property (no setter)

**XOM:**
```java
public boolean isBloodTypeCompatible() { return ...; }  // No setter!
```

**BOM (CORRECT):**
```
// DO NOT declare - let auto-mapping handle it
```

**BOM (WRONG - will cause error):**
```
public readonly boolean isBloodTypeCompatible property "factory.ignore" "true";
```

**Vocabulary (CORRECT):**
```
com.example.TransfusionRequest.bloodTypeCompatible#phrase.navigation = {blood type compatible} of {this}
```

### Example 3: Standard property

**XOM:**
```java
public String getName() { return name; }
public void setName(String name) { this.name = name; }
```

**BOM (CORRECT):**
```
public string name;
```

## File Naming Convention (CRITICAL)

All three files (.bom, .b2xa, .voc) MUST share the same base filename:

1. The base filename should be a descriptive project name (e.g., "blood-transfusion", "aviation-compliance")
2. File naming pattern:
   - BOM file: `{base-name}.bom` (e.g., blood-transfusion.bom)
   - B2XA file: `{base-name}.b2xa` (e.g., blood-transfusion.b2xa)
   - Vocabulary file: `{base-name}_{LOCALE}.voc` (e.g., blood-transfusion_en_US.voc)
3. ONLY the vocabulary file has the locale suffix before the extension

### Correct Naming Examples

- blood-transfusion.bom, blood-transfusion.b2xa, blood-transfusion_en_US.voc
- aviation-compliance.bom, aviation-compliance.b2xa, aviation-compliance_en_US.voc
- fraud-detection.bom, fraud-detection.b2xa, fraud-detection_fr_FR.voc

### INCORRECT Naming (DO NOT USE)

- transfusion.bom, com.hospital.transfusion.b2xa, blood-transfusion_en_US.voc (mismatched base names)
- blood-transfusion.bom, blood-transfusion_en_US.b2xa, blood-transfusion_en_US.voc (locale in wrong files)

## BOM Reserved Keywords

**"operator" is a RESERVED KEYWORD in BOM syntax** - causes compilation errors

- WRONG XOM property name: `private Operator operator;`
- CORRECT XOM property name: `private Operator airlineOperator;`
- When you encounter "operator" as a property name, ALWAYS rename it in XOM
- Update all references in BOM, vocabulary, and rules after renaming
- Other known reserved keywords to avoid: "function", "rule", "package", "import"