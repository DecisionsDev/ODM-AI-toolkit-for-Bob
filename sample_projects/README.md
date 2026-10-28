# ODM Decision Service Projects

This directory contains 7 IBM ODM Decision Service projects demonstrating various business domains.

## Projects

1. **AML_Detection_service** - Anti-Money Laundering detection rules
2. **Aviation_Pollution_Compliance** - Aviation emissions compliance checking
3. **Cardiovascular_Risk_Assessment** - Medical cardiovascular risk assessment
4. **Geolocation_Fraud_Detection** - Location-based fraud detection
5. **Loan_Compliance_Service** - Loan eligibility and pricing rules
6. **Luggage_Compliance_Service** - Airline baggage compliance rules
7. **Mineral_Classification** - Geological mineral classification

## Quick Rebuild

All generated artifacts (compiled classes, JARs, reports) have been removed to reduce repository size. Use these scripts to rebuild:

### Rebuild All Projects

```bash
./rebuild-all.sh
```

This will:
- Auto-detect or download IBM ODM Build Command
- Compile all XOM Java classes
- Build all RuleApp JARs
- Generate deployment artifacts

**Time:** ~5-10 minutes for all projects

### Rebuild Single Project

```bash
./rebuild-single.sh <project-name>
```

Example:
```bash
./rebuild-single.sh AML_Detection_service
```

**Time:** ~1 minute per project

### Available Projects

- `AML_Detection_service`
- `Aviation_Pollution_Compliance`
- `Cardiovascular_Risk_Assessment`
- `Geolocation_Fraud_Detection`
- `Loan_Compliance_Service`
- `Luggage_Compliance_Service`
- `Mineral_Classification`

## Prerequisites

- **Java:** IBM Semeru OpenJ9 JDK 21 (or JDK 17+)
- **Docker:** Required for downloading ODM Build Command (first run only)
- **ODM:** Build Command will be auto-downloaded if not present

## Project Structure

Each project follows this structure:

```
Project_Name/
├── Project Name/              # Rule project
│   ├── .ruleproject          # ODM project configuration
│   ├── .project              # Eclipse project file
│   ├── bom/                  # Business Object Model
│   │   ├── *.bom            # BOM definitions (text format)
│   │   ├── *.b2xa           # ARL association
│   │   └── *_en_US.voc      # Vocabulary (properties format)
│   ├── rules/                # Business rules
│   │   ├── *.rfl            # Ruleflow orchestration
│   │   ├── *.var            # Variables
│   │   └── package/         # Rule packages
│   │       └── *.brl        # Business rules (BAL syntax)
│   ├── deployment/           # Deployment operations
│   │   ├── *.dop            # Decision operations
│   │   └── *.dep            # Deployment configuration
│   └── output/               # Generated (rebuild creates this)
│       └── *.jar            # RuleApp JAR
├── project-xom/              # XOM (Java domain model)
│   ├── .project             # Eclipse project file
│   ├── .classpath           # Java classpath
│   ├── src/                 # Java source files
│   └── classes/             # Generated (rebuild creates this)
└── README.md                # Project documentation
```

## What Gets Rebuilt

The rebuild scripts regenerate:

- ✅ Compiled Java classes (`classes/` directories)
- ✅ XOM JAR files (`*-xom-*.jar`)
- ✅ RuleApp JARs (`output/*.jar`)
- ✅ Deployment artifacts (`output/*.dsar`, `*.sem`)
- ✅ Build reports (`reports/*.html`)

## What's Preserved (Source Files)

- ✅ Java source files (`.java`)
- ✅ Business rules (`.brl`)
- ✅ BOM definitions (`.bom`)
- ✅ Vocabularies (`.voc`)
- ✅ Ruleflows (`.rfl`)
- ✅ All configuration files

## Troubleshooting

### Java Not Found

Install IBM Semeru OpenJ9 JDK 21:
```bash
# macOS with Homebrew
brew install --cask semeru-jdk-open
```

### Build Command Download Fails

Ensure Docker is running:
```bash
docker ps
```

### Build Errors

Check the error message - common issues:
- Missing XOM classes → Ensure XOM compiled first
- UUID mismatches → Check .ruleproject and .dop files match
- BAL syntax errors → Review rule files for correct syntax
