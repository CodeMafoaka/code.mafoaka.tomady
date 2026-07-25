package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "compoundontologyterm")
public class CompoundOntologyTerm {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "compound_id")
    private Integer compoundId;

    private Boolean export;

    @ColumnInfo(name = "ontology_term_id")
    private Integer ontologyTermId;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    public CompoundOntologyTerm() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCompoundId() { return compoundId; }
    public void setCompoundId(Integer compoundId) { this.compoundId = compoundId; }
    public Boolean getExport() { return export; }
    public void setExport(Boolean export) { this.export = export; }
    public Integer getOntologyTermId() { return ontologyTermId; }
    public void setOntologyTermId(Integer ontologyTermId) { this.ontologyTermId = ontologyTermId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
