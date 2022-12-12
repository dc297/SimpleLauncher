package com.dc297.simplelauncher.domain.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class AppUsage {
    @PrimaryKey
    @NonNull
    public String packageName;
    public int launchCount;
    public long lastLaunchTime;

    public AppUsage() {
    }

    @Ignore
    public AppUsage(@NonNull String packageName) {
        this.packageName = packageName;
    }

    public AppUsage incrementCount() {
        launchCount++;
        lastLaunchTime = System.currentTimeMillis();
        return this;
    }
}
