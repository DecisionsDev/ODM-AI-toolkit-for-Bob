# ODM Rule Compiler MCP Server

A Model Context Protocol (MCP) server that provides tools for compiling IBM Operational Decision Manager (ODM) rule projects.

## Overview

This MCP server wraps the ODM `rule-compiler.jar` to enable AI assistants to compile ODM rule projects. It handles:

- Configuration file generation from MCP parameters
- File system path management in MCP context
- Rule project compilation
- Project validation
- Project discovery

## Features

### Tools

1. **compile_rule_project** - Compile an ODM rule project
   - Parameters:
     - `project_path` (required): Path to the rule project directory
     - `output_path` (optional): Path for compilation output (defaults to `project/output`)
     - `deployment_name` (optional): Name of the deployment to compile
     - `xom_classpath` (optional): Classpath for XOM (eXecution Object Model) jars
     - `properties_file` (optional): Path to properties file with compilation settings

2. **validate_project** - Validate an ODM rule project structure
   - Parameters:
     - `project_path` (required): Path to the rule project directory

3. **list_projects** - List available ODM rule projects in a directory
   - Parameters:
     - `directory` (required): Path to search for rule projects
     - `recursive` (optional): Whether to search recursively (default: false)

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- IBM ODM rule-compiler.jar (located at `../../buildcommand/rules-compiler/rules-compiler.jar`)

## Building

```bash
cd mcp-servers/odm-rule-compiler
mvn clean package
```

This creates an executable JAR with all dependencies: `target/odm-rule-compiler-mcp-1.0.0.jar`

## Running

### Standalone

```bash
java -jar target/odm-rule-compiler-mcp-1.0.0.jar
```

### With Bob (Recommended)

The easiest way to use this MCP server is through Bob, which has built-in support for MCP servers.

#### Setup in Bob

1. **Build the MCP server** (if not already built):
   ```bash
   cd mcp-servers/odm-rule-compiler
   mvn clean package
   ```

2. **Configure Bob to use the MCP server**:
   
   Add the following to your Bob MCP configuration file (`~/.config/Code/User/globalStorage/rooveterinaryinc.roo-cline/settings/cline_mcp_settings.json` on macOS):

   ```json
   {
     "mcpServers": {
       "odm-rule-compiler": {
         "command": "java",
         "args": [
           "-jar",
           "/absolute/path/to/ODM-AI-toolkit-for-Bob/mcp-servers/odm-rule-compiler/target/odm-rule-compiler-mcp-1.0.0.jar"
         ]
       }
     }
   }
   ```

   **Important**: Replace `/absolute/path/to/` with your actual path to the project.

3. **Restart VS Code** to load the new MCP server configuration.

4. **Verify the connection** by asking Bob:
   - "List ODM projects in the projects directory"
   - "Compile the Mineral_Classification project"
   - "Validate the PetShop_Service project"

#### Using the MCP Server with Bob

Once configured, you can ask Bob to compile ODM projects naturally:

**Examples:**
- "Compile the PetShop_Service project"
- "Compile the Mineral_Classification rule project"
- "List all ODM projects in the projects directory"
- "Validate the AML_Detection_service project structure"

Bob will automatically:
- Locate the project directory
- Find the deployment configuration
- Determine the XOM classpath
- Execute the compilation
- Report results and any errors

**Benefits over command line:**
- No need to remember complex command syntax
- Automatic path resolution
- Integrated error handling and reporting
- Natural language interface
- Context-aware suggestions

### With Other MCP Clients (e.g., Claude Desktop)

Add to your MCP client configuration (e.g., `~/Library/Application Support/Claude/claude_desktop_config.json` on macOS):

```json
{
  "mcpServers": {
    "odm-rule-compiler": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/ODM-AI-toolkit-for-Bob/mcp-servers/odm-rule-compiler/target/odm-rule-compiler-mcp-1.0.0.jar"
      ]
    }
  }
}
```

**Important**:
- Use the absolute path to the JAR file
- Restart your MCP client after updating the configuration
- The server will appear as "odm-rule-compiler" with three tools available

## Configuration

### Properties File Format

The compiler uses a properties file with the following format:

```properties
# Path to the rule project directory
project = /path/to/project

# Output directory for compiled artifacts
output = /path/to/output

# Deployment name (optional)
dep = MyDeployment

# XOM classpath (optional, colon-separated on Unix, semicolon on Windows)
xom-classpath = /path/to/xom.jar:/path/to/other.jar
```

See [IBM ODM Documentation](https://www.ibm.com/docs/en/odm/9.0.0?topic=line-writing-configuration-file) for more details.

## Usage Examples

### With Bob (Natural Language)

Simply ask Bob to compile your projects:

```
"Compile the Mineral_Classification project"
```

Bob will automatically:
1. Locate the project at `projects/Mineral_Classification/Mineral Classification`
2. Find the deployment configuration (`deployment.dep`)
3. Locate the XOM jar (`mineral-classification-xom/mineral-classification-xom.jar`)
4. Execute the compilation
5. Report the output location and any warnings/errors

**Real Example Output:**
```
Compilation successful! The Mineral Classification rule project has been compiled.

Output location: Mineral Classification/output

The compilation generated the decision service archive (.dsar file) containing
the executable ruleset Mineral_Classification_Ruleset with the deployment
configuration from deployment.dep.
```

### Direct Tool Usage (JSON)

#### Compile a Project

```json
{
  "project_path": "/Users/user/projects/Mineral_Classification/Mineral Classification",
  "deployment_name": "deployment",
  "xom_classpath": "/Users/user/projects/Mineral_Classification/mineral-classification-xom/mineral-classification-xom.jar"
}
```

#### Compile with Properties File

```json
{
  "project_path": "/Users/user/projects/PetShop_Service/PetShop_Service",
  "properties_file": "/Users/user/projects/PetShop_Service/PetShop_Service.properties"
}
```

#### Validate a Project

```json
{
  "project_path": "/Users/user/projects/AML_Detection_service/AML Detection Service"
}
```

#### List Projects

```json
{
  "directory": "/Users/user/projects",
  "recursive": true
}
```

## Project Structure

```
mcp-servers/odm-rule-compiler/
├── pom.xml                          # Maven configuration
├── README.md                        # This file
├── src/
│   └── main/
│       ├── java/
│       │   └── com/ibm/odm/mcp/
│       │       ├── ODMRuleCompilerServer.java    # Main MCP server
│       │       └── RuleCompilerService.java      # Compilation logic
│       └── resources/
│           └── logback.xml          # Logging configuration
└── target/                          # Build output (generated)
```

## Logging

Logs are written to:
- `stderr` for real-time monitoring
- `logs/odm-rule-compiler.log` for persistent storage

Log level can be adjusted in `src/main/resources/logback.xml`.

## Troubleshooting

### rule-compiler.jar not found

Ensure the `rule-compiler.jar` is located at:
- `../../buildcommand/rules-compiler/rules-compiler.jar` (relative to project)
- Or `buildcommand/rules-compiler/rules-compiler.jar` (from current directory)

### Compilation fails

1. Check that the project has a valid `.ruleproject` file
2. Verify XOM classpath includes all required JARs
3. Check logs in `logs/odm-rule-compiler.log` for detailed error messages

### MCP connection issues

Ensure the server is running and the MCP client configuration is correct. Check stderr output for connection logs.

## Development

### Running Tests

```bash
mvn test
```

### Building without Tests

```bash
mvn clean package -DskipTests
```

## References

- [IBM ODM Documentation - Building Projects](https://www.ibm.com/docs/en/odm/9.0.0?topic=line-building-projects)
- [IBM ODM Documentation - Configuration File](https://www.ibm.com/docs/en/odm/9.0.0?topic=line-writing-configuration-file)
- [Model Context Protocol](https://modelcontextprotocol.io/)

## License

See LICENSE file in the root directory.