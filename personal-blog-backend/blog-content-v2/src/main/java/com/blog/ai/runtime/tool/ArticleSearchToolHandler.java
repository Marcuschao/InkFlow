package com.blog.ai.runtime.tool;
import com.blog.ai.agent.tools.ArticleSearchTools;
import com.blog.ai.runtime.model.AgentExecutionContext;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
@Component
public class ArticleSearchToolHandler implements ToolHandler<ArticleSearchToolHandler.Arguments> {
    private final ArticleSearchTools tools;
    public ArticleSearchToolHandler(ArticleSearchTools tools){this.tools=tools;}
    public ToolDefinition definition(){return new ToolDefinition("searchBlogArticles","1.0.0","Search published blog articles","{\"type\":\"object\",\"properties\":{\"searchQuery\":{\"type\":\"string\"}},\"required\":[\"searchQuery\"]}",ToolRiskLevel.READ_ONLY,Duration.ofSeconds(10),1,false,Set.of(),false,false);}
    public Class<Arguments> argumentType(){return Arguments.class;}
    public ToolResult execute(Arguments a,AgentExecutionContext c){return new ToolResult(true,tools.searchStructured(a.searchQuery()),null,null,Map.of("untrusted",true));}
    public record Arguments(@NotBlank String searchQuery){}
}
