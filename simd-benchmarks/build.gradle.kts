plugins {
    `java`
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    implementation(project(":pufferfork-api"))
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    runtimeOnly("org.junit.platform:junit-platform-launcher:1.13.1")
}

jmh {
    resultsFile.set(layout.buildDirectory.file("benchmark-results.txt"))
    jvmArgs.set(listOf("--add-modules", "jdk.incubator.vector"))
}