package com.voxeldev.steammarkethelper.models.inventory;

import com.voxeldev.steammarkethelper.models.common.ActionModel;

import java.util.List;

public class InventoryItemModel {
    public String classid;
    public String icon_url;
    public List<ActionModel> actions;
    public List<InventoryOwnerDescription> owner_descriptions;
    public String name;
    public int tradable;
    public int marketable;
}
