package br.com.shirokaidex.config;

import br.com.shirokaidex.captures.CaptureRetention;
import com.google.gson.stream.*;
import java.io.IOException;
import java.util.*;

final class CaptureRetentionCodec {
    private CaptureRetentionCodec() { }
    static CaptureRetention read(JsonReader reader) throws IOException {
        boolean enabled = true, cleanup = true; int days = 90, batch = 1000;
        var fields = new HashSet<String>(); reader.beginObject();
        while (reader.hasNext()) {
            String field = reader.nextName();
            if (!fields.add(field)) throw new IOException("Duplicate capture_history field");
            switch (field) {
                case "enabled" -> enabled = bool(reader);
                case "retention_days" -> days = integer(reader);
                case "cleanup" -> {
                    var nested = new HashSet<String>(); reader.beginObject();
                    while (reader.hasNext()) {
                        String key = reader.nextName();
                        if (!nested.add(key)) throw new IOException("Duplicate capture cleanup field");
                        switch (key) {
                            case "enabled" -> cleanup = bool(reader);
                            case "batch_size" -> batch = integer(reader);
                            default -> throw new IOException("Unknown capture cleanup field");
                        }
                    }
                    reader.endObject();
                }
                default -> throw new IOException("Unknown capture_history field");
            }
        }
        reader.endObject(); return new CaptureRetention(enabled, days, cleanup, batch);
    }
    private static boolean bool(JsonReader reader) throws IOException {
        if (reader.peek() != JsonToken.BOOLEAN) throw new IOException("Capture history boolean required");
        return reader.nextBoolean();
    }
    private static int integer(JsonReader reader) throws IOException {
        if (reader.peek() != JsonToken.NUMBER) throw new IOException("Capture history integer required");
        String value = reader.nextString();
        if (!value.matches("[0-9]{1,5}")) throw new IOException("Capture history integer required");
        return Integer.parseInt(value);
    }
}
