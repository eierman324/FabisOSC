package de.fabisosc.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import de.fabisosc.core.OscSettings;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Liest und schreibt config/fabisosc.json. */
final class FabricConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private FabricConfig() {
    }

    static OscSettings load(Logger log) {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("fabisosc.json");
        OscSettings settings = new OscSettings();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                OscSettings read = GSON.fromJson(reader, OscSettings.class);
                if (read != null) settings = read;
            } catch (IOException | JsonParseException e) {
                log.warn("config/fabisosc.json ist kaputt, nutze Standardwerte", e);
                return settings.sanitize();
            }
        }
        settings.sanitize();
        // Datei (neu) schreiben, damit auch neue Optionen drinstehen
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(settings, writer);
            }
        } catch (IOException e) {
            log.warn("Konnte config/fabisosc.json nicht schreiben", e);
        }
        return settings;
    }
}
