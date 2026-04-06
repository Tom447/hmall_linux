package com.hmall.gateway.routes;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.AllArgsConstructor;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
@AllArgsConstructor
public class RouteConfigLoader {

    private final NacosConfigManager configManager;
    private final RouteDefinitionWriter writer;

    private final static String DATAID = "gateway-routes.json";

    private final static String GROUP = "DEFAULT_GROUP";

    private Set<String> routeIds = new HashSet<>();

    @PostConstruct
    public void initRouteConfiguration() throws NacosException {

        //第一次启动时，拉取路由表，并且添加监听器
        String configInfo = configManager.getConfigService().getConfigAndSignListener(DATAID, GROUP, 1000, new Listener() {
            @Override
            public Executor getExecutor() {

                return Executors.newSingleThreadExecutor();
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                //更新路由表
                updateRouteConfigInfo(configInfo);
            }
        });
        //2. 写入路由表
        updateRouteConfigInfo(configInfo);

    }

    private void updateRouteConfigInfo(String configInfo) {
        //1.解析路由信息
        List<RouteDefinition> routeDefinitionList = JSONUtil.toList(configInfo, RouteDefinition.class);
        //2.删除旧的路由
        for (String routeId : routeIds) {
            writer.delete(Mono.just(routeId)).subscribe();
        }
        routeIds.clear();
        //3.是否有新路由
        // 修复建议
        if (routeDefinitionList == null || routeDefinitionList.isEmpty()) {
            return;
        }
        //4.更新路由表
        routeDefinitionList.forEach(routeDefinition -> {
            writer.save(Mono.just(routeDefinition)).subscribe();
            routeIds.add(routeDefinition.getId());
        });

    }

}
