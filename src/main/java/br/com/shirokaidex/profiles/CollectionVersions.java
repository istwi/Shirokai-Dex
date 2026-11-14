package br.com.shirokaidex.profiles;

import br.com.shirokaidex.pokedex.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

/** Canonical identity of the actual registry-derived counting function, not of its display metadata. */
public final class CollectionVersions {
    private CollectionVersions() { }
    public static Map<String,String> of(Collection<ProgressCalculator.Species> catalog,List<CollectionDefinition> definitions){
        var versions=new HashMap<String,String>();
        for(var definition:definitions){
            var entries=new TreeMap<String,Set<String>>();
            for(var species:catalog){if(!definition.criteria().acceptsSpecies(species.id()))continue;var forms=new TreeSet<String>();for(var form:species.forms())if(definition.acceptsForm(species.id(),form))forms.add(form.name());if(definition.filtersForms()&&forms.isEmpty())continue;entries.computeIfAbsent(species.id(),unused->new TreeSet<>()).addAll(forms);}
            var value=new StringBuilder(definition.mode().name()).append('|').append(definition.filtersForms()).append('\n');for(var entry:entries.entrySet()){value.append(entry.getKey()).append('\n');for(String form:entry.getValue())value.append(form.length()).append(':').append(form).append('\n');}
            try{versions.put(definition.id(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8))));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
        }return Map.copyOf(versions);
    }
}
