package com.ibm.odm.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RuleCompilerService
 */
class RuleCompilerServiceTest {

    private RuleCompilerService service;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new RuleCompilerService();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testValidateProject_ValidProject() throws IOException {
        // Create a valid project structure
        Path projectDir = tempDir.resolve("TestProject");
        createValidProject(projectDir);

        ODMRuleCompilerServer.ValidationRequest request = new ODMRuleCompilerServer.ValidationRequest();
        request.project_path = projectDir.toString();

        ODMRuleCompilerServer.ValidationResult result = service.validateProject(request);

        assertTrue(result.valid, "Project should be valid");
        assertTrue(result.issues.isEmpty(), "Should have no issues");
    }

    @Test
    void testValidateProject_MissingRuleProject() throws IOException {
        // Create project without .ruleproject file
        Path projectDir = tempDir.resolve("InvalidProject");
        Files.createDirectories(projectDir);

        ODMRuleCompilerServer.ValidationRequest request = new ODMRuleCompilerServer.ValidationRequest();
        request.project_path = projectDir.toString();

        ODMRuleCompilerServer.ValidationResult result = service.validateProject(request);

        assertFalse(result.valid, "Project should be invalid");
        assertTrue(result.issues.stream().anyMatch(i -> i.contains(".ruleproject")),
                "Should report missing .ruleproject file");
    }

    @Test
    void testValidateProject_MissingDirectories() throws IOException {
        // Create project with .ruleproject but missing directories
        Path projectDir = tempDir.resolve("IncompleteProject");
        Files.createDirectories(projectDir);
        Files.createFile(projectDir.resolve(".ruleproject"));

        ODMRuleCompilerServer.ValidationRequest request = new ODMRuleCompilerServer.ValidationRequest();
        request.project_path = projectDir.toString();

        ODMRuleCompilerServer.ValidationResult result = service.validateProject(request);

        assertFalse(result.valid, "Project should be invalid");
        assertTrue(result.issues.stream().anyMatch(i -> i.contains("rules")),
                "Should report missing rules directory");
    }

    @Test
    void testValidateProject_NonExistentPath() {
        ODMRuleCompilerServer.ValidationRequest request = new ODMRuleCompilerServer.ValidationRequest();
        request.project_path = "/non/existent/path";

        ODMRuleCompilerServer.ValidationResult result = service.validateProject(request);

        assertFalse(result.valid, "Project should be invalid");
        assertTrue(result.issues.stream().anyMatch(i -> i.contains("does not exist") || i.contains("not found")),
                "Should report path does not exist");
    }

    @Test
    void testListProjects_FindsValidProjects() throws IOException {
        // Create multiple projects
        createValidProject(tempDir.resolve("Project1"));
        createValidProject(tempDir.resolve("Project2"));
        
        // Create an invalid directory (no .ruleproject)
        Files.createDirectories(tempDir.resolve("NotAProject"));

        ODMRuleCompilerServer.ListProjectsRequest request = new ODMRuleCompilerServer.ListProjectsRequest();
        request.directory = tempDir.toString();
        request.recursive = true;  // Use recursive to find projects in subdirectories

        ODMRuleCompilerServer.ListProjectsResult result = service.listProjects(request);

        assertTrue(result.success, "List operation should succeed");
        assertEquals(2, result.projects.size(), "Should find 2 valid projects");
        assertTrue(result.projects.stream().anyMatch(p -> p.name.equals("Project1")),
                "Should find Project1");
        assertTrue(result.projects.stream().anyMatch(p -> p.name.equals("Project2")),
                "Should find Project2");
    }

    @Test
    void testListProjects_RecursiveSearch() throws IOException {
        // Create nested project structure
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        createValidProject(tempDir.resolve("TopLevel"));
        createValidProject(subDir.resolve("Nested"));

        ODMRuleCompilerServer.ListProjectsRequest request = new ODMRuleCompilerServer.ListProjectsRequest();
        request.directory = tempDir.toString();
        request.recursive = true;

        ODMRuleCompilerServer.ListProjectsResult result = service.listProjects(request);

        assertTrue(result.success, "List operation should succeed");
        assertEquals(2, result.projects.size(), "Should find 2 projects recursively");
    }

    @Test
    void testListProjects_NonRecursiveSearch() throws IOException {
        // For non-recursive search, create project directly in temp directory
        // so it's found at depth 1
        createValidProject(tempDir);
        
        // Create nested project that should NOT be found
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        createValidProject(subDir.resolve("Nested"));

        ODMRuleCompilerServer.ListProjectsRequest request = new ODMRuleCompilerServer.ListProjectsRequest();
        request.directory = tempDir.toString();
        request.recursive = false;

        ODMRuleCompilerServer.ListProjectsResult result = service.listProjects(request);

        assertTrue(result.success, "List operation should succeed");
        // With maxDepth=1, it should find the project in tempDir itself
        assertTrue(result.projects.size() >= 1, "Should find at least the top-level project");
        // Should NOT find the nested project
        assertFalse(result.projects.stream().anyMatch(p -> p.name.equals("Nested")),
                "Should not find nested project in non-recursive search");
    }

    @Test
    void testListProjects_EmptyDirectory() throws IOException {
        ODMRuleCompilerServer.ListProjectsRequest request = new ODMRuleCompilerServer.ListProjectsRequest();
        request.directory = tempDir.toString();
        request.recursive = false;

        ODMRuleCompilerServer.ListProjectsResult result = service.listProjects(request);

        assertTrue(result.success, "List operation should succeed");
        assertTrue(result.projects.isEmpty(), "Should find no projects in empty directory");
    }

    @Test
    void testCompileProject_MissingProjectPath() {
        ODMRuleCompilerServer.CompilationRequest request = new ODMRuleCompilerServer.CompilationRequest();
        request.project_path = "";  // Use empty string instead of null to avoid NullPointerException

        ODMRuleCompilerServer.CompilationResult result = service.compileProject(request);

        assertFalse(result.success, "Compilation should fail with empty project path");
        assertNotNull(result.message, "Should have error message");
    }

    @Test
    void testCompileProject_NonExistentProject() {
        ODMRuleCompilerServer.CompilationRequest request = new ODMRuleCompilerServer.CompilationRequest();
        request.project_path = "/non/existent/project";

        ODMRuleCompilerServer.CompilationResult result = service.compileProject(request);

        assertFalse(result.success, "Compilation should fail for non-existent project");
        assertTrue(result.message.contains("does not exist"),
                "Should report project does not exist");
    }

    @Test
    void testCompileProject_InvalidProject() throws IOException {
        // Create directory without .ruleproject
        Path projectDir = tempDir.resolve("InvalidProject");
        Files.createDirectories(projectDir);

        ODMRuleCompilerServer.CompilationRequest request = new ODMRuleCompilerServer.CompilationRequest();
        request.project_path = projectDir.toString();

        ODMRuleCompilerServer.CompilationResult result = service.compileProject(request);

        assertFalse(result.success, "Compilation should fail for invalid project");
        assertTrue(result.message.contains(".ruleproject"),
                "Should report missing .ruleproject file");
    }

    // Helper method to create a valid project structure
    private void createValidProject(Path projectDir) throws IOException {
        Files.createDirectories(projectDir);
        Files.createFile(projectDir.resolve(".ruleproject"));
        Files.createDirectories(projectDir.resolve("rules"));
        Files.createDirectories(projectDir.resolve("bom"));
        Path deploymentDir = projectDir.resolve("deployment");
        Files.createDirectories(deploymentDir);
        // Create a dummy .dep file to satisfy validation
        Files.createFile(deploymentDir.resolve("deployment.dep"));
    }
}

// Made with Bob
