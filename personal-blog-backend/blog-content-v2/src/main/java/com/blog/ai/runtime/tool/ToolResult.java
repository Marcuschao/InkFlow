package com.blog.ai.runtime.tool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
@Data @NoArgsConstructor @AllArgsConstructor
public class ToolResult { private boolean success; private Object data; private String errorCode; private String errorMessage; private Map<String, Object> metadata; }
