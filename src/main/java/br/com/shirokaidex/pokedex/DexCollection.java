package br.com.shirokaidex.pokedex;

public enum DexCollection {
    NATIONAL("national"), SHINY("shiny"), LEGENDARY("legendary");

    private final String id;
    DexCollection(String id) { this.id = id; }
    public String id() { return id; }
}
