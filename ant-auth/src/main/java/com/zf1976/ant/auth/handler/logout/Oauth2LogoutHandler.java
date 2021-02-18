package com.zf1976.ant.auth.handler.logout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zf1976.ant.auth.exception.ExpiredJwtException;
import com.zf1976.ant.auth.exception.IllegalAccessException;
import com.zf1976.ant.auth.exception.IllegalJwtException;
import com.zf1976.ant.common.core.foundation.ResultData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author mac
 * Create by Ant on 2020/10/4 01:03
 */
@SuppressWarnings("rawtypes")
public class Oauth2LogoutHandler implements LogoutHandler {

    public final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 校验请求方法
     *
     * @param request request
     */
    private void support(HttpServletRequest request) {
        final String method = request.getMethod();
        if (!method.equals(HttpMethod.POST.name())) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }
    }

    @Override
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        AuthenticationException authenticationException = null;
        try {
            this.support(httpServletRequest);

        } catch (ExpiredJwtException | IllegalJwtException | IllegalAccessException | AuthenticationServiceException e) {
            authenticationException = e;
        }
        if (authenticationException != null) {
            SecurityContextHolder.clearContext();
            try {
                this.unSuccessLogoutHandler(httpServletRequest, httpServletResponse, authenticationException);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 登出失败处理
     *
     * @param httpServletRequest request
     * @param httpServletResponse  response
     * @param e 异常
     * @throws IOException 向上抛异常
     */
    private void unSuccessLogoutHandler(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException {
        ResultData fail = null;
        if (e instanceof ExpiredJwtException) {
            ExpiredJwtException exception = (ExpiredJwtException) e;
            fail = ResultData.fail(exception.getValue(), exception.getReasonPhrase());
        }else if (e instanceof IllegalAccessException) {
            IllegalAccessException exception = (IllegalAccessException) e;
            fail = ResultData.fail(exception.getValue(), exception.getReasonPhrase());
        } else if (e instanceof IllegalJwtException) {
            IllegalJwtException exception = (IllegalJwtException) e;
            fail = ResultData.fail(exception.getValue(), exception.getReasonPhrase());
        } else if (e instanceof AuthenticationServiceException) {
            AuthenticationServiceException exception = (AuthenticationServiceException) e;
            fail = ResultData.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());
        } else {
            fail = ResultData.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        }
        httpServletResponse.setStatus(HttpStatus.OK.value());
        httpServletResponse.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        fail.setPath(httpServletRequest.getRequestURI());
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(httpServletResponse.getOutputStream(), fail);
    }

}
