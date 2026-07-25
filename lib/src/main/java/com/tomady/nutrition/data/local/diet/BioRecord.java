package com.tomady.nutrition.data.local.diet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bio_records")
public class BioRecord {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int profileId;
    private double bloodPressure;
    private double heartRate;
    private String recordedAt;

    public BioRecord() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }
    public double getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(double bloodPressure) { this.bloodPressure = bloodPressure; }
    public double getHeartRate() { return heartRate; }
    public void setHeartRate(double heartRate) { this.heartRate = heartRate; }
    public String getRecordedAt() { return recordedAt; }
    public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }
}
