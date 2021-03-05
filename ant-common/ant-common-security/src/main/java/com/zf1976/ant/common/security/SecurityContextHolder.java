package com.zf1976.ant.common.security;

import com.zf1976.ant.common.core.util.ApplicationConfigUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author mac
 */
@Component
public class SecurityContextHolder extends org.springframework.security.core.context.SecurityContextHolder {

    private static final String ANONYMOUS_AUTH = "anonymousUser";
    private static final AntPathMatcher PATH_MATCHER= new AntPathMatcher();
    private static final Map<Class<?>, Object> CONTENTS_MAP = new HashMap<>(16);
    private static UserDetailsService userDetailsService;
    private static DynamicDataSourceService dynamicDataSourceService;
    private static SecurityProperties securityProperties;

    public static void setShareObject(Class<?> clazz, Object object) {
        Assert.isInstanceOf(clazz, object, "must be an instance of class");
        CONTENTS_MAP.put(clazz, object);
    }

    public static <T> T getShareObject(Class<T> clazz){
        return clazz.cast(CONTENTS_MAP.get(clazz));
    }

    public static Authentication getAuthentication() {
        return getContext().getAuthentication();
    }

    /**
     * 获取当前用户
     *
     * @return userDetails
     */
    public static UserDetails getDetails(){
        final Authentication authentication = getAuthentication();
        final String username = (String) authentication.getPrincipal();
        return userDetailsService.loadUserByUsername(username);
    }

    /**
     * 获取用户id
     *
     * @return id
     */
    public static Long getPrincipalId() {
        String token = ((String) getAuthentication().getCredentials());
        return JwtTokenProvider.getSessionId(token);
    }

    /**
     * 获取主体
     *
     * @return 用户名
     */
    public static String getPrincipal() {
        String token = (String) getAuthentication().getCredentials();
        try {
            return JwtTokenProvider.getClaims(token)
                                   .getSubject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ANONYMOUS_AUTH;
    }

    /**
     * 获取凭证
     *
     * @return token
     */
    public static String getCredentials() {
        return ((String) getAuthentication().getCredentials());
    }

    /**
     * 是否为super admin
     * @return ture / false
     */
    public static boolean isSuperAdmin() {
        return getAuthentication().getAuthorities()
                                  .stream()
                                  .map(GrantedAuthority::getAuthority)
                                  .anyMatch(s -> s.equals(securityProperties.getAdmin()));
    }

    /**
     * 校验是否为管理员
     *
     * @param principal 主体/用户名
     * @return /
     */
    public static boolean validateSuperAdmin(String principal) {
        return ObjectUtils.nullSafeEquals(principal, securityProperties.getAdmin());
    }

    /**
     * 获取放行uri
     *
     * @return /
     */
    public static Set<String> getAllowedUri() {
        // 匿名方向uri
        String[] allowUri = securityProperties.getAllowUri();
        Set<String> allow = dynamicDataSourceService.getAllowUri();
        allow.addAll(Arrays.asList(allowUri));
        return allow;
    }

    /**
     * 验证uri
     *
     * @param request request
     * @return /
     */
    public static boolean validateUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        Set<String> allowedUri = getAllowedUri();
        return allowedUri.stream()
                         .anyMatch(var -> PATH_MATCHER.match(var, uri));
    }

    /**
     * 获取签发方
     *
     * @return /
     */
    public static String getIssuer(){
        return securityProperties.getTokenIssuer();
    }


    @Autowired
    public void setSecurityProperties(SecurityProperties securityProperties) {
        SecurityContextHolder.securityProperties = securityProperties;
    }

    @Autowired
    public void setUserDetailsService(UserDetailsService userDetailsService) {
        SecurityContextHolder.userDetailsService = userDetailsService;
    }

    @Autowired
    public void setDynamicDataSourceService(DynamicDataSourceService dynamicDataSourceService) {
        SecurityContextHolder.dynamicDataSourceService = dynamicDataSourceService;
    }
}