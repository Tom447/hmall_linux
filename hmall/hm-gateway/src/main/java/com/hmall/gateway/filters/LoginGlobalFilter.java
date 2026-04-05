package com.hmall.gateway.filters;

import com.hmall.common.exception.UnauthorizedException;
import com.hmall.common.utils.CollUtils;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@AllArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class LoginGlobalFilter implements GlobalFilter, Ordered {


    private final AuthProperties authProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtTool jwtTool;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取request的对象
        ServerHttpRequest request = exchange.getRequest();

    /*   2.AuthProperties中的excludePaths是不用拦截的
     *      所以需要做判断。判断当前的请求是否需要被拦截
     * */
        if (isAllowPath(request)) {
            //无需拦截
            return chain.filter(exchange);
        }
        //3.获取tooken
        // headers是一个请求头，请求头中可以有多个内容，所以是headers
        // 3.获取请求头中的token
        String token = null;
        List<String> headers = request.getHeaders().get("authorization");
        if (!CollUtils.isEmpty(headers)) {
            token = headers.get(0);
        }
        // 4.校验并解析token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
            System.out.println("userId = " + userId);
        } catch (UnauthorizedException e) {
            // 如果无效，拦截
            ServerHttpResponse response = exchange.getResponse();
            response.setRawStatusCode(401);
            return response.setComplete();
        }
        //5.如果有效，传递用户信息
        String userInfo = userId.toString();
        ServerWebExchange ex = exchange.mutate().request(builder -> builder.header("user-info", userInfo)).build();
        //6.放行
        return chain.filter(ex);
    }

    private boolean isAllowPath(ServerHttpRequest request) {
        boolean flag = false;
//        String method = request.getMethodValue();
        String path = request.getPath().toString();

        for (String excludePath : authProperties.getExcludePaths()) {
            //GET /users
            //POST /users
            //因此用==只能判断路径无法判断方法
            //因此这里需要用到一个工具，spring的AntPathMatcher
//            boolean isMatch = pathMatcher.match(excludePath, method + ":" + path);
            boolean isMatch = pathMatcher.match(excludePath, path);
            if (isMatch) {
                flag = true;
                break;
            }
        }
        return flag;
    }


    @Override
    public int getOrder() {
        return 0;
    }
}
