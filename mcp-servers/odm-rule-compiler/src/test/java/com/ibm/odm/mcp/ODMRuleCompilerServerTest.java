package com.ibm.odm.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ODMRuleCompilerServer MCP protocol handling
 */
class ODMRuleCompilerServerTest {

    private ObjectMapper objectMapper;
    private ODMRuleCompilerServer server;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        server = new ODMRuleCompilerServer();
    }

    @Test
    void testHandleInitialize() throws Exception {
        // Create initialize request
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 0);
        request.put("method", "initialize");
        request.putObject("params")
                .put("protocolVersion", "2024-11-05")
                .putObject("clientInfo")
                .put("name", "test-client")
                .put("version", "1.0.0");

        // Use reflection to call handleRequest
        Method handleRequest = ODMRuleCompilerServer.class.getDeclaredMethod("handleRequest", JsonNode.class);
        handleRequest.setAccessible(true);
        JsonNode response = (JsonNode) handleRequest.invoke(server, request);

        assertNotNull(response, "Response should not be null");
        assertEquals("2.0", response.get("jsonrpc").asText(), "Should have jsonrpc version");
        assertEquals(0, response.get("id").asInt(), "Should have matching id");
        
        JsonNode result = response.get("result");
        assertNotNull(result, "Should have result");
        assertEquals("2024-11-05", result.get("protocolVersion").asText(), "Should return protocol version");
        
        JsonNode serverInfo = result.get("serverInfo");
        assertNotNull(serverInfo, "Should have serverInfo");
        assertEquals("odm-rule-compiler", serverInfo.get("name").asText(), "Should have server name");
        assertEquals("1.0.0", serverInfo.get("version").asText(), "Should have server version");
        
        JsonNode capabilities = result.get("capabilities");
        assertNotNull(capabilities, "Should have capabilities");
        assertTrue(capabilities.has("tools"), "Should have tools capability");
    }

    @Test
    void testHandleToolsList() throws Exception {
        // Create tools/list request
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "tools/list");

        Method handleRequest = ODMRuleCompilerServer.class.getDeclaredMethod("handleRequest", JsonNode.class);
        handleRequest.setAccessible(true);
        JsonNode response = (JsonNode) handleRequest.invoke(server, request);

        assertNotNull(response, "Response should not be null");
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals(1, response.get("id").asInt());
        
        JsonNode result = response.get("result");
        assertNotNull(result, "Should have result");
        
        JsonNode tools = result.get("tools");
        assertNotNull(tools, "Should have tools array");
        assertTrue(tools.isArray(), "Tools should be an array");
        assertEquals(3, tools.size(), "Should have 3 tools");
        
        // Verify tool names
        boolean hasCompile = false;
        boolean hasValidate = false;
        boolean hasList = false;
        
        for (JsonNode tool : tools) {
            String name = tool.get("name").asText();
            if ("compile_rule_project".equals(name)) hasCompile = true;
            if ("validate_project".equals(name)) hasValidate = true;
            if ("list_projects".equals(name)) hasList = true;
            
            // Verify tool structure
            assertNotNull(tool.get("description"), "Tool should have description");
            assertNotNull(tool.get("inputSchema"), "Tool should have inputSchema");
        }
        
        assertTrue(hasCompile, "Should have compile_rule_project tool");
        assertTrue(hasValidate, "Should have validate_project tool");
        assertTrue(hasList, "Should have list_projects tool");
    }

    @Test
    void testHandleResourcesList() throws Exception {
        // Create resources/list request
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 2);
        request.put("method", "resources/list");

        Method handleRequest = ODMRuleCompilerServer.class.getDeclaredMethod("handleRequest", JsonNode.class);
        handleRequest.setAccessible(true);
        JsonNode response = (JsonNode) handleRequest.invoke(server, request);

        assertNotNull(response, "Response should not be null");
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals(2, response.get("id").asInt());
        
        JsonNode result = response.get("result");
        assertNotNull(result, "Should have result");
        
        JsonNode resources = result.get("resources");
        assertNotNull(resources, "Should have resources array");
        assertTrue(resources.isArray(), "Resources should be an array");
        assertEquals(0, resources.size(), "Should have empty resources array");
    }

    @Test
    void testHandleResourcesTemplatesList() throws Exception {
        // Create resources/templates/list request
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 3);
        request.put("method", "resources/templates/list");

        Method handleRequest = ODMRuleCompilerServer.class.getDeclaredMethod("handleRequest", JsonNode.class);
        handleRequest.setAccessible(true);
        JsonNode response = (JsonNode) handleRequest.invoke(server, request);

        assertNotNull(response, "Response should not be null");
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals(3, response.get("id").asInt());
        
        JsonNode result = response.get("result");
        assertNotNull(result, "Should have result");
        
        JsonNode resourceTemplates = result.get("resourceTemplates");
        assertNotNull(resourceTemplates, "Should have resourceTemplates array");
        assertTrue(resourceTemplates.isArray(), "ResourceTemplates should be an array");
        assertEquals(0, resourceTemplates.size(), "Should have empty resourceTemplates array");
    }

    @Test
    void testHandlePromptsList() throws Exception {
        // Create prompts/list request
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 4);
        request.put("method", "prompts/list");

        Method handleRequest = ODMRuleCompilerServer.class.getDeclaredMethod("handleRequest", JsonNode.class);
        handleRequest.setAccessible(true);
        JsonNode response = (JsonNode) handleRequest.invoke(server, request);

        assertNotNull(response, "Response should not be null");
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals(4, response.get("id").asInt());
        
        JsonNode result = response.get("result");
        assertNotNull(result, "Should have result");
        
        JsonNode prompts = result.get("prompts");
        assertNotNull(prompts, "Should have prompts array");
        assertTrue(prompts.isArray(), "Prompts should be an array");
        assertEquals(0, prompts.size(), "Should have empty prompts array");
    }

    @Test
    void testHandleNotification() throws Exception {
        // Create notification (no id field)
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", "notifications/initialized");

        Method handleRequest = ODMRuleCompilerServer.class.getDeclaredMethod("handleRequest", JsonNode.class);
        handleRequest.setAccessible(true);
        JsonNode response = (JsonNode) handleRequest.invoke(server, request);

        assertNull(response, "Notifications should not return a response");
    }

    @Test
    void testHandleUnknownMethod() throws Exception {
        // Create request with unknown method
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 99);
        request.put("method", "unknown/method");

        Method handleRequest = ODMRuleCompilerServer.class.getDeclaredMethod("handleRequest", JsonNode.class);
        handleRequest.setAccessible(true);
        JsonNode response = (JsonNode) handleRequest.invoke(server, request);

        assertNotNull(response, "Response should not be null");
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals(99, response.get("id").asInt());
        
        JsonNode error = response.get("error");
        assertNotNull(error, "Should have error");
        assertEquals(-32601, error.get("code").asInt(), "Should have method not found error code");
        assertTrue(error.get("message").asText().contains("Method not found"),
                "Error message should indicate method not found");
    }

    @Test
    void testCreateSuccessResponse() throws Exception {
        ObjectNode resultData = objectMapper.createObjectNode();
        resultData.put("test", "value");
        
        Method createSuccessResponse = ODMRuleCompilerServer.class.getDeclaredMethod(
                "createSuccessResponse", JsonNode.class, ObjectNode.class);
        createSuccessResponse.setAccessible(true);
        
        JsonNode idNode = objectMapper.valueToTree(42);
        JsonNode response = (JsonNode) createSuccessResponse.invoke(server, idNode, resultData);

        assertNotNull(response, "Response should not be null");
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals(42, response.get("id").asInt());
        assertNotNull(response.get("result"), "Should have result");
        assertEquals("value", response.get("result").get("test").asText());
    }

    @Test
    void testCreateErrorResponse() throws Exception {
        Method createErrorResponse = ODMRuleCompilerServer.class.getDeclaredMethod(
                "createErrorResponse", JsonNode.class, int.class, String.class);
        createErrorResponse.setAccessible(true);
        
        JsonNode idNode = objectMapper.valueToTree(42);
        JsonNode response = (JsonNode) createErrorResponse.invoke(
                server, idNode, -32600, "Invalid Request");

        assertNotNull(response, "Response should not be null");
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals(42, response.get("id").asInt());
        
        JsonNode error = response.get("error");
        assertNotNull(error, "Should have error");
        assertEquals(-32600, error.get("code").asInt());
        assertEquals("Invalid Request", error.get("message").asText());
    }

    @Test
    void testCompilationRequestDTO() {
        ODMRuleCompilerServer.CompilationRequest request = new ODMRuleCompilerServer.CompilationRequest();
        request.project_path = "/path/to/project";
        request.output_path = "/path/to/output";
        request.deployment_name = "TestDeployment";
        request.xom_classpath = "/path/to/xom.jar";
        request.properties_file = "/path/to/config.properties";

        assertEquals("/path/to/project", request.project_path);
        assertEquals("/path/to/output", request.output_path);
        assertEquals("TestDeployment", request.deployment_name);
        assertEquals("/path/to/xom.jar", request.xom_classpath);
        assertEquals("/path/to/config.properties", request.properties_file);
    }

    @Test
    void testCompilationResultDTO() {
        ODMRuleCompilerServer.CompilationResult result = new ODMRuleCompilerServer.CompilationResult();
        result.success = true;
        result.message = "Compilation successful";
        result.output_path = "/path/to/output";
        result.warnings.add("Warning 1");
        result.errors.add("Error 1");

        assertTrue(result.success);
        assertEquals("Compilation successful", result.message);
        assertEquals("/path/to/output", result.output_path);
        assertEquals(1, result.warnings.size());
        assertEquals(1, result.errors.size());
    }

    @Test
    void testValidationRequestDTO() {
        ODMRuleCompilerServer.ValidationRequest request = new ODMRuleCompilerServer.ValidationRequest();
        request.project_path = "/path/to/project";

        assertEquals("/path/to/project", request.project_path);
    }

    @Test
    void testValidationResultDTO() {
        ODMRuleCompilerServer.ValidationResult result = new ODMRuleCompilerServer.ValidationResult();
        result.valid = true;
        result.message = "Valid project";
        result.issues.add("Issue 1");

        assertTrue(result.valid);
        assertEquals("Valid project", result.message);
        assertEquals(1, result.issues.size());
    }

    @Test
    void testListProjectsRequestDTO() {
        ODMRuleCompilerServer.ListProjectsRequest request = new ODMRuleCompilerServer.ListProjectsRequest();
        request.directory = "/path/to/directory";
        request.recursive = true;

        assertEquals("/path/to/directory", request.directory);
        assertTrue(request.recursive);
    }

    @Test
    void testListProjectsResultDTO() {
        ODMRuleCompilerServer.ListProjectsResult result = new ODMRuleCompilerServer.ListProjectsResult();
        
        ODMRuleCompilerServer.ProjectInfo project = new ODMRuleCompilerServer.ProjectInfo();
        project.name = "TestProject";
        project.path = "/path/to/project";
        project.type = "ODM Rule Project";
        
        result.projects.add(project);
        result.success = true;

        assertTrue(result.success);
        assertEquals(1, result.projects.size());
        assertEquals("TestProject", result.projects.get(0).name);
        assertEquals("/path/to/project", result.projects.get(0).path);
        assertEquals("ODM Rule Project", result.projects.get(0).type);
    }
}

// Made with Bob
