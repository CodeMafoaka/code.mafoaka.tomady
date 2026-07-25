package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "compoundsynonym")
public class CompoundSynonym {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    private String synonym;

    @ColumnInfo(name = "synonym_source")
    private String synonymSource;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "source_id")
    private Integer sourceId;

    @ColumnInfo(name = "source_type")
    private String sourceType;

    public CompoundSynonym() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSynonym() { return synonym; }
    public void setSynonym(String synonym) { this.synonym = synonym; }
    public String getSynonymSource() { return synonymSource; }
    public void setSynonymSource(String synonymSource) { this.synonymSource = synonymSource; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer sourceId) { this.sourceId = sourceId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
