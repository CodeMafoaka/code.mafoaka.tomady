package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "healtheffect")
public class HealthEffect {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    private String name;
    private String description;

    @ColumnInfo(name = "chebi_name")
    private String chebiName;

    @ColumnInfo(name = "chebi_id")
    private String chebiId;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "creator_id")
    private String creatorId;

    @ColumnInfo(name = "updater_id")
    private String updaterId;

    @ColumnInfo(name = "chebi_definition")
    private String chebiDefinition;

    public HealthEffect() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getChebiName() { return chebiName; }
    public void setChebiName(String chebiName) { this.chebiName = chebiName; }
    public String getChebiId() { return chebiId; }
    public void setChebiId(String chebiId) { this.chebiId = chebiId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getUpdaterId() { return updaterId; }
    public void setUpdaterId(String updaterId) { this.updaterId = updaterId; }
    public String getChebiDefinition() { return chebiDefinition; }
    public void setChebiDefinition(String chebiDefinition) { this.chebiDefinition = chebiDefinition; }
}
