package com.hh.apt.compile.generator;

import com.hh.apt.compile.exception.IdAlreadyUsedException;
import com.hh.apt.compile.utils.AptContext;
import com.hh.apt.compile.utils.LogMessager;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * Package: com.hh.apt.compile.generator
 * User: hehao3
 * Email: hehao3@jd.com
 * Date: 2021/4/12
 * Time: 上午11:00
 * Description:
 */
public class FactoryClassesGenerator {
    /**
     * Will be added to the name of the generated factory class
     */
    private static final String SUFFIX = "Factory";
    private String qualifiedClassName;

    private Map<String, FactoryAnnotatedClass> itemsMap = new LinkedHashMap<>();

    public FactoryClassesGenerator(String qualifiedClassName) {
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
     * @throws IOException
     */
    public void generateCode() throws IOException {
        TypeElement superClassName = AptContext.getInstance().getElementUtils().getTypeElement(qualifiedClassName);
        LogMessager.info("generateCode  superClassName:" + superClassName);
        //得到生成的类的名称
        String factoryClassName = superClassName.getSimpleName() + SUFFIX;

        PackageElement pkg = AptContext.getInstance().getElementUtils().getPackageOf(superClassName);
        //得到生成工厂类的包名，即接口Ishape的包名
        String packageName = pkg.isUnnamed() ? null : pkg.getQualifiedName().toString();
        LogMessager.info("generateCode  packageName:" + packageName);

        //要生成的"create"函数代码结构
        MethodSpec.Builder method = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .returns(TypeName.get(superClassName.asType()));
        // "create"函数内容1
        method.beginControlFlow("if (id == null)")
                .addStatement("throw new IllegalArgumentException($S)", "id is null!")
                .endControlFlow();
        // "create"函数内容2
        for (FactoryAnnotatedClass item : itemsMap.values()) {
            method.beginControlFlow("if ($S.equals(id))", item.getId())
                    .addStatement("return new $L()", item.getTypeElement().getQualifiedName().toString())
                    .endControlFlow();
        }
        // "create"函数内容3
        method.addStatement("throw new IllegalArgumentException($S + id)", "Unknown id = ");

        //构造工厂class
        TypeSpec typeSpec = TypeSpec
                .classBuilder(factoryClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(method.build())
                .build();
        // 写文件，指定包名
        JavaFile.builder(packageName, typeSpec).build().writeTo(AptContext.getInstance().getFiler());
    }
}
