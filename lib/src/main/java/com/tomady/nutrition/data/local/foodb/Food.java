package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "food")
public class Food {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    private String name;

    @ColumnInfo(name = "name_scientific")
    private String nameScientific;

    private String description;

    @ColumnInfo(name = "itis_id")
    private String itisId;

    @ColumnInfo(name = "wikipedia_id")
    private String wikipediaId;

    @ColumnInfo(name = "picture_file_name")
    private String pictureFileName;

    @ColumnInfo(name = "picture_content_type")
    private String pictureContentType;

    @ColumnInfo(name = "picture_file_size")
    private Integer pictureFileSize;

    @ColumnInfo(name = "picture_updated_at")
    private String pictureUpdatedAt;

    @ColumnInfo(name = "legacy_id")
    private Integer legacyId;

    @ColumnInfo(name = "food_group")
    private String foodGroup;

    @ColumnInfo(name = "food_subgroup")
    private String foodSubgroup;

    @ColumnInfo(name = "food_type")
    private String foodType;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "creator_id")
    private String creatorId;

    @ColumnInfo(name = "updater_id")
    private Integer updaterId;

    @ColumnInfo(name = "export_to_afcdb")
    private Boolean exportToAfcdb;

    private String category;

    @ColumnInfo(name = "ncbi_taxonomy_id")
    private Integer ncbiTaxonomyId;

    @ColumnInfo(name = "export_to_foodb")
    private Boolean exportToFoodb;

    @ColumnInfo(name = "public_id")
    private String publicId;

    public Food() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameScientific() { return nameScientific; }
    public void setNameScientific(String nameScientific) { this.nameScientific = nameScientific; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getItisId() { return itisId; }
    public void setItisId(String itisId) { this.itisId = itisId; }
    public String getWikipediaId() { return wikipediaId; }
    public void setWikipediaId(String wikipediaId) { this.wikipediaId = wikipediaId; }
    public String getPictureFileName() { return pictureFileName; }
    public void setPictureFileName(String pictureFileName) { this.pictureFileName = pictureFileName; }
    public String getPictureContentType() { return pictureContentType; }
    public void setPictureContentType(String pictureContentType) { this.pictureContentType = pictureContentType; }
    public Integer getPictureFileSize() { return pictureFileSize; }
    public void setPictureFileSize(Integer pictureFileSize) { this.pictureFileSize = pictureFileSize; }
    public String getPictureUpdatedAt() { return pictureUpdatedAt; }
    public void setPictureUpdatedAt(String pictureUpdatedAt) { this.pictureUpdatedAt = pictureUpdatedAt; }
    public Integer getLegacyId() { return legacyId; }
    public void setLegacyId(Integer legacyId) { this.legacyId = legacyId; }
    public String getFoodGroup() { return foodGroup; }
    public void setFoodGroup(String foodGroup) { this.foodGroup = foodGroup; }
    public String getFoodSubgroup() { return foodSubgroup; }
    public void setFoodSubgroup(String foodSubgroup) { this.foodSubgroup = foodSubgroup; }
    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public Integer getUpdaterId() { return updaterId; }
    public void setUpdaterId(Integer updaterId) { this.updaterId = updaterId; }
    public Boolean getExportToAfcdb() { return exportToAfcdb; }
    public void setExportToAfcdb(Boolean exportToAfcdb) { this.exportToAfcdb = exportToAfcdb; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getNcbiTaxonomyId() { return ncbiTaxonomyId; }
    public void setNcbiTaxonomyId(Integer ncbiTaxonomyId) { this.ncbiTaxonomyId = ncbiTaxonomyId; }
    public Boolean getExportToFoodb() { return exportToFoodb; }
    public void setExportToFoodb(Boolean exportToFoodb) { this.exportToFoodb = exportToFoodb; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
}
