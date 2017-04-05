package com.sp27.capstone.boxblue.constants;

/**
 * Created by crejaud on 4/4/17.
 */

public enum BoxBlueStorageType {
    NULL            (0),
    TEXT            (1),
    IMAGE           (2),
    AUDIO           (3),
    VIDEO           (4);

    private final int storageTypeId;

    BoxBlueStorageType(int storageTypeId) {
        this.storageTypeId = storageTypeId;
    }

    public byte getStorageTypeId() {
        return (byte) storageTypeId;
    }
}
