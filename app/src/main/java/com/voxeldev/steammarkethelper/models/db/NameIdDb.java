package com.voxeldev.steammarkethelper.models.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {NameIdPair.class}, version = 1)
public abstract class NameIdDb extends RoomDatabase {
    public abstract NameIdDao nameIdDao();
}
