package com.voxeldev.steammarkethelper.models.market;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class MarketModel {
    public int start;
    public String query;
    public int total_count;
    public List<MarketItemModel> results;
}
