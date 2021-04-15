package com.dev.hh.annotation


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class GetMsg(val id: Int, val name: String)