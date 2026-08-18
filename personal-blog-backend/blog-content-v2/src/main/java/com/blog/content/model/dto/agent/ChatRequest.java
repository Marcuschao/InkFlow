package com.blog.content.model.dto.agent;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.List;

@Data
public class ChatRequest {

    private String question;
    /** 文章详情页问答：限定仅基于该篇已发布正文作答 */
    private Long articleId;
    private List<ChatMessage> messages;
    private Boolean stream;

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }

    /**
     * 实例方法，不需要传参
     * @return
     */
    public String resolveQuestion() {
        if (StringUtils.hasText(question)) {
            return question.trim();
        }
        if (messages != null) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage m = messages.get(i);  //从后面往前找，优先用户最新输入的消息
                if (m == null || !"user".equalsIgnoreCase(m.getRole())) {
                    continue;
                }
                if (StringUtils.hasText(m.getContent())) {
                    return m.getContent().trim();
                }
            }
        }
        return null;
    }
}
