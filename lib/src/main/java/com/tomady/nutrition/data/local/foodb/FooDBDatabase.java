package com.tomady.nutrition.data.local.foodb;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {
    Compound.class, Content.class, AccessionNumber.class, CompoundAlternateParent.class,
    CompoundExternalDescriptor.class, CompoundOntologyTerm.class, CompoundSubstituent.class,
    CompoundSynonym.class, CompoundsEnzyme.class, CompoundsFlavor.class, Enzyme.class,
    Food.class, CompoundsPathway.class, Flavor.class, FoodTaxonomy.class,
    CompoundsHealthEffect.class, HealthEffect.class, NcbiTaxonomyMap.class, Nutrient.class,
    OntologySynonym.class, Pathway.class, OntologyTerm.class, Reference.class
}, version = 1, exportSchema = false)
public abstract class FooDBDatabase extends RoomDatabase {
    public abstract FooDBDao fooDBDao();
}
