package br.com.shirokaidex.pokedex;

import java.util.Locale;

/** One species/form backed by a verified shiny capture, independent of official seen/owned knowledge. */
public record CapturedShinyForm(String species,String form) {
    public CapturedShinyForm {CollectionDefinition.identifier(species);if(form==null||form.isBlank()||form.length()>128)throw new IllegalArgumentException("Invalid captured form");form=form.toLowerCase(Locale.ROOT);}
}
