plugins {
    id("net.fabricmc.fabric-loom")
}

base {
    archivesName.set("FabisOSC-Fabric")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation(project(":common"))
}

tasks.jar {
    // common direkt mit in die Mod-Jar packen
    from(project(":common").the<SourceSetContainer>()["main"].output)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}
