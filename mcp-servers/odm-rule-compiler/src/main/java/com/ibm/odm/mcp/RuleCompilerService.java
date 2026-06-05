package com.ibm.odm.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for compiling ODM rule projects using the rule-compiler.jar
 * 
 * This service manages:
 * - Configuration file generation from MCP parameters
 * - Invocation of the rule compiler
 * - File system path handling in MCP context
 * - Compilation result parsing
 */
public class RuleCompilerService {
    private static final Logger logger = LoggerFactory.getLogger(RuleCompilerService.class);
    private static final String RULE_PROJECT_FILE = ".ruleproject";
    
    /**
     * Compile an ODM rule project
     */
    public ODMRuleCompilerServer.CompilationResult compileProject(ODMRuleCompilerServer.CompilationRequest request) {
        ODMRuleCompilerServer.CompilationResult result = new ODMRuleCompilerServer.CompilationResult();
        result.warnings = new ArrayList<>();
        result.errors = new ArrayList<>();
        
        try {
            // Validate project path
            Path projectPath = Paths.get(request.project_path);
            if (!Files.exists(projectPath)) {
                result.success = false;
                result.message = "Project path does not exist: " + request.project_path;
                result.errors.add(result.message);
                return result;
            }
            
            // Check if it's a valid rule project
            Path ruleProjectFile = projectPath.resolve(RULE_PROJECT_FILE);
            if (!Files.exists(ruleProjectFile)) {
                result.success = false;
                result.message = "Not a valid ODM rule project (missing .ruleproject file): " + request.project_path;
                result.errors.add(result.message);
                return result;
            }
            
            // Determine output path
            String outputPath = request.output_path;
            if (outputPath == null || outputPath.isEmpty()) {
                outputPath = projectPath.resolve("output").toString();
            }
            result.output_path = outputPath;
            
            // Create or use properties file
            Path propertiesFile;
            if (request.properties_file != null && !request.properties_file.isEmpty()) {
                propertiesFile = Paths.get(request.properties_file);
                if (!Files.exists(propertiesFile)) {
                    result.success = false;
                    result.message = "Properties file not found: " + request.properties_file;
                    result.errors.add(result.message);
                    return result;
                }
            } else {
                // Generate properties file from request parameters
                propertiesFile = generatePropertiesFile(request, projectPath, outputPath);
            }
            
            // Execute compilation
            logger.info("Compiling project: {} with properties: {}", projectPath, propertiesFile);
            CompilationOutput output = executeCompilation(propertiesFile);
            
            result.success = output.exitCode == 0;
            result.message = output.exitCode == 0 ? 
                "Compilation completed successfully" : 
                "Compilation failed with exit code: " + output.exitCode;
            
            // Parse output for warnings and errors
            parseCompilationOutput(output.stdout, output.stderr, result);
            
            logger.info("Compilation result: success={}, warnings={}, errors={}", 
                result.success, result.warnings.size(), result.errors.size());
            
        } catch (Exception e) {
            logger.error("Error during compilation", e);
            result.success = false;
            result.message = "Compilation error: " + e.getMessage();
            result.errors.add(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate an ODM rule project structure
     */
    public ODMRuleCompilerServer.ValidationResult validateProject(ODMRuleCompilerServer.ValidationRequest request) {
        ODMRuleCompilerServer.ValidationResult result = new ODMRuleCompilerServer.ValidationResult();
        result.issues = new ArrayList<>();
        
        try {
            Path projectPath = Paths.get(request.project_path);
            
            // Check if path exists
            if (!Files.exists(projectPath)) {
                result.valid = false;
                result.message = "Project path does not exist";
                result.issues.add("Path not found: " + request.project_path);
                return result;
            }
            
            // Check if it's a directory
            if (!Files.isDirectory(projectPath)) {
                result.valid = false;
                result.message = "Project path is not a directory";
                result.issues.add("Not a directory: " + request.project_path);
                return result;
            }
            
            // Check for .ruleproject file
            Path ruleProjectFile = projectPath.resolve(RULE_PROJECT_FILE);
            if (!Files.exists(ruleProjectFile)) {
                result.valid = false;
                result.message = "Missing .ruleproject file";
                result.issues.add("Required file not found: " + RULE_PROJECT_FILE);
                return result;
            }
            
            // Check for common directories
            checkDirectory(projectPath, "rules", result.issues);
            checkDirectory(projectPath, "bom", result.issues);
            checkDirectory(projectPath, "deployment", result.issues);
            
            // Check for deployment files
            Path deploymentDir = projectPath.resolve("deployment");
            if (Files.exists(deploymentDir)) {
                try (var stream = Files.list(deploymentDir)) {
                    long depCount = stream.filter(p -> p.toString().endsWith(".dep")).count();
                    if (depCount == 0) {
                        result.issues.add("No deployment (.dep) files found in deployment directory");
                    }
                }
            }
            
            result.valid = result.issues.isEmpty();
            result.message = result.valid ? 
                "Project structure is valid" : 
                "Project has " + result.issues.size() + " validation issue(s)";
            
        } catch (Exception e) {
            logger.error("Error validating project", e);
            result.valid = false;
            result.message = "Validation error: " + e.getMessage();
            result.issues.add(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * List ODM rule projects in a directory
     */
    public ODMRuleCompilerServer.ListProjectsResult listProjects(ODMRuleCompilerServer.ListProjectsRequest request) {
        ODMRuleCompilerServer.ListProjectsResult result = new ODMRuleCompilerServer.ListProjectsResult();
        result.projects = new ArrayList<>();
        
        try {
            Path directory = Paths.get(request.directory);
            
            if (!Files.exists(directory) || !Files.isDirectory(directory)) {
                result.success = false;
                return result;
            }
            
            int maxDepth = request.recursive ? Integer.MAX_VALUE : 1;
            
            try (var stream = Files.walk(directory, maxDepth)) {
                List<Path> ruleProjects = stream
                    .filter(p -> p.getFileName().toString().equals(RULE_PROJECT_FILE))
                    .map(Path::getParent)
                    .collect(Collectors.toList());
                
                for (Path projectPath : ruleProjects) {
                    ODMRuleCompilerServer.ProjectInfo info = new ODMRuleCompilerServer.ProjectInfo();
                    info.name = projectPath.getFileName().toString();
                    info.path = projectPath.toString();
                    info.type = "ODM Rule Project";
                    result.projects.add(info);
                }
            }
            
            result.success = true;
            logger.info("Found {} rule projects in {}", result.projects.size(), directory);
            
        } catch (Exception e) {
            logger.error("Error listing projects", e);
            result.success = false;
        }
        
        return result;
    }
    
    /**
     * Generate a properties file for compilation
     */
    private Path generatePropertiesFile(ODMRuleCompilerServer.CompilationRequest request, 
                                       Path projectPath, String outputPath) throws IOException {
        Path tempPropsFile = Files.createTempFile("odm-compile-", ".properties");
        
        Properties props = new Properties();
        props.setProperty("project", projectPath.toString());
        props.setProperty("output", outputPath);
        
        if (request.deployment_name != null && !request.deployment_name.isEmpty()) {
            props.setProperty("dep", request.deployment_name);
        }
        
        if (request.xom_classpath != null && !request.xom_classpath.isEmpty()) {
            props.setProperty("xom-classpath", request.xom_classpath);
        }
        
        try (FileWriter writer = new FileWriter(tempPropsFile.toFile())) {
            props.store(writer, "Generated by ODM Rule Compiler MCP Server");
        }
        
        logger.debug("Generated properties file: {}", tempPropsFile);
        return tempPropsFile;
    }
    
    /**
     * Execute the rule compiler
     */
    private CompilationOutput executeCompilation(Path propertiesFile) throws IOException, InterruptedException {
        // Find the rule-compiler.jar
        Path compilerJar = findCompilerJar();
        
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(compilerJar.toString());
        command.add("-config");
        command.add(propertiesFile.toString());
        
        logger.info("Executing: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        
        Process process = pb.start();
        
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());
        
        int exitCode = process.waitFor();
        
        return new CompilationOutput(exitCode, stdout, stderr);
    }
    
    /**
     * Find the rule-compiler.jar file
     */
    private Path findCompilerJar() throws IOException {
        // Try relative path from project
        Path relativeJar = Paths.get("../../buildcommand/rules-compiler/rules-compiler.jar");
        if (Files.exists(relativeJar)) {
            return relativeJar.toAbsolutePath();
        }
        
        // Try from current directory
        Path currentDirJar = Paths.get("buildcommand/rules-compiler/rules-compiler.jar");
        if (Files.exists(currentDirJar)) {
            return currentDirJar.toAbsolutePath();
        }
        
        throw new IOException("Could not find rule-compiler.jar. Please ensure it's in buildcommand/rules-compiler/");
    }
    
    /**
     * Read an input stream to string
     */
    private String readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    /**
     * Parse compilation output for warnings and errors
     */
    private void parseCompilationOutput(String stdout, String stderr, ODMRuleCompilerServer.CompilationResult result) {
        // Parse stdout for warnings and errors
        if (stdout != null && !stdout.isEmpty()) {
            String[] lines = stdout.split("\n");
            for (String line : lines) {
                String lower = line.toLowerCase();
                if (lower.contains("warning")) {
                    result.warnings.add(line.trim());
                } else if (lower.contains("error")) {
                    result.errors.add(line.trim());
                }
            }
        }
        
        // Parse stderr
        if (stderr != null && !stderr.isEmpty()) {
            String[] lines = stderr.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    result.errors.add(line.trim());
                }
            }
        }
    }
    
    /**
     * Check if a directory exists and add to issues if not
     */
    private void checkDirectory(Path projectPath, String dirName, List<String> issues) {
        Path dir = projectPath.resolve(dirName);
        if (!Files.exists(dir)) {
            issues.add("Missing recommended directory: " + dirName);
        }
    }
    
    /**
     * Compilation output holder
     */
    private static class CompilationOutput {
        final int exitCode;
        final String stdout;
        final String stderr;
        
        CompilationOutput(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}

// Made with Bob
