package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "content")
public class Content {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "source_id")
    private Integer sourceId;

    @ColumnInfo(name = "source_type")
    private String sourceType;

    @ColumnInfo(name = "food_id")
    private Integer foodId;

    @ColumnInfo(name = "orig_food_id")
    private String origFoodId;

    @ColumnInfo(name = "orig_food_common_name")
    private String origFoodCommonName;

    @ColumnInfo(name = "orig_food_scientific_name")
    private String origFoodScientificName;

    @ColumnInfo(name = "orig_food_part")
    private String origFoodPart;

    @ColumnInfo(name = "orig_source_id")
    private String origSourceId;

    @ColumnInfo(name = "orig_source_name")
    private String origSourceName;

    @ColumnInfo(name = "orig_content")
    private String origContent;

    @ColumnInfo(name = "orig_min")
    private String origMin;

    @ColumnInfo(name = "orig_max")
    private String origMax;

    @ColumnInfo(name = "orig_unit")
    private String origUnit;

    @ColumnInfo(name = "orig_citation")
    private String origCitation;

    private String citation;

    @ColumnInfo(name = "citation_type")
    private String citationType;

    @ColumnInfo(name = "creator_id")
    private String creatorId;

    @ColumnInfo(name = "updater_id")
    private String updaterId;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "orig_method")
    private String origMethod;

    @ColumnInfo(name = "orig_unit_expression")
    private String origUnitExpression;

    @ColumnInfo(name = "standard_content")
    private String standardContent;

    @ColumnInfo(name = "preparation_type")
    private String preparationType;

    private Integer export;

    public Content() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer sourceId) { this.sourceId = sourceId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Integer getFoodId() { return foodId; }
    public void setFoodId(Integer foodId) { this.foodId = foodId; }
    public String getOrigFoodId() { return origFoodId; }
    public void setOrigFoodId(String origFoodId) { this.origFoodId = origFoodId; }
    public String getOrigFoodCommonName() { return origFoodCommonName; }
    public void setOrigFoodCommonName(String origFoodCommonName) { this.origFoodCommonName = origFoodCommonName; }
    public String getOrigFoodScientificName() { return origFoodScientificName; }
    public void setOrigFoodScientificName(String origFoodScientificName) { this.origFoodScientificName = origFoodScientificName; }
    public String getOrigFoodPart() { return origFoodPart; }
    public void setOrigFoodPart(String origFoodPart) { this.origFoodPart = origFoodPart; }
    public String getOrigSourceId() { return origSourceId; }
    public void setOrigSourceId(String origSourceId) { this.origSourceId = origSourceId; }
    public String getOrigSourceName() { return origSourceName; }
    public void setOrigSourceName(String origSourceName) { this.origSourceName = origSourceName; }
    public String getOrigContent() { return origContent; }
    public void setOrigContent(String origContent) { this.origContent = origContent; }
    public String getOrigMin() { return origMin; }
    public void setOrigMin(String origMin) { this.origMin = origMin; }
    public String getOrigMax() { return origMax; }
    public void setOrigMax(String origMax) { this.origMax = origMax; }
    public String getOrigUnit() { return origUnit; }
    public void setOrigUnit(String origUnit) { this.origUnit = origUnit; }
    public String getOrigCitation() { return origCitation; }
    public void setOrigCitation(String origCitation) { this.origCitation = origCitation; }
    public String getCitation() { return citation; }
    public void setCitation(String citation) { this.citation = citation; }
    public String getCitationType() { return citationType; }
    public void setCitationType(String citationType) { this.citationType = citationType; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getUpdaterId() { return updaterId; }
    public void setUpdaterId(String updaterId) { this.updaterId = updaterId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getOrigMethod() { return origMethod; }
    public void setOrigMethod(String origMethod) { this.origMethod = origMethod; }
    public String getOrigUnitExpression() { return origUnitExpression; }
    public void setOrigUnitExpression(String origUnitExpression) { this.origUnitExpression = origUnitExpression; }
    public String getStandardContent() { return standardContent; }
    public void setStandardContent(String standardContent) { this.standardContent = standardContent; }
    public String getPreparationType() { return preparationType; }
    public void setPreparationType(String preparationType) { this.preparationType = preparationType; }
    public Integer getExport() { return export; }
    public void setExport(Integer export) { this.export = export; }
}
