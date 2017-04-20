package com.sp27.capstone.boxblue.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.sp27.capstone.boxblue.constants.BoxBlueDataTransferType;
import com.sp27.capstone.boxblue.data_transfer.BoxBlueDataTransfer;

import java.io.IOException;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * This thread class will let the phone connect to the bluetooth device
 * and get the Bluetooth Socket for that connection. The device is a server,
 * and will be a BoxBlue capable device.
 *
 * Once it gets the connection running, it will call the manageMyConnectedSocket,
 * which will direct the socket to the BoxBlueDataTransfer class for reading and writing.
 *
 * Created by crejaud on 4/1/17.
 */

public class BoxBlueClientThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final Handler mmHandler;
    private final BoxBlueDataTransferType mmDataTransferType;
    private final BluetoothAdapter mmBluetoothAdapter;
    private final byte[][] mmBytesToTransfer;

    public BoxBlueClientThread(BluetoothDevice device,
                               BoxBlueDataTransferType boxBlueDataTransferType,
                               byte[][] bytesToTransfer,
                               Handler handler,
                               BluetoothAdapter bluetoothAdapter,
                               BluetoothSocket bluetoothSocket) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        mmDevice = device;

        mmDataTransferType = boxBlueDataTransferType;

        mmBytesToTransfer = bytesToTransfer;

        mmHandler = handler;

        mmBluetoothAdapter = bluetoothAdapter;

        mmSocket = bluetoothSocket;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        mmBluetoothAdapter.cancelDiscovery();

        /*
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            Log.d(TAG, "trying to connect to RPI");
            if (mmDevice.createBond()) {
                Log.d(TAG, "Bonded");
                mmSocket.connect();
                Log.d(TAG, "connected to RPI");
            } else {
                Log.d(TAG, "Could not bond");
            }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            Log.d(TAG, "unable to connect! Message: " + connectException.getLocalizedMessage());
            cancel();
            return;
        }
        */

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        manageMyConnectedSocket();
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    // Manage the connected socket by transfering data based off the data transfer type
    private void manageMyConnectedSocket() {
        Log.d(TAG, "Managing my connected socket");
        BoxBlueDataTransfer boxBlueDataTransfer = new BoxBlueDataTransfer(mmSocket, mmHandler);

        switch (mmDataTransferType) {
            case SEARCH:
                Log.d(TAG, "Going to start write then read for search");
                writeThenRead(boxBlueDataTransfer);
                break;
            case SORT:
                writeThenRead(boxBlueDataTransfer);
                break;
            case RECEIVE_DATA:
                read(boxBlueDataTransfer);
                break;
            case TRANSFER_DATA:
                write(boxBlueDataTransfer);
                break;
        }

        // cancel socket
        cancel();
    }

    // Helper functions for managing connected socket
    private void writeThenRead(BoxBlueDataTransfer boxBlueDataTransfer) {
        long nanoStart = SystemClock.elapsedRealtimeNanos();
        write(boxBlueDataTransfer);
        Log.d(TAG, "Post writing");
        read(boxBlueDataTransfer);
        long nanoEnd = SystemClock.elapsedRealtimeNanos();
        Log.d("TIME", "Post reading, elapsed for BoxBlue = " + (nanoEnd - nanoStart));
    }

    private void read(BoxBlueDataTransfer boxBlueDataTransfer) {
        boxBlueDataTransfer.read();
    }

    private void write(BoxBlueDataTransfer boxBlueDataTransfer) {
        boxBlueDataTransfer.write(mmBytesToTransfer);
    }
}
