package com.gmail.marcosav2010.module

import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener
import org.atteo.classindex.IndexAnnotated
import kotlin.reflect.KClass

@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@IndexAnnotated
annotation class ModuleDescriptor(val load: Boolean = true,
                                  val name: String,
                                  val priority: Int = 0,
                                  val scope: KClass<out ModuleScope>,
                                  val listeners: Array<KClass<out PacketListener>>)