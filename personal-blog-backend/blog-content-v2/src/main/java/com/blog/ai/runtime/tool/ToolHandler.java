package com.blog.ai.runtime.tool;
import com.blog.ai.runtime.model.AgentExecutionContext;
public interface ToolHandler<T> { ToolDefinition definition(); Class<T> argumentType(); ToolResult execute(T arguments, AgentExecutionContext context); }
