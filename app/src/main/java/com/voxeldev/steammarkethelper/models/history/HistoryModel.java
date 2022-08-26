package com.voxeldev.steammarkethelper.models.history;

import java.util.List;

public class HistoryModel {
    public boolean success;
    public int pageSize;
    public int totalCount;
    public int start;
    public List<HistoryAssetModel> historyAssets;
    public List<HistoryEventModel> historyEvents;
}
