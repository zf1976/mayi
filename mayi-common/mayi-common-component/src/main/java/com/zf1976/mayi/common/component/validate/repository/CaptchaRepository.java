/*
 * Copyright (c) 2021 zf1976
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.zf1976.mayi.common.component.validate.repository;


import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author WINDOWS
 */
public interface CaptchaRepository {

    /**
     * 保存验证码
     *
     * @param key      key
     * @param value    value
     * @param expire   过期时间戳
     * @param timeUnit 时间单位
     * @return boolean
     */
    void store(String key, String value, Long expire, TimeUnit timeUnit);

    /**
     * 读取存储验证码
     *
     * @param key key
     * @return 验证码
     */
    String read(String key);

    /**
     * 删除存储验证码
     *
     * @param key key
     */
    void remove(String key);

    /**
     * 是否可用
     *
     * @return true false
     */
    boolean isAvailable();

}

