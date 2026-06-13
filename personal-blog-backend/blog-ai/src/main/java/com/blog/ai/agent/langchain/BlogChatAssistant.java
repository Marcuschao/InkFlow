package com.blog.ai.agent.langchain;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface BlogChatAssistant {

    @SystemMessage(
            "你是博客问答助手。必须先调用工具 searchBlogArticles 获取与用户问题相关的站内文章资料再作答。"
                    + "若参考资料与用户问题无关或工具返回无可用资料，请只回复：抱歉，我只能回答博客相关的问题。"
                    + "回答须基于工具返回的正文片段，不要编造。表述简洁。"
                    + "若用户问你能做什么、如何提问等元问题，根据检索到的文章主题说明可回答的问题类型。")
    String respond(@UserMessage String question);
}
