package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "compoundshealtheffect")
public class CompoundsHealthEffect {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "compound_id")
    private Integer compoundId;

    @ColumnInfo(name = "health_effect_id")
    private Integer healthEffectId;

    @ColumnInfo(name = "orig_health_effect_name")
    private String origHealthEffectName;

    @ColumnInfo(name = "orig_compound_name")
    private String origCompoundName;

    @ColumnInfo(name = "orig_citation")
    private String origCitation;

    private String citation;

    @ColumnInfo(name = "citation_type")
    private String citationType;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "creator_id")
    private String creatorId;

    @ColumnInfo(name = "updater_id")
    private String updaterId;

    @ColumnInfo(name = "source_id")
    private Integer sourceId;

    @ColumnInfo(name = "source_type")
    private String sourceType;

    public CompoundsHealthEffect() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCompoundId() { return compoundId; }
    public void setCompoundId(Integer compoundId) { this.compoundId = compoundId; }
    public Integer getHealthEffectId() { return healthEffectId; }
    public void setHealthEffectId(Integer healthEffectId) { this.healthEffectId = healthEffectId; }
    public String getOrigHealthEffectName() { return origHealthEffectName; }
    public void setOrigHealthEffectName(String origHealthEffectName) { this.origHealthEffectName = origHealthEffectName; }
    public String getOrigCompoundName() { return origCompoundName; }
    public void setOrigCompoundName(String origCompoundName) { this.origCompoundName = origCompoundName; }
    public String getOrigCitation() { return origCitation; }
    public void setOrigCitation(String origCitation) { this.origCitation = origCitation; }
    public String getCitation() { return citation; }
    public void setCitation(String citation) { this.citation = citation; }
    public String getCitationType() { return citationType; }
    public void setCitationType(String citationType) { this.citationType = citationType; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getUpdaterId() { return updaterId; }
    public void setUpdaterId(String updaterId) { this.updaterId = updaterId; }
    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer sourceId) { this.sourceId = sourceId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
