package com.jd.dypos.compile.module;

import com.jd.dypos.compile.exception.IdAlreadyUsedException;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Package: com.dev.hh.dypos_aptlib_compiler
 * User: hehao3
 * Email: hehao3@jd.com
 * Date: 2021/4/12
 * Time: 上午11:00
 * Description:
 */
public class FactoryGroupedClasses {
    /**
     * Will be added to the name of the generated factory class
     */
    private static final String SUFFIX = "Factory";
    private String qualifiedClassName;

    private Map<String, FactoryAnnotatedClass> itemsMap = new LinkedHashMap<>();

    public FactoryGroupedClasses(String qualifiedClassName) {
        this.qualifiedClassName = qualifiedClassName;
    }

    /**
     * 一个工厂对应着多个对象的创建，这里将要创建的对象的ID加入，便于后续生成代码
     * @param toInsert
     */
    public void add(FactoryAnnotatedClass toInsert) {
        FactoryAnnotatedClass factoryAnnotatedClass = itemsMap.get(toInsert.getId());
        if (factoryAnnotatedClass != null) {
            throw new IdAlreadyUsedException(factoryAnnotatedClass);
        }
        itemsMap.put(toInsert.getId(), toInsert);
    }

    /**
     * 生成代码
     * @param elementUtils
     * @param filer
     * @throws IOException
     */
    public void generateCode(Elements elementUtils, Filer filer) throws IOException {

        TypeElement superClassName = elementUtils.getTypeElement(qualifiedClassName);
        //得到生成的类的名称
        String factoryClassName = superClassName.getSimpleName() + SUFFIX;
        String qualifiedFactoryClassName = qualifiedClassName + SUFFIX;

        //得到包名
        PackageElement pkg = elementUtils.getPackageOf(superClassName);
        String packageName = pkg.isUnnamed() ? null : pkg.getQualifiedName().toString();

        //梳理要生成的代码结构
        MethodSpec.Builder method = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .returns(TypeName.get(superClassName.asType()));

        // check if id is null
        method.beginControlFlow("if (id == null)")
                .addStatement("throw new IllegalArgumentException($S)", "id is null!")
                .endControlFlow();

        // Generate items map

        for (FactoryAnnotatedClass item : itemsMap.values()) {
            method.beginControlFlow("if ($S.equals(id))", item.getId())
                    .addStatement("return new $L()", item.getTypeElement().getQualifiedName().toString())
                    .endControlFlow();
        }

        method.addStatement("throw new IllegalArgumentException($S + id)", "Unknown id = ");

        TypeSpec typeSpec = TypeSpec
                .classBuilder(factoryClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(method.build())
                .build();

        // Write file
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }
}
