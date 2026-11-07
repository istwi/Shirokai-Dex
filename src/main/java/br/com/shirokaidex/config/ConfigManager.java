package br.com.shirokaidex.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Reads/writes must be called from the configuration executor, never a menu callback. */
public final class ConfigManager {
    private final Path directory;
    private final AtomicReference<Settings> current = new AtomicReference<>();
    private volatile boolean recovered;

    public ConfigManager(Path directory) { this.directory = directory; }
    public Settings current() {
        Settings settings = current.get();
        if (settings == null) throw new IllegalStateException("Configuration has not initialized");
        return settings;
    }
    public boolean recoveredFromBackup() { return recovered; }

    public synchronized void initialize() throws IOException {
        Files.createDirectories(directory);
        Path config = directory.resolve("config.json");
        if (Files.notExists(config)) {
            writeAtomic(config, "{\n  \"schema_version\": 1,\n  \"language\": \"pt_br\",\n  \"menu_title\": \"✦ Shirokai Dex\"\n}\n");
        }
        try { reload(); }
        catch (IOException failure) {
            Path backup = directory.resolve("config.last-good.json");
            if (!Files.isRegularFile(backup)) throw failure;
            try { current.set(parse(readBounded(backup))); recovered = true; }
            catch (IOException invalidBackup) { failure.addSuppressed(invalidBackup); throw failure; }
        }
    }

    public synchronized Settings reload() throws IOException {
        String json = readBounded(directory.resolve("config.json"));
        Settings candidate = parse(json);
        // Persist the validated recovery copy before publishing the immutable candidate.
        writeAtomic(directory.resolve("config.last-good.json"), json);
        current.set(candidate);
        recovered = false;
        return candidate;
    }

    private static String readBounded(Path path) throws IOException {
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(65537);
            if (bytes.length > 65536) throw new IOException(path.getFileName() + ": maximum size is 64 KiB");
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public static Settings parse(String json) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            Set<String> keys = new HashSet<>();
            Integer version = null;
            String language = null, title = null;
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (!keys.add(key)) throw new IOException("config.json: duplicate field " + key);
                switch (key) {
                    case "schema_version" -> {
                        if (reader.peek() != JsonToken.NUMBER) throw new IOException("schema_version must be an integer");
                        String value = reader.nextString();
                        if (!value.equals("1")) throw new IOException("Unsupported schema_version: " + value);
                        version = 1;
                    }
                    case "language" -> language = string(reader, key);
                    case "menu_title" -> title = string(reader, key);
                    default -> throw new IOException("config.json: unknown field " + key);
                }
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("config.json: trailing content");
            if (version == null || language == null || title == null) throw new IOException("config.json: schema_version, language and menu_title are required");
            if (!Set.of("pt_br", "en_us").contains(language)) throw new IOException("config.json: unsupported language");
            if (title.isBlank() || title.length() > 128 || title.codePoints().anyMatch(Character::isISOControl)) throw new IOException("config.json: invalid menu_title");
            return new Settings(version, language, title);
        } catch (IllegalStateException | IllegalArgumentException failure) {
            throw new IOException("config.json: invalid field or JSON structure", failure);
        }
    }

    private static String string(JsonReader reader, String field) throws IOException {
        if (reader.peek() != JsonToken.STRING) throw new IOException("config.json: " + field + " must be a string");
        return reader.nextString();
    }
    private static void writeAtomic(Path target, String value) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), ".config-", ".tmp");
        try {
            Files.writeString(temporary, value, StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}
