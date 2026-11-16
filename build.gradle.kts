plugins {
    id("net.fabricmc.fabric-loom") version "1.19.3" apply false
}

subprojects {
    group = "de.fabisosc"
    version = property("mod_version") as String

    plugins.withType<JavaPlugin> {
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(25)
        }
    }
}
