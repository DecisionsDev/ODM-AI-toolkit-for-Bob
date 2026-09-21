# XOM (eXecution Object Model)

The XOM is plain Java source representing your business domain objects. The BOM references it; rules ultimately execute against it.

## Project structure

Create an Eclipse-compatible Java project:

```
xom-project/
├── .project          # Eclipse project descriptor
├── .classpath        # source folder (src), JRE container, Jackson JARs, output folder (classes)
├── lib/               # Jackson JARs (see below)
├── src/
│   └── package/
│       └── ClassName.java
└── classes/          # compiled output
```

- `.project` must declare `org.eclipse.jdt.core.javanature` and `org.eclipse.jdt.core.javabuilder`.
- `.classpath` must specify the `src` source folder, a JRE container, the Jackson JARs in `lib/`, and `classes` as output.
- Compile classes immediately after creation with `javac`.

## JSON serialization — use Jackson, NOT `Serializable`

ODM Decision Server uses **Jackson** for REST API JSON serialization/deserialization, not Java's built-in `Serializable`. Do not implement `java.io.Serializable` — it's Java-specific and carries known security vulnerabilities; use standard JavaBean conventions (getters/setters) instead.

**Download Jackson 2.15.2+ before compiling:**

```bash
mkdir -p [xom-project]/lib
cd [xom-project]/lib

curl -L -o jackson-core-2.15.2.jar \
  https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar
curl -L -o jackson-databind-2.15.2.jar \
  https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar
curl -L -o jackson-annotations-2.15.2.jar \
  https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar
```

Verify each is ~300-500KB, then add each as a `.classpath` entry:
```xml
<classpathentry kind="lib" path="lib/jackson-core-2.15.2.jar"/>
```

Compile with Jackson on the classpath:
```bash
cd [xom-project]
javac -cp "lib/*" -d bin src/[package]/*.java
```

**Two mandatory Jackson annotations:**

- `@JsonIgnore` on every computed property (a method with no setter — `isX()`, `hasX()`, `requiresX()` with no corresponding setter). Jackson serializes computed properties but can't deserialize them (no setter), which throws `UnrecognizedPropertyException` on JSON round-trip. Always annotate:
  ```java
  @JsonIgnore
  public boolean isEligible() { return ...; }
  ```
- `@JsonInclude(JsonInclude.Include.NON_NULL)` at the class level — excludes null-valued properties from JSON output, standard practice for REST APIs:
  ```java
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public class LoanApplication { ... }
  ```

```java
package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BusinessObject {
    private String id;
    private String name;
    private List<String> messages;

    public BusinessObject() {
        this.messages = new ArrayList<>();
    }

    @JsonIgnore
    public boolean isComputedFlag() { return /* ... */ true; }
    // other getters and setters...
}
```

## Computed properties

Use a lazy, cached pattern; expose as a read-only BOM property with no setter, and annotate the getter `@JsonIgnore` (see above).

```java
public double getYearlyRepayment() {
    if (yearlyRepayment == 0 && yearlyInterestRate != 0 && duration != 0) {
        // compute once, cache
    }
    return yearlyRepayment;
}
```

- Cache the result; don't recompute on every access.
- Check for zero values before computing.
- Do NOT add a setter — that breaks the lazy pattern.

## XOM ↔ BOM synchronization (manual)

1. XOM is Java source that MUST be compiled before BOM changes.
2. The BOM references XOM via property origin `xom:/[ProjectName]/[xom-project-name]`.
3. Changing XOM classes requires regenerating the BOM — it does not auto-sync.
4. The XOM path in `.ruleproject` uses an Eclipse workspace reference `platform:/[xom-project-name]`, not a filesystem path.
5. Order: compile XOM first, then regenerate BOM.

## Structural reference

`../schemas/model.ecore` defines the exact `xom.ecore`/`bom.ecore`/`base.ecore` metamodel used to validate `.ruleproject` XOMPath/BOMPath entries — consult it if a generated `.ruleproject` is rejected and the cause isn't obvious from the templates.
