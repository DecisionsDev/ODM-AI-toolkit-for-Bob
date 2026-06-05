#!/usr/bin/env python3
"""
ODM Rule Project Report Generator

This script generates a comprehensive Markdown documentation report for IBM ODM Decision Service projects.
It parses rule files (.brl), decision tables (.dta), ruleflows (.rfl), variables (.var), and other ODM artifacts
to create human-readable documentation.

Usage:
    python odm-report-generator.py <project-path> [output-file]

Example:
    python odm-report-generator.py "Mineral Classification" report.md
"""

import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import datetime
import re


class ODMReportGenerator:
    """Generates Markdown documentation for ODM rule projects."""
    
    def __init__(self, project_path):
        self.project_path = Path(project_path)
        self.project_name = self.project_path.name
        self.report_lines = []
        self.quality_metrics = {
            'total_rules': 0,
            'total_decision_tables': 0,
            'total_packages': 0,
            'rules_with_documentation': 0,
            'complex_rules': 0,
            'rules_per_package': {},
            'vocabulary_coverage': 0,
            'bom_classes': 0,
            'issues': []
        }
        
    def generate_report(self):
        """Generate the complete report."""
        # First pass: collect metrics by parsing all artifacts
        self._collect_metrics()
        
        # Second pass: generate report with quality assessment at the beginning
        self._add_header()
        self._add_project_overview()
        self._add_quality_assessment()  # Quality assessment right after overview
        self._add_deployment_info()
        self._add_ruleflow()
        self._add_rules()  # Display rules without re-collecting metrics
        self._add_decision_tables()
        self._add_variables()
        self._add_business_object_model()
        self._add_vocabulary()
        self._add_footer()
        
        return "\n".join(self.report_lines)
    
    def _collect_metrics(self):
        """First pass: collect all metrics without generating report content."""
        # Collect BOM metrics
        bom_dir = self.project_path / "bom"
        if bom_dir.exists():
            for bom_file in bom_dir.glob("*.bom"):
                self._collect_bom_metrics(bom_file)
        
        # Collect rule metrics
        rules_dir = self.project_path / "rules"
        if rules_dir.exists():
            rule_packages = [d for d in rules_dir.iterdir() if d.is_dir()]
            for package in rule_packages:
                self._collect_package_metrics(package)
        
        # Collect decision table metrics
        if rules_dir.exists():
            dta_files = list(rules_dir.rglob("*.dta"))
            self.quality_metrics['total_decision_tables'] = len(dta_files)
    
    def _collect_bom_metrics(self, bom_file):
        """Collect BOM metrics without generating report content."""
        try:
            with open(bom_file, 'r', encoding='utf-8') as f:
                content = f.read()
            class_pattern = r'public class (\w+)\s*\{'
            classes = list(re.finditer(class_pattern, content))
            self.quality_metrics['bom_classes'] += len(classes)
        except Exception:
            pass
    
    def _collect_package_metrics(self, package_dir):
        """Collect package metrics without generating report content."""
        brl_files = sorted(package_dir.glob("*.brl"))
        if not brl_files:
            return
        
        self.quality_metrics['total_packages'] += 1
        self.quality_metrics['rules_per_package'][package_dir.name] = len(brl_files)
        
        for brl_file in brl_files:
            self._collect_rule_metrics(brl_file)
    
    def _collect_rule_metrics(self, brl_file):
        """Collect rule metrics without generating report content."""
        try:
            tree = ET.parse(brl_file)
            root = tree.getroot()
            
            documentation = None
            definition = None
            for elem in root.iter():
                if elem.tag.endswith('documentation'):
                    documentation = elem
                elif elem.tag.endswith('definition'):
                    definition = elem
            
            self.quality_metrics['total_rules'] += 1
            
            if documentation is not None and documentation.text and documentation.text.strip():
                self.quality_metrics['rules_with_documentation'] += 1
            
            if definition is not None and definition.text:
                rule_text = definition.text.strip()
                condition_count = rule_text.count('and') + rule_text.count('or')
                action_count = rule_text.count(';')
                
                if condition_count > 5 or action_count > 5:
                    self.quality_metrics['complex_rules'] += 1
                    name = None
                    for elem in root.iter():
                        if elem.tag.endswith('name'):
                            name = elem
                            break
                    rule_name = name.text if name is not None else brl_file.stem
                    self.quality_metrics['issues'].append({
                        'type': 'complexity',
                        'severity': 'medium',
                        'rule': rule_name,
                        'message': f'Complex rule with {condition_count} conditions and {action_count} actions'
                    })
        except Exception:
            pass
    
    def _add_header(self):
        """Add report header."""
        self.report_lines.extend([
            f"# ODM Rule Project Documentation",
            f"",
            f"**Project:** {self.project_name}",
            f"",
            f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            f"",
            f"---",
            f"",
        ])
    
    def _add_project_overview(self):
        """Add project overview section."""
        self.report_lines.extend([
            f"## 1. Project Overview",
            f"",
        ])
        
        # Read .ruleproject file
        ruleproject_path = self.project_path / ".ruleproject"
        if ruleproject_path.exists():
            try:
                tree = ET.parse(ruleproject_path)
                root = tree.getroot()
                
                # Extract namespace
                ns = {'base': 'http://ilog.rules.studio/model/base.ecore'}
                
                project_name = root.find('.//name', ns)
                project_uuid = root.find('.//uuid', ns)
                
                self.report_lines.extend([
                    f"- **Project Name:** {project_name.text if project_name is not None else 'N/A'}",
                    f"- **Project UUID:** {project_uuid.text if project_uuid is not None else 'N/A'}",
                    f"- **Project Type:** Decision Service",
                    f"",
                ])
            except Exception as e:
                self.report_lines.append(f"*Error reading project file: {e}*\n")
        
        self.report_lines.append("")
    
    def _add_quality_assessment(self):
        """Add quality assessment section with metrics and recommendations."""
        self.report_lines.extend([
            f"## 2. Quality Assessment Summary",
            f"",
            f"### Quality Metrics",
            f"",
        ])
        
        # Calculate quality score
        total_score = 0
        max_score = 0
        
        # Rule documentation score (20 points)
        max_score += 20
        if self.quality_metrics['total_rules'] > 0:
            doc_percentage = (self.quality_metrics['rules_with_documentation'] / self.quality_metrics['total_rules']) * 100
            doc_score = (doc_percentage / 100) * 20
            total_score += doc_score
            self.report_lines.append(f"- **Rule Documentation:** {doc_percentage:.1f}% ({self.quality_metrics['rules_with_documentation']}/{self.quality_metrics['total_rules']}) - Score: {doc_score:.1f}/20")
        else:
            self.report_lines.append(f"- **Rule Documentation:** N/A (no rules found)")
        
        # Rule complexity score (20 points)
        max_score += 20
        if self.quality_metrics['total_rules'] > 0:
            complex_percentage = (self.quality_metrics['complex_rules'] / self.quality_metrics['total_rules']) * 100
            complexity_score = max(0, 20 - (complex_percentage / 100) * 20)
            total_score += complexity_score
            self.report_lines.append(f"- **Rule Complexity:** {100 - complex_percentage:.1f}% simple rules ({self.quality_metrics['total_rules'] - self.quality_metrics['complex_rules']}/{self.quality_metrics['total_rules']}) - Score: {complexity_score:.1f}/20")
        else:
            self.report_lines.append(f"- **Rule Complexity:** N/A (no rules found)")
        
        # Project organization score (20 points)
        max_score += 20
        org_score = 0
        if self.quality_metrics['total_packages'] > 0:
            org_score += 10
            # Check for balanced package distribution
            if self.quality_metrics['rules_per_package']:
                avg_rules = sum(self.quality_metrics['rules_per_package'].values()) / len(self.quality_metrics['rules_per_package'])
                if 3 <= avg_rules <= 15:  # Good range
                    org_score += 10
                elif 1 <= avg_rules <= 20:  # Acceptable range
                    org_score += 5
        total_score += org_score
        self.report_lines.append(f"- **Project Organization:** {self.quality_metrics['total_packages']} packages - Score: {org_score}/20")
        
        # BOM coverage score (20 points)
        max_score += 20
        bom_score = 0
        if self.quality_metrics['bom_classes'] > 0:
            bom_score = min(20, self.quality_metrics['bom_classes'] * 5)
        total_score += bom_score
        self.report_lines.append(f"- **BOM Coverage:** {self.quality_metrics['bom_classes']} classes defined - Score: {bom_score}/20")
        
        # Vocabulary coverage score (20 points)
        max_score += 20
        vocab_score = min(20, self.quality_metrics['vocabulary_coverage'])
        total_score += vocab_score
        self.report_lines.append(f"- **Vocabulary Coverage:** Score: {vocab_score}/20")
        
        self.report_lines.append("")
        
        # Overall quality score
        overall_percentage = (total_score / max_score) * 100 if max_score > 0 else 0
        quality_grade = self._get_quality_grade(overall_percentage)
        
        self.report_lines.extend([
            f"### Overall Quality Score",
            f"",
            f"**{overall_percentage:.1f}%** ({total_score:.1f}/{max_score}) - Grade: **{quality_grade}**",
            f"",
        ])
        
        # Quality interpretation
        self.report_lines.extend([
            f"**Quality Interpretation:**",
            f"",
        ])
        
        if overall_percentage >= 90:
            self.report_lines.append("✅ **Excellent** - Project follows best practices with comprehensive documentation and well-organized structure.")
        elif overall_percentage >= 75:
            self.report_lines.append("✅ **Good** - Project is well-structured with minor areas for improvement.")
        elif overall_percentage >= 60:
            self.report_lines.append("⚠️ **Fair** - Project is functional but has several areas that need attention.")
        elif overall_percentage >= 40:
            self.report_lines.append("⚠️ **Poor** - Project needs significant improvements in documentation and organization.")
        else:
            self.report_lines.append("❌ **Critical** - Project requires major refactoring and documentation.")
        
        self.report_lines.append("")
        
        # Issues and recommendations
        if self.quality_metrics['issues']:
            self.report_lines.extend([
                f"### Issues Found ({len(self.quality_metrics['issues'])})",
                f"",
            ])
            
            # Group issues by severity
            critical_issues = [i for i in self.quality_metrics['issues'] if i['severity'] == 'critical']
            high_issues = [i for i in self.quality_metrics['issues'] if i['severity'] == 'high']
            medium_issues = [i for i in self.quality_metrics['issues'] if i['severity'] == 'medium']
            low_issues = [i for i in self.quality_metrics['issues'] if i['severity'] == 'low']
            
            if critical_issues:
                self.report_lines.append(f"#### 🔴 Critical Issues ({len(critical_issues)})")
                self.report_lines.append("")
                for issue in critical_issues:
                    self.report_lines.append(f"- **{issue['rule']}**: {issue['message']} (Type: {issue['type']})")
                self.report_lines.append("")
            
            if high_issues:
                self.report_lines.append(f"#### 🟠 High Priority Issues ({len(high_issues)})")
                self.report_lines.append("")
                for issue in high_issues:
                    self.report_lines.append(f"- **{issue['rule']}**: {issue['message']} (Type: {issue['type']})")
                self.report_lines.append("")
            
            if medium_issues:
                self.report_lines.append(f"#### 🟡 Medium Priority Issues ({len(medium_issues)})")
                self.report_lines.append("")
                for issue in medium_issues:
                    self.report_lines.append(f"- **{issue['rule']}**: {issue['message']} (Type: {issue['type']})")
                self.report_lines.append("")
            
            if low_issues:
                self.report_lines.append(f"#### 🟢 Low Priority Issues ({len(low_issues)})")
                self.report_lines.append("")
                for issue in low_issues:
                    self.report_lines.append(f"- **{issue['rule']}**: {issue['message']} (Type: {issue['type']})")
                self.report_lines.append("")
        
        # Recommendations
        self.report_lines.extend([
            f"### Recommendations",
            f"",
        ])
        
        recommendations = []
        
        # Documentation recommendations
        if self.quality_metrics['total_rules'] > 0:
            doc_percentage = (self.quality_metrics['rules_with_documentation'] / self.quality_metrics['total_rules']) * 100
            if doc_percentage < 50:
                recommendations.append("📝 **Add Documentation**: Less than 50% of rules have documentation. Add meaningful descriptions to help users understand rule purpose and business logic.")
            elif doc_percentage < 80:
                recommendations.append("📝 **Improve Documentation**: Consider adding documentation to remaining rules for better maintainability.")
        
        # Complexity recommendations
        if self.quality_metrics['complex_rules'] > 0:
            recommendations.append(f"🔧 **Simplify Complex Rules**: {self.quality_metrics['complex_rules']} rule(s) have high complexity. Consider breaking them into smaller, more maintainable rules.")
        
        # Organization recommendations
        if self.quality_metrics['total_packages'] == 0:
            recommendations.append("📁 **Organize Rules**: No rule packages found. Organize rules into logical packages for better maintainability.")
        elif self.quality_metrics['total_packages'] == 1:
            recommendations.append("📁 **Consider More Packages**: Only one rule package found. Consider organizing rules into multiple packages by business domain or functionality.")
        
        if self.quality_metrics['rules_per_package']:
            max_rules_in_package = max(self.quality_metrics['rules_per_package'].values())
            if max_rules_in_package > 20:
                recommendations.append(f"📁 **Split Large Packages**: Some packages contain more than 20 rules. Consider splitting into smaller, focused packages.")
        
        # BOM recommendations
        if self.quality_metrics['bom_classes'] == 0:
            recommendations.append("🏗️ **Define Business Objects**: No BOM classes found. Define business object model to represent domain concepts.")
        elif self.quality_metrics['bom_classes'] < 3:
            recommendations.append("🏗️ **Expand BOM**: Consider adding more business object classes to better represent your domain model.")
        
        # Decision table recommendations
        if self.quality_metrics['total_decision_tables'] == 0 and self.quality_metrics['total_rules'] > 10:
            recommendations.append("📊 **Consider Decision Tables**: For rules with similar structure, consider using decision tables for better readability and maintenance.")
        
        # General best practices
        recommendations.append("✅ **Regular Reviews**: Conduct periodic code reviews to maintain quality standards.")
        recommendations.append("🧪 **Add Test Cases**: Ensure comprehensive test coverage for all rules and decision tables.")
        recommendations.append("📖 **Update Vocabulary**: Keep business vocabulary aligned with domain expert terminology.")
        
        for rec in recommendations:
            self.report_lines.append(f"- {rec}")
        
        self.report_lines.append("")
        self.report_lines.append("---")
        self.report_lines.append("")
    
    def _get_quality_grade(self, percentage):
        """Convert percentage to letter grade."""
        if percentage >= 90:
            return "A"
        elif percentage >= 80:
            return "B"
        elif percentage >= 70:
            return "C"
        elif percentage >= 60:
            return "D"
        else:
            return "F"
    
    def _add_business_object_model(self):
        """Add Business Object Model section (display only, metrics already collected)."""
        self.report_lines.extend([
            f"## 8. Business Object Model (BOM)",
            f"",
        ])
        
        bom_dir = self.project_path / "bom"
        if bom_dir.exists():
            bom_files = list(bom_dir.glob("*.bom"))
            
            for bom_file in bom_files:
                self._parse_bom_file(bom_file)
        else:
            self.report_lines.append("*No BOM files found.*\n")
        
        self.report_lines.append("")
    
    def _parse_bom_file(self, bom_file):
        """Parse a BOM file and extract class information (display only, metrics already collected)."""
        self.report_lines.append(f"### {bom_file.stem}")
        self.report_lines.append("")
        
        try:
            with open(bom_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Extract package
            package_match = re.search(r'package\s+([\w.]+);', content)
            if package_match:
                self.report_lines.append(f"**Package:** `{package_match.group(1)}`\n")
            
            # Extract classes
            class_pattern = r'public class (\w+)\s*\{'
            classes = list(re.finditer(class_pattern, content))
            
            for class_match in classes:
                class_name = class_match.group(1)
                self.report_lines.append(f"#### Class: `{class_name}`\n")
                
                # Extract properties for this class
                class_start = class_match.start()
                # Find the closing brace for this class
                brace_count = 0
                class_end = class_start
                for i, char in enumerate(content[class_start:], start=class_start):
                    if char == '{':
                        brace_count += 1
                    elif char == '}':
                        brace_count -= 1
                        if brace_count == 0:
                            class_end = i
                            break
                
                class_content = content[class_start:class_end]
                
                # Extract properties
                property_pattern = r'^\s*(public|private|protected)?\s*(readonly)?\s*([\w.]+)\s+(\w+);'
                properties = re.finditer(property_pattern, class_content, re.MULTILINE)
                
                prop_list = []
                for prop in properties:
                    modifiers = []
                    if prop.group(1):
                        modifiers.append(prop.group(1))
                    if prop.group(2):
                        modifiers.append(prop.group(2))
                    
                    prop_type = prop.group(3)
                    prop_name = prop.group(4)
                    
                    modifier_str = " ".join(modifiers) if modifiers else ""
                    prop_list.append(f"- `{modifier_str} {prop_type} {prop_name}`".strip())
                
                if prop_list:
                    self.report_lines.append("**Properties:**\n")
                    self.report_lines.extend(prop_list)
                    self.report_lines.append("")
                
                # Extract methods
                method_pattern = r'^\s*public\s+(\w+)\s+(\w+)\((.*?)\)'
                methods = re.finditer(method_pattern, class_content, re.MULTILINE)
                
                method_list = []
                for method in methods:
                    return_type = method.group(1)
                    method_name = method.group(2)
                    params = method.group(3)
                    
                    method_list.append(f"- `{return_type} {method_name}({params})`")
                
                if method_list:
                    self.report_lines.append("**Methods:**\n")
                    self.report_lines.extend(method_list)
                    self.report_lines.append("")
        
        except Exception as e:
            self.report_lines.append(f"*Error parsing BOM file: {e}*\n")
    
    def _add_vocabulary(self):
        """Add vocabulary section."""
        self.report_lines.extend([
            f"## 9. Business Vocabulary",
            f"",
        ])
        
        bom_dir = self.project_path / "bom"
        if bom_dir.exists():
            voc_files = list(bom_dir.glob("*.voc"))
            
            for voc_file in voc_files:
                self._parse_vocabulary_file(voc_file)
        else:
            self.report_lines.append("*No vocabulary files found.*\n")
        
        self.report_lines.append("")
    
    def _parse_vocabulary_file(self, voc_file):
        """Parse a vocabulary file."""
        self.report_lines.append(f"### {voc_file.stem}")
        self.report_lines.append("")
        
        try:
            with open(voc_file, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            
            current_class = None
            class_phrases = {}
            
            for line in lines:
                line = line.strip()
                
                # Skip comments and empty lines
                if not line or line.startswith('#'):
                    continue
                
                # Parse vocabulary entries
                if '#concept.label' in line:
                    match = re.match(r'([\w.]+)#concept\.label\s*=\s*(.+)', line)
                    if match:
                        class_name = match.group(1).split('.')[-1]
                        label = match.group(2).strip()
                        current_class = class_name
                        if current_class not in class_phrases:
                            class_phrases[current_class] = {'label': label, 'phrases': []}
                
                elif '#phrase.' in line:
                    match = re.match(r'([\w.]+)\.(\w+)#phrase\.(navigation|action)\s*=\s*(.+)', line)
                    if match:
                        class_name = match.group(1).split('.')[-1]
                        property_name = match.group(2)
                        phrase_type = match.group(3)
                        phrase_text = match.group(4).strip()
                        
                        if class_name not in class_phrases:
                            class_phrases[class_name] = {'label': class_name, 'phrases': []}
                        
                        class_phrases[class_name]['phrases'].append({
                            'property': property_name,
                            'type': phrase_type,
                            'text': phrase_text
                        })
            
            # Output organized vocabulary
            for class_name, data in class_phrases.items():
                self.report_lines.append(f"#### {class_name} - *{data['label']}*\n")
                
                if data['phrases']:
                    self.report_lines.append("| Property | Type | Phrase |")
                    self.report_lines.append("|----------|------|--------|")
                    
                    for phrase in data['phrases']:
                        prop = phrase['property']
                        ptype = phrase['type'].capitalize()
                        text = phrase['text']
                        self.report_lines.append(f"| `{prop}` | {ptype} | {text} |")
                    
                    self.report_lines.append("")
        
        except Exception as e:
            self.report_lines.append(f"*Error parsing vocabulary file: {e}*\n")
    
    def _add_variables(self):
        """Add variables section."""
        self.report_lines.extend([
            f"## 7. Rule Variables",
            f"",
        ])
        
        rules_dir = self.project_path / "rules"
        var_files = []
        if rules_dir.exists():
            var_files = list(rules_dir.glob("*.var"))
            
            for var_file in var_files:
                self._parse_variable_file(var_file)
        
        if not var_files:
            self.report_lines.append("*No variable files found.*\n")
        
        self.report_lines.append("")
    
    def _parse_variable_file(self, var_file):
        """Parse a variable definition file."""
        try:
            tree = ET.parse(var_file)
            root = tree.getroot()
            
            # Find name element (ignore namespace)
            name = None
            for elem in root.iter():
                if elem.tag.endswith('name'):
                    name = elem
                    break
            
            self.report_lines.append(f"### {name.text if name is not None else var_file.stem}")
            self.report_lines.append("")
            
            # Get variables - they are attributes of the root element
            variables = []
            for elem in root.iter():
                if elem.tag.endswith('variables'):
                    variables.append(elem)
            
            if variables:
                self.report_lines.append("| Variable Name | Type | Verbalization |")
                self.report_lines.append("|---------------|------|---------------|")
                
                for var in variables:
                    var_name = var.get('name', 'N/A')
                    var_type = var.get('type', 'N/A')
                    var_verb = var.get('verbalization', 'N/A')
                    
                    self.report_lines.append(f"| `{var_name}` | `{var_type}` | {var_verb} |")
                
                self.report_lines.append("")
            else:
                self.report_lines.append("*No variables defined.*\n")
        
        except Exception as e:
            self.report_lines.append(f"*Error parsing variable file: {e}*\n")
    
    def _add_ruleflow(self):
        """Add ruleflow section."""
        self.report_lines.extend([
            f"## 4. Ruleflow",
            f"",
        ])
        
        rules_dir = self.project_path / "rules"
        rfl_files = []
        if rules_dir.exists():
            rfl_files = list(rules_dir.glob("*.rfl"))
            
            for rfl_file in rfl_files:
                self._parse_ruleflow_file(rfl_file)
        
        if not rfl_files:
            self.report_lines.append("*No ruleflow files found.*\n")
        
        self.report_lines.append("")
    
    def _parse_ruleflow_file(self, rfl_file):
        """Parse a ruleflow file."""
        try:
            tree = ET.parse(rfl_file)
            root = tree.getroot()
            
            # Find name element (ignore namespace)
            name = None
            for elem in root.iter():
                if elem.tag.endswith('name'):
                    name = elem
                    break
            
            self.report_lines.append(f"### {name.text if name is not None else rfl_file.stem}")
            self.report_lines.append("")
            
            # Parse the embedded Ruleflow XML
            rfmodel = None
            for elem in root.iter():
                if elem.tag.endswith('rfModel'):
                    rfmodel = elem
                    break
            
            if rfmodel is not None:
                # The rfModel contains child elements directly (not as text)
                # Find the Ruleflow element
                inner_root = None
                for child in rfmodel:
                    if child.tag.endswith('Ruleflow') or 'Ruleflow' in child.tag:
                        inner_root = child
                        break
                
                if inner_root is None:
                    self.report_lines.append("*No ruleflow content found.*\n")
                    return
                
                # Find all rule tasks (ignore namespace)
                tasks = []
                for elem in inner_root.iter():
                    if elem.tag.endswith('RuleTask'):
                        tasks.append(elem)
                
                if tasks:
                    self.report_lines.append("**Execution Flow:**\n")
                    self.report_lines.append("| Step | Task | Execution Mode | Rule Package |")
                    self.report_lines.append("|------|------|----------------|--------------|")
                    
                    # Get resources for labels
                    resources = None
                    for elem in inner_root.iter():
                        if elem.tag.endswith('Resources'):
                            resources = elem
                            break
                    
                    for idx, task in enumerate(tasks, 1):
                        task_id = task.get('Identifier', 'N/A')
                        exec_mode = task.get('ExecutionMode', 'N/A')
                        
                        # Find package name
                        package = None
                        for elem in task.iter():
                            if elem.tag.endswith('Package'):
                                package = elem
                                break
                        package_name = package.get('Name', 'N/A') if package is not None else 'N/A'
                        
                        # Get label from resources
                        label = task_id
                        if resources is not None:
                            for data in resources.iter():
                                if data.tag.endswith('Data') and data.get('Name') == f'node_{idx}#label':
                                    label = data.text if data.text else task_id
                                    break
                        
                        self.report_lines.append(f"| {idx} | {label} | `{exec_mode}` | `{package_name}` |")
                    
                    self.report_lines.append("")
                    
                    # Add execution mode explanation
                    self.report_lines.extend([
                        "**Execution Modes:**",
                        "",
                        "- **Fastpath**: Sequential rule execution where order matters. Rules are evaluated in the order they appear.",
                        "- **RetePlus**: Rete algorithm-based execution with pattern matching. Order-independent evaluation.",
                        "",
                    ])
                else:
                    self.report_lines.append("*No rule tasks found in ruleflow.*\n")
            else:
                self.report_lines.append("*No ruleflow model found.*\n")
        
        except Exception as e:
            self.report_lines.append(f"*Error parsing ruleflow file: {e}*\n")
    
    def _add_rules(self):
        """Add rules section (display only, metrics already collected)."""
        self.report_lines.extend([
            f"## 5. Business Rules",
            f"",
        ])
        
        rules_dir = self.project_path / "rules"
        rule_packages = []
        if rules_dir.exists():
            # Find all rule packages (subdirectories)
            rule_packages = [d for d in rules_dir.iterdir() if d.is_dir()]
            
            for package in sorted(rule_packages):
                self._parse_rule_package(package)
        
        if not rule_packages:
            self.report_lines.append("*No rule packages found.*\n")
        
        self.report_lines.append("")
    
    def _parse_rule_package(self, package_dir):
        """Parse all rules in a package (display only, metrics already collected)."""
        package_name = package_dir.name
        
        self.report_lines.append(f"### Package: `{package_name}`")
        self.report_lines.append("")
        
        # Find all .brl files
        brl_files = sorted(package_dir.glob("*.brl"))
        
        if not brl_files:
            self.report_lines.append("*No rules found in this package.*\n")
            return
        
        for brl_file in brl_files:
            self._parse_rule_file(brl_file)
    
    def _parse_rule_file(self, brl_file):
        """Parse a single rule file (display only, metrics already collected)."""
        try:
            tree = ET.parse(brl_file)
            root = tree.getroot()
            
            # Find name and definition elements (ignore namespace)
            name = None
            definition = None
            for elem in root.iter():
                if elem.tag.endswith('name') and name is None:
                    name = elem
                elif elem.tag.endswith('definition'):
                    definition = elem
            
            rule_name = name.text if name is not None else brl_file.stem
            
            self.report_lines.append(f"#### Rule: `{rule_name}`")
            self.report_lines.append("")
            
            if definition is not None and definition.text:
                rule_text = definition.text.strip()
                
                # Format the rule text
                self.report_lines.append("```")
                self.report_lines.append(rule_text)
                self.report_lines.append("```")
                self.report_lines.append("")
                
                # Extract and display conditions and actions
                self._extract_rule_logic(rule_text)
            else:
                self.report_lines.append("*No rule definition found.*\n")
        
        except Exception as e:
            self.report_lines.append(f"*Error parsing rule file: {e}*\n")
    
    def _extract_rule_logic(self, rule_text):
        """Extract conditions and actions from rule text."""
        # Split by 'then' keyword
        parts = rule_text.split('then', 1)
        
        if len(parts) == 2:
            conditions_part = parts[0].replace('if', '').strip()
            actions_part = parts[1].strip()
            
            # Extract conditions
            conditions = [c.strip() for c in conditions_part.split('and') if c.strip()]
            
            if conditions:
                self.report_lines.append("**Conditions:**")
                self.report_lines.append("")
                for cond in conditions:
                    self.report_lines.append(f"- {cond}")
                self.report_lines.append("")
            
            # Extract actions (split by semicolon)
            actions = [a.strip() for a in actions_part.split(';') if a.strip()]
            
            if actions:
                self.report_lines.append("**Actions:**")
                self.report_lines.append("")
                for action in actions:
                    self.report_lines.append(f"- {action}")
                self.report_lines.append("")
        
        self.report_lines.append("---")
        self.report_lines.append("")
    
    def _add_decision_tables(self):
        """Add decision tables section."""
        self.report_lines.extend([
            f"## 6. Decision Tables",
            f"",
        ])
        
        rules_dir = self.project_path / "rules"
        dta_files = []
        
        if rules_dir.exists():
            # Find all .dta files recursively
            dta_files = list(rules_dir.rglob("*.dta"))
        
        if dta_files:
            for dta_file in sorted(dta_files):
                self._parse_decision_table(dta_file)
        else:
            self.report_lines.append("*No decision tables found in this project.*\n")
        
        self.report_lines.append("")
    
    def _parse_decision_table(self, dta_file):
        """Parse a decision table file."""
        try:
            tree = ET.parse(dta_file)
            root = tree.getroot()
            
            name = root.find('.//{http://ilog.rules.studio/model/dt.ecore}name')
            
            # Track metrics
            self.quality_metrics['total_decision_tables'] += 1
            
            self.report_lines.append(f"### Decision Table: `{name.text if name is not None else dta_file.stem}`")
            self.report_lines.append("")
            
            # Parse the embedded DT XML
            definition = root.find('.//{http://ilog.rules.studio/model/dt.ecore}definition')
            
            if definition is not None and definition.text:
                inner_xml = definition.text.strip()
                inner_root = ET.fromstring(inner_xml)
                
                # Extract condition and action definitions
                cond_defs = inner_root.findall('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}ConditionDefinition')
                action_defs = inner_root.findall('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}ActionDefinition')
                
                # Get resources for headers
                resources = inner_root.find('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}Resources')
                
                # Build table structure
                headers = []
                
                # Add condition headers
                for idx, cond_def in enumerate(cond_defs):
                    cond_id = cond_def.get('Id', f'C{idx}')
                    header_text = self._get_resource_text(resources, f'Definitions({cond_id})#HeaderText', f'Condition {idx+1}')
                    headers.append(header_text)
                
                # Add action headers
                for idx, action_def in enumerate(action_defs):
                    action_id = action_def.get('Id', f'A{idx}')
                    header_text = self._get_resource_text(resources, f'Definitions({action_id})#HeaderText', f'Action {idx+1}')
                    headers.append(header_text)
                
                # Create table header
                self.report_lines.append("| " + " | ".join(headers) + " |")
                self.report_lines.append("|" + "|".join(["---" for _ in headers]) + "|")
                
                # Extract rows from Contents
                contents = inner_root.find('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}Contents')
                if contents is not None:
                    conditions = contents.findall('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}Condition')
                    
                    for condition in conditions:
                        row_values = []
                        
                        # Get condition values
                        expr = condition.find('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}Expression')
                        if expr is not None:
                            params = expr.findall('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}Param')
                            for param in params:
                                row_values.append(param.text if param.text else '')
                        
                        # Get action values
                        action_set = condition.find('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}ActionSet')
                        if action_set is not None:
                            actions = action_set.findall('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}Action')
                            for action in actions:
                                action_expr = action.find('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}Expression')
                                if action_expr is not None:
                                    action_params = action_expr.findall('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}Param')
                                    for param in action_params:
                                        row_values.append(param.text if param.text else '')
                        
                        if row_values:
                            self.report_lines.append("| " + " | ".join(row_values) + " |")
                
                self.report_lines.append("")
        
        except Exception as e:
            self.report_lines.append(f"*Error parsing decision table: {e}*\n")
    
    def _get_resource_text(self, resources, name, default):
        """Get resource text by name."""
        if resources is not None:
            for data in resources.findall('.//{http://schemas.ilog.com/Rules/7.0/DecisionTable}Data'):
                if data.get('Name') == name:
                    return data.text if data.text else default
        return default
    
    def _add_deployment_info(self):
        """Add deployment information section."""
        self.report_lines.extend([
            f"## 3. Deployment Configuration",
            f"",
        ])
        
        deployment_dir = self.project_path / "deployment"
        dop_files = []
        if deployment_dir.exists():
            dop_files = list(deployment_dir.glob("*.dop"))
            
            for dop_file in dop_files:
                self._parse_deployment_operation(dop_file)
        
        if not dop_files:
            self.report_lines.append("*No deployment operations found.*\n")
        
        self.report_lines.append("")
    
    def _parse_deployment_operation(self, dop_file):
        """Parse a deployment operation file."""
        try:
            tree = ET.parse(dop_file)
            root = tree.getroot()
            
            # Find name element (ignore namespace)
            name = None
            for elem in root.iter():
                if elem.tag.endswith('name'):
                    name = elem
                    break
            
            self.report_lines.append(f"### Operation: `{name.text if name is not None else dop_file.stem}`")
            self.report_lines.append("")
            
            # Extract operation attributes
            ruleset_name = root.get('rulesetName', 'N/A')
            using_ruleflow = root.get('usingRuleflow', 'false')
            ruleflow_name = root.get('ruleflowName', 'N/A')
            
            self.report_lines.extend([
                f"- **Ruleset Name:** `{ruleset_name}`",
                f"- **Using Ruleflow:** {using_ruleflow}",
                f"- **Ruleflow Name:** `{ruleflow_name}`",
                "",
            ])
            
            # Extract referenced variables
            ref_vars = []
            for elem in root.iter():
                if elem.tag.endswith('referencedVariables'):
                    ref_vars.append(elem)
            
            if ref_vars:
                self.report_lines.append("**Input/Output Parameters:**\n")
                self.report_lines.append("| Variable | Type | Direction |")
                self.report_lines.append("|----------|------|-----------|")
                
                for var in ref_vars:
                    var_name = var.get('variableName', 'N/A')
                    var_set = var.get('variableSetName', 'N/A')
                    direction = var.get('direction', 'IN')
                    
                    self.report_lines.append(f"| `{var_name}` | `{var_set}` | {direction} |")
                
                self.report_lines.append("")
            else:
                self.report_lines.append("*No parameters defined.*\n")
        
        except Exception as e:
            self.report_lines.append(f"*Error parsing deployment operation: {e}*\n")
    
    def _add_footer(self):
        """Add report footer."""
        self.report_lines.extend([
            "---",
            "",
            f"*Report generated by ODM Report Generator*",
            "",
        ])


def main():
    """Main entry point."""
    if len(sys.argv) < 2:
        print("Usage: python odm-report-generator.py <project-path> [output-file]")
        print("\nExample:")
        print('  python odm-report-generator.py "Mineral Classification" report.md')
        sys.exit(1)
    
    project_path = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else "odm-report.md"
    
    if not os.path.exists(project_path):
        print(f"Error: Project path '{project_path}' does not exist.")
        sys.exit(1)
    
    print(f"Generating report for project: {project_path}")
    
    generator = ODMReportGenerator(project_path)
    report = generator.generate_report()
    
    # Write report to file
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"Report generated successfully: {output_file}")
    print(f"Report size: {len(report)} characters")


if __name__ == "__main__":
    main()

# Made with Bob
