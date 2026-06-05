# MCP Server Integration Guide

## Overview

The ODM Designer skill now integrates with the **odm-rule-compiler** MCP server to provide automated rule compilation without manual setup. This eliminates the need to extract `rules-compiler.jar`, create properties files, or run manual build commands.

## MCP Server Setup

### Prerequisites

1. **Java 11+** installed on your system
2. **Maven** for building the MCP server
3. **rules-compiler.jar** from IBM ODM installation

### Building the MCP Server

```bash
cd mcp-servers/odm-rule-compiler

# Build the server
mvn clean package

# Verify the JAR was created
ls -lh target/odm-rule-compiler-mcp-1.0.0.jar
```

### Configuring Bob to Use the MCP Server

Add the MCP server to your Bob configuration (typically in `~/.config/bob/mcp-servers.json` or similar):

```json
{
  "mcpServers": {
    "odm-rule-compiler": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/ODM-AI-toolkit-for-Bob/mcp-servers/odm-rule-compiler/target/odm-rule-compiler-mcp-1.0.0.jar"
      ]
    }
  }
}
```

## Available MCP Tools

### 1. compile_rule_project

Compiles an ODM rule project and generates the RuleApp JAR file.

**Parameters:**
- `project_path` (required): Path to the rule project directory
- `output_path` (optional): Path for compilation output (defaults to `project/output`)
- `deployment_name` (optional): Name of the deployment to compile
- `xom_classpath` (optional): Classpath for XOM jars
- `properties_file` (optional): Path to properties file with compilation settings

**Example Usage in Bob:**

```
Please compile the PetShop_Service project using the compile_rule_project tool with:
- project_path: "projects/PetShop_Service/PetShop_Service"
- xom_classpath: "projects/PetShop_Service/petshop-xom/petshop-xom-1.0.0.jar"
```

**Success Response:**
```json
{
  "success": true,
  "message": "Compilation successful",
  "output_path": "projects/PetShop_Service/PetShop_Service/output",
  "ruleapp_jar": "PetShop_Service.jar",
  "compilation_log": "..."
}
```

**Failure Response:**
```json
{
  "success": false,
  "message": "Compilation failed",
  "errors": [
    {
      "rule": "adoption-eligibility.check-large-dog-eligibility",
      "message": "The word 'is' is expected in place of 'are'"
    }
  ],
  "compilation_log": "..."
}
```

### 2. validate_project

Validates an ODM rule project structure and configuration without compiling.

**Parameters:**
- `project_path` (required): Path to the rule project directory

**Example Usage:**

```
Please validate the project structure using validate_project with:
- project_path: "projects/PetShop_Service/PetShop_Service"
```

**Response:**
```json
{
  "valid": true,
  "project_path": "projects/PetShop_Service/PetShop_Service",
  "project_name": "PetShop_Service",
  "has_ruleproject": true,
  "has_deployment": true,
  "has_bom": true,
  "has_rules": true,
  "issues": []
}
```

### 3. list_projects

Lists available ODM rule projects in a directory.

**Parameters:**
- `directory` (required): Path to search for rule projects
- `recursive` (optional): Whether to search recursively (default: false)

**Example Usage:**

```
Please list all ODM projects using list_projects with:
- directory: "projects"
- recursive: true
```

**Response:**
```json
{
  "projects": [
    {
      "name": "PetShop_Service",
      "path": "projects/PetShop_Service/PetShop_Service",
      "has_deployment": true
    },
    {
      "name": "AML Detection Service",
      "path": "projects/AML_Detection_service/AML Detection Service",
      "has_deployment": true
    }
  ],
  "count": 2
}
```

## Integration with ODM Designer Workflow

### Updated Workflow

When creating a new ODM Decision Service:

1. **Create XOM Project** - Java classes for business domain
2. **Compile XOM** - Generate JAR file
3. **Create Rule Project** - Configuration, BOM, vocabulary, rules
4. **Validate Project** - Use `validate_project` tool to check structure
5. **Compile Project** - Use `compile_rule_project` tool
6. **Fix Errors** - If compilation fails, fix issues and recompile
7. **Deploy** - RuleApp JAR is ready in `output/` directory

### Example: Complete Project Creation

```
User: Create a pet adoption decision service

Bob (using odm-designer skill):
1. Creates XOM project with Pet, Customer, AdoptionRequest classes
2. Compiles XOM: javac + jar
3. Creates rule project with BOM, vocabulary, rules
4. Uses validate_project to check structure
5. Uses compile_rule_project to build RuleApp
6. If errors occur, fixes them and recompiles
7. Confirms successful compilation with RuleApp JAR location
```

## Error Handling

### Common Compilation Errors

The MCP server returns detailed error messages that help identify issues:

**BAL Syntax Errors:**
```json
{
  "rule": "package.rule-name",
  "message": "The word 'is' is expected in place of 'are'"
}
```

**BOM Mapping Errors:**
```json
{
  "rule": "package.rule-name",
  "message": "Cannot find attribute 'petAge' in execution class 'com.petshop.Pet'"
}
```

**Type Mismatch Errors:**
```json
{
  "rule": "package.rule-name",
  "message": "Invalid type 'String', it is not assignable from type 'int'"
}
```

### Fix-and-Recompile Cycle

1. **Read Error Message** - Identify the failing rule and error type
2. **Read Rule File** - Examine the rule implementation
3. **Read Vocabulary** - Check phrase definitions
4. **Apply Fix** - Correct the issue (see `bal-syntax-guide.md`)
5. **Recompile** - Use `compile_rule_project` again
6. **Repeat** - Continue until compilation succeeds

## Advantages Over Manual Build Command

### MCP Server Benefits

1. **No Manual Setup** - No need to extract `rules-compiler.jar`
2. **Automatic Configuration** - Properties file generated automatically
3. **Structured Errors** - JSON format with detailed error information
4. **Project Validation** - Built-in structure validation
5. **Project Discovery** - Automatic detection of ODM projects
6. **Consistent Paths** - Handles relative/absolute paths correctly
7. **Better Integration** - Native Bob tool integration

### Manual Build Command (Legacy)

Still available as fallback in `build-validation.md`:
- Requires manual JAR extraction
- Manual properties file creation
- Manual path configuration
- Text-based error parsing
- More setup steps

## Testing the MCP Server

### Unit Tests

The MCP server includes comprehensive unit tests:

```bash
cd mcp-servers/odm-rule-compiler
mvn test
```

**Test Coverage:**
- 15 MCP protocol tests
- 11 compilation service tests
- All 26 tests passing

### Manual Testing

Test with a sample project:

```bash
# Build the server
mvn clean package

# Run the server (it uses stdio for communication)
java -jar target/odm-rule-compiler-mcp-1.0.0.jar

# Send test request (JSON-RPC format)
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "validate_project",
    "arguments": {
      "project_path": "projects/PetShop_Service/PetShop_Service"
    }
  }
}
```

## Troubleshooting

### MCP Server Not Starting

**Issue:** Server fails to start
**Solution:** Check Java version (requires Java 11+)

```bash
java -version
```

### Compilation Fails with "JAR not found"

**Issue:** `rules-compiler.jar` not found
**Solution:** Ensure JAR is in correct location

```bash
ls -lh buildcommand/rules-compiler/rules-compiler.jar
```

### XOM Classes Not Found

**Issue:** Compilation fails with "Cannot find class"
**Solution:** Verify XOM JAR path in `xom_classpath` parameter

```bash
ls -lh projects/[ProjectName]/[xom-project]/[xom-project]-1.0.0.jar
```

### Project Validation Fails

**Issue:** `validate_project` reports missing files
**Solution:** Check project structure:

```bash
# Required files
ls projects/[ProjectName]/[ProjectName]/.ruleproject
ls projects/[ProjectName]/[ProjectName]/deployment/*.dep
ls projects/[ProjectName]/[ProjectName]/bom/*.bom
ls projects/[ProjectName]/[ProjectName]/rules/
```

## Best Practices

1. **Always Validate First** - Use `validate_project` before compiling
2. **Check XOM Path** - Ensure XOM JAR exists and path is correct
3. **Read Error Messages** - MCP server provides detailed error information
4. **Fix One Error at a Time** - Later errors may be cascading
5. **Recompile XOM if Changed** - XOM changes require JAR rebuild
6. **Use Relative Paths** - Paths relative to workspace directory work best

## Migration from Manual Build Command

If you have existing projects using manual build commands:

1. **No Changes Needed** - Project structure remains the same
2. **Remove Properties Files** - MCP server generates them automatically
3. **Update Workflow** - Replace manual commands with MCP tools
4. **Keep JAR Location** - `rules-compiler.jar` stays in `buildcommand/rules-compiler/`

## Summary

The MCP server integration streamlines ODM rule compilation by:
- Eliminating manual setup steps
- Providing structured error messages
- Enabling automatic project validation
- Integrating natively with Bob's tool system

Use `compile_rule_project` as the primary compilation method, with manual build commands available as a fallback if needed.