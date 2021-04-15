package com.jd.dypos.compile.exception;

import javax.lang.model.element.Element;

/**
 * Package: com.dev.hh.dypos_aptlib_compiler
 * User: hehao3
 * Email: hehao3@jd.com
 * Date: 2021/4/12
 * Time: 上午10:51
 * Description:
 */
public class ProcessingException extends Exception{
    Element element;

    public ProcessingException(Element element, String msg, Object... args) {
        super(String.format(msg, args));
        this.element = element;
    }

    public Element getElement() {
        return element;
    }

}
