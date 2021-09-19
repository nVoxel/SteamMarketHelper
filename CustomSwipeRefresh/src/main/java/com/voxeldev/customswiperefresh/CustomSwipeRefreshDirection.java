package com.voxeldev.customswiperefresh;

public enum CustomSwipeRefreshDirection {

    TOP(0),
    BOTTOM(1),
    BOTH(2);

    private final int mValue;

    CustomSwipeRefreshDirection(int value) {
        this.mValue = value;
    }

    public static CustomSwipeRefreshDirection getFromInt(int value) {
        for (CustomSwipeRefreshDirection direction : CustomSwipeRefreshDirection.values()) {
            if (direction.mValue == value) {
                return direction;
            }
        }
        return BOTH;
    }

}