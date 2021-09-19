package com.voxeldev.steammarkethelper.models.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface NameIdDao {
    @Query("SELECT * FROM nameidpair WHERE item_name LIKE :name LIMIT 1")
    NameIdPair getNameId(String name);

    @Insert
    void insertNameId(NameIdPair nameId);

    @Update
    void updateNameId(NameIdPair nameId);
}
