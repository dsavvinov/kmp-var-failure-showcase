# Kotlin Multiplatform: Variant-Aware Resolution failure showcase

## Context

Build consists of two gradle projects: 
- `producer`, declares `jvm` and `linuxX64` targets (means that this Gradle project compiles to JVM and Linux)
- `consumer`, depends on `producer` (source-to-source dependency), contains an additional
  `js` target (meaning that this Gradle project compiles to JVM, Linux and JS)

This configuration can not work, shouldn't work, and it doesn't work currently

> Short explanation why it can't/shouldn't work. Feel free to skip if you understand why.
> 
> You can see that `producer` in its common code declares `expect object Expect` with
> `expect` function `greeting(): String` inside. `expect`/`actual` is Kotlin's substitute for 
> macroses. You can observe that in common code there's no body for function `greeting`. Bodies
> instead are provided in `actual` counterparts in platform-specific code (for JVM and Linux)
> 
> Then, consumer calls `Expect.greeting()` in its shared code as well. In order to actually
> compile that code to runnable platform-specific code, Kotlin compiler will pull `actual`-counterpart
> during JVM-compilation (yielding "Hello from JVM"), and `actual`-counterpart during Linux compilation.
> However, as `producer` doesn't provide implementation for JS, and `consumer` **wants** to compile for JS,
> that compilation is bound to fail (compiler won't be able to find the body for `Expect.greeting())

So, this is **a user's mistake**. However, the issue is how it is communicated currently

## Current behavior

1. `./gradlew build` fails with the following message:

```
FAILURE: Build failed with an exception.

* What went wrong:
Could not determine the dependencies of task ':consumer:compileKotlinJs'.
> Could not resolve all task dependencies for configuration ':consumer:jsCompileClasspath'.
   > Could not resolve project :producer.
     Required by:
         project :consumer
      > No matching variant of project :producer was found. The consumer was configured to find a usage of 'kotlin-api' of a library, preferably optimized for non-jvm, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'js', attribute 'org.jetbrains.kotlin.js.compiler' with value 'ir' but:
          - Variant 'jvmApiElements' capability kmp-var-failure-showcase:producer:unspecified declares an API of a library, preferably optimized for standard JVMs:
              - Incompatible because this component declares a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm' and the consumer needed a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
              - Other compatible attribute:
                  - Doesn't say anything about org.jetbrains.kotlin.js.compiler (required 'ir')
          - Variant 'jvmRuntimeElements' capability kmp-var-failure-showcase:producer:unspecified declares a runtime of a library, preferably optimized for standard JVMs:
              - Incompatible because this component declares a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm' and the consumer needed a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
              - Other compatible attribute:
                  - Doesn't say anything about org.jetbrains.kotlin.js.compiler (required 'ir')
          - Variant 'linuxX64ApiElements' capability kmp-var-failure-showcase:producer:unspecified declares a usage of 'kotlin-api' of a library, preferably optimized for non-jvm:
              - Incompatible because this component declares a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'native' and the consumer needed a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
              - Other compatible attribute:
                  - Doesn't say anything about org.jetbrains.kotlin.js.compiler (required 'ir')
          - Variant 'linuxX64CInteropApiElements' capability kmp-var-failure-showcase:producer:unspecified declares a library, preferably optimized for non-jvm:
              - Incompatible because this component declares a usage of 'kotlin-cinterop' of a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'native' and the consumer needed a usage of 'kotlin-api' of a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
              - Other compatible attribute:
                  - Doesn't say anything about org.jetbrains.kotlin.js.compiler (required 'ir')
          - Variant 'linuxX64MetadataElements' capability kmp-var-failure-showcase:producer:unspecified declares a library, preferably optimized for non-jvm:
              - Incompatible because this component declares a usage of 'kotlin-metadata' of a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'native' and the consumer needed a usage of 'kotlin-api' of a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
              - Other compatible attribute:
                  - Doesn't say anything about org.jetbrains.kotlin.js.compiler (required 'ir')
          - Variant 'metadataApiElements' capability kmp-var-failure-showcase:producer:unspecified declares a library, preferably optimized for non-jvm:
              - Incompatible because this component declares a usage of 'kotlin-metadata' of a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'common' and the consumer needed a usage of 'kotlin-api' of a component, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
              - Other compatible attribute:
                  - Doesn't say anything about org.jetbrains.kotlin.js.compiler (required 'ir')

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.

* Get more help at https://help.gradle.org

BUILD FAILED in 3s
```

**What happened?** 

During compilation for JS, we resolve the corresponding 
configuration (`:consumer:jsCompileClasspath` to be precise). This configuration has the following attribute set:

`org.jetbrains.kotlin.platform.type=js`

This is the main attribute Kotlin Multiplatform uses for modeling multiplatform libraries and their variants (there 
are more, but it's not relevant)

As the `producer` doesn't declare `js`-target, KGP doesn't create corresponding variants&configurations, so VAR doesn't find 
suitable configuration and fails. If you're a well-versed Gradle user, you can get that from the output: (parts 
irrelevant for the current failure are hidden)

```
<...>
> No matching variant of project :producer was found. <...>
  - Variant 'jvmApiElements' <...>
      - Incompatible because this component declares <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm' and the consumer needed <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
      <...>
  - Variant 'jvmRuntimeElements' <...>
      - Incompatible because this component declares <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm' and the consumer needed <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
      <...>
  - Variant 'linuxX64ApiElements' <...>
      - Incompatible because this component declares <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'native' and the consumer needed <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
      <...>
  - Variant 'linuxX64CInteropApiElements' <...>
      - Incompatible because this component declares <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'native' and the consumer needed <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
      <...>
  - Variant 'linuxX64MetadataElements' <...>
      - Incompatible because this component declares <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'native' and the consumer needed <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
      <...>
  - Variant 'metadataApiElements' <...>
      - Incompatible because this component declares <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'common' and the consumer needed <...> attribute 'org.jetbrains.kotlin.platform.type' with value 'js'
      <...>
```

## Desired behavior

The very first simplest draft:

```
* What went wrong:
Could not determine the dependencies of task ':consumer:compileKotlinJs'.
> Could not resolve all task dependencies for configuration ':consumer:jsCompileClasspath'.
   > Could not resolve project :producer.
     Required by:
         project :consumer
    > Kotlin Multiplatform: project :consumer requested a JS-target, but :producer doesn't provide JS-support

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.

* Get more help at https://help.gradle.org

BUILD FAILED in 3s
  
```

The predicate for emitting this message:
* There's no variant of the dependency which declares `org.jetbrains.kotlin.platform.type` with a compatible value

You can see that the current verbose explanation actually already contains enough information for detecting this 
predicate, even with the plain String matching
