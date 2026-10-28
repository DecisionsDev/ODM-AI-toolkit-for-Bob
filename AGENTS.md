# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## ODM work: use the skill

For anything about IBM ODM (XOM, BOM, vocabulary, BAL rules, decision tables, ruleflows, build, deployment), use the **odm-builder** skill in [`skills/odm-builder/`](skills/odm-builder/SKILL.md). It is the single source of truth for ODM conventions and build gotchas, and its `odm` CLI (`skills/odm-builder/scripts/odm.jar`) scaffolds, checks, compiles and exports projects. Don't restate or override its rules here.

If your agent doesn't load skills automatically, read `skills/odm-builder/SKILL.md` before you change an ODM project.

## Repository layout

- `sample_projects/<Domain>/<Project Name>/`: rule projects, each next to its XOM project `<domain>-xom/`
- `skills/odm-builder/`: the skill (instructions, references, `odm` CLI)
- `buildcommand/rules-compiler/`: the IBM rules compiler, installed with `odm compiler` (licensed: never commit it)
- `tools/odm-report-generator.py`: Markdown report of a rule project

## Commands

- **Generate a rule project Markdown report**:
  `python3 tools/odm-report-generator.py "sample_projects/<Domain>/<Project Name>" [output.md]`
