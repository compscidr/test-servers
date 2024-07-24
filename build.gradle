plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id "application"
}

dependencies {
    api(libs.slf4j.api)
    implementation(libs.logback.classic)
    testImplementation "org.jetbrains.kotlin:kotlin-test"
}

test {
    useJUnitPlatform()
}

compileKotlin {
    kotlinOptions.jvmTarget = '17'
}

compileTestKotlin {
    kotlinOptions.jvmTarget = '17'
}

application {
    mainClassName = 'UDPServer'
}