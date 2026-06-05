# JSON Serialization Support

The `MineralSpecimen` XOM class now supports JSON serialization/deserialization using Jackson annotations.

## Features

- **Full JSON Support**: All properties can be serialized to and deserialized from JSON
- **Computed Properties Excluded**: Helper methods (like `isHardMineral()`, `hasMetallicLuster()`) are marked with `@JsonIgnore` and excluded from JSON output
- **Null Value Handling**: Null values are excluded from JSON output using `@JsonInclude(JsonInclude.Include.NON_NULL)`
- **Clean JSON Structure**: Only actual data properties are included, making the JSON compact and focused

## Dependencies

The following Jackson libraries are included in the `lib/` directory:

- `jackson-core-2.15.2.jar`
- `jackson-databind-2.15.2.jar`
- `jackson-annotations-2.15.2.jar`

## Usage Example

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mineral.classification.MineralSpecimen;

// Create ObjectMapper
ObjectMapper mapper = new ObjectMapper();

// Serialize to JSON
MineralSpecimen specimen = new MineralSpecimen("SPEC-001", "Quartz");
specimen.setMohsHardness(7.0);
specimen.setLuster("vitreous");
String json = mapper.writeValueAsString(specimen);

// Deserialize from JSON
MineralSpecimen deserialized = mapper.readValue(json, MineralSpecimen.class);
```

## Example JSON Output

```json
{
  "specimenId" : "SPEC-001",
  "specimenName" : "Quartz Sample",
  "containsSiliconOxygenTetrahedra" : true,
  "luster" : "vitreous",
  "mohsHardness" : 7.0,
  "specificGravity" : 2.65,
  "color" : "clear",
  "crystalHabit" : "hexagonal",
  "cleavage" : "none",
  "fracture" : "conchoidal",
  "frameworkStructure" : true,
  "messages" : []
}
```

## Running the Example

```bash
cd mineral-classification-xom
javac -cp "lib/*:classes" -d classes src/com/mineral/classification/JsonExample.java
java -cp "lib/*:classes" com.mineral.classification.JsonExample
```

## Integration with ODM Decision Services

When using this XOM with IBM ODM Decision Services:

1. **REST API Input**: Send JSON requests to the Decision Service
2. **Rule Execution**: ODM deserializes JSON to `MineralSpecimen` objects
3. **Rule Processing**: Business rules classify the mineral
4. **REST API Output**: ODM serializes the result back to JSON

Example REST API request:
```json
{
  "specimen": {
    "specimenId": "SAMPLE-123",
    "specimenName": "Unknown Mineral",
    "mohsHardness": 7.0,
    "luster": "vitreous",
    "containsSiliconOxygenTetrahedra": true,
    "frameworkStructure": true,
    "cleavage": "none",
    "fracture": "conchoidal"
  }
}
```

Example REST API response:
```json
{
  "specimen": {
    "specimenId": "SAMPLE-123",
    "specimenName": "Unknown Mineral",
    "mohsHardness": 7.0,
    "luster": "vitreous",
    "containsSiliconOxygenTetrahedra": true,
    "frameworkStructure": true,
    "cleavage": "none",
    "fracture": "conchoidal",
    "mineralClass": "Silicate",
    "silicateSubclass": "Tectosilicate",
    "messages": [
      "Rule 9: Classified as Silicate - hard (Mohs > 5), displays vitreous luster, shows no acid reactivity, and is composed of silicon-oxygen tetrahedra",
      "Rule 14: Classified as Tectosilicate - forms three-dimensional network of SiO4 tetrahedra with no cleavage and conchoidal fracture"
    ]
  }
}
```

## Benefits

1. **Easy Integration**: Standard JSON format works with any REST client
2. **Compact Payload**: Only relevant data is transmitted
3. **Type Safety**: Jackson handles type conversion automatically
4. **Validation**: JSON schema can be generated for validation
5. **Documentation**: JSON structure is self-documenting

## Notes

- Computed properties are calculated on-the-fly and not stored in JSON
- The `messages` collection accumulates rule execution messages
- All boolean flags default to `false` if not specified in JSON
- Numeric properties default to `0.0` if not specified