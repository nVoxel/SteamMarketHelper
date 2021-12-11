package com.voxeldev.steammarkethelper.models.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class NameIdPair {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "item_name")
    public String itemName;

    @ColumnInfo(name = "name_id")
    public String nameId;

    public NameIdPair(String itemName, String nameId) {
        this.itemName = itemName;
        this.nameId = nameId;
    }
}
