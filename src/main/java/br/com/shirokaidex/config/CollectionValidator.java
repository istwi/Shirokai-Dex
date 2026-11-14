package br.com.shirokaidex.config;

import br.com.shirokaidex.pokedex.*;
import br.com.shirokaidex.categories.CategoryCatalog;
import java.io.IOException;
import java.util.*;

/** Immutable registry snapshot captured on the server thread; validation itself is safe off-thread. */
public final class CollectionValidator {
    private final Set<String> species, labels, types, forms, items;
    public CollectionValidator(Collection<ProgressCalculator.Species> catalog, Set<String> items) {
        Set<String> ids = new HashSet<>(), labels = new HashSet<>(), types = new HashSet<>(), forms = new HashSet<>();
        // These are verified Cobblemon labels even if the current datapacks contain no matching species.
        labels.addAll(Set.of("legendary", "mythical", "ultra_beast"));
        for (var entry : catalog) {
            ids.add(entry.id());
            for (var form : entry.forms()) { labels.addAll(form.labels()); types.addAll(form.types()); forms.add(form.name()); }
        }
        this.species = Set.copyOf(ids); this.labels = Set.copyOf(labels); this.types = Set.copyOf(types);
        this.forms = Set.copyOf(forms); this.items = Set.copyOf(items);
    }
    public void validate(Settings settings) throws IOException {
        validate(settings.categories());
        for(var achievement:settings.achievements()){if(!items.contains(achievement.icon())||achievement.icon().equals("minecraft:air"))throw new IOException("Unknown achievement icon");validateRequirement(achievement.requirement());}
        for(var layout:settings.menus().layouts().values())for(var button:layout.buttons().values())
            if(!button.icon().isEmpty()&&!items.contains(button.icon()))throw new IOException("config.json menus."+layout.screen().id()+": unknown menu icon "+button.icon());
        for (var reward : settings.rewards()) {
            try { reward.json(); }
            catch(IllegalArgumentException failure){throw new IOException("config.json rewards."+reward.id()+": encoded plan exceeds limits",failure);}
            for (var item : reward.items()) if (item.id().equals("minecraft:air") || !items.contains(item.id()))
                throw new IOException("config.json rewards." + reward.id() + ": unknown or empty item " + item.id());
        }
        for (var definition : settings.collections()) {
            if (!items.contains(definition.icon())) throw new IOException("config.json collections." + definition.id() + ".icon: unknown item " + definition.icon());
            check(definition, "include_species", definition.criteria().includeSpecies(), species);
            check(definition, "exclude_species", definition.criteria().excludeSpecies(), species);
            check(definition, "labels", definition.criteria().labels(), labels);
            check(definition, "types", definition.criteria().types(), types);
            check(definition, "forms", definition.criteria().forms(), forms);
        }
    }
    private void validateRequirement(br.com.shirokaidex.achievements.Requirement requirement)throws IOException {
        if(requirement instanceof br.com.shirokaidex.achievements.Requirement.Form form){check("achievement.form.","species",Set.of(form.species()),species);check("achievement.form.","form",Set.of(form.form()),forms);}
        else if(requirement instanceof br.com.shirokaidex.achievements.Requirement.Count count&&count.metric()==br.com.shirokaidex.rankings.CaptureRanking.Metric.SPECIES)check("achievement.capture.","species",Set.of(count.key()),species);
        else if(requirement instanceof br.com.shirokaidex.achievements.Requirement.All all)for(var child:all.conditions())validateRequirement(child);
        else if(requirement instanceof br.com.shirokaidex.achievements.Requirement.Any any)for(var child:any.conditions())validateRequirement(child);
    }
    public void validate(CategoryCatalog catalog) throws IOException {
        for (var definition : catalog.definitions().values()) {
            String path = "config.json categories." + definition.id() + ".criteria.";
            var criteria = definition.criteria();
            check(path, "include_species", criteria.includeSpecies(), species);
            check(path, "exclude_species", criteria.excludeSpecies(), species);
            check(path, "labels", criteria.labels(), labels);
            check(path, "types", criteria.types(), types);
            check(path, "forms", criteria.forms(), forms);
        }
    }
    private static void check(CollectionDefinition definition, String key, Set<String> values, Set<String> known) throws IOException {
        check("config.json collections." + definition.id() + ".criteria.", key, values, known);
    }
    private static void check(String path, String key, Set<String> values, Set<String> known) throws IOException {
        for (String value : values) if (!known.contains(value)) throw new IOException(path + key + ": unknown reference " + value);
    }
}
