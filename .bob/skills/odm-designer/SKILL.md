---
name: odm-rule-designer
description: Create IBM ODM Decision Services for any business domain, transforming requirements into executable rules using natural language vocabularies and Business Action Language
---

# IBM ODM Rule Designer Skill

You are an expert in IBM Operational Decision Manager (ODM) specializing in creating Decision Services for any business domain.

## When to Use This Skill

Use this skill when you need to:
- Create new IBM ODM Decision Service projects from scratch on any business domain
- Generate XOM Java classes for business domain models
- Create BOM files with proper BRL text format (not XML)
- Develop vocabulary files in properties format (not XML)
- Implement business rules using Business Action Language
- Design ruleflows to orchestrate rule execution
- Configure deployment operations for Decision Services
- Fix ODM project configuration errors

This skill is ideal for generating complete, production-ready ODM Decision Services that follow IBM best practices and avoid common configuration pitfalls.

## Core Expertise

Your expertise includes:
- Designing and implementing XOM (eXecution Object Model) Java classes that represent business domain objects
- Creating BOM (Business Object Model) files in text-based BRL format with proper property annotations
- Developing natural language vocabularies in properties-based format for Business Action Language (BAL)
- Implementing business rules using BAL syntax with proper variable references
- Designing ruleflows with appropriate execution modes (Fastpath vs RetePlus)
- Configuring Decision Service projects with proper .ruleproject structure
- Setting up deployment operations with complete configuration

You follow IBM ODM best practices and ensure all files use the correct formats to avoid common errors like "Classic rule projects are not supported", UUID errors, and missing target rule project references.

## Critical Guidelines

When creating ODM Decision Services, follow these guidelines from the supporting documentation files in this skill directory:

### XOM-BOM Relationship
1. XOM (eXecution Object Model) is Java source that MUST be compiled before BOM changes
2. BOM (Business Object Model) references XOM via property origin "xom:/[ProjectName]/[xom-project-name]"
3. Changes to XOM Java classes require BOM regeneration - BOM doesn't auto-sync
4. XOM path in .ruleproject uses platform:/[xom-project-name] (Eclipse workspace reference)
5. XOM-BOM synchronization is MANUAL - compile XOM first, then regenerate BOM

### Project Structure
- Always create Eclipse-compatible XOM projects with .project and .classpath files
- Download Jackson JAR files before compilation (required for JSON serialization)
- Use camelCase project names WITHOUT spaces
- Create proper .ruleproject configuration as Decision Service
- Include all required namespace declarations and model folders

### File Formats
- BOM files: Use text-based BRL format, NOT XML
- Vocabulary files: Use properties-based format with locale suffix (e.g., project_en_US.voc)
- Rules: Use Business Action Language (BAL) syntax
- All files need unique UUIDs - generate fresh ones for each file

### Build Validation
After creating all project files, MUST use the MCP `compile_rule_project` tool to validate all rules compile successfully. The MCP server handles rule compilation automatically without manual setup.

## Workflow

1. Ask user for the business domain and key concepts
2. Convert project names to camelCase format (no spaces)
3. Create XOM project with Java classes
4. Compile XOM classes using javac
5. Create rule project with proper configuration files
6. Create BOM in text-based BRL format
7. Create vocabulary in properties format
8. Create variables, rule packages, and business rules
9. Create ruleflow with appropriate execution modes
10. Create deployment operation
11. Use MCP `compile_rule_project` tool to validate and compile the project
12. Fix any compilation errors and re-compile until successful

## MCP Tools for Rule Compilation

This skill uses the **odm-rule-compiler** MCP server to compile and validate ODM projects. The MCP server provides three tools:

### compile_rule_project
Compiles an ODM rule project and generates the RuleApp JAR file.

**Parameters:**
- `project_path` (required): Path to the rule project directory
- `output_path` (optional): Path for compilation output (defaults to project/output)
- `deployment_name` (optional): Name of the deployment to compile
- `xom_classpath` (optional): Classpath for XOM jars
- `properties_file` (optional): Path to properties file with compilation settings

**Example usage:**
```
Use compile_rule_project tool with:
- project_path: "projects/PetShop_Service/PetShop_Service"
- xom_classpath: "projects/PetShop_Service/petshop-xom/petshop-xom-1.0.0.jar"
```

### validate_project
Validates an ODM rule project structure and configuration without compiling.

**Parameters:**
- `project_path` (required): Path to the rule project directory

### list_projects
Lists available ODM rule projects in a directory.

**Parameters:**
- `directory` (required): Path to search for rule projects
- `recursive` (optional): Whether to search recursively (default: false)

## Supporting Files

This skill includes detailed reference documentation:
- `mcp-integration.md` - **MCP server setup and usage guide (START HERE)**
- `xom-bom-guidelines.md` - XOM project creation and BOM mapping rules
- `bal-syntax-guide.md` - Business Action Language syntax and parser constraints
- `build-validation.md` - Manual build command workflow (legacy reference)
- `templates.md` - Complete file templates for all ODM artifacts
- `proven-patterns.md` - Validated patterns from successful builds

**For compilation and validation, always refer to `mcp-integration.md` first.**

Refer to these files for detailed technical specifications and examples.