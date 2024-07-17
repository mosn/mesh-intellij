import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.5.2"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.6"

    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "1.3.1"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"


    // build for cli
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenLocal()
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven(url = "https://mvn.cloud.alipay.com/nexus/content/groups/open")
    mavenCentral()
}

val goland: Configuration by configurations.creating
val terminal: Configuration by configurations.creating {
    extendsFrom(goland)
}

dependencies {
    // https://mavenlibs.com/maven/dependency/com.google.code.gson/gson
    goland("com.google.code.gson:gson:2.10")

    // https://mvnrepository.com/artifact/com.alibaba/fastjson
    goland("com.alibaba:fastjson:1.2.83")

    // https://mvnrepository.com/artifact/io.fabric8/kubernetes-client
    goland("io.fabric8:kubernetes-client:6.8.1") {
        exclude(group = "org.slf4j")
    }

    goland("com.mysql:mysql-connector-j:8.0.33"){
        exclude(group = "com.google.protobuf")
    }

    // sofa registry for fetch mesh server console address

    goland("javax.xml.bind:jaxb-api:2.3.1")
    goland("com.alipay.sofa.cloud:sdk-core:0.1")

    goland("commons-lang:commons-lang:2.6")
    goland("commons-logging:commons-logging:1.2")
    goland("commons-pool:commons-pool:1.6")
    goland("commons-io:commons-io:2.7")

    goland("com.alipay.sofa:hessian:3.3.10")
    goland("com.alipay.sofa:bolt:1.6.5") {
        exclude(group = "org.slf4j")
    }
    goland("com.alipay.sofa.common:sofa-common-tools:1.0.22") {
        exclude(group = "org.slf4j")
    }
    goland("com.alipay.sofa:sofa-common-configs:1.1.6") {
        exclude(group = "org.slf4j")
        exclude(group = "com.alipay.bkmi")
    }
    goland("com.alipay.sofa.lookout:lookout-api:1.6.1")
    goland("com.alipay.sofa:registry-client-all:5.3.1.cloud.20220531") {
        exclude(group = "org.slf4j")
    }

    goland("com.antcloud.antvip:antcloud-antvip-common:1.1.5")
    goland("com.alibaba.toolkit.common:toolkit-common-lang:1.1.5")
    goland("com.alibaba.toolkit.common:toolkit-common-logging:1.0")
    goland("com.antcloud.antvip:antcloud-antvip-client:1.1.5")

    goland("com.alipay.sofa:registry-client-enterprise-all:5.5.1.RELEASE")
    // end of : sofa registry for fetch mesh server console address

    goland("com.intellij:forms_rt:7.0.3")

    // local dependencies
    goland(fileTree(mapOf("dir" to "src/main/libs", "include" to listOf("*.jar"))))

    // only local start SubscribeConsoleAddress.main
//     goland("org.slf4j:slf4j-api:1.7.21")
    // goland 出包，禁用一下包
//    terminal("org.slf4j:slf4j-api:1.7.21")
//    terminal("ch.qos.logback:logback-classic:1.2.9")
}

configurations {
    compileClasspath.extendsFrom(goland)
    runtimeClasspath.extendsFrom(goland)
}


// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(properties("pluginVersion"))
    groups.set(emptyList())
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(projectDir.resolve(".qodana").canonicalPath)
    reportPath.set(projectDir.resolve("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}

// build for cli
application {
    mainClass.set("io.mosn.coder.cli.Cli")
}

tasks.runIde {
    jvmArgs("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED", "-Xmx2048m")
}

tasks {

    // Set the JVM compatibility versions
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
                projectDir.resolve("README.md").readText().lines().run {
                    val start = "<!-- Plugin description -->"
                    val end = "<!-- Plugin description end -->"

                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end))
                }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(provider {
            changelog.run {
                getOrNull(properties("pluginVersion")) ?: getLatest()
            }.toHTML()
        })
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}

/**
 * Generate goland extension for running plugin.
 */
tasks.register("goland") {

    configurations {
        compileClasspath.extendsFrom(goland)
        runtimeClasspath.extendsFrom(goland)
    }

    project.delete {
        delete(fileTree("build/distributions"))
        delete(fileTree(mapOf("dir" to "build/plugins", "include" to listOf("mosn-intellij-*.zip"))))
    }

    // clean build and package dist zip first
    dependsOn(tasks["distZip"])

    doLast {

        /**
         * copy to release directory
         */
        copy {
            into("build/plugins/goland")
            from("build/distributions") {
                exclude("mosn-intellij-shadow*.zip")
                include("mosn-intellij-*.zip")
            }
        }
    }
}

/**
 * Generate terminal fat jar for running plugin command.
 */
tasks.register("terminal") {

    configurations {
        compileClasspath.extendsFrom(terminal)
        runtimeClasspath.extendsFrom(terminal)
    }

    project.delete {
        delete(fileTree("build/libs"))
        delete(fileTree(mapOf("dir" to "build/plugins", "include" to listOf("*-all.jar"))))
    }

    // clean build and package dist zip first
    dependsOn(tasks["build"])

    doLast {

        /**
         * copy to release directory
         */
        copy {
            into("build/plugins/terminal")
            from("build/libs") {
                include("*-all.jar")
            }
        }
    }
}