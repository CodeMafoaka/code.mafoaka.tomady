package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "pathway")
public class Pathway {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "smpdb_id")
    private String smpdbId;

    @ColumnInfo(name = "kegg_map_id")
    private String keggMapId;

    private String name;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    public Pathway() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSmpdbId() { return smpdbId; }
    public void setSmpdbId(String smpdbId) { this.smpdbId = smpdbId; }
    public String getKeggMapId() { return keggMapId; }
    public void setKeggMapId(String keggMapId) { this.keggMapId = keggMapId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
