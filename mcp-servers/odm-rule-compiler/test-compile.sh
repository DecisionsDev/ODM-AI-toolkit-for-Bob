#!/bin/bash

# Test script for ODM Rule Compiler MCP Server
# This script tests compilation of the AML Detection Service project

set -e

echo "=== ODM Rule Compiler MCP Server Test ==="
echo ""

# Paths
PROJECT_ROOT="$(cd ../.. && pwd)"
AML_PROJECT="$PROJECT_ROOT/projects/AML_Detection_service/AML Detection Service"
AML_XOM="$PROJECT_ROOT/projects/AML_Detection_service/aml-detection-xom"
OUTPUT_DIR="$AML_PROJECT/output"

echo "Project root: $PROJECT_ROOT"
echo "AML project: $AML_PROJECT"
echo "AML XOM: $AML_XOM"
echo ""

# Step 1: Compile the XOM (Java classes)
echo "Step 1: Compiling AML XOM classes..."
if [ ! -d "$AML_XOM/bin" ]; then
    mkdir -p "$AML_XOM/bin"
fi

javac -d "$AML_XOM/bin" \
    "$AML_XOM/src/com/banking/aml"/*.java

if [ $? -eq 0 ]; then
    echo "✓ XOM classes compiled successfully"
else
    echo "✗ XOM compilation failed"
    exit 1
fi

# Create XOM jar
echo ""
echo "Step 2: Creating XOM JAR..."
cd "$AML_XOM/bin"
jar cf "$AML_XOM/aml-detection-xom-1.0.0.jar" com/
cd -

if [ -f "$AML_XOM/aml-detection-xom-1.0.0.jar" ]; then
    echo "✓ XOM JAR created: $AML_XOM/aml-detection-xom-1.0.0.jar"
else
    echo "✗ XOM JAR creation failed"
    exit 1
fi

# Step 3: Test rule compilation using rule-compiler.jar directly
echo ""
echo "Step 3: Testing rule compilation..."

# Create properties file
PROPS_FILE="/tmp/aml-compile-test.properties"
cat > "$PROPS_FILE" << EOF
project = $AML_PROJECT
output = $OUTPUT_DIR
dep = AMLDetection
xom-classpath = $AML_XOM/aml-detection-xom-1.0.0.jar
EOF

echo "Properties file created:"
cat "$PROPS_FILE"
echo ""

# Run the compiler
COMPILER_JAR="$PROJECT_ROOT/buildcommand/rules-compiler/rules-compiler.jar"

if [ ! -f "$COMPILER_JAR" ]; then
    echo "✗ Compiler JAR not found: $COMPILER_JAR"
    exit 1
fi

echo "Running rule compiler..."
java -jar "$COMPILER_JAR" -config "$PROPS_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Rule compilation completed successfully"
    echo ""
    echo "Output directory: $OUTPUT_DIR"
    if [ -d "$OUTPUT_DIR" ]; then
        echo "Generated files:"
        ls -lh "$OUTPUT_DIR"
    fi
else
    echo "✗ Rule compilation failed"
    exit 1
fi

echo ""
echo "=== Test completed successfully ==="

# Made with Bob
