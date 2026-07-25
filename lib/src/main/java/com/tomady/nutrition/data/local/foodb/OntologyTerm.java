package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "ontologyterm")
public class OntologyTerm {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    private String term;
    private String definition;

    @ColumnInfo(name = "external_id")
    private String externalId;

    @ColumnInfo(name = "external_source")
    private String externalSource;

    private String comment;
    private String curator;

    @ColumnInfo(name = "parent_id")
    private Integer parentId;

    private Integer level;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "legacy_id")
    private String legacyId;

    public OntologyTerm() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }
    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getExternalSource() { return externalSource; }
    public void setExternalSource(String externalSource) { this.externalSource = externalSource; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getCurator() { return curator; }
    public void setCurator(String curator) { this.curator = curator; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getLegacyId() { return legacyId; }
    public void setLegacyId(String legacyId) { this.legacyId = legacyId; }
}
