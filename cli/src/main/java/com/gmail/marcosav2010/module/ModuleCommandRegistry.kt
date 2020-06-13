package com.gmail.marcosav2010.module

import com.gmail.marcosav2010.command.CommandRegistry
import kotlin.reflect.KClass

@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class ModuleCommandRegistry(val value: KClass<out CommandRegistry>)