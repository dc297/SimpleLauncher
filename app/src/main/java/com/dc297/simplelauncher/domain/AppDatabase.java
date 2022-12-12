package com.dc297.simplelauncher.domain;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.dc297.simplelauncher.domain.entity.AppUsage;
import com.dc297.simplelauncher.domain.repository.AppUsageDao;

@Database(entities = {AppUsage.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AppUsageDao appUsageDao();
}
