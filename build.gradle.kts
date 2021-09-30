plugins {
    kotlin("jvm") version "1.4.32"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.apache.pdfbox:pdfbox:2.0.24")
}

sourceSets {
    main {
    }
}
