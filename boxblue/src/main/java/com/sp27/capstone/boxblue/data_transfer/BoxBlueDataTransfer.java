package com.sp27.capstone.boxblue.data_transfer;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
                // Send the obtained bytes to the UI activity.
                Message readMsg = mHandler.obtainMessage(
                        BoxBlueMessageConstants.MESSAGE_READ, numBytes, -1, mmBuffer);
                readMsg.sendToTarget();
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                break;
            }
        }
    }

    public void write(byte[] bytes) {
        try {
            int offset = 0;
            while (bytes.length - offset >= 301) {
                byte[] temp_bytes = new byte[301];
                System.arraycopy(bytes, offset,
                        temp_bytes, 0, 301);
                mmOutStream.write(temp_bytes);
                mmOutStream.flush();
                offset += 301;
                Log.d(TAG, "temp array: " + Arrays.toString(temp_bytes));
            }
            // then send out the rest
            if (bytes.length - offset > 0) {
                byte[] rest_of_bytes = new byte[bytes.length - offset];
                System.arraycopy(bytes, offset, rest_of_bytes, 0, bytes.length - offset);
                mmOutStream.write(rest_of_bytes);
                mmOutStream.flush();
                Log.d(TAG, "remaining array: " + Arrays.toString(rest_of_bytes));
            }

            // Share the sent message with the UI activity.
            Message writtenMsg = mHandler.obtainMessage(
                    BoxBlueMessageConstants.MESSAGE_WRITE, -1, -1, bytes);
            writtenMsg.sendToTarget();
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
