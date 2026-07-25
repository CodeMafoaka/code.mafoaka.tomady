package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "compoundexternaldescriptor")
public class CompoundExternalDescriptor {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "external_id")
    private String externalId;

    private String annotations;

    @ColumnInfo(name = "compound_id")
    private Integer compoundId;

    @ColumnInfo(name = "creator_id")
    private String creatorId;

    @ColumnInfo(name = "updater_id")
    private String updaterId;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    public CompoundExternalDescriptor() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getAnnotations() { return annotations; }
    public void setAnnotations(String annotations) { this.annotations = annotations; }
    public Integer getCompoundId() { return compoundId; }
    public void setCompoundId(Integer compoundId) { this.compoundId = compoundId; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getUpdaterId() { return updaterId; }
    public void setUpdaterId(String updaterId) { this.updaterId = updaterId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
