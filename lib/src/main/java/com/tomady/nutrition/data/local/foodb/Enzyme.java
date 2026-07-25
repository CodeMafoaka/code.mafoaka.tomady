package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "enzyme")
public class Enzyme {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    private String name;

    @ColumnInfo(name = "gene_name")
    private String geneName;

    private String description;

    @ColumnInfo(name = "go_classification")
    private String goClassification;

    @ColumnInfo(name = "general_function")
    private String generalFunction;

    @ColumnInfo(name = "specific_function")
    private String specificFunction;

    private String pathway;
    private String reaction;

    @ColumnInfo(name = "cellular_location")
    private String cellularLocation;

    private String signals;

    @ColumnInfo(name = "transmembrane_regions")
    private String transmembraneRegions;

    @ColumnInfo(name = "molecular_weight")
    private String molecularWeight;

    @ColumnInfo(name = "theoretical_pi")
    private String theoreticalPi;

    private String locus;
    private String chromosome;

    @ColumnInfo(name = "uniprot_name")
    private String uniprotName;

    @ColumnInfo(name = "uniprot_id")
    private String uniprotId;

    @ColumnInfo(name = "pdb_id")
    private String pdbId;

    @ColumnInfo(name = "genbank_protein_id")
    private String genbankProteinId;

    @ColumnInfo(name = "genbank_gene_id")
    private String genbankGeneId;

    @ColumnInfo(name = "genecard_id")
    private String genecardId;

    @ColumnInfo(name = "genatlas_id")
    private String genatlasId;

    @ColumnInfo(name = "hgnc_id")
    private String hgncId;

    @ColumnInfo(name = "hprd_id")
    private String hprdId;

    private String organism;

    @ColumnInfo(name = "general_citations")
    private String generalCitations;

    private String comments;

    @ColumnInfo(name = "creator_id")
    private String creatorId;

    @ColumnInfo(name = "updater_id")
    private String updaterId;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    public Enzyme() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGeneName() { return geneName; }
    public void setGeneName(String geneName) { this.geneName = geneName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getGoClassification() { return goClassification; }
    public void setGoClassification(String goClassification) { this.goClassification = goClassification; }
    public String getGeneralFunction() { return generalFunction; }
    public void setGeneralFunction(String generalFunction) { this.generalFunction = generalFunction; }
    public String getSpecificFunction() { return specificFunction; }
    public void setSpecificFunction(String specificFunction) { this.specificFunction = specificFunction; }
    public String getPathway() { return pathway; }
    public void setPathway(String pathway) { this.pathway = pathway; }
    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }
    public String getCellularLocation() { return cellularLocation; }
    public void setCellularLocation(String cellularLocation) { this.cellularLocation = cellularLocation; }
    public String getSignals() { return signals; }
    public void setSignals(String signals) { this.signals = signals; }
    public String getTransmembraneRegions() { return transmembraneRegions; }
    public void setTransmembraneRegions(String transmembraneRegions) { this.transmembraneRegions = transmembraneRegions; }
    public String getMolecularWeight() { return molecularWeight; }
    public void setMolecularWeight(String molecularWeight) { this.molecularWeight = molecularWeight; }
    public String getTheoreticalPi() { return theoreticalPi; }
    public void setTheoreticalPi(String theoreticalPi) { this.theoreticalPi = theoreticalPi; }
    public String getLocus() { return locus; }
    public void setLocus(String locus) { this.locus = locus; }
    public String getChromosome() { return chromosome; }
    public void setChromosome(String chromosome) { this.chromosome = chromosome; }
    public String getUniprotName() { return uniprotName; }
    public void setUniprotName(String uniprotName) { this.uniprotName = uniprotName; }
    public String getUniprotId() { return uniprotId; }
    public void setUniprotId(String uniprotId) { this.uniprotId = uniprotId; }
    public String getPdbId() { return pdbId; }
    public void setPdbId(String pdbId) { this.pdbId = pdbId; }
    public String getGenbankProteinId() { return genbankProteinId; }
    public void setGenbankProteinId(String genbankProteinId) { this.genbankProteinId = genbankProteinId; }
    public String getGenbankGeneId() { return genbankGeneId; }
    public void setGenbankGeneId(String genbankGeneId) { this.genbankGeneId = genbankGeneId; }
    public String getGenecardId() { return genecardId; }
    public void setGenecardId(String genecardId) { this.genecardId = genecardId; }
    public String getGenatlasId() { return genatlasId; }
    public void setGenatlasId(String genatlasId) { this.genatlasId = genatlasId; }
    public String getHgncId() { return hgncId; }
    public void setHgncId(String hgncId) { this.hgncId = hgncId; }
    public String getHprdId() { return hprdId; }
    public void setHprdId(String hprdId) { this.hprdId = hprdId; }
    public String getOrganism() { return organism; }
    public void setOrganism(String organism) { this.organism = organism; }
    public String getGeneralCitations() { return generalCitations; }
    public void setGeneralCitations(String generalCitations) { this.generalCitations = generalCitations; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getUpdaterId() { return updaterId; }
    public void setUpdaterId(String updaterId) { this.updaterId = updaterId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
