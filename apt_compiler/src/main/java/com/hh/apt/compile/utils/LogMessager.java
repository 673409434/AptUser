package com.hh.apt.compile.utils;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * Package: com.hh.apt.compile.utils
 * User: hehao3
 * Email: hehao3@jd.com
 * Date: 2021/4/15
 * Time: 下午2:07
 * Description:
 */
public class LogMessager {

    public static void error(Element e, String msg, Object... args) {
        AptContext.getInstance().getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }

    public static void error(String msg) {
        AptContext.getInstance().getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

    public static void info(String msg, Object... args) {
        AptContext.getInstance().getMessager().printMessage(Diagnostic.Kind.NOTE, String.format(msg, args));
    }

    public static void info(String msg) {
        AptContext.getInstance().getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

}
