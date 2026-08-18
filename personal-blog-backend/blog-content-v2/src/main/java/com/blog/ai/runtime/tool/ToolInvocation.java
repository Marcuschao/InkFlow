package com.blog.ai.runtime.tool;
import lombok.Data;
import java.util.LinkedHashMap;
import java.util.Map;
@Data public class ToolInvocation {
    private String runId; private String traceId; private String name; private String version;
    private Map<String, Object> arguments = new LinkedHashMap<>();
    private String idempotencyKey; private boolean userConfirmed; private boolean adminApproved;
}
