# ODM Rule Designer Skill

This skill transforms Bob into an expert in IBM Operational Decision Manager (ODM), capable of creating complete Decision Service projects for any business domain.

## Skill Structure

This skill includes comprehensive documentation organized into focused reference files:

### Core Files

- **SKILL.md** - Main skill definition with overview and workflow
- **README.md** - This file, documenting the skill structure

### Reference Documentation

1. **xom-bom-guidelines.md** - XOM project creation, BOM mapping rules, and file naming conventions
   - Eclipse project structure
   - Jackson JAR dependencies for JSON serialization
   - Auto-mapped properties and boolean naming rules
   - BOM reserved keywords

2. **bal-syntax-guide.md** - Business Action Language syntax and parser constraints
   - Basic BAL syntax rules
   - Comparison operators and set membership
   - Boolean negation patterns
   - Method call phrase syntax
   - Vocabulary phrase token conflicts
   - Null-safe navigation

3. **build-validation.md** - Build Command CLI validation workflow
   - Installing rules-compiler.jar
   - Creating build properties files
   - Running build command with JDK 21
   - Interpreting build output and error messages
   - Fix-and-rebuild cycle

4. **templates.md** - Complete file templates for all ODM artifacts
   - UUID generation strategy
   - Rule files (.brl)
   - Variables files (.var)
   - Deployment operations (.dop)
   - BOM files (text-based BRL)
   - Vocabulary files (properties-based)
   - Ruleflows (.rfl)
   - Rule packages (.rulepackage)
   - Eclipse project files (.project, .classpath, .ruleproject)
   - B2XA and deployment configuration files

5. **proven-patterns.md** - Validated patterns from successful builds
   - Computed property patterns
   - Property naming consistency
   - Collection property patterns
   - Multi-action rule patterns
   - Null-safe navigation
   - Vocabulary disambiguation
   - Arithmetic expressions
   - Boolean property patterns
   - Project naming conventions

6. **tools/** - Optional directory for embedded build tools
   - Can contain `rules-compiler.jar` (~52MB) for immediate availability
   - Eliminates extraction steps when creating new projects
   - See `tools/README.md` for setup instructions

## How to Use This Skill

When Bob activates this skill, it gains access to all the reference documentation in this directory. Bob will:

1. Follow the workflow defined in SKILL.md
2. Reference the appropriate documentation files for technical details
3. Apply proven patterns from successful builds
4. Validate all rules using the Build Command CLI

## Key Capabilities

- Create complete ODM Decision Service projects from business requirements
- Generate XOM Java classes with proper Jackson annotations
- Create BOM files in text-based BRL format
- Develop natural language vocabularies
- Implement business rules using Business Action Language
- Design ruleflows with appropriate execution modes
- Configure deployment operations
- Validate builds and fix compilation errors iteratively

## Best Practices

1. **Always generate fresh UUIDs** for each file - never reuse
2. **Use camelCase project names** without spaces
3. **Compile XOM before creating BOM** - synchronization is manual
4. **Run Build Command validation** after creating all files
5. **Fix errors iteratively** - one at a time, starting with the first
6. **Follow proven patterns** from the proven-patterns.md file

## ODM Rule Compiler MCP Server (Recommended)

This skill integrates with the ODM Rule Compiler MCP Server for seamless project compilation through natural language commands.

**Benefits:**
- Compile projects using natural language (e.g., "compile the PetShop_Service project")
- Automatic path resolution and XOM classpath detection
- Integrated error handling and reporting
- No need to remember complex command-line syntax
- Validate project structure before compilation
- List and discover ODM projects in your workspace

**Setup:**

1. **Build the MCP server** (one-time setup):
   ```bash
   cd mcp-servers/odm-rule-compiler
   mvn clean package
   ```

2. **Configure Bob to use the MCP server**:
   
   It's already configured in the Bob MCP configuration file but if it's not the case,
   Add to your Bob MCP configuration file (`` on macOS):

   ```json
  {
     "mcpServers": {
       "odm-rule-compiler": {
         "command": "java",
         "args": [
           "-jar",
           "./mcp-servers/odm-rule-compiler/target/odm-rule-compiler-mcp-1.0.0.jar"
         ]
       }
     }
   }
   ```


3. **Verify the setup** by asking Bob:
   - "List ODM projects in the projects directory"
   - "Compile the Mineral_Classification project"

**Usage Examples:**
- "Compile the PetShop_Service rule project"
- "Validate the AML_Detection_service project structure"
- "List all ODM projects"

See [`mcp-servers/odm-rule-compiler/README.md`](../../mcp-servers/odm-rule-compiler/README.md) for detailed documentation.

## Version

This skill is based on the ODM Rule Designer custom mode v1.0, transformed into Bob's skill format for better organization and reusability.