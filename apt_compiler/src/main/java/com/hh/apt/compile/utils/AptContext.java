package com.hh.apt.compile.utils;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Package: com.hh.apt.compile.utils
 * User: hehao3
 * Email: hehao3@jd.com
 * Date: 2021/4/15
 * Time: 上午10:57
 * Description:
 */
public class AptContext {
    private Types typeUtils;
    private Messager messager;
    private Filer filer;
    private Elements elementUtils;

    private static volatile AptContext mAptContext = null;

    private AptContext() {
    }

    public static AptContext getInstance() {
        if (mAptContext == null) {
            synchronized (AptContext.class) {
                if (mAptContext == null) {
                    mAptContext = new AptContext();
                }
            }
        }
        return mAptContext;
    }

    public void init(ProcessingEnvironment processingEnvironment) {
        typeUtils = processingEnvironment.getTypeUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        elementUtils = processingEnvironment.getElementUtils();
    }


    public Types getTypeUtils() {
        return typeUtils;
    }

    public void setTypeUtils(Types typeUtils) {
        this.typeUtils = typeUtils;
    }

    public Messager getMessager() {
        return messager;
    }

    public void setMessager(Messager messager) {
        this.messager = messager;
    }

    public Filer getFiler() {
        return filer;
    }

    public void setFiler(Filer filer) {
        this.filer = filer;
    }

    public Elements getElementUtils() {
        return elementUtils;
    }

    public void setElementUtils(Elements elementUtils) {
        this.elementUtils = elementUtils;
    }
}
