package com.hh.apt.compile.generator;

import com.dev.hh.annotation.Factory;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

/**
 * Package: com.hh.apt.compile.generator
 * User: hehao3
 * Email: hehao3@jd.com
 * Date: 2021/4/12
 * Time: 上午10:41
 * Description: 将annotatedElement中包含的信息封装成一个对象，方便后续使用
 */
public class FactoryAnnotatedClass {
    //注解的class类元素
    private TypeElement mAnnotatedClassElement;
    //Factory注解的第二个参数指定class的名称：xx.xx.x.classname
    private String mQualifiedSuperClassName;
    //Factory注解的第二个参数指定class的名称：classname
    private String mSimpleTypeName;
    //Factory注解的第一个参数：id
    private String mId;

    public FactoryAnnotatedClass(TypeElement classElement) {
        this.mAnnotatedClassElement = classElement;
        Factory annotation = classElement.getAnnotation(Factory.class);
        mId = annotation.id();
        if (mId.length() == 0) {
            throw new IllegalArgumentException(
                    String.format("id() in @%s for class %s is null or empty! that's not allowed",
                            Factory.class.getSimpleName(), classElement.getQualifiedName().toString()));
        }

        /**
         * 这个类已被编译 如我们的其他.jar中包涵已经被我们的注解编译过的的.class文件。这种情况下，注解处理器可以直接获取注解的类对象。
         * 如果还没有被编译，直接获取类对象会抛出MirroredTypeException异常，所以我们需要try-catch去捕获这个异常，从中获取TypeMirror，再经过一系列强转，最终获得TypeElement类型，从中读取类对象信息。
         */
        try {  // 该类已经被编译
            //Factory注解的第二个参数：class
            Class<?> clazz = annotation.type();
            //获取class的名称：xx.xx.x.classname
            mQualifiedSuperClassName = clazz.getCanonicalName();
            //获取class的名称：classname
            mSimpleTypeName = clazz.getSimpleName();
        } catch (MirroredTypeException mte) {// 该类未被编译
            DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
            mQualifiedSuperClassName = classTypeElement.getQualifiedName().toString();
            mSimpleTypeName = classTypeElement.getSimpleName().toString();
        }
    }

    public String getId() {
        return mId;
    }

    public String getQualifiedFactoryGroupName() {
        return mQualifiedSuperClassName;
    }


    public String getSimpleFactoryGroupName() {
        return mSimpleTypeName;
    }

    public TypeElement getTypeElement() {
        return mAnnotatedClassElement;
    }
}