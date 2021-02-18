package com.zf1976.ant.auth.enhance;

import com.zf1976.ant.auth.AuthorizationConstants;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

/**
 * @author mac
 * @date 2021/2/16
 **/
public class RedisTokenStoreEnhancer {

    private final RedisTokenStore tokenStore;

    public RedisTokenStoreEnhancer(RedisConnectionFactory connectionFactory) {
        this.tokenStore = new RedisTokenStore(connectionFactory);
    }

    public TokenStore enhance() {
        tokenStore.setPrefix(AuthorizationConstants.PROJECT_OAUTH_TOKEN);
        return tokenStore;
    }

}