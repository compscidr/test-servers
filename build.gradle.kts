import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlinter)
    id("jacoco")
    alias(libs.plugins.git.version) // https://stackoverflow.com/a/71212144
    alias(libs.plugins.sonatype.maven.central)
    alias(libs.plugins.gradleup.nmcp.aggregation)
}

dependencies {
    api(libs.slf4j.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.test.runtime)
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

version = "0.0.0-SNAPSHOT"
gitVersioning.apply {
    refs {
        branch(".+") { version = "\${ref}-SNAPSHOT" }
        tag("v(?<version>.*)") { version = "\${ref.version}" }
    }
}

// see: https://github.com/vanniktech/gradle-maven-publish-plugin/issues/747#issuecomment-2066762725
// and: https://github.com/GradleUp/nmcp
nmcpAggregation {
    val props = project.properties
    centralPortal {
        username = props["centralPortalToken"] as String? ?: ""
        password = props["centralPortalPassword"] as String? ?: ""
        // or if you want to publish automatically
        publishingType = "AUTOMATIC"
    }
}

// see: https://vanniktech.github.io/gradle-maven-publish-plugin/central/#configuring-the-pom
mavenPublishing {
    coordinates("com.jasonernst.test-servers", "test-servers", version.toString())
    pom {
        name = "test-servers"
        description = "A collection of test servers writte in Kotlin to run protocol tests against"
        inceptionYear = "2024"
        url = "https://github.com/compscidr/test-servers"
        licenses {
            license {
                name = "GPL-3.0"
                url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "compscidr"
                name = "Jason Ernst"
                url = "https://www.jasonernst.com"
            }
        }
        scm {
            url = "https://github.com/compscidr/test-servers"
            connection = "scm:git:git://github.com/test-servers.git"
            developerConnection = "scm:git:ssh://git@github.com/compscidr/test-servers.git"
        }
    }

    signAllPublications()
}