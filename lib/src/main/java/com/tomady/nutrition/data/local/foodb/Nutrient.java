package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "nutrient")
public class Nutrient {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "legacy_id")
    private Integer legacyId;

    @ColumnInfo(name = "public_id")
    private String publicId;

    private String name;
    private Boolean export;
    private String state;

    @ColumnInfo(name = "annotation_quality")
    private String annotationQuality;

    private String description;

    @ColumnInfo(name = "wikipedia_id")
    private String wikipediaId;

    private String comments;

    @ColumnInfo(name = "dfc_id")
    private String dfcId;

    @ColumnInfo(name = "duke_id")
    private String dukeId;

    @ColumnInfo(name = "eafus_id")
    private String eafusId;

    @ColumnInfo(name = "dfc_name")
    private String dfcName;

    @ColumnInfo(name = "compound_source")
    private String compoundSource;

    private String metabolism;

    @ColumnInfo(name = "synthesis_citations")
    private String synthesisCitations;

    @ColumnInfo(name = "general_citations")
    private String generalCitations;

    @ColumnInfo(name = "creator_id")
    private String creatorId;

    @ColumnInfo(name = "updater_id")
    private String updaterId;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    public Nutrient() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getLegacyId() { return legacyId; }
    public void setLegacyId(Integer legacyId) { this.legacyId = legacyId; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getExport() { return export; }
    public void setExport(Boolean export) { this.export = export; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getAnnotationQuality() { return annotationQuality; }
    public void setAnnotationQuality(String annotationQuality) { this.annotationQuality = annotationQuality; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getWikipediaId() { return wikipediaId; }
    public void setWikipediaId(String wikipediaId) { this.wikipediaId = wikipediaId; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public String getDfcId() { return dfcId; }
    public void setDfcId(String dfcId) { this.dfcId = dfcId; }
    public String getDukeId() { return dukeId; }
    public void setDukeId(String dukeId) { this.dukeId = dukeId; }
    public String getEafusId() { return eafusId; }
    public void setEafusId(String eafusId) { this.eafusId = eafusId; }
    public String getDfcName() { return dfcName; }
    public void setDfcName(String dfcName) { this.dfcName = dfcName; }
    public String getCompoundSource() { return compoundSource; }
    public void setCompoundSource(String compoundSource) { this.compoundSource = compoundSource; }
    public String getMetabolism() { return metabolism; }
    public void setMetabolism(String metabolism) { this.metabolism = metabolism; }
    public String getSynthesisCitations() { return synthesisCitations; }
    public void setSynthesisCitations(String synthesisCitations) { this.synthesisCitations = synthesisCitations; }
    public String getGeneralCitations() { return generalCitations; }
    public void setGeneralCitations(String generalCitations) { this.generalCitations = generalCitations; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getUpdaterId() { return updaterId; }
    public void setUpdaterId(String updaterId) { this.updaterId = updaterId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
