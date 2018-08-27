package com.xahon.codegen.generators

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.google.auto.service.AutoService
import com.xahon.codegen.annotations.GenerateComponent
import com.xahon.codegen.annotations.InitFlagComponent
import com.xahon.codegen.generators.ComponentProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement


private class BuildInfo(
        val originalTypeName: TypeName,
        val generationPrefix: String,
        val simpleName: String,
        val packageName: String,
        val isInit: Boolean
)

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ComponentProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(GenerateComponent::class.java.name)
    }

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        println("process")
        roundEnv.getElementsAnnotatedWith(GenerateComponent::class.java)
                .forEach {
                    val className = it.simpleName.toString()
                    println("Processing: $className")
                    val packageName = processingEnv.elementUtils.getPackageOf(it).toString()
                    generateClass(
                            BuildInfo(
                                    it.asType().asTypeName(),
                                    it.getAnnotation(GenerateComponent::class.java).prefix,
                                    className,
                                    packageName,
                                    it.getAnnotation(InitFlagComponent::class.java) != null
                            )
                    )
                }
        return true
    }

    private fun generateClass(buildInfo: BuildInfo) {
        val fileName = "${buildInfo.generationPrefix}${buildInfo.simpleName}"

        val file = FileSpec.builder(buildInfo.packageName, fileName)
                .addAliasedImport(com.badlogic.ashley.core.Entity::class.java, "Entity")

                // Create class with mapper
                .addType(TypeSpec.classBuilder(fileName)
                        .addSuperinterface(Component::class)

                        // Mapper field
                        .companionObject(
                                TypeSpec.companionObjectBuilder()
                                        .addProperty(
                                                PropertySpec.builder(
                                                        "Mapper",
                                                        ParameterizedTypeName.get(ComponentMapper::class.java.asClassName(), buildInfo.originalTypeName),
                                                        KModifier.PUBLIC
                                                ).initializer("ComponentMapper.getFor(${buildInfo.originalTypeName}::class.java)").build()
                                        ).build()
                        )
                        .build()
                )

        // Add function
        file.addFunction(
                generateAddFunction(buildInfo).build()
        )

        // Remove function
        file.addFunction(
                generateRemoveFunction(buildInfo).build()
        )

        // Get property
        file.addProperty(
                generateGetFunction(buildInfo).build()
        )

        // Has function
        file.addProperty(
                generateHasFunction(buildInfo).build()
        )

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.build().writeTo(File(kaptKotlinGeneratedDir, "$fileName.kt"))
    }

    private fun generateHasFunction(buildInfo: BuildInfo): PropertySpec.Builder {
        return PropertySpec.builder("Entity.has${buildInfo.simpleName}", Boolean::class)
                .getter(
                        FunSpec.getterBuilder()
                                .addStatement("return get${buildInfo.simpleName} != null")
                                .build()
                )
    }

    private fun generateGetFunction(buildInfo: BuildInfo): PropertySpec.Builder {
        return PropertySpec.builder(
                "Entity.get${buildInfo.simpleName}",
                buildInfo.originalTypeName.asNullable(),
                if (buildInfo.isInit) KModifier.PRIVATE else KModifier.PUBLIC
        )
                .getter(
                        FunSpec.getterBuilder()
                                .addStatement("return ${buildInfo.generationPrefix}${buildInfo.simpleName}.Mapper[this]")
                                .build()
                )
    }

    private fun generateRemoveFunction(buildInfo: BuildInfo): FunSpec.Builder {
        return FunSpec.builder("Entity.remove${buildInfo.simpleName}")
                .addStatement("this.remove(${buildInfo.originalTypeName}::class.java)")
    }

    private fun generateAddFunction(buildInfo: BuildInfo): FunSpec.Builder {
        val funSpec = FunSpec.builder("Entity.add${buildInfo.simpleName}")

        if (!buildInfo.isInit) {
            funSpec.addParameter(buildInfo.simpleName.toLowerCase(), buildInfo.originalTypeName)
                    .returns(buildInfo.originalTypeName)
                    .addStatement("return this.addAndReturn(${buildInfo.simpleName.toLowerCase()}) as ${buildInfo.originalTypeName}")
        } else {
            funSpec.addStatement("return this.addAndReturn(${buildInfo.originalTypeName}())")
        }

        return funSpec
    }
}
