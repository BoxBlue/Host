package com.boxblue.android.boxblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.boxblue.android.boxblue.connection.BoxBlueClientThread;
import com.boxblue.android.boxblue.exception.BoxBlueDeviceNotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoxBlue {
    BluetoothAdapter mmBluetoothAdapter;
    BluetoothDevice mmDevice;
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

    public BoxBlue instantiateBoxBlue() throws BoxBlueDeviceNotFoundException {
        mmBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mmBluetoothAdapter == null) {
            // Device does not support Bluetooth
            return null;
        }

        // Enable Bluetooth
        // TODO: This should be in the Activity, not the library
        if (!mmBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


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

    public boolean search(int[] array, int key) {
        // 
        BoxBlueClientThread boxBlueClientThread = new BoxBlueClientThread();
        return true;
    }

    public int[] sort(int[] array) {
        return array;
    }



}
