package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "accessionnumber")
public class AccessionNumber {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    private String number;

    @ColumnInfo(name = "compound_id")
    private Integer compoundId;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "source_id")
    private String sourceId;

    @ColumnInfo(name = "source_type")
    private String sourceType;

    public AccessionNumber() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public Integer getCompoundId() { return compoundId; }
    public void setCompoundId(Integer compoundId) { this.compoundId = compoundId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
