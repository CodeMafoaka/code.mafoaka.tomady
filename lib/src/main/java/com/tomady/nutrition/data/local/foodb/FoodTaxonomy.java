package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "foodtaxonomy")
public class FoodTaxonomy {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "food_id")
    private Integer foodId;

    @ColumnInfo(name = "ncbi_taxonomy_id")
    private Integer ncbiTaxonomyId;

    @ColumnInfo(name = "classification_name")
    private String classificationName;

    @ColumnInfo(name = "classification_order")
    private Integer classificationOrder;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    public FoodTaxonomy() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFoodId() { return foodId; }
    public void setFoodId(Integer foodId) { this.foodId = foodId; }
    public Integer getNcbiTaxonomyId() { return ncbiTaxonomyId; }
    public void setNcbiTaxonomyId(Integer ncbiTaxonomyId) { this.ncbiTaxonomyId = ncbiTaxonomyId; }
    public String getClassificationName() { return classificationName; }
    public void setClassificationName(String classificationName) { this.classificationName = classificationName; }
    public Integer getClassificationOrder() { return classificationOrder; }
    public void setClassificationOrder(Integer classificationOrder) { this.classificationOrder = classificationOrder; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
