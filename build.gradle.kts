plugins {
    kotlin("jvm") version "2.0.21"
    `maven-publish`
    signing
}

group = "com.philiprehberger"
version = project.findProperty("version") as String? ?: "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("cache-kit")
                description.set("Lightweight coroutine-aware in-memory cache with TTL and LRU eviction")
                url.set("https://github.com/philiprehberger/kt-cache-kit")
                licenses { license { name.set("MIT License"); url.set("https://opensource.org/licenses/MIT") } }
                developers { developer { id.set("philiprehberger"); name.set("Philip Rehberger") } }
                scm { url.set("https://github.com/philiprehberger/kt-cache-kit"); connection.set("scm:git:git://github.com/philiprehberger/kt-cache-kit.git"); developerConnection.set("scm:git:ssh://github.com/philiprehberger/kt-cache-kit.git") }
            }
        }
    }
    repositories { maven { name = "OSSRH"; url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"); credentials { username = System.getenv("OSSRH_USERNAME"); password = System.getenv("OSSRH_PASSWORD") } } }
}

signing {
    val signingKey = System.getenv("GPG_PRIVATE_KEY")
    val signingPassword = System.getenv("GPG_PASSPHRASE")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(publishing.publications["maven"])
}

tasks.withType<Sign>().configureEach {
    onlyIf { System.getenv("GPG_PRIVATE_KEY") != null }
}
