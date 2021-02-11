package com.zf1976.ant.common.security.safe.exception;

import com.zf1976.ant.common.security.safe.enums.AuthenticationState;
import org.springframework.security.core.AuthenticationException;

/**
 * @author mac
 */
public class IllegalAccessException extends AuthenticationException {

    private final int value;

    private final String reasonPhrase;

    public IllegalAccessException(AuthenticationState e) {
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