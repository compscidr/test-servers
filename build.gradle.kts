import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlinter)
    id("jacoco")
    alias(libs.plugins.git.version) // https://stackoverflow.com/a/71212144
    alias(libs.plugins.sonatype.maven.central)
    alias(libs.plugins.gradleup.nmcp)
}

dependencies {
    api(libs.slf4j.api)
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
    implementation(libs.logback.classic)
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
    testLogging {
        // get the test stdout / stderr to show up when we run gradle from command line
        // https://itecnote.com/tecnote/gradle-how-to-get-output-from-test-stderr-stdout-into-console/
        // https://developer.android.com/studio/test/advanced-test-setup
        // https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html
        outputs.upToDateWhen {true}
        showStandardStreams = true
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}