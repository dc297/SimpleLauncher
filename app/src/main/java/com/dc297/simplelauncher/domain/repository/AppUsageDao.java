package com.dc297.simplelauncher.domain.repository;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.dc297.simplelauncher.domain.entity.AppUsage;


import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

@Dao
public interface AppUsageDao {
    @Query("SELECT * FROM AppUsage")
    Flowable<List<AppUsage>> getAll();

    @Query("SELECT * FROM AppUsage where packageName = :packageName")
    Maybe<AppUsage> getByPackageName(String packageName);

    @Insert(onConflict = REPLACE)
    Completable save(AppUsage appUsage);
}
