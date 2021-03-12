package com.zf1976.ant.gateway.filter;

import com.power.common.model.CommonResult;
import com.zf1976.ant.gateway.GatewayRouteConstants;
import com.zf1976.ant.gateway.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 不支持参数形式access_token访问
 *
 * @author ant
 * Create by Ant on 2021/3/6 8:52 AM
 */
public class Oauth2TokenAuthenticationFilter implements WebFilter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Pattern authorizationPattern = Pattern.compile(
            "^Bearer (?<token>[a-zA-Z0-9-._~+/]+)=*$",
            Pattern.CASE_INSENSITIVE);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final String[] ignoreUri = new String[]{};
    RestTemplate restTemplate = new RestTemplate();
    private String jwtCheckUrl;


    public Oauth2TokenAuthenticationFilter(String checkTokenUrl) {
        this.jwtCheckUrl = checkTokenUrl + "?token={value}";
    }

    public Oauth2TokenAuthenticationFilter setJwtCheckUrl(String jwtCheckUrl) {
        this.jwtCheckUrl = jwtCheckUrl + "?token={value}";
        return this;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange serverWebExchange, @NonNull WebFilterChain webFilterChain) {
        final ServerHttpRequest request = serverWebExchange.getRequest();
        final ServerHttpResponse response = serverWebExchange.getResponse();
        String requestUri = request.getURI().getPath();
        // 认证中心路由放行
        if (pathMatcher.match(GatewayRouteConstants.AUTH_ROUTE, requestUri)) {
            return webFilterChain.filter(serverWebExchange);
        }
        // 白名单放行
        for (String ignored : ignoreUri) {
            if (pathMatcher.match(ignored, requestUri)) {
                return webFilterChain.filter(serverWebExchange);
            }
        }
        // 管理中心路由
        if (pathMatcher.match(GatewayRouteConstants.ADMIN_ROUTE, requestUri)) {
            try {
                String token = this.token(request);
                // 无请求token放行
                if (StringUtils.isEmpty(token)) {
                    return webFilterChain.filter(serverWebExchange);
                } else {
                    if (this.checkToken(token)) {
                        return webFilterChain.filter(serverWebExchange);
                    } else {
                        BearerTokenError bearerTokenError = this.bearerTokenError();
                        throw new OAuth2AuthenticationException(bearerTokenError);
                    }
                }
            } catch (AuthenticationException e) {
                SecurityContextHolder.clearContext();
                return this.exceptionHandler(response, e);
            }
        }
        return webFilterChain.filter(serverWebExchange);
    }

    @SuppressWarnings("all")
    private boolean checkToken(String token) {
        try {
            ResponseEntity<Map> responseEntity = this.restTemplate.getForEntity(this.jwtCheckUrl, Map.class, token);
            return responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful();
        } catch (Exception ignored) {
            System.out.println(ignored);
        }
        return false;
    }

    private String token(ServerHttpRequest request) {
        String authorizationHeaderToken = this.resolveRequestAuthorizationHeader(request.getHeaders());
        String accessToken = request.getQueryParams()
                                     .getFirst("access_token");
        if (authorizationHeaderToken != null) {
            if (accessToken != null) {
                BearerTokenError error = BearerTokenErrors.invalidRequest("Found multiple bearer tokens in the request");
                throw new OAuth2AuthenticationException(error);
            } else {
                return authorizationHeaderToken;
            }
        }
        return accessToken != null && this.isParameterTokenSupportedForRequest(request)? accessToken : null;
    }

    private String resolveRequestAuthorizationHeader(HttpHeaders headers) {
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.startsWithIgnoreCase(authorization, OAuth2AccessToken.TokenType.BEARER.getValue())) {
            Matcher matcher = authorizationPattern.matcher(authorization);
            if (!matcher.matches()) {
                BearerTokenError bearerTokenError = bearerTokenError();
                throw new OAuth2AuthenticationException(bearerTokenError);
            } else {
                return matcher.group("token");
            }
        } else {
            return null;
        }
    }

    private boolean isParameterTokenSupportedForRequest(ServerHttpRequest request) {
        return false;
    }

    private BearerTokenError bearerTokenError(){
        return BearerTokenErrors.invalidToken("invalid token!");
    }

    @SuppressWarnings("rawtypes")
    private Mono<Void> exceptionHandler(ServerHttpResponse response, AuthenticationException e) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().set("Access-Control-Allow-Origin", "*");
        response.getHeaders().set("Cache-Control", "no-cache");
        CommonResult fail = CommonResult.fail(String.valueOf(HttpStatus.UNAUTHORIZED.value()), e.getMessage());
        String body = JsonUtil.toJsonString(fail);
        DataBuffer buffer = response.bufferFactory()
                                    .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer))
                       .doOnError(error -> DataBufferUtils.release(buffer));

    }
}
