package com.blog.ai.gateway.config;

import com.blog.ai.mapper.AiModelConfigMapper;
import com.blog.ai.model.entity.AiModelConfig;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelProviderConfigTest {

    @Test
    void reloadReadsTheCurrentExternalModelValue() {
        GatewayProperties properties = externalProperties("deepseek-v4-flash");
        ModelProviderConfig config = new ModelProviderConfig(properties,
                mock(AiModelConfigMapper.class), mock(AiApiKeyCipher.class));

        config.reload();
        assertThat(config.defaultTarget().getModel()).isEqualTo("deepseek-v4-flash");

        properties.getProviders().get(0).setModels(List.of("deepseek-v4-pro"));
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
                mock(AiApiKeyCipher.class));
        properties.getProviders().add(externalProvider("deepseek-v4-flash"));
        config.reload();

        assertThat(config.defaultTarget().getModel()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    void databaseConfigurationKeepsItsLegacyPrecedenceByDefault() {
        AiModelConfigMapper mapper = mock(AiModelConfigMapper.class);
        when(mapper.selectList(null)).thenReturn(List.of(databaseProvider("deepseek-v4-pro")));

        GatewayProperties properties = externalProperties("deepseek-v4-flash");
        properties.setDatabaseOverridesEnabled(true);
        ModelProviderConfig config = new ModelProviderConfig(properties, mapper,
                mock(AiApiKeyCipher.class));
        config.reload();

        assertThat(config.defaultTarget().getModel()).isEqualTo("deepseek-v4-pro");
    }

    private static GatewayProperties externalProperties(String model) {
        GatewayProperties properties = new GatewayProperties();
        properties.getProviders().add(externalProvider(model));
        properties.setDatabaseOverridesEnabled(false);
        return properties;
    }

    private static GatewayProperties.ProviderDef externalProvider(String model) {
        GatewayProperties.ProviderDef provider = new GatewayProperties.ProviderDef();
        provider.setId("deepseek");
        provider.setName("deepseek");
        provider.setApiKey("test-key");
        provider.setBaseUrl("https://api.deepseek.com");
        provider.setModels(List.of(model));
        return provider;
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
