package br.com.shirokaidex.menus.layout;

import br.com.shirokaidex.pokedex.DexProgress;
import java.util.OptionalInt;

public final class ProgressMeter {
    private ProgressMeter() { }
    /** Never fills the visual bar before real completion; empty/unknown data has no bar. */
    public static OptionalInt filled(DexProgress value,int segments){
        if(segments<1||segments>32)throw new IllegalArgumentException("invalid progress bar length");
        if(value.total()==0||value.registered().isEmpty())return OptionalInt.empty();
        return OptionalInt.of((int)((long)value.registered().getAsInt()*segments/value.total()));
    }
}
