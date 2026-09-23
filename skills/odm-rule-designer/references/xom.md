# XOM (eXecution Object Model)

Plain Java POJOs under `<xom>/src/<package path>/`. `odm.py init` creates `.project`, `.classpath` (src → `bin`, Jackson jars in `lib/`) and, with `--fetch-jackson`, downloads Jackson 2.15.2. `odm.py xom <xom-dir>` compiles with `--release 17` (the ODM runtime rejects newer bytecode) and writes `<xom>/<xom>-1.0.0.jar`, which is the jar the build `.properties` points to.

## Conventions

- **No `java.io.Serializable`.** Decision Server uses Jackson.
- `@JsonInclude(JsonInclude.Include.NON_NULL)` on every class.
- `@JsonIgnore` on every computed getter without a setter (`isX()`, `hasX()`, `getTotalX()`). Without it, JSON round-trips fail with `UnrecognizedPropertyException`.
- Standard JavaBean getters/setters and a public no-arg constructor. Initialize collections in the constructor.
- Enums are fine (declare them in the BOM as shown in `bom-format.md`).
- Avoid reserved property names (`operator`, `function`, `rule`, `package`, `import`).

```java
package com.example.loan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanRequest {
    private int amount;
    private boolean approved;
    private List<String> messages = new ArrayList<>();

    public LoanRequest() {}

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public boolean isApproved() { return approved; }          // BOM: public boolean approved;
    public void setApproved(boolean approved) { this.approved = approved; }
    public List<String> getMessages() { return messages; }
    public void setMessages(List<String> messages) { this.messages = messages; }
    public void addMessage(String m) { messages.add(m); }

    @JsonIgnore
    public boolean isHighRisk() { return amount > 100_000; } // BOM: readonly + factory.ignore
}
```

Computed values that are expensive can be cached lazily. Guard against zero or null inputs, and don't add a setter.

## Sync

After any XOM change, run `odm.py xom`, then update the BOM and vocabulary by hand (nothing regenerates automatically), then rebuild.
