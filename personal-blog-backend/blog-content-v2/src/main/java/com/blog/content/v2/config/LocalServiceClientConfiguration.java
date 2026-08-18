package com.blog.content.v2.config;

import com.blog.ai.service.AgentService;
import com.blog.ai.service.ArticleSeoService;
import com.blog.common.dto.AutoTagItemDto;
import com.blog.common.dto.AutoTagRequest;
import com.blog.common.dto.KnowledgeGraphDto;
import com.blog.common.dto.LearningPathRequest;
import com.blog.common.dto.LearningPathResult;
import com.blog.common.dto.SeoGenerateRequest;
import com.blog.common.dto.SeoGenerateResult;
import com.blog.common.dto.TagNodeDetailDto;
import com.blog.common.feign.AiAgentFeignClient;
import com.blog.common.feign.AiFeignClient;
import com.blog.common.feign.KnowledgeFeignClient;
import com.blog.common.support.Result;
import com.blog.content.knowledge.service.KnowledgeGraphService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import java.util.List;

/**
 * Replaces service-discovery calls with in-process delegation in the unified deployment.
 */
@Configuration(proxyBeanMethods = false)
public class LocalServiceClientConfiguration {

    @Bean
    public FilterRegistrationBean<com.blog.content.config.security.JwtAuthenticationFilter>
            contentJwtFilterRegistration(com.blog.content.config.security.JwtAuthenticationFilter filter) {
        return disabledFilterRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<com.blog.ai.config.security.JwtAuthenticationFilter>
            aiJwtFilterRegistration(com.blog.ai.config.security.JwtAuthenticationFilter filter) {
        return disabledFilterRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<com.blog.ai.gateway.web.GatewayUserContextFilter>
            gatewayUserContextFilterRegistration(com.blog.ai.gateway.web.GatewayUserContextFilter filter) {
        return disabledFilterRegistration(filter);
    }

    private static <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> disabledFilterRegistration(T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public static BeanFactoryPostProcessor unifiedCompatibilityAliases() {
        return beanFactory -> {
            String source = "com.blog.content.config.properties.NotificationRabbitProperties";
            if (beanFactory instanceof DefaultListableBeanFactory registry
                    && registry.containsBeanDefinition(source)
                    && !registry.isAlias("notificationRabbitProperties")) {
                registry.registerAlias(source, "notificationRabbitProperties");
            }
        };
    }

    @Bean
    public AiFeignClient localAiFeignClient(ObjectProvider<ArticleSeoService> articleSeoService) {
        return (id, request) -> Result.success(articleSeoService.getObject().generateSeo(id, request));
    }

    @Bean
    public AiAgentFeignClient localAiAgentFeignClient(ObjectProvider<AgentService> agentService) {
        return new AiAgentFeignClient() {
            @Override
            public Result<List<AutoTagItemDto>> autoTag(AutoTagRequest request) {
                return Result.success(agentService.getObject().autoTag(request));
            }

            @Override
            public Result<LearningPathResult> learningPath(LearningPathRequest request) {
                return Result.success(agentService.getObject().learningPath(request));
            }

            @Override
            public Result<String> weeklyInsight() {
                return Result.success(agentService.getObject().weeklyInsight());
            }
        };
    }

    @Bean
    public KnowledgeFeignClient localKnowledgeFeignClient(
            ObjectProvider<KnowledgeGraphService> knowledgeGraphService,
            ObjectMapper objectMapper) {
        return new KnowledgeFeignClient() {
            @Override
            public Result<KnowledgeGraphDto> getGraph() {
                KnowledgeGraphDto dto = objectMapper.convertValue(
                        knowledgeGraphService.getObject().getGraph(), KnowledgeGraphDto.class);
                return Result.success(dto);
            }

            @Override
            public Result<TagNodeDetailDto> getNode(Long tagId) {
                TagNodeDetailDto dto = objectMapper.convertValue(
                        knowledgeGraphService.getObject().getNodeDetail(tagId, 10, null), TagNodeDetailDto.class);
                return Result.success(dto);
            }
        };
    }
}
