package com.tomady.nutrition.data.local.foodb;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface FooDBDao {
    @Insert
    void insertCompound(Compound compound);
    @Update
    void updateCompound(Compound compound);
    @Delete
    void deleteCompound(Compound compound);
    @Query("SELECT * FROM compound WHERE id = :id")
    Compound getCompoundById(int id);
    @Query("SELECT * FROM compound")
    List<Compound> getAllCompounds();

    @Insert
    void insertContent(Content content);
    @Update
    void updateContent(Content content);
    @Delete
    void deleteContent(Content content);
    @Query("SELECT * FROM content WHERE id = :id")
    Content getContentById(int id);
    @Query("SELECT * FROM content")
    List<Content> getAllContents();

    @Insert
    void insertAccessionNumber(AccessionNumber accessionNumber);
    @Update
    void updateAccessionNumber(AccessionNumber accessionNumber);
    @Delete
    void deleteAccessionNumber(AccessionNumber accessionNumber);
    @Query("SELECT * FROM accessionnumber WHERE id = :id")
    AccessionNumber getAccessionNumberById(int id);
    @Query("SELECT * FROM accessionnumber")
    List<AccessionNumber> getAllAccessionNumbers();

    @Insert
    void insertCompoundAlternateParent(CompoundAlternateParent compoundAlternateParent);
    @Update
    void updateCompoundAlternateParent(CompoundAlternateParent compoundAlternateParent);
    @Delete
    void deleteCompoundAlternateParent(CompoundAlternateParent compoundAlternateParent);
    @Query("SELECT * FROM compoundalternateparent WHERE id = :id")
    CompoundAlternateParent getCompoundAlternateParentById(int id);
    @Query("SELECT * FROM compoundalternateparent")
    List<CompoundAlternateParent> getAllCompoundAlternateParents();

    @Insert
    void insertCompoundExternalDescriptor(CompoundExternalDescriptor compoundExternalDescriptor);
    @Update
    void updateCompoundExternalDescriptor(CompoundExternalDescriptor compoundExternalDescriptor);
    @Delete
    void deleteCompoundExternalDescriptor(CompoundExternalDescriptor compoundExternalDescriptor);
    @Query("SELECT * FROM compoundexternaldescriptor WHERE id = :id")
    CompoundExternalDescriptor getCompoundExternalDescriptorById(int id);
    @Query("SELECT * FROM compoundexternaldescriptor")
    List<CompoundExternalDescriptor> getAllCompoundExternalDescriptors();

    @Insert
    void insertCompoundOntologyTerm(CompoundOntologyTerm compoundOntologyTerm);
    @Update
    void updateCompoundOntologyTerm(CompoundOntologyTerm compoundOntologyTerm);
    @Delete
    void deleteCompoundOntologyTerm(CompoundOntologyTerm compoundOntologyTerm);
    @Query("SELECT * FROM compoundontologyterm WHERE id = :id")
    CompoundOntologyTerm getCompoundOntologyTermById(int id);
    @Query("SELECT * FROM compoundontologyterm")
    List<CompoundOntologyTerm> getAllCompoundOntologyTerms();

    @Insert
    void insertCompoundSubstituent(CompoundSubstituent compoundSubstituent);
    @Update
    void updateCompoundSubstituent(CompoundSubstituent compoundSubstituent);
    @Delete
    void deleteCompoundSubstituent(CompoundSubstituent compoundSubstituent);
    @Query("SELECT * FROM compoundsubstituent WHERE id = :id")
    CompoundSubstituent getCompoundSubstituentById(int id);
    @Query("SELECT * FROM compoundsubstituent")
    List<CompoundSubstituent> getAllCompoundSubstituents();

    @Insert
    void insertCompoundSynonym(CompoundSynonym compoundSynonym);
    @Update
    void updateCompoundSynonym(CompoundSynonym compoundSynonym);
    @Delete
    void deleteCompoundSynonym(CompoundSynonym compoundSynonym);
    @Query("SELECT * FROM compoundsynonym WHERE id = :id")
    CompoundSynonym getCompoundSynonymById(int id);
    @Query("SELECT * FROM compoundsynonym")
    List<CompoundSynonym> getAllCompoundSynonyms();

    @Insert
    void insertCompoundsEnzyme(CompoundsEnzyme compoundsEnzyme);
    @Update
    void updateCompoundsEnzyme(CompoundsEnzyme compoundsEnzyme);
    @Delete
    void deleteCompoundsEnzyme(CompoundsEnzyme compoundsEnzyme);
    @Query("SELECT * FROM compoundsenzyme WHERE id = :id")
    CompoundsEnzyme getCompoundsEnzymeById(int id);
    @Query("SELECT * FROM compoundsenzyme")
    List<CompoundsEnzyme> getAllCompoundsEnzymes();

    @Insert
    void insertCompoundsFlavor(CompoundsFlavor compoundsFlavor);
    @Update
    void updateCompoundsFlavor(CompoundsFlavor compoundsFlavor);
    @Delete
    void deleteCompoundsFlavor(CompoundsFlavor compoundsFlavor);
    @Query("SELECT * FROM compoundsflavor WHERE id = :id")
    CompoundsFlavor getCompoundsFlavorById(int id);
    @Query("SELECT * FROM compoundsflavor")
    List<CompoundsFlavor> getAllCompoundsFlavors();

    @Insert
    void insertEnzyme(Enzyme enzyme);
    @Update
    void updateEnzyme(Enzyme enzyme);
    @Delete
    void deleteEnzyme(Enzyme enzyme);
    @Query("SELECT * FROM enzyme WHERE id = :id")
    Enzyme getEnzymeById(int id);
    @Query("SELECT * FROM enzyme")
    List<Enzyme> getAllEnzymes();

    @Insert
    void insertFood(Food food);
    @Update
    void updateFood(Food food);
    @Delete
    void deleteFood(Food food);
    @Query("SELECT * FROM food WHERE id = :id")
    Food getFoodById(int id);
    @Query("SELECT * FROM food")
    List<Food> getAllFoods();

    @Insert
    void insertCompoundsPathway(CompoundsPathway compoundsPathway);
    @Update
    void updateCompoundsPathway(CompoundsPathway compoundsPathway);
    @Delete
    void deleteCompoundsPathway(CompoundsPathway compoundsPathway);
    @Query("SELECT * FROM compoundspathway WHERE id = :id")
    CompoundsPathway getCompoundsPathwayById(int id);
    @Query("SELECT * FROM compoundspathway")
    List<CompoundsPathway> getAllCompoundsPathways();

    @Insert
    void insertFlavor(Flavor flavor);
    @Update
    void updateFlavor(Flavor flavor);
    @Delete
    void deleteFlavor(Flavor flavor);
    @Query("SELECT * FROM flavor WHERE id = :id")
    Flavor getFlavorById(int id);
    @Query("SELECT * FROM flavor")
    List<Flavor> getAllFlavors();

    @Insert
    void insertFoodTaxonomy(FoodTaxonomy foodTaxonomy);
    @Update
    void updateFoodTaxonomy(FoodTaxonomy foodTaxonomy);
    @Delete
    void deleteFoodTaxonomy(FoodTaxonomy foodTaxonomy);
    @Query("SELECT * FROM foodtaxonomy WHERE id = :id")
    FoodTaxonomy getFoodTaxonomyById(int id);
    @Query("SELECT * FROM foodtaxonomy")
    List<FoodTaxonomy> getAllFoodTaxonomies();

    @Insert
    void insertCompoundsHealthEffect(CompoundsHealthEffect compoundsHealthEffect);
    @Update
    void updateCompoundsHealthEffect(CompoundsHealthEffect compoundsHealthEffect);
    @Delete
    void deleteCompoundsHealthEffect(CompoundsHealthEffect compoundsHealthEffect);
    @Query("SELECT * FROM compoundshealtheffect WHERE id = :id")
    CompoundsHealthEffect getCompoundsHealthEffectById(int id);
    @Query("SELECT * FROM compoundshealtheffect")
    List<CompoundsHealthEffect> getAllCompoundsHealthEffects();

    @Insert
    void insertHealthEffect(HealthEffect healthEffect);
    @Update
    void updateHealthEffect(HealthEffect healthEffect);
    @Delete
    void deleteHealthEffect(HealthEffect healthEffect);
    @Query("SELECT * FROM healtheffect WHERE id = :id")
    HealthEffect getHealthEffectById(int id);
    @Query("SELECT * FROM healtheffect")
    List<HealthEffect> getAllHealthEffects();

    @Insert
    void insertNcbiTaxonomyMap(NcbiTaxonomyMap ncbiTaxonomyMap);
    @Update
    void updateNcbiTaxonomyMap(NcbiTaxonomyMap ncbiTaxonomyMap);
    @Delete
    void deleteNcbiTaxonomyMap(NcbiTaxonomyMap ncbiTaxonomyMap);
    @Query("SELECT * FROM ncbitaxonomymap WHERE id = :id")
    NcbiTaxonomyMap getNcbiTaxonomyMapById(int id);
    @Query("SELECT * FROM ncbitaxonomymap")
    List<NcbiTaxonomyMap> getAllNcbiTaxonomyMaps();

    @Insert
    void insertNutrient(Nutrient nutrient);
    @Update
    void updateNutrient(Nutrient nutrient);
    @Delete
    void deleteNutrient(Nutrient nutrient);
    @Query("SELECT * FROM nutrient WHERE id = :id")
    Nutrient getNutrientById(int id);
    @Query("SELECT * FROM nutrient")
    List<Nutrient> getAllNutrients();

    @Insert
    void insertOntologySynonym(OntologySynonym ontologySynonym);
    @Update
    void updateOntologySynonym(OntologySynonym ontologySynonym);
    @Delete
    void deleteOntologySynonym(OntologySynonym ontologySynonym);
    @Query("SELECT * FROM ontologysynonym WHERE id = :id")
    OntologySynonym getOntologySynonymById(int id);
    @Query("SELECT * FROM ontologysynonym")
    List<OntologySynonym> getAllOntologySynonyms();

    @Insert
    void insertPathway(Pathway pathway);
    @Update
    void updatePathway(Pathway pathway);
    @Delete
    void deletePathway(Pathway pathway);
    @Query("SELECT * FROM pathway WHERE id = :id")
    Pathway getPathwayById(int id);
    @Query("SELECT * FROM pathway")
    List<Pathway> getAllPathways();

    @Insert
    void insertOntologyTerm(OntologyTerm ontologyTerm);
    @Update
    void updateOntologyTerm(OntologyTerm ontologyTerm);
    @Delete
    void deleteOntologyTerm(OntologyTerm ontologyTerm);
    @Query("SELECT * FROM ontologyterm WHERE id = :id")
    OntologyTerm getOntologyTermById(int id);
    @Query("SELECT * FROM ontologyterm")
    List<OntologyTerm> getAllOntologyTerms();

    @Insert
    void insertReference(Reference reference);
    @Update
    void updateReference(Reference reference);
    @Delete
    void deleteReference(Reference reference);
    @Query("SELECT * FROM reference WHERE id = :id")
    Reference getReferenceById(int id);
    @Query("SELECT * FROM reference")
    List<Reference> getAllReferences();
}
