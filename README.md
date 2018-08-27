# AshleyECS-API-Generator
Generates entities API like in Entitas (C#) using developer's ECS components (Kotlin)

# What for?
The generator creates `Entity` extension methods for your components.  

e.g. for component
```Kotlin
@GenerateComponent
class Position(val x: Float, val y: Float) : Component
```

will be generated next API:  

```Kotlin
class GeneratedPosition : Component {
    companion object {
        val Mapper: ComponentMapper<fully.qualified.name.Position> =
                ComponentMapper.getFor(fully.qualified.name.Position::class.java)
    }
}

fun Entity.addPosition(position: fully.qualified.name.Position): fully.qualified.name.Position = this.addAndReturn(position) as fully.qualified.name.Position

fun Entity.removePosition(): fully.qualified.name.Position? = this.remove(fully.qualified.name.Position::class.java) as fully.qualified.name.Position

val Entity.getPosition: fully.qualified.name.Position?
    get() = GeneratedPosition.Mapper[this]

val Entity.hasPosition: Boolean
    get() = getPosition != null
```

So, you will be able to use:
```Kotlin
// Some system

val entity: Entity = /* get some entity */

if (entity.hasPosition) {
  val x = entity.getPosition!!.x
  // Do something
} else {
  entity.addPosition(fully.qualified.name.Position(2f, 10f))
}

entity.removePosition()
if (entity.getPosition == null) {
   // This branch will be executed because get() will return null
}

engine.addEntity(Entity().apply {
  this.addPosition(/* Some position */)
  this.addRotation(/* Some rotation */)
  this.addWhateverElse(/* x_x */)
})

```

There is another annotation `@InitFlagComponent` in combination with `@GenerateComponent` will omit 
`get*()` function (It will be private in source code) and `add*()` function will be generated without 
arguments

```Kotlin
@GenerateComponent
@InitFlagComponent
class PlayerInitFlag: Component

entity.addPlayerInitFlag() 

...

if (entity.hasPlayerInitFlag) {
  // Do something
}

```

That API uses Ashley's `ComponentMapper` and as developers say it has `O(1)` complexity to 
get needed component


# How does it work in Intellij IDEA?
To use that generator you don't need to enable 'Annotation processing'
To process annotations you don't need to run gradle from terminal

JetBrains says that kapt is not supported by IDEA's build system: 
> Please note that kapt is still not supported for IntelliJ IDEA’s own build system. Launch the build from the “Maven Projects” toolbar whenever you want to re-run the annotation processing.  
>  
> Source (https://kotlinlang.org/docs/reference/kapt.html)

But this is working fine for me even if I just run project after clean


# How to add into project?
I'm using it in Libgdx application like this:

```Gradle
project(":core") {
    apply plugin: "kotlin"
    apply plugin: "kotlin-kapt"

    dependencies {
        compileOnly "com.github.xahon:AshleyECS-API-Generator:-SNAPSHOT"
        kapt "com.github.xahon:AshleyECS-API-Generator:-SNAPSHOT"
        ...
    }
}
```
