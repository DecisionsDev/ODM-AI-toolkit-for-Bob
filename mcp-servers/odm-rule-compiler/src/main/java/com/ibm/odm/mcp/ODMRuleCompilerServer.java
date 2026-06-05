package com.ibm.odm.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * MCP Server for IBM ODM Rule Compilation
 * 
 * This server provides tools to compile ODM rule projects using the rule-compiler.jar.
 * It implements the Model Context Protocol over stdio.
 */
public class ODMRuleCompilerServer {
    private static final Logger logger = LoggerFactory.getLogger(ODMRuleCompilerServer.class);
    private static final String SERVER_NAME = "odm-rule-compiler";
    private static final String SERVER_VERSION = "1.0.0";

    private final ObjectMapper objectMapper;
    private final RuleCompilerService compilerService;
    private final BufferedReader reader;
    private final BufferedWriter writer;

    public ODMRuleCompilerServer() {
        this.objectMapper = new ObjectMapper();
        this.compilerService = new RuleCompilerService();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new BufferedWriter(new OutputStreamWriter(System.out));
    }

    /**
     * Start the MCP server and process requests
     */
    public void start() throws IOException {
        logger.info("Starting ODM Rule Compiler MCP Server v{}", SERVER_VERSION);
        
        String line;
        while ((line = reader.readLine()) != null) {
            try {
                JsonNode request = objectMapper.readTree(line);
                JsonNode response = handleRequest(request);
                
                if (response != null) {
                    writer.write(objectMapper.writeValueAsString(response));
                    writer.newLine();
                    writer.flush();
                }
            } catch (Exception e) {
                logger.error("Error processing request", e);
                sendError(-32603, "Internal error: " + e.getMessage(), null);
            }
        }
    }

    /**
     * Handle an MCP request
     */
    private JsonNode handleRequest(JsonNode request) throws IOException {
        String method = request.path("method").asText();
        JsonNode params = request.path("params");
        JsonNode id = request.path("id");

        logger.debug("Handling request: method={}, id={}", method, id.isMissingNode() ? "null" : id);

        // Handle notifications (no id field) - these don't get responses
        if (id.isMissingNode() || id.isNull()) {
            if (method.startsWith("notifications/")) {
                logger.debug("Received notification: {}", method);
                return null; // No response for notifications
            }
        }

        try {
            switch (method) {
                case "initialize":
                    return handleInitialize(id);
                    
                case "tools/list":
                    return handleToolsList(id);
                    
                case "tools/call":
                    return handleToolsCall(params, id);
                
                case "resources/list":
                    return handleResourcesList(id);
                
                case "resources/templates/list":
                    return handleResourcesTemplatesList(id);
                    
                case "prompts/list":
                    return handlePromptsList(id);
                    
                default:
                    return createErrorResponse(id, -32601, "Method not found: " + method);
            }
        } catch (Exception e) {
            logger.error("Error handling method: " + method, e);
            return createErrorResponse(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Handle initialize request
     */
    private JsonNode handleInitialize(JsonNode id) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);
        
        ObjectNode capabilities = result.putObject("capabilities");
        ObjectNode tools = capabilities.putObject("tools");
        tools.put("listChanged", false);
        
        return createSuccessResponse(id, result);
    }

    /**
     * Handle resources/list request
     */
    private JsonNode handleResourcesList(JsonNode id) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode resources = result.putArray("resources");
        // Return empty resources list since we don't provide resources
        return createSuccessResponse(id, result);
    }

    /**
     * Handle resources/templates/list request
     */
    private JsonNode handleResourcesTemplatesList(JsonNode id) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode resourceTemplates = result.putArray("resourceTemplates");
        // Return empty resource templates list
        return createSuccessResponse(id, result);
    }

    /**
     * Handle prompts/list request
     */
    private JsonNode handlePromptsList(JsonNode id) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode prompts = result.putArray("prompts");
        // Return empty prompts list since we don't provide prompts
        return createSuccessResponse(id, result);
    }

    /**
     * Handle tools/list request
     */
    private JsonNode handleToolsList(JsonNode id) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode tools = result.putArray("tools");
        
        // Tool 1: compile_rule_project
        ObjectNode compileTool = tools.addObject();
        compileTool.put("name", "compile_rule_project");
        compileTool.put("description", "Compile an IBM ODM rule project using the rule-compiler");
        ObjectNode compileSchema = compileTool.putObject("inputSchema");
        compileSchema.put("type", "object");
        ObjectNode compileProps = compileSchema.putObject("properties");
        
        ObjectNode projectPath = compileProps.putObject("project_path");
        projectPath.put("type", "string");
        projectPath.put("description", "Path to the rule project directory");
        
        ObjectNode outputPath = compileProps.putObject("output_path");
        outputPath.put("type", "string");
        outputPath.put("description", "Path for compilation output (optional, defaults to project/output)");
        
        ObjectNode deploymentName = compileProps.putObject("deployment_name");
        deploymentName.put("type", "string");
        deploymentName.put("description", "Name of the deployment to compile (optional)");
        
        ObjectNode xomClasspath = compileProps.putObject("xom_classpath");
        xomClasspath.put("type", "string");
        xomClasspath.put("description", "Classpath for XOM jars (optional)");
        
        ObjectNode propertiesFile = compileProps.putObject("properties_file");
        propertiesFile.put("type", "string");
        propertiesFile.put("description", "Path to properties file with compilation settings (optional)");
        
        ArrayNode compileRequired = compileSchema.putArray("required");
        compileRequired.add("project_path");
        
        // Tool 2: validate_project
        ObjectNode validateTool = tools.addObject();
        validateTool.put("name", "validate_project");
        validateTool.put("description", "Validate an ODM rule project structure and configuration");
        ObjectNode validateSchema = validateTool.putObject("inputSchema");
        validateSchema.put("type", "object");
        ObjectNode validateProps = validateSchema.putObject("properties");
        
        ObjectNode validateProjectPath = validateProps.putObject("project_path");
        validateProjectPath.put("type", "string");
        validateProjectPath.put("description", "Path to the rule project directory");
        
        ArrayNode validateRequired = validateSchema.putArray("required");
        validateRequired.add("project_path");
        
        // Tool 3: list_projects
        ObjectNode listTool = tools.addObject();
        listTool.put("name", "list_projects");
        listTool.put("description", "List available ODM rule projects in a directory");
        ObjectNode listSchema = listTool.putObject("inputSchema");
        listSchema.put("type", "object");
        ObjectNode listProps = listSchema.putObject("properties");
        
        ObjectNode directory = listProps.putObject("directory");
        directory.put("type", "string");
        directory.put("description", "Path to search for rule projects");
        
        ObjectNode recursive = listProps.putObject("recursive");
        recursive.put("type", "boolean");
        recursive.put("description", "Whether to search recursively (default: false)");
        
        ArrayNode listRequired = listSchema.putArray("required");
        listRequired.add("directory");
        
        return createSuccessResponse(id, result);
    }

    /**
     * Handle tools/call request
     */
    private JsonNode handleToolsCall(JsonNode params, JsonNode id) throws IOException {
        String toolName = params.path("name").asText();
        JsonNode arguments = params.path("arguments");
        
        logger.info("Calling tool: {}", toolName);
        
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode content = result.putArray("content");
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        
        String responseText;
        switch (toolName) {
            case "compile_rule_project":
                responseText = compileRuleProject(arguments);
                break;
                
            case "validate_project":
                responseText = validateProject(arguments);
                break;
                
            case "list_projects":
                responseText = listProjects(arguments);
                break;
                
            default:
                return createErrorResponse(id, -32602, "Unknown tool: " + toolName);
        }
        
        textContent.put("text", responseText);
        return createSuccessResponse(id, result);
    }

    /**
     * Compile a rule project
     */
    private String compileRuleProject(JsonNode arguments) throws IOException {
        CompilationRequest request = objectMapper.treeToValue(arguments, CompilationRequest.class);
        CompilationResult result = compilerService.compileProject(request);
        return objectMapper.writeValueAsString(result);
    }

    /**
     * Validate a project
     */
    private String validateProject(JsonNode arguments) throws IOException {
        ValidationRequest request = objectMapper.treeToValue(arguments, ValidationRequest.class);
        ValidationResult result = compilerService.validateProject(request);
        return objectMapper.writeValueAsString(result);
    }

    /**
     * List projects
     */
    private String listProjects(JsonNode arguments) throws IOException {
        ListProjectsRequest request = objectMapper.treeToValue(arguments, ListProjectsRequest.class);
        ListProjectsResult result = compilerService.listProjects(request);
        return objectMapper.writeValueAsString(result);
    }

    /**
     * Create a success response
     */
    private JsonNode createSuccessResponse(JsonNode id, ObjectNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    /**
     * Create an error response
     */
    private JsonNode createErrorResponse(JsonNode id, int code, String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        
        return response;
    }

    /**
     * Send an error message
     */
    private void sendError(int code, String message, JsonNode id) {
        try {
            JsonNode error = createErrorResponse(id, code, message);
            writer.write(objectMapper.writeValueAsString(error));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            logger.error("Failed to send error", e);
        }
    }

    public static void main(String[] args) {
        try {
            ODMRuleCompilerServer server = new ODMRuleCompilerServer();
            server.start();
        } catch (Exception e) {
            logger.error("Fatal error starting server", e);
            System.exit(1);
        }
    }

    // Request/Response DTOs
    static class CompilationRequest {
        public String project_path;
        public String output_path;
        public String deployment_name;
        public String xom_classpath;
        public String properties_file;
    }

    static class CompilationResult {
        public boolean success;
        public String message;
        public String output_path;
        public List<String> warnings = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
    }

    static class ValidationRequest {
        public String project_path;
    }

    static class ValidationResult {
        public boolean valid;
        public String message;
        public List<String> issues = new ArrayList<>();
    }

    static class ListProjectsRequest {
        public String directory;
        public boolean recursive = false;
    }

    static class ListProjectsResult {
        public boolean success;
        public List<ProjectInfo> projects = new ArrayList<>();
    }

    static class ProjectInfo {
        public String name;
        public String path;
        public String type;
    }
}

// Made with Bob
