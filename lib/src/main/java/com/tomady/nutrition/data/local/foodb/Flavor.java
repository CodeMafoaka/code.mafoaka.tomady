package com.tomady.nutrition.data.local.foodb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "flavor")
public class Flavor {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    private String name;

    @ColumnInfo(name = "flavor_group")
    private String flavorGroup;

    private String category;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "creator_id")
    private String creatorId;

    @ColumnInfo(name = "updater_id")
    private String updaterId;

    public Flavor() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFlavorGroup() { return flavorGroup; }
    public void setFlavorGroup(String flavorGroup) { this.flavorGroup = flavorGroup; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getUpdaterId() { return updaterId; }
    public void setUpdaterId(String updaterId) { this.updaterId = updaterId; }
}
