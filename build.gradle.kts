plugins {
    java
    id("net.neoforged.moddev") version "2.0.147"
}

group = "dev.neobugify"
version = project.property("modVersion") as String
base.archivesName.set("NeoBugify")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

neoForge {
    version = project.property("neoForgeVersion") as String

    // NeoBugify is a single source set: the vanilla joined jar contains the client
    // classes, the client only parts are guarded with a FMLEnvironment.dist check.
    addModdingDependenciesTo(sourceSets.main.get())

    mods {
        create("neobugify") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()

            systemProperty("neoforge.enabledGameTestNamespaces", "neobugify")
            jvmArgument("-Dneobugify.forceMacFixes=true")
            jvmArgument("-Dneobugify.forceLinuxFixes=true")
            jvmArgument("-Dneobugify.forceWindowsFixes=true")
        }

        create("server") {
            server()

            programArgument("--nogui")
            jvmArgument("-Dneobugify.forceMacFixes=true")
            jvmArgument("-Dneobugify.forceLinuxFixes=true")
            jvmArgument("-Dneobugify.forceWindowsFixes=true")
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // YACL is an optional dependency at runtime, it is only needed to build the
    // config screen. The jar is the one shipped next to this project.
    compileOnly(files("libs/yet_another_config_lib_v3-3.8.2+1.21.1-neoforge.jar"))
    runtimeOnly(files("libs/yet_another_config_lib_v3-3.8.2+1.21.1-neoforge.jar"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val replacements = mapOf("version" to project.version.toString())
    inputs.properties(replacements)

    filesMatching(listOf("META-INF/neoforge.mods.toml")) {
        expand(replacements)
    }
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to "NeoBugify",
            "Specification-Vendor" to "NeoBugify",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "NeoBugify",
            "MixinConfigs" to "neobugify.mixins.json,neobugify.client.mixins.json"
        )
    }
}