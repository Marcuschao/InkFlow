package com.blog.ai.runtime.tool;

import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.runtime.model.AgentErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Component
public class ToolSchemaValidator {
    private final ObjectMapper json;
    public ToolSchemaValidator(ObjectMapper json){this.json=json;}

    public void validate(String schemaJson, Map<String,Object> arguments){
        try{
            JsonNode schema=json.readTree(schemaJson);
            if(!"object".equals(schema.path("type").asText()))throw invalid("root schema type must be object");
            Set<String> required=new HashSet<>(); schema.path("required").forEach(n->required.add(n.asText()));
            for(String field:required)if(!arguments.containsKey(field)||arguments.get(field)==null)throw invalid("missing required field: "+field);
            JsonNode properties=schema.path("properties");
            Iterator<Map.Entry<String,JsonNode>> fields=properties.fields();
            while(fields.hasNext()){
                var entry=fields.next(); Object value=arguments.get(entry.getKey()); if(value==null)continue;
                String type=entry.getValue().path("type").asText();
                boolean valid=switch(type){case "string"->value instanceof String;case "integer"->value instanceof Byte||value instanceof Short||value instanceof Integer||value instanceof Long;case "number"->value instanceof Number;case "boolean"->value instanceof Boolean;case "array"->value instanceof java.util.Collection<?>;case "object"->value instanceof Map<?,?>;default->true;};
                if(!valid)throw invalid("field "+entry.getKey()+" must be "+type);
            }
            if(!schema.path("additionalProperties").asBoolean(true))for(String field:arguments.keySet())if(!properties.has(field))throw invalid("unknown field: "+field);
        }catch(ServiceException ex){throw ex;}catch(Exception ex){throw invalid("invalid tool schema: "+ex.getMessage());}
    }
    private ServiceException invalid(String message){return new ServiceException(400,AgentErrorCode.AGENT_TOOL_INVALID_ARGUMENT.name()+": "+message);}
}
