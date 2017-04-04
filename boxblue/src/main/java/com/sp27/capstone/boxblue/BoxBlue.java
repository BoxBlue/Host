package com.sp27.capstone.boxblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import com.sp27.capstone.boxblue.connection.BoxBlueClientThread;
import com.sp27.capstone.boxblue.constants.BoxBlueDataTransferType;
import com.sp27.capstone.boxblue.exception.BoxBlueDeviceNotFoundException;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class BoxBlue {
    private BluetoothAdapter mmBluetoothAdapter;
    private BluetoothDevice mmDevice;
    private Handler mmHandler;
    public static final String TAG = "BoxBlue";

    private Set<BluetoothDevice> getSetOfPairedDevices() {
        return mmBluetoothAdapter.getBondedDevices();
    }

    private Set<BluetoothDevice> getSetOfPairedBoxBlueDevices() {
        Set<BluetoothDevice> pairedBoxBlueDevices = new HashSet<>();

        for (BluetoothDevice device : getSetOfPairedDevices()) {
            String deviceName = device.getName();               // Device Name
            String deviceHardwareAddress = device.getAddress(); // Device MAC address


            // Getting uuids of the device
            ParcelUuid[] uuids = device.getUuids();

            for (ParcelUuid uuid : uuids) {
                if (uuid.getUuid().equals("boxblue")) {
                    pairedBoxBlueDevices.add(device);
                    break;
                }
            }

        }

        return pairedBoxBlueDevices;
    }

    public BoxBlue(BluetoothAdapter bluetoothAdapter, Handler handler) throws BoxBlueDeviceNotFoundException {
        mmBluetoothAdapter = bluetoothAdapter;
        mmHandler = handler;

        Set<BluetoothDevice> pairedBoxBlueDevices = getSetOfPairedBoxBlueDevices();

        // get the first one
        if (pairedBoxBlueDevices.size() > 0) {
            mmDevice = pairedBoxBlueDevices.iterator().next();
        }
        else {
            Log.d(TAG, "No box blue device found");
            // throw exception so Activity knows
            throw new BoxBlueDeviceNotFoundException("No box blue device found");
        }
    }

    public void search(int[] array, int key) {

        // organize byte array as such:
        // first 4 bytes (int): BocBlueDataTransferType.SEARCH
        // second 4 bytes (int): key
        // remaining bytes is the int array
        ByteBuffer b = ByteBuffer.allocate(array.length);

        b.putInt(BoxBlueDataTransferType.SEARCH.getTransferTypeId());
        b.putInt(key);
        for (int i = 0; i < array.length; i++)
            b.putInt(array[i]);

        byte[] bytes = b.array();

        //
        BoxBlueClientThread boxBlueClientThread = new BoxBlueClientThread(mmDevice,
                BoxBlueDataTransferType.SEARCH,
                bytes,
                mmHandler,
                mmBluetoothAdapter);

        // start thread which connects to boxblue and sends the data then reads the data
        boxBlueClientThread.start();
    }

    public int[] sort(int[] array) {
        return array;
    }



}
