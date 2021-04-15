# aptUser




------------------------------------APT------------------------------------
1、概念：APT(Annotation Processing Tool)是一种处理注释的工具，APT可以用来在编译时扫描和处理注解。
	
2、作用：通过APT可以在编译时获取到注解和被注解对象的相关信息，在拿到这些信息后我们可以根据需求来自动的生成一些代码，省去了手动编写。

3、注意：获取注解及生成代码都是在代码编译时候完成的，相比反射在运行时处理注解大大提高了程序性能。APT的核心是AbstractProcessor类，关于AbstractProcessor类后面会做详细说明。

note：KAPT（APT for kotlin）与APT完全相同，只是在Kotlin下的注解处理器










------------------------------------构建APT项目------------------------------------
项目参考：aptuser：https://github.com/673409434/AptUser.git 


一、步骤一：新建两个java library（File–>New–>New Module）。
1、首先需要一个Annotation模块，这个用来存放自定义的注解。
2、另外需要一个Compiler模块，这个模块依赖Annotation模块。

note：
------一个APT项目至少应该由两个Java Library模块，两个模块一定要是Java Library，因为需要用到jdk下的 【 *javax.~ *】包下的AbstractProcessor类，Android sdk是基于OpenJDK的，而OpenJDK中不包含APT的相关代码。






二、步骤二——————依赖关系
这里共有三个模块：android APP模块；java Annotation模块；java Compiler模块

1、android APP模块依赖（和其他业务模块）依赖Annotation模块和Compiler模块；
------如果android app模块是java实现，则依赖如下：
    implementation project(':dypos_annotation')
	//把注解处理器挂载到项目
    annotationProcessor project(':dypos_compiler')
------如果android app模块是kotlin实现，则用到kapt，依赖如下：
    implementation project(':dypos_annotation')
	//把注解处理器挂载到项目
    kapt project(':dypos_compiler')
------其中dypos_compiler模块负责在编译时检查注解，并动态生成代码，不会参与到APK代码的编译中，即其不会编译到APK中；



2、java Compiler模块依赖java Annotation模块，具体依赖如下
	//依赖注解模块
    implementation project(path: ':dypos_annotation')
	//步骤三具体讲解
    implementation 'com.google.auto.service:auto-service:1.0-rc7'
    annotationProcessor 'com.google.auto.service:auto-service:1.0-rc7'
    implementation 'com.squareup:javapoet:1.11.1'







三、步骤三——————编写注解模块（这里以编写一个通过注解，生成对对象的工厂类为例）
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)//编译时注解
public @interface Factory {

    Class type();

    String id();
}

Note：kotlin的注解书写方式略有不同








四、步骤四——————编写注解处理器模块
1、定义注解处理器
注解处理器要继承自AbstractProcessor类，核心是process方法，如下：

//@AutoService用来自动生成META-INF/services/javax.annotation.processing.Processor文件，自动在javac中注册"注解处理器"服务；
//AutoService是Google开发的一个库，使用时需要添加依赖，如下：implementation 'com.google.auto.service:auto-service:1.0-rc7'
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



2、生成代码核心逻辑
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




3、注解处理器模块依赖
	//依赖注解模块
    implementation project(path: ':dypos_annotation')
	//Process需要使用@AutoService，用来自动生成META-INF/services/javax.annotation.processing.Processor文件，该注解自动帮我们在javac中注册"注解处理器"服务，使用该注解需要依赖如下
    implementation 'com.google.auto.service:auto-service:1.0-rc7'
	//低版本gradle（如3.3以下）不需要添加如下依赖，高版本需要天极爱，否则不会生成上述文件，不会注册服务
    annotationProcessor 'com.google.auto.service:auto-service:1.0-rc7'
	//生成代码使用
    implementation 'com.squareup:javapoet:1.11.1'





4、注意事项：
------如果android app模块是java实现，则依赖如下：
    implementation project(':dypos_annotation')
    annotationProcessor project(':dypos_compiler')
------如果android app模块是kotlin实现，则用到kapt，依赖如下：
    implementation project(':dypos_annotation')
    kapt project(':dypos_compiler')

------如果注解处理器模块使用了kotlin，则需要将上述的annotationProcessor变为kapt，并引入kotlin-kapt插件；
plugins {
    id 'java-library'
    id 'kotlin'
    id 'kotlin-kapt'
}
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"

    implementation project(path: ':annotation')
    implementation 'com.google.auto.service:auto-service:1.0-rc7'
    kapt 'com.google.auto.service:auto-service:1.0-rc7'
    implementation 'com.squareup:javapoet:1.11.1'
}














note：
1、什么是Element？
在Java语言中，Element是一个接口，表示一个程序元素，它可以指代包、类、方法或者一个变量。Element已知的子接口有如下几种：
------PackageElement 表示一个包程序元素。提供对有关包及其成员的信息的访问。
------ExecutableElement 表示某个类或接口的方法、构造方法或初始化程序（静态或实例），包括注释类型元素。
------TypeElement 表示一个类或接口程序元素。提供对有关类型及其成员的信息的访问。注意，枚举类型是一种类，而注解类型是一种接口。
------VariableElement 表示一个字段、enum 常量、方法或构造方法参数、局部变量或异常参数。

------示例：不同类型Element其实就是映射了Java中不同的类元素！
package com.zhpan.mannotation.factory;  //    PackageElement

public class Circle {  //  TypeElement

    private int i; //   VariableElement
    private Triangle triangle;  //  VariableElement

    public Circle() {} //    ExecuteableElement

    public void draw(   //  ExecuteableElement
                        String s)   //  VariableElement
    {
        System.out.println(s);
    }

    @Override
    public void draw() {    //  ExecuteableElement
        System.out.println("Draw a circle");
    }
}

如果我们得到了一个类，就可以遍历这个类的成员：
TypeElement fooClass = ... ;    
for (Element e : fooClass.getEnclosedElements()){ // iterate over children    
    Element parent = e.getEnclosingElement();  // parent == fooClass  
}  























