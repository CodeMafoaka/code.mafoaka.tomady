package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "ncbitaxonomymap")
public class NcbiTaxonomyMap {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "TaxonomyName")
    private String taxonomyName;

    @ColumnInfo(name = "Rank")
    private String rank;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    public NcbiTaxonomyMap() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTaxonomyName() { return taxonomyName; }
    public void setTaxonomyName(String taxonomyName) { this.taxonomyName = taxonomyName; }
    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
