package com.sp27.capstone.boxblue.data_transfer;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.sp27.capstone.boxblue.constants.BoxBlueMessageConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * This class will transfer data between the phone and the boxblue device.
 *
 * The BluetoothSocket is connected to the boxblue device.
 *
 * Created by crejaud on 4/1/17.
 */

public class BoxBlueDataTransfer {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private Handler mHandler; // handler that gets info from Bluetooth service
    private static final String TAG = "BoxBlueDataTransfer";
    long nanoStart;
    long nanoEnd;

    public BoxBlueDataTransfer(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        mHandler = handler;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void read() {
        mmBuffer = new byte[1024];
        int numBytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs.
        // This exception means that this is the end of the read
        while (true) {
            try{
                // Read from the InputStream
                numBytes = mmInStream.read(mmBuffer);
                nanoEnd = SystemClock.elapsedRealtimeNanos();
                Log.d("TIME","elapsed boxblue = " + (nanoEnd - nanoStart));
                Log.d(TAG,"numBytes = " + numBytes);
                // Send the obtained bytes to the UI activity.
                Message readMsg = mHandler.obtainMessage(
                        BoxBlueMessageConstants.MESSAGE_READ, numBytes, -1, mmBuffer);
                readMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Input stream was disconnected", e);
                break;
            }
        }
    }

    public void write(byte[][] bytes) {
        try {
            nanoStart = SystemClock.elapsedRealtimeNanos() / 1000;
            for (int i = 0; i < bytes.length; i++) {
                mmOutStream.write(bytes[i]);
                mmOutStream.flush();
            }
            // Share the sent message with the UI activity.
            Message writtenMsg = mHandler.obtainMessage(
                    BoxBlueMessageConstants.MESSAGE_WRITE, -1, -1, bytes);
            //writtenMsg.sendToTarget();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
            Message writeErrorMsg =
                    mHandler.obtainMessage(BoxBlueMessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            mHandler.sendMessage(writeErrorMsg);
        }
    }
}
