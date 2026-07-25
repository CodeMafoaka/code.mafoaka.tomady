package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "compound")
public class Compound {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "public_id")
    private String publicId;

    private String name;
    private String state;

    @ColumnInfo(name = "annotation_quality")
    private String annotationQuality;

    private String description;

    @ColumnInfo(name = "cas_number")
    private String casNumber;

    @ColumnInfo(name = "moldb_smiles")
    private String moldbSmiles;

    @ColumnInfo(name = "moldb_inchi")
    private String moldbInchi;

    @ColumnInfo(name = "moldb_mono_mass")
    private String moldbMonoMass;

    @ColumnInfo(name = "moldb_inchikey")
    private String moldbInchikey;

    @ColumnInfo(name = "moldb_iupac")
    private String moldbIupac;

    private String kingdom;

    @ColumnInfo(name = "superklass")
    private String superklass;

    private String klass;

    @ColumnInfo(name = "subklass")
    private String subklass;

    public Compound() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getAnnotationQuality() { return annotationQuality; }
    public void setAnnotationQuality(String annotationQuality) { this.annotationQuality = annotationQuality; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCasNumber() { return casNumber; }
    public void setCasNumber(String casNumber) { this.casNumber = casNumber; }
    public String getMoldbSmiles() { return moldbSmiles; }
    public void setMoldbSmiles(String moldbSmiles) { this.moldbSmiles = moldbSmiles; }
    public String getMoldbInchi() { return moldbInchi; }
    public void setMoldbInchi(String moldbInchi) { this.moldbInchi = moldbInchi; }
    public String getMoldbMonoMass() { return moldbMonoMass; }
    public void setMoldbMonoMass(String moldbMonoMass) { this.moldbMonoMass = moldbMonoMass; }
    public String getMoldbInchikey() { return moldbInchikey; }
    public void setMoldbInchikey(String moldbInchikey) { this.moldbInchikey = moldbInchikey; }
    public String getMoldbIupac() { return moldbIupac; }
    public void setMoldbIupac(String moldbIupac) { this.moldbIupac = moldbIupac; }
    public String getKingdom() { return kingdom; }
    public void setKingdom(String kingdom) { this.kingdom = kingdom; }
    public String getSuperklass() { return superklass; }
    public void setSuperklass(String superklass) { this.superklass = superklass; }
    public String getKlass() { return klass; }
    public void setKlass(String klass) { this.klass = klass; }
    public String getSubklass() { return subklass; }
    public void setSubklass(String subklass) { this.subklass = subklass; }
}
