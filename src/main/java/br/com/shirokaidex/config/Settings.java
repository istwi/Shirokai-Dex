package br.com.shirokaidex.config;

public record Settings(int schemaVersion, String language, String menuTitle) {
    public static Settings defaults() {
        return new Settings(1, "pt_br", "✦ Shirokai Dex");
    }
}
