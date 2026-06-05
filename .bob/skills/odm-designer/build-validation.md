# Build Command Validation Workflow

## Build Validation (MANDATORY)

After creating all project files, you MUST validate that all rules compile successfully. Use the MCP `compile_rule_project` tool as the primary method. Repeat the fix-and-compile cycle until successful.

## Recommended Method: MCP Server Compilation

The **odm-rule-compiler** MCP server provides automated compilation without manual setup.

### Using compile_rule_project Tool

```
Use the compile_rule_project MCP tool with:
- project_path: "projects/[ProjectName]/[ProjectName]"
- xom_classpath: "projects/[ProjectName]/[xom-project-name]/[xom-project-name]-1.0.0.jar"
```

The MCP server will:
1. Validate project structure (checks `.ruleproject`, required directories)
2. Generate properties file automatically from parameters
3. Execute the rule compiler with proper configuration
4. Return compilation results with detailed error messages

**Success Response:**
```json
{
  "success": true,
  "message": "Compilation successful",
  "output_path": "projects/[ProjectName]/[ProjectName]/output",
  "ruleapp_jar": "[ProjectName].jar"
}
```

**Failure Response:**
```json
{
  "success": false,
  "message": "Compilation failed",
  "errors": [
    {
      "rule": "package.rule-name",
      "message": "Error description"
    }
  ]
}
```

### Fix-and-Recompile Cycle

When compilation fails:
1. Read the error message to identify the failing rule
2. Read the rule file and vocabulary to understand the issue
3. Apply the fix (see BAL Parser Constraints in bal-syntax-guide.md)
4. If XOM was changed: recompile Java + rebuild JAR first
5. Use `compile_rule_project` tool again
6. Repeat until successful

## Alternative Method: Manual Build Command CLI

If the MCP server is unavailable, you can use the manual build command workflow below.

## Step 1 - Ensure rules-compiler.jar is Available (MANDATORY)

The rules-compiler.jar (~52MB) is required for building and validating ODM projects.

### Check Skill Directory First (Recommended)

The skill can include a pre-configured `rules-compiler.jar` in `.bob/skills/odm-designer/tools/`:

```bash
# Check if JAR exists in skill directory
ls -lh .bob/skills/odm-designer/tools/rules-compiler.jar
```

If the JAR exists in the skill directory, copy it to your project:

```bash
# Create project buildcommand directory
mkdir -p buildcommand/rules-compiler

# Copy from skill directory to project
cp .bob/skills/odm-designer/tools/rules-compiler.jar buildcommand/rules-compiler/

# Verify
ls -lh buildcommand/rules-compiler/rules-compiler.jar
```

### If Not in Skill Directory, Extract It

If the JAR is not in the skill directory, use one of these methods to obtain it:

### Option A - Copy from ODM On-Premises Installation

If you have ODM installed locally (on-prem), copy rules-compiler.jar to the project:

```bash
# Check if ODM_HOME is set
echo $ODM_HOME

# If ODM_HOME is set, copy from there
mkdir -p buildcommand/rules-compiler
cp "$ODM_HOME/buildcommand/rules-compiler/rules-compiler.jar" buildcommand/rules-compiler/

# If ODM_HOME is not set, search common installation locations:
# Linux/Unix:
find /opt/ibm/odm /opt/IBM/ODM ~/IBM/ODM* -name "rules-compiler.jar" 2>/dev/null | head -1

# macOS:
find /Applications/IBM/ODM* -name "rules-compiler.jar" 2>/dev/null | head -1

# Once found, copy to project:
mkdir -p buildcommand/rules-compiler
cp /path/to/found/rules-compiler.jar buildcommand/rules-compiler/
```

### Option B - Extract from ODM Docker Container

If you don't have ODM on-prem, extract rules-compiler.jar from ODM Docker image:

```bash
# Start ODM 9.5 standalone container (runs in background)
docker run -d -e LICENSE=accept -p 9060:9060 -p 9443:9443 -u $(id -u) \
  -e SAMPLE=false \
  --name odm-buildcmd icr.io/cpopen/odm-k8s/odm:9.5

# Wait for Decision Center to be ready (check logs)
echo "Waiting for ODM Decision Center to start..."
sleep 60
docker logs -f odm-buildcmd 2>&1 | grep -m1 "The defaultServer server is ready to run a smarter planet"

# Download buildcommand.zip from Decision Center
echo "Downloading buildcommand.zip..."
curl http://localhost:9060/decisioncenter/assets/buildcommand.zip --output buildcommand.zip

# Unzip into project directory
echo "Extracting buildcommand tools..."
unzip -o buildcommand.zip -d buildcommand 'rules-compiler/*'

# Verify rules-compiler.jar exists
ls -lh buildcommand/rules-compiler/rules-compiler.jar

# Optional: Stop container if no longer needed for deployment
# docker stop odm-buildcmd && docker rm odm-buildcmd
# Note: Keep container running if you plan to deploy the RuleApp later
```

### Verification

After copying, verify the file exists and has correct size (~52MB):

```bash
ls -lh buildcommand/rules-compiler/rules-compiler.jar
```

Expected output: `-rw-r--r-- 1 user group 52M ... rules-compiler.jar`

If file is missing or size is wrong, repeat the copy/extraction process.

## Step 2 - Create Build Properties File

Create `buildcommand/samples/config-files/[ProjectName].properties`:

```properties
project = ../../../[ProjectName]
output = ../../../[ProjectName]/output
dep = deployment
xom-classpath = ../../../[xom-project-name]/[xom-project-name]-1.0.0.jar
```

Notes:
- "project" path is relative to the config-files directory
- "dep" is the RuleApp name (no spaces, used as JAR filename prefix)
- "xom-classpath" points to the compiled XOM JAR

## Step 3 - Clean XML Files (Remove Comments)

Before running the build command, remove any XML comments that may cause parser errors. The ODM XML parser can fail on comments in certain contexts.

**Remove "Made with Bob" comments from all XML files:**

```bash
# Remove XML comments from all .brl, .rfl, .dop, .dep, .b2xa files in the project
find "projects/[ProjectName]" \( -name "*.brl" -o -name "*.rfl" -o -name "*.dop" -o -name "*.dep" -o -name "*.b2xa" \) -type f -exec sed -i '' '/<!-- Made with Bob -->/d' {} +
```

**For Linux (without the empty string after -i):**
```bash
find "projects/[ProjectName]" \( -name "*.brl" -o -name "*.rfl" -o -name "*.dop" -o -name "*.dep" -o -name "*.b2xa" \) -type f -exec sed -i '/<!-- Made with Bob -->/d' {} +
```

**Why this is needed:**
- XML comments in rule files can cause "Unexpected character" parse errors
- The ODM parser is strict about comment placement in certain XML contexts
- Comments are fine in some files but problematic in others (especially .brl files)
- Removing them ensures clean parsing

**Alternative - Manual removal:**
If the sed command doesn't work on your system, manually remove any lines containing `<!-- Made with Bob -->` from XML files before building.

## Step 4 - Run Build Command with JDK 21

Use IBM Semeru OpenJ9 JDK 21 (required for ODM 9.5 compatibility):

```bash
# Find IBM Semeru JDK 21 path
/usr/libexec/java_home -V 2>&1 | grep -i semeru

# Run build command
cd buildcommand/samples/config-files && \
  /Library/Java/JavaVirtualMachines/ibm-semeru-open-21.jdk/Contents/Home/bin/java \
  -jar ../../rules-compiler/rules-compiler.jar \
  -config [ProjectName].properties 2>&1
```

JDK selection priority:
1. IBM Semeru OpenJ9 21 (preferred - best ODM compatibility)
2. IBM Semeru OpenJ9 17 (acceptable)
3. Standard OpenJDK 21 (may work but less tested)

NEVER use JDK 8 or JDK 11 - ODM 9.5 requires JDK 17+

## Step 5 - Interpret Build Output

### EXIT CODE 0 = BUILD SUCCESS

Output: `[INFO] RuleApp "[Name].jar" saved in "[path]/output"`

Action: Done - RuleApp JAR is ready for deployment

### EXIT CODE 2 = BUILD FAILURE

Output: `[ERROR] Error: [message] ([package].[rule-name])`

Action: Fix the identified rule, then re-run build

### Error Message Patterns and Their Causes

- "The word 'X' is expected in place of 'Y'" → Wrong BAL keyword or phrase mismatch
- "The word 'X' is not required" → Extra tokens ODM doesn't recognize
- "The word 'X' is missing" → Missing required token (often reserved keyword conflict)
- "Invalid type 'X', it is not assignable from type 'Y'" → Type mismatch in expression
- "Cannot find attribute 'X' in execution class" → BOM property name mismatch with XOM
- "Ambiguous sentence" → Two vocabulary phrases start with same token sequence

## Step 6 - Fix-and-Rebuild Cycle

Repeat until exit code 0:

1. Read the failing rule file
2. Read the vocabulary file to understand phrase definitions
3. Apply the fix (see BAL Parser Constraints in bal-syntax-guide.md)
4. If XOM was changed: recompile Java + rebuild JAR first
   ```bash
   cd [xom-project] && javac -d classes src/[package]/*.java && jar cf [name].jar -C classes .
   ```
5. Re-run build command
6. If new errors appear: they may be cascading - fix the first error first

## Step 7 - Verify Output

After BUILD SUCCESS, confirm the JAR exists:

```bash
ls -la "[ProjectName]/output/"
```

Expected: `[ProjectName_with_underscores].jar`

## Build Command CLI Validation Workflow Summary

Always validate rules using the IBM ODM Build Command CLI before declaring success:
- Command: `java -jar rules-compiler.jar -config [project].properties`
- Properties file must specify: project, output, dep, xom-classpath
- Exit code 0 = BUILD SUCCESS with RuleApp JAR generated
- Exit code 2 = BUILD FAILURE with specific rule and error message
- Error messages identify the failing rule as "package.rule-name"
- Fix one error at a time - later errors may be cascading from the first
- After XOM changes: recompile Java, rebuild JAR, then re-run build command

## Deployment Configuration Requirements (CRITICAL)

The .dep file MUST include ruleAppName attribute in root element:

WRONG:
```xml
<com.ibm.rules.studio.model.decisionservice:Deployment xmi:version="2.0" ...>
```

CORRECT:
```xml
<com.ibm.rules.studio.model.decisionservice:Deployment xmi:version="2.0" ... ruleAppName="ProjectName" managingXom="true">
```

Without ruleAppName, Build Command fails with "A RuleApp name cannot be empty"

The ruleAppName should match the project name without spaces (use camelCase)

## Build Properties File Configuration

When creating `buildcommand/samples/config-files/[ProjectName].properties`, include:

```properties
project = ../../../[ProjectName]
output = ../../../[ProjectName]/output
dep = [ProjectName]  # this becomes the RuleApp JAR filename prefix
xom-classpath = ../../../[xom-project-name]/[xom-project-name]-1.0.0.jar
ruleapp-name = ProjectName  # optional but recommended for clarity