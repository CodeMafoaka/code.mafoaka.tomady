package com.tomady.nutrition.data.local.diet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "food_items")
public class FoodItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String groupName;

    public FoodItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
