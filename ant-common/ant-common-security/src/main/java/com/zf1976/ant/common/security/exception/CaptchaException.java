package com.zf1976.ant.common.security.exception;

import com.zf1976.ant.common.security.enums.AuthenticationState;
import org.springframework.security.core.AuthenticationException;

/**
 * @author mac
 */
public class CaptchaException extends AuthenticationException {

    private final int value;

    private final String reasonPhrase;

    public CaptchaException(AuthenticationState e) {
        super(e.getReasonPhrase());
        this.value = e.getValue();
        this.reasonPhrase = e.getReasonPhrase();
    }

    public int getValue() {
        return value;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }
}