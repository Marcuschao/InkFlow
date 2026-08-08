package com.blog.ai.runtime.tool;

import com.blog.ai.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolSchemaValidatorTest {
    private static final String SCHEMA="{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"},\"limit\":{\"type\":\"integer\"}},\"required\":[\"query\"],\"additionalProperties\":false}";
    private final ToolSchemaValidator validator=new ToolSchemaValidator(new ObjectMapper());
    @Test void acceptsValidArguments(){assertDoesNotThrow(()->validator.validate(SCHEMA,Map.of("query","java","limit",5)));}
    @Test void rejectsMissingWrongAndUnknownFields(){assertThrows(ServiceException.class,()->validator.validate(SCHEMA,Map.of("limit",5)));assertThrows(ServiceException.class,()->validator.validate(SCHEMA,Map.of("query",5)));assertThrows(ServiceException.class,()->validator.validate(SCHEMA,Map.of("query","java","extra",true)));}
}
