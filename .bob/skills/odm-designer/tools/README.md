# ODM Build Tools

This directory contains the IBM ODM Build Command tools needed for validating Decision Service projects.

## rules-compiler.jar

The `rules-compiler.jar` file (~52MB) is required for building and validating ODM projects. Due to its size and licensing considerations, it is not included in version control by default.

### Option 1: Add rules-compiler.jar to This Directory (Recommended for Skill)

If you want the skill to have immediate access to the build tools without extraction steps:

1. Copy `rules-compiler.jar` to this directory:
   ```bash
   cp /path/to/rules-compiler.jar .bob/skills/odm-designer/tools/
   ```

2. Verify the file:
   ```bash
   ls -lh .bob/skills/odm-designer/tools/rules-compiler.jar
   ```
   Expected: ~52MB file

3. The skill will automatically use this JAR when building projects

### Option 2: Extract from ODM Docker Container

If you don't have the JAR file locally:

```bash
# Start ODM container
docker run -d -e LICENSE=accept -p 9060:9060 -p 9443:9443 \
  -e SAMPLE=false --name odm-buildcmd icr.io/cpopen/odm-k8s/odm:9.5

# Wait for startup
sleep 60

# Download buildcommand.zip
curl http://localhost:9060/decisioncenter/assets/buildcommand.zip -o /tmp/buildcommand.zip

# Extract rules-compiler.jar to skill directory
unzip -j /tmp/buildcommand.zip 'rules-compiler/rules-compiler.jar' \
  -d .bob/skills/odm-designer/tools/

# Cleanup
docker stop odm-buildcmd && docker rm odm-buildcmd
rm /tmp/buildcommand.zip
```

### Option 3: Copy from ODM Installation

If you have ODM installed locally:

```bash
# Find rules-compiler.jar
find /opt/ibm/odm /Applications/IBM/ODM* -name "rules-compiler.jar" 2>/dev/null

# Copy to skill directory
cp /path/to/found/rules-compiler.jar .bob/skills/odm-designer/tools/
```

## Usage in Projects

When the skill creates a new ODM project, it will:

1. Check if `rules-compiler.jar` exists in `.bob/skills/odm-designer/tools/`
2. If found, copy it to the project's `buildcommand/rules-compiler/` directory
3. If not found, provide instructions to extract it

This approach allows:
- **Immediate availability** if JAR is in skill directory
- **No duplication** across multiple projects (single source)
- **Easy updates** by replacing the JAR in one location
- **Optional inclusion** in version control (add to .gitignore if needed)

## .gitignore Recommendation

If you add `rules-compiler.jar` to this directory and want to exclude it from version control:

Add to your `.gitignore`:
```
.bob/skills/odm-designer/tools/rules-compiler.jar
```

This keeps the repository size manageable while allowing local use of the embedded JAR.

## File Structure

```
.bob/skills/odm-designer/tools/
├── README.md                    # This file
└── rules-compiler.jar          # IBM ODM Build Command JAR (optional, ~52MB)
```

## Licensing Note

The `rules-compiler.jar` is part of IBM Operational Decision Manager, which is commercial software. Ensure you have appropriate licensing before using it. The JAR is typically obtained from:
- IBM ODM installation
- IBM ODM Docker images (with accepted license)
- IBM Passport Advantage downloads