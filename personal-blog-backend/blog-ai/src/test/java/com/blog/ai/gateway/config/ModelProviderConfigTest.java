package com.blog.ai.gateway.config;

import com.blog.ai.mapper.AiModelConfigMapper;
import com.blog.ai.model.entity.AiModelConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelProviderConfigTest {

    @Test
    void reloadReadsTheCurrentExternalModelValue() {
        MockEnvironment environment = baseEnvironment("deepseek-v4-flash");
        ModelProviderConfig config = new ModelProviderConfig(new GatewayProperties(),
                mock(AiModelConfigMapper.class), mock(AiApiKeyCipher.class), environment);

        config.reload();
        assertThat(config.defaultTarget().getModel()).isEqualTo("deepseek-v4-flash");

        environment.setProperty("spring.ai.openai.chat.options.model", "deepseek-v4-pro");
        config.reload();
        assertThat(config.defaultTarget().getModel()).isEqualTo("deepseek-v4-pro");
    }

    @Test
    void externalConfigurationWinsWhenDatabaseOverridesAreDisabled() {
        GatewayProperties properties = new GatewayProperties();
        properties.setDatabaseOverridesEnabled(false);
        AiModelConfigMapper mapper = mock(AiModelConfigMapper.class);
        when(mapper.selectList(null)).thenReturn(List.of(databaseProvider("deepseek-v4-pro")));

        ModelProviderConfig config = new ModelProviderConfig(properties, mapper,
                mock(AiApiKeyCipher.class), baseEnvironment("deepseek-v4-flash"));
        config.reload();

        assertThat(config.defaultTarget().getModel()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    void databaseConfigurationKeepsItsLegacyPrecedenceByDefault() {
        AiModelConfigMapper mapper = mock(AiModelConfigMapper.class);
        when(mapper.selectList(null)).thenReturn(List.of(databaseProvider("deepseek-v4-pro")));

        ModelProviderConfig config = new ModelProviderConfig(new GatewayProperties(), mapper,
                mock(AiApiKeyCipher.class), baseEnvironment("deepseek-v4-flash"));
        config.reload();

        assertThat(config.defaultTarget().getModel()).isEqualTo("deepseek-v4-pro");
    }

    private static MockEnvironment baseEnvironment(String model) {
        return new MockEnvironment()
                .withProperty("spring.ai.openai.api-key", "test-key")
                .withProperty("spring.ai.openai.base-url", "https://api.deepseek.com")
                .withProperty("spring.ai.openai.chat.options.model", model);
    }

    private static AiModelConfig databaseProvider(String model) {
        AiModelConfig row = new AiModelConfig();
        row.setProviderId("deepseek");
        row.setName("deepseek");
        row.setApiKey("encrypted-test-key");
        row.setBaseUrl("https://api.deepseek.com");
        row.setModels(model);
        row.setEnabled(1);
        return row;
    }
}
