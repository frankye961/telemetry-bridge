package com.smart.watering.system.be.utils;

import java.util.function.UnaryOperator;

public class Utils {

    public static <T> T applyIf(T builder, boolean condition, UnaryOperator<T> fn) {
        return condition ? fn.apply(builder) : builder;
    }
}
