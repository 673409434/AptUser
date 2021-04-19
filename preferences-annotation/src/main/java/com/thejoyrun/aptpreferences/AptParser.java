package com.thejoyrun.aptpreferences;

/**
 *
 */
public interface AptParser {
    Object deserialize(Class clazz, String text);

    String serialize(Object object);
}
