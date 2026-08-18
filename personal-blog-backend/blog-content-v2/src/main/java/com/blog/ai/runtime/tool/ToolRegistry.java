package com.blog.ai.runtime.tool;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Component
public class ToolRegistry {
    private final Map<String, ToolHandler<?>> handlers = new LinkedHashMap<>();
    public ToolRegistry(List<ToolHandler<?>> registered) {
        for (ToolHandler<?> handler : registered) {
            ToolDefinition d = handler.definition(); String key = key(d.name(), d.version());
            if (handlers.putIfAbsent(key, handler) != null) throw new IllegalStateException("Duplicate agent tool registration: " + key);
        }
    }
    public ToolHandler<?> require(String name, String version) {
        ToolHandler<?> handler = handlers.get(key(name, version));
        if (handler == null) throw new IllegalArgumentException("Unknown agent tool: " + name + "@" + version);
        return handler;
    }
    public Collection<ToolDefinition> definitions() { return handlers.values().stream().map(ToolHandler::definition).toList(); }
    private String key(String name, String version) { return name + "@" + version; }
}
