plugins {
    java
}

base {
    archivesName.set("FabisOSC-Paper")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":common"))
    compileOnly("io.papermc.paper:paper-api:${property("paper_api_version")}")
}

tasks.jar {
    // common direkt mit in die Plugin-Jar packen
    from(project(":common").the<SourceSetContainer>()["main"].output)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}
