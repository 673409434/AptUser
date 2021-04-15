package com.hh.apt.compile;

import com.dev.hh.annotation.Factory;
import com.google.auto.service.AutoService;
import com.hh.apt.compile.exception.ProcessingException;
import com.hh.apt.compile.generator.FactoryAnnotatedClass;
import com.hh.apt.compile.generator.FactoryClassesGenerator;
import com.hh.apt.compile.utils.AptContext;
import com.hh.apt.compile.utils.LogMessager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Package: com.hh.apt.compile
 * User: hehao3
 * Email: hehao3@jd.com
 * Date: 2021/4/9
 * Time: 下午3:36
 * Description:
 */

//@AutoService用来自动生成META-INF/services/javax.annotation.processing.Processor文件，自动在javac中注册"注解处理器"服务；
//AutoService是Google开发的一个库，使用时需要添加依赖，如下：implementation 'com.google.auto.service:auto-service:1.0-rc4'
@AutoService(Processor.class)
public class FactoryProcessor extends AbstractProcessor {

    /**
     * 这个方法用于初始化处理器，方法中有一个ProcessingEnvironment类型的参数，ProcessingEnvironment是一个注解处理工具的集合。它包含了众多工具类。例如：
     * Filer可以用来编写新文件；
     * Messager可以用来打印错误信息；
     * Elements是一个可以处理Element的工具类。
     *
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        AptContext.getInstance().init(processingEnvironment);
    }

    /**
     * 这个方法的返回值是一个Set集合，集合中指要处理的注解类型的名称(这里必须是完整的包名+类名，例如com.example.annotation.Factory)。
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Factory.class.getCanonicalName());
        return annotations;
    }

    /**
     * 用来指示注释处理器所支持的最新源版本的注释，通常return SourceVersion.latestSupported()即可
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        LogMessager.info("startProcess");
        try {
            // 步骤一：扫描所有被@Factory注解的类元素
            Map<String, FactoryClassesGenerator> factoryClasses = new LinkedHashMap<>();
            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {
                //本例中Factory注解的是类，正常情况下返回的TypeElement，如果把@Factory用在接口或者抽象类上，不符合我们的标准，这里校验报错，终止编译
                if (annotatedElement.getKind() != ElementKind.CLASS) {
                    throw new ProcessingException(annotatedElement, "Only classes can be annotated with @%s",
                            Factory.class.getSimpleName());
                }
                // We can cast it, because we know that it of ElementKind.CLASS
                TypeElement typeElement = (TypeElement) annotatedElement;
                //将annotatedElement中包含的信息封装成一个对象，方便后续使用
                FactoryAnnotatedClass annotatedClass = new FactoryAnnotatedClass(typeElement);
                //校验注解元素是否符合规则
                checkValidClass(annotatedClass);


                //
                FactoryClassesGenerator factoryClass = factoryClasses.get(annotatedClass.getQualifiedFactoryGroupName());
                if (factoryClass == null) {
                    String qualifiedGroupName = annotatedClass.getQualifiedFactoryGroupName();
                    factoryClass = new FactoryClassesGenerator(qualifiedGroupName);
                    factoryClasses.put(qualifiedGroupName, factoryClass);
                }

                // Checks if id is conflicting with another @Factory annotated class with the same id
                factoryClass.add(annotatedClass);
            }

            // 步骤二：生成代码
            for (FactoryClassesGenerator factoryClass : factoryClasses.values()) {
                factoryClass.generateCode();
            }
            factoryClasses.clear();
        } catch (ProcessingException e) {
            LogMessager.error(e.getElement(), e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 校验注解元素知否符合规则
     *
     * Factory自定义注解的使用规则：
     * 1.只有类才能被@Factory注解。因为在ShapeFactory中我们需要实例化Shape对象，虽然@Factory注解声明了Target为ElementType.TYPE，但接口和枚举并不符合我们的要求。
     * 2.被@Factory注解的类中需要有public的构造方法，这样才能实例化对象。
     * 3.被注解的类必须是type指定的类的子类
     * 4.id需要为String类型，并且需要在相同type组中唯一
     * 5.具有相同type的注解类会被生成在同一个工厂类中
     *
     */
    private void checkValidClass(FactoryAnnotatedClass item) throws ProcessingException {
        // Cast to TypeElement, has more type specific methods
        TypeElement classElement = item.getTypeElement();

        //作用域检查：
        if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
            throw new ProcessingException(classElement, "The class %s is not public.",
                    classElement.getQualifiedName().toString());
        }

        // 如果是抽象方法则抛出异常终止编译
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new ProcessingException(classElement,
                    "The class %s is abstract. You can't annotate abstract classes with @%",
                    classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
        }

        // Check inheritance: Class must be child class as specified in @Factory.type();
        // 这个类必须是在@Factory.type()中指定的类的子类，否则抛出异常终止编译
        TypeElement superClassElement = AptContext.getInstance().getElementUtils().getTypeElement(item.getQualifiedFactoryGroupName());
        if (superClassElement.getKind() == ElementKind.INTERFACE) {
            // 检查被注解类是否实现或继承了@Factory.type()所指定的类型，此处均为IShape
            if (!classElement.getInterfaces().contains(superClassElement.asType())) {
                throw new ProcessingException(classElement,
                        "The class %s annotated with @%s must implement the interface %s",
                        classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                        item.getQualifiedFactoryGroupName());
            }
        } else {
            // 检查子类
            TypeElement currentClass = classElement;
            while (true) {
                /**
                 * getSuperclass()
                 * Returns the direct superclass of this type element.
                 * If this type element represents an interface or the class java.lang.Object,
                 * then a NoType with kind NONE is returned.
                 */
                TypeMirror superClassType = currentClass.getSuperclass();

                if (superClassType.getKind() == TypeKind.NONE) {
                    // 向上遍历父类，直到Object也没获取到所需父类，终止编译抛出异常
                    throw new ProcessingException(classElement,
                            "The class %s annotated with @%s must inherit from %s",
                            classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                            item.getQualifiedFactoryGroupName());
                }

                if (superClassType.toString().equals(item.getQualifiedFactoryGroupName())) {
                    // 找到了要求的父类，校验通过，终止遍历
                    break;
                }

                // Moving up in inheritance tree
                currentClass = (TypeElement) AptContext.getInstance().getTypeUtils().asElement(superClassType);
            }
        }

        // Check if an empty public constructor is given
        // 检查是否提供了默认公开构造函数
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructorElement = (ExecutableElement) enclosed;
                if (constructorElement.getParameters().size() == 0 &&
                        constructorElement.getModifiers().contains(Modifier.PUBLIC)) {
                    // Found an empty constructor
                    return;
                }
            }
        }

        // No empty constructor found
        // 为检测到public的无参构造方法，抛出异常，终止编译
        throw new ProcessingException(classElement,
                "The class %s must provide an public empty default constructor",
                classElement.getQualifiedName().toString());
    }


}
