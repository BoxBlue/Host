package com.sp27.capstone.boxblue.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Created by shreyashirday on 4/13/17.
 */

public class BoxBlueClientReceiver extends BroadcastReceiver {

    private BluetoothSocket mmSocket = null;
    private BluetoothDevice mmDevice = null;
    private String intendedDeviceName;
    private String intendedDeviceMAC;

    private final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG,"onReceive called");
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Discovery has found a device. Get the BluetoothDevice
            // object and its info from the Intent.
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            final String deviceName = device.getName();
            final String deviceHardwareAddress = device.getAddress(); // MAC address
            mmDevice = device;
            Log.d(TAG, "name = " + deviceName + ", MAC: " + deviceHardwareAddress);
            if (deviceName != null && (deviceName.equals(intendedDeviceName) || deviceHardwareAddress.equals(intendedDeviceMAC))) {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                Log.d(TAG, "trying to connect to RPI");
                final boolean isBonding = mmDevice.createBond();
                Log.d(TAG,"isBonding?" + isBonding);
            }
        }
        else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                connectSocket(device);
            }
        }
        else {
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG,"STARTED");
            } else {
                Log.d(TAG,"FINISHED");
            }
        }
    }

    public void setIntendedDeviceName(final String dName) {
        this.intendedDeviceName = dName;
    }

    public void setIntendedDeviceMAC(final String dMac) {
        this.intendedDeviceMAC = dMac;
    }

    public IntentFilter getFilter() {
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        return filter;
    }

    public void connectSocket(BluetoothDevice device) {
        try {
            mmSocket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
            Log.d(TAG, "Trying to connect to socket.");
            if (mmSocket != null) {
                try {
                    mmSocket.connect();
                    Log.d(TAG,"regualr connected");
                } catch (Exception e) {
                    final Class<?> clazz = mmSocket.getRemoteDevice().getClass();
                    final Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
                    final Method m = clazz.getMethod("createRfcommSocket",paramTypes);
                    final Object[] params = new Object[] {Integer.valueOf(1)} ;
                    final BluetoothSocket fallback = (BluetoothSocket)m.invoke(mmSocket.getRemoteDevice(), params);
                    fallback.connect();
                    Log.d(TAG, "fallback connected");
                    mmSocket = fallback;
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public BluetoothSocket getSocket() {
        return mmSocket;
    }

    public BluetoothDevice getDevice() {
        return mmDevice;
    }
}
