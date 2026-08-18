package com.blog.ai.gateway.router;

import com.blog.ai.gateway.AiTaskType;
import com.blog.ai.gateway.config.GatewayProperties;
import com.blog.ai.gateway.config.ModelProviderConfig;
import com.blog.ai.gateway.health.ModelHealthChecker;
import com.blog.ai.gateway.model.ModelTarget;
import com.blog.ai.mapper.AiRouteRuleMapper;
import com.blog.ai.model.entity.AiRouteRule;
import jakarta.annotation.PostConstruct;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ModelRouter {

    private final GatewayProperties gatewayProperties;
    private final ModelProviderConfig modelProviderConfig;
    private final ModelHealthChecker healthChecker;
    private final AiRouteRuleMapper aiRouteRuleMapper;

    private volatile List<AiRouteRule> dbRules = List.of();

    public ModelRouter(GatewayProperties gatewayProperties,
                       ModelProviderConfig modelProviderConfig,
                       ModelHealthChecker healthChecker,
                       AiRouteRuleMapper aiRouteRuleMapper) {
        this.gatewayProperties = gatewayProperties;
        this.modelProviderConfig = modelProviderConfig;
        this.healthChecker = healthChecker;
        this.aiRouteRuleMapper = aiRouteRuleMapper;
    }

    @PostConstruct
    public void loadDbRules() {
        reloadRules();
    }

    public void reloadRules() {
        if (!gatewayProperties.isDatabaseOverridesEnabled()) {
            dbRules = List.of();
            return;
        }
        try {
            List<AiRouteRule> rows = aiRouteRuleMapper.selectList(null);
            dbRules = rows == null ? List.of() : rows.stream()
                    .filter(r -> r.getEnabled() != null && r.getEnabled() == 1)
                    .sorted(Comparator.comparingInt(r -> r.getSortOrder() != null ? r.getSortOrder() : 0))
                    .toList();
        } catch (Exception e) {
            dbRules = List.of();
        }
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onConfigRefresh(EnvironmentChangeEvent event) {
        if (event.getKeys().stream().anyMatch(key -> key.startsWith("blog.ai.gateway"))) {
            reloadRules();
        }
    }

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefreshScopeRefreshed(RefreshScopeRefreshedEvent event) {
        reloadRules();
    }

    public List<ModelTarget> resolveChain(AiTaskType taskType) {
        LinkedHashSet<ModelTarget> chain = new LinkedHashSet<>();
        String code = taskType.code();

        if (gatewayProperties.isDatabaseOverridesEnabled()) {
            for (AiRouteRule rule : dbRules) {
                if (matches(rule.getTaskPattern(), code)) {
                    addTarget(chain, rule.getPrimaryModel());
                    addFallbacks(chain, rule.getFallbackChain());
                    break;
                }
            }
        }

        if (chain.isEmpty()) {
            for (GatewayProperties.RouteDef route : gatewayProperties.getRoutes()) {
                if (matches(route.getTaskType(), code)) {
                    addTarget(chain, route.getPrimary());
                    if (route.getFallbacks() != null) {
                        route.getFallbacks().forEach(f -> addTarget(chain, f));
                    }
                    break;
                }
            }
        }

        if (chain.isEmpty()) {
            ModelTarget def = modelProviderConfig.defaultTarget();
            if (def != null) {
                chain.add(def);
            }
        }

        List<ModelTarget> available = new ArrayList<>();
        for (ModelTarget t : chain) {
            if (healthChecker.isAvailable(t.getProviderId())) {
                available.add(t);
            }
        }
        if (available.isEmpty()) {
            return new ArrayList<>(chain);
        }
        return available;
    }

    private void addFallbacks(LinkedHashSet<ModelTarget> chain, String fallbackChain) {
        if (!StringUtils.hasText(fallbackChain)) {
            return;
        }
        for (String part : fallbackChain.split(",")) {
            addTarget(chain, part.trim());
        }
    }

    private void addTarget(LinkedHashSet<ModelTarget> chain, String spec) {
        ModelTarget t = modelProviderConfig.parseTarget(spec);
        if (t != null) {
            chain.add(t);
        }
    }

    private boolean matches(String pattern, String taskCode) {
        if (!StringUtils.hasText(pattern)) {
            return false;
        }
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(taskCode).find();
        } catch (Exception e) {
            return pattern.equalsIgnoreCase(taskCode);
        }
    }
}
