package com.zf1976.ant.common.core.validate;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 校验工具类
 *
 * @author mac
 * @date 2021/5/20
 */
public class Validator<T> {

    /**
     * 初始化为真
     */
    private Predicate<T> predicate = t -> true;
    /**
     * 校验对象
     */
    private T t;

    private Validator() {
    }

    public static <T> Validator<T> of(T value) {
        if (value == null) {
            throw new RuntimeException("data cannot been null!");
        }
        return new Validator<T>().init(value);
    }

    /**
     * 添加逻辑与一个校验策略
     *
     * @param predicate 校验策略
     * @return {@link Validator<T>}
     */
    public Validator<T> with(Predicate<T> predicate) {
        this.predicate = this.predicate.and(predicate);
        return this;
    }

    /**
     * 初始化校验
     *
     * @param t 类型
     * @return {@link Validator<T>}
     */
    private Validator<T> init(T t) {
        if (t == null) {
            throw new RuntimeException("data cannot been null!");
        }
        this.t = t;
        return this;
    }

    /**
     * 链式校验抛异常
     *
     * @param predicate         一个校验策略
     * @param exceptionSupplier 异常处理
     * @param <X>               如果没有值
     * @return Validator<T>
     * @throws X 异常
     */
    public <X extends Throwable> Validator<T> withValidated(Predicate<T> predicate,
                                                            Supplier<? extends X> exceptionSupplier) throws X {
        boolean validated = this.with(predicate)
                                .validated(this.t);
        if (!validated) {
            throw exceptionSupplier.get();
        }
        return this;
    }

    /**
     * 添加一个逻辑或校验策略
     *
     * @param predicate 校验策略
     * @return {@link Validator<T>}
     */
    public Validator<T> or(Predicate<T> predicate) {
        this.predicate = this.predicate.or(predicate);
        return this;
    }


    /**
     * 执行校验
     *
     * @param t 对象
     * @return {@link boolean}
     */
    public boolean validated(T t) {
        if (t == null) {
            throw new RuntimeException("data cannot been null!");
        }
        return this.predicate.test(t);
    }

}
