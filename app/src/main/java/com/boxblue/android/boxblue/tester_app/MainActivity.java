package com.boxblue.android.boxblue.tester_app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.boxblue.android.boxblue.R;
import com.sp27.capstone.boxblue.BoxBlue;
import com.sp27.capstone.boxblue.exception.BoxBlueDeviceNotFoundException;
import com.sp27.capstone.boxblue.handler.BoxBlueHandler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_ENABLE_BT = 1; // request code for enabling bluetooth

    private int arr_size = 100000;
    private int[] arr = new int[arr_size];
    private int key = 0;

    private Button searchButton;

    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"onReceive called");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                BluetoothDevice mmDevice = device;
                Log.d(TAG, "name = " + deviceName + ", MAC: " + deviceHardwareAddress);
                if (deviceName != null && deviceName.equals("raspberrypi")) {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    Log.d(TAG, "trying to connect to RPI");
                    boolean isBonding = mmDevice.createBond();
                    Log.d(TAG,"isBonding?" + isBonding);
                }
            }
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    BluetoothSocket mmSocket = null;
                    try {
                        mmSocket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
                        Log.d(TAG, "Trying to connect to socket.");
                        Class<?> clazz = mmSocket.getRemoteDevice().getClass();
                        Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
                        Method m = clazz.getMethod("createRfcommSocket",paramTypes);
                        Object[] params = new Object[] {Integer.valueOf(1)} ;
                        BluetoothSocket fallback = (BluetoothSocket)m.invoke(mmSocket.getRemoteDevice(), params);
                        Log.d(TAG, "Connected.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
    };

    @Override
    protected void onStart() {
        super.onStart();
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // create button for computing search to boxblue
        searchButton = (Button) findViewById(R.id.search_button);

        // get default bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // ensure that bluetooth adapter works
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "This device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // Enable Bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            // disable search button temporarily
            searchButton.setEnabled(false);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        // initialize handler with context of this activity
        mHandler = new BoxBlueHandler(this);

        // populate array with random numbers
        for (int i = 0; i < arr_size; i++)
            arr[i] = i;


        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search(arr, key);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "Bluetooth is successfully enabled now", Toast.LENGTH_SHORT).show();

                // enable search button
                searchButton.setEnabled(true);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"WE GOOOODOOODODODOD");
            }
        }
    }

    private void search(int[] arr, int key) {
        boolean isDiscovering = mBluetoothAdapter.startDiscovery();
        Log.d(TAG, "isDiscovering? " + isDiscovering);
        /*
        BoxBlue boxBlue = null;
        try {
            boxBlue = new BoxBlue(mBluetoothAdapter, mHandler);
        } catch (BoxBlueDeviceNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        boxBlue.search(arr, key);
        */
    }
}
