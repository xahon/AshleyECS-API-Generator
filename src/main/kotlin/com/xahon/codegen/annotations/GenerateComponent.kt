package com.xahon.codegen.annotations

@Target(AnnotationTarget.CLASS)
annotation class GenerateComponent(val prefix: String = "Generated")