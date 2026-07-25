package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "ontologysynonym")
public class OntologySynonym {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "ontology_term_id")
    private Integer ontologyTermId;

    private String synonym;

    @ColumnInfo(name = "external_id")
    private String externalId;

    @ColumnInfo(name = "external_srouce")
    private String externalSource;

    @ColumnInfo(name = "parent_id")
    private String parentId;

    @ColumnInfo(name = "parent_source")
    private String parentSource;

    private String comment;
    private String curator;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    public OntologySynonym() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getOntologyTermId() { return ontologyTermId; }
    public void setOntologyTermId(Integer ontologyTermId) { this.ontologyTermId = ontologyTermId; }
    public String getSynonym() { return synonym; }
    public void setSynonym(String synonym) { this.synonym = synonym; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getExternalSource() { return externalSource; }
    public void setExternalSource(String externalSource) { this.externalSource = externalSource; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getParentSource() { return parentSource; }
    public void setParentSource(String parentSource) { this.parentSource = parentSource; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getCurator() { return curator; }
    public void setCurator(String curator) { this.curator = curator; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
