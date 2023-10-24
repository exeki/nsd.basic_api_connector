import org.gradle.configurationcache.serialization.codecs.Encoding
import org.gradle.configurationcache.serialization.codecs.EncodingProducer
import org.gradle.internal.impldep.com.amazonaws.util.EncodingScheme
import org.gradle.internal.impldep.com.amazonaws.util.EncodingSchemeEnum

plugins {
    id("java-library")
    id("maven-publish")
    id("groovy")
}

group = "ru.ekazantsev"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("org.apache.httpcomponents:httpclient:4.5.14")
    api("org.apache.httpcomponents:httpmime:4.5.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("org.slf4j:slf4j-api:2.0.9")

    testImplementation("ch.qos.logback:logback-classic:1.4.11")
    testImplementation("org.codehaus.groovy:groovy-all:3.0.19")
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.javadoc {
    options.encoding = "UTF-8"
}


