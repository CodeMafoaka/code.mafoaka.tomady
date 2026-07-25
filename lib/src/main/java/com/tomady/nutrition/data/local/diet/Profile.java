package com.tomady.nutrition.data.local.diet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "profiles")
public class Profile {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int userId;
    private int age;
    private double height;
    private double weight;

    public Profile() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}
