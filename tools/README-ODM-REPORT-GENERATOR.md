# ODM Rule Project Report Generator

A Python tool that generates comprehensive Markdown documentation for IBM Operational Decision Manager (ODM) Decision Service projects **with built-in quality assessment**.

## Overview

This tool automatically parses ODM project artifacts and creates human-readable documentation that includes:

- **Project Overview** - Basic project information and configuration
- **Business Object Model (BOM)** - Classes, properties, and methods
- **Business Vocabulary** - Natural language phrases for rules
- **Rule Variables** - Input/output parameters
- **Ruleflow** - Execution flow and orchestration
- **Business Rules** - Complete rule definitions with conditions and actions
- **Decision Tables** - Tabular rule representations
- **Deployment Configuration** - Operation settings and parameters
- **Quality Assessment** - Comprehensive quality metrics, issue detection, and recommendations ✨ **NEW!**

## Features

### Documentation Generation
✅ **Comprehensive Coverage** - Documents all major ODM artifacts
✅ **Markdown Output** - Easy to read, version control friendly
✅ **Rule Extraction** - Displays rules as formatted text with conditions and actions
✅ **Decision Table Support** - Parses and formats decision tables
✅ **Namespace Agnostic** - Works with various ODM versions
✅ **Error Handling** - Gracefully handles missing or malformed files

### Quality Assessment ✨ **NEW!**
✅ **Quality Metrics** - 5-dimensional scoring system (100 points total):
  - Rule Documentation Coverage (20 points)
  - Rule Complexity Analysis (20 points)
  - Project Organization (20 points)
  - BOM Coverage (20 points)
  - Vocabulary Coverage (20 points)

✅ **Overall Quality Score** - Letter grade (A-F) with percentage and interpretation

✅ **Issue Detection** - Identifies and categorizes issues by severity:
  - 🔴 Critical Issues
  - 🟠 High Priority Issues
  - 🟡 Medium Priority Issues
  - 🟢 Low Priority Issues

✅ **Complexity Analysis** - Flags rules with high complexity (>5 conditions or >5 actions)

✅ **Actionable Recommendations** - Specific suggestions for improvement based on detected issues

## Requirements

- Python 3.6 or higher
- No external dependencies (uses standard library only)

## Installation

Simply download the `odm-report-generator.py` script to your local machine. No installation required.

```bash
# Make the script executable (optional)
chmod +x odm-report-generator.py
```

## Usage

### Basic Usage

```bash
python3 odm-report-generator.py <project-path> [output-file]
```

### Parameters

- `<project-path>` - Path to the ODM rule project directory (required)
- `[output-file]` - Output Markdown file name (optional, default: `odm-report.md`)

### Examples

**Generate report with default output file:**
```bash
python3 odm-report-generator.py "Mineral Classification"
```

**Generate report with custom output file:**
```bash
python3 odm-report-generator.py "Mineral Classification" mineral-report.md
```

**Generate report for project in different directory:**
```bash
python3 odm-report-generator.py "/path/to/My Decision Service" my-service-report.md
```

## Output Format

The generated Markdown report includes the following sections:

### 1. Project Overview
- Project name and UUID
- Project type (Decision Service)

### 2. Business Object Model (BOM)
- Package information
- Class definitions with properties and methods
- Property types and modifiers

### 3. Business Vocabulary
- Natural language phrases for each property
- Navigation and action phrases
- Organized by class

### 4. Rule Variables
- Variable names and types
- Verbalization (how they appear in rules)

### 5. Ruleflow
- Execution flow diagram
- Task names and execution modes (Fastpath/RetePlus)
- Rule package assignments

### 6. Business Rules
- Organized by package
- Complete rule text in BAL (Business Action Language)
- Extracted conditions and actions
- Rule-by-rule documentation

### 7. Decision Tables
- Table structure with conditions and actions
- Row-by-row values
- Column headers

### 8. Deployment Configuration
- Ruleset name
- Ruleflow usage
- Input/output parameters with directions

### 9. Quality Assessment Summary ✨ **NEW!**
- Quality metrics with scores
- Overall quality grade (A-F)
- Detected issues grouped by severity
- Actionable recommendations for improvement

## Example Output

Here's a sample of what the generated report looks like:

```markdown
# ODM Rule Project Documentation

**Project:** Mineral Classification

**Generated:** 2026-03-06 08:45:03

---

## 1. Project Overview

- **Project Name:** Mineral Classification
- **Project UUID:** 1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d
- **Project Type:** Decision Service

## 6. Business Rules

### Package: `general-classification`

#### Rule: `Rule1-NativeElement`

```
if
    'the specimen' consists of a single element
then
    set the mineral class of 'the specimen' to "Native Element" ;
    add "Rule 1: Classified as Native Element" to the messages of 'the specimen' ;
```

**Conditions:**

- 'the specimen' consists of a single element

**Actions:**

- set the mineral class of 'the specimen' to "Native Element"
- add "Rule 1: Classified as Native Element" to the messages of 'the specimen'
```

## Supported ODM Artifacts

The tool parses the following ODM file types:

| File Type | Extension | Description |
|-----------|-----------|-------------|
| Rule Project | `.ruleproject` | Project configuration |
| BOM | `.bom` | Business Object Model (text format) |
| Vocabulary | `.voc` | Business vocabulary (properties format) |
| Variables | `.var` | Rule variable definitions |
| Ruleflow | `.rfl` | Rule execution orchestration |
| Business Rules | `.brl` | Individual rule definitions |
| Decision Tables | `.dta` | Tabular rule representations |
| Deployment Operation | `.dop` | Service deployment configuration |

## Project Structure Requirements

The tool expects a standard ODM project structure:

```
Project Name/
├── .ruleproject          # Project configuration
├── .project              # Eclipse project file
├── bom/                  # Business Object Model
│   ├── *.bom            # BOM files
│   ├── *.voc            # Vocabulary files
│   └── *.b2xa           # BOM-to-XOM association
├── rules/                # Business rules
│   ├── *.var            # Variable definitions
│   ├── *.rfl            # Ruleflow files
│   └── package-name/    # Rule packages
│       ├── .rulepackage
│       ├── *.brl        # Business rules
│       └── *.dta        # Decision tables
├── deployment/           # Deployment configuration
│   └── *.dop            # Deployment operations
└── output/              # Build output (ignored)
```

## Use Cases

### Documentation Generation
Generate up-to-date documentation whenever rules change:
```bash
python3 odm-report-generator.py "My Project" docs/rules-documentation.md
```

### Code Review
Create readable documentation for rule review sessions:
```bash
python3 odm-report-generator.py "Loan Validation" review-$(date +%Y%m%d).md
```

### Knowledge Transfer
Document rule logic for new team members:
```bash
python3 odm-report-generator.py "Fraud Detection" onboarding-guide.md
```

### Version Control
Track rule changes over time by committing generated reports:
```bash
python3 odm-report-generator.py "Compliance Rules" docs/rules.md
git add docs/rules.md
git commit -m "Update rule documentation"
```

## Troubleshooting

### "Error: Project path does not exist"
- Verify the project path is correct
- Use quotes around paths with spaces: `"My Project"`

### "No rules found in this package"
- Check that `.brl` files exist in the package directory
- Verify the package directory structure is correct

### "Error parsing rule file"
- The rule file may be corrupted or in an unsupported format
- Check that the file is valid XML

### Missing Content in Report
- Ensure all required ODM artifacts are present
- Check file permissions (files must be readable)

## Limitations

- **Classic Rule Projects**: Only supports Decision Service projects (not classic rule projects)
- **XML Format**: BOM and vocabulary files must be in text/properties format (not XML)
- **Binary Files**: Cannot parse compiled RuleApp JARs (only source files)
- **Custom Extensions**: May not support custom ODM extensions or plugins

## Tips for Best Results

1. **Run from project parent directory** - Makes paths simpler
2. **Use descriptive output names** - Include project name and date
3. **Commit to version control** - Track documentation changes over time
4. **Review generated output** - Verify all rules are captured correctly
5. **Update regularly** - Regenerate after significant rule changes

## Integration with CI/CD

You can integrate the report generator into your build pipeline:

```bash
# Example Jenkins/GitLab CI script
python3 odm-report-generator.py "$PROJECT_PATH" "docs/rules-$BUILD_NUMBER.md"
```

## Contributing

To extend the tool:

1. **Add new artifact types** - Extend parsing methods
2. **Improve formatting** - Enhance Markdown output
3. **Add export formats** - Support HTML, PDF, etc.
4. **Enhance error handling** - Better diagnostics

## License

This tool is provided as-is for use with IBM ODM projects.

## Support

For issues or questions:
- Check the troubleshooting section above
- Review the example output
- Verify your project structure matches ODM standards

## Quality Scoring Criteria

### Rule Documentation (20 points)
- Measures percentage of rules with documentation comments
- Full points awarded for 100% documentation coverage
- Helps ensure rules are understandable and maintainable

### Rule Complexity (20 points)
- Analyzes number of conditions (AND/OR operators) and actions (semicolons)
- Flags rules with >5 conditions or >5 actions as complex
- Higher score for simpler, more maintainable rules
- Recommends breaking complex rules into smaller units

### Project Organization (20 points)
- Evaluates package structure and rule distribution
- Optimal: 3-15 rules per package
- Penalizes single package projects or packages with >20 rules
- Encourages logical grouping of related rules

### BOM Coverage (20 points)
- Counts number of business object classes defined
- 5 points per class (maximum 20 points)
- Ensures adequate domain model representation

### Vocabulary Coverage (20 points)
- Evaluates completeness of business vocabulary definitions
- Currently placeholder for future enhancement
- Will measure phrase coverage for all BOM properties

## Example Quality Assessment Output

```markdown
## Quality Assessment Summary

### Quality Metrics

- **Rule Documentation:** 0.0% (0/14) - Score: 0.0/20
- **Rule Complexity:** 50.0% simple rules (7/14) - Score: 10.0/20
- **Project Organization:** 2 packages - Score: 20/20
- **BOM Coverage:** 1 classes defined - Score: 5/20
- **Vocabulary Coverage:** Score: 0/20

### Overall Quality Score

**35.0%** (35.0/100) - Grade: **F**

**Quality Interpretation:**

❌ **Critical** - Project requires major refactoring and documentation.

### Issues Found (7)

#### 🟡 Medium Priority Issues (7)

- **Rule4-Halide**: Complex rule with 7 conditions and 2 actions (Type: complexity)
- **Rule5-Borate**: Complex rule with 7 conditions and 2 actions (Type: complexity)
- **Rule7-Sulfate**: Complex rule with 8 conditions and 2 actions (Type: complexity)

### Recommendations

- 📝 **Add Documentation**: Less than 50% of rules have documentation.
- 🔧 **Simplify Complex Rules**: 7 rule(s) have high complexity.
- 🏗️ **Expand BOM**: Consider adding more business object classes.
- 📊 **Consider Decision Tables**: For rules with similar structure.
- ✅ **Regular Reviews**: Conduct periodic code reviews.
- 🧪 **Add Test Cases**: Ensure comprehensive test coverage.
```

## Version History

- **v2.0** - Quality Assessment Release ✨
  - Added comprehensive quality metrics (5 dimensions)
  - Issue detection with severity levels
  - Actionable recommendations
  - Complexity analysis for rules
  - Overall quality scoring with letter grades

- **v1.0** - Initial release with support for all major ODM artifacts
  - BOM, vocabulary, rules, ruleflows, decision tables
  - Markdown output format
  - Namespace-agnostic XML parsing

---

*Generated documentation helps teams understand, maintain, and improve their business rules.*