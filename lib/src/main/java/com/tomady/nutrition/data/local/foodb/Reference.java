package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "reference")
public class Reference {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "ref_type")
    private String refType;

    private String text;

    @ColumnInfo(name = "pubmed_id")
    private String pubmedId;

    private String link;
    private String title;

    @ColumnInfo(name = "creator_id")
    private String creatorId;

    @ColumnInfo(name = "updater_id")
    private String updaterId;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "source_id")
    private Integer sourceId;

    @ColumnInfo(name = "source_type")
    private String sourceType;

    public Reference() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getPubmedId() { return pubmedId; }
    public void setPubmedId(String pubmedId) { this.pubmedId = pubmedId; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getUpdaterId() { return updaterId; }
    public void setUpdaterId(String updaterId) { this.updaterId = updaterId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer sourceId) { this.sourceId = sourceId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
