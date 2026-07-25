package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "compoundsenzyme")
public class CompoundsEnzyme {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "compound_id")
    private Integer compoundId;

    @ColumnInfo(name = "enzyme_id")
    private Integer enzymeId;

    private String citations;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "creator_id")
    private String creatorId;

    @ColumnInfo(name = "updater_id")
    private String updaterId;

    public CompoundsEnzyme() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCompoundId() { return compoundId; }
    public void setCompoundId(Integer compoundId) { this.compoundId = compoundId; }
    public Integer getEnzymeId() { return enzymeId; }
    public void setEnzymeId(Integer enzymeId) { this.enzymeId = enzymeId; }
    public String getCitations() { return citations; }
    public void setCitations(String citations) { this.citations = citations; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getUpdaterId() { return updaterId; }
    public void setUpdaterId(String updaterId) { this.updaterId = updaterId; }
}
