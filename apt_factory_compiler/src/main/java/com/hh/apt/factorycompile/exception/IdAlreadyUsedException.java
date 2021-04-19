package com.hh.apt.factorycompile.exception;


import com.hh.apt.factorycompile.generator.FactoryAnnotatedClass;

/**
 * Package: com.hh.apt.compile.exception
 * User: hehao3
 * Email: hehao3@jd.com
 * Date: 2021/4/12
 * Time: 上午10:56
 * Description:
 */
public class IdAlreadyUsedException extends RuntimeException {
    public IdAlreadyUsedException(FactoryAnnotatedClass factoryAnnotatedClass) {

    }
}
