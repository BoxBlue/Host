package com.boxblue.android.boxblue.constants;

/**
 * Created by crejaud on 4/1/17.
 */

public enum BoxBlueDataTransferType {
    SEARCH          (0),
    SORT            (1),
    TRANSFER_DATA   (2),
    RECEIVE_DATA    (3);

    private final int transferTypeId;

    BoxBlueDataTransferType(int transferTypeId) {
        this.transferTypeId = transferTypeId;
    }

    public int getTransferTypeId() {
        return transferTypeId;
    }
}
