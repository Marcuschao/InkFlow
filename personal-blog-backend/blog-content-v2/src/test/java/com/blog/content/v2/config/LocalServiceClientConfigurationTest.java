package com.blog.content.v2.config;

import com.blog.ai.service.AgentService;
import com.blog.ai.service.ArticleSeoService;
import com.blog.common.dto.AutoTagItemDto;
import com.blog.common.dto.AutoTagRequest;
import com.blog.common.dto.SeoGenerateRequest;
import com.blog.common.dto.SeoGenerateResult;
import com.blog.common.feign.AiAgentFeignClient;
import com.blog.common.feign.AiFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalServiceClientConfigurationTest {

    private final LocalServiceClientConfiguration configuration = new LocalServiceClientConfiguration();

    @Test
    void delegatesSeoGenerationInProcess() {
        ArticleSeoService service = mock(ArticleSeoService.class);
        ObjectProvider<ArticleSeoService> provider = providerOf(service);
        SeoGenerateRequest request = new SeoGenerateRequest();
        SeoGenerateResult expected = new SeoGenerateResult();
        when(service.generateSeo(42L, request)).thenReturn(expected);

        AiFeignClient client = configuration.localAiFeignClient(provider);

        assertThat(client.generateSeo(42L, request).getData()).isSameAs(expected);
        verify(service).generateSeo(42L, request);
    }

    @Test
    void delegatesAutoTaggingInProcess() {
        AgentService service = mock(AgentService.class);
        ObjectProvider<AgentService> provider = providerOf(service);
        AutoTagRequest request = new AutoTagRequest();
        List<AutoTagItemDto> expected = List.of(new AutoTagItemDto());
        when(service.autoTag(request)).thenReturn(expected);

        AiAgentFeignClient client = configuration.localAiAgentFeignClient(provider);

        assertThat(client.autoTag(request).getData()).isSameAs(expected);
        verify(service).autoTag(request);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> providerOf(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(value);
        return provider;
    }
}
