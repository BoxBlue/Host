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

    private BoxBlue boxBlue;

    @Override
    protected void onStart() {
        super.onStart();
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
        } else {
            // initialize handler with context of this activity
            mHandler = new BoxBlueHandler(this);
            boxBlue = new BoxBlue(mBluetoothAdapter, mHandler, "B8:27:EB:9B:8B:74", "raspberrypi");
            boxBlue.registerClientReceiver(this);
            boxBlue.connect();
        }



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (boxBlue != null) {
            boxBlue.unRegisterClientReceiver(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // create button for computing search to boxblue
        searchButton = (Button) findViewById(R.id.search_button);



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
                mHandler = new BoxBlueHandler(this);
                boxBlue = new BoxBlue(mBluetoothAdapter, mHandler, "B8:27:EB:9B:8B:74", "raspberrypi");
                boxBlue.registerClientReceiver(this);
                boxBlue.connect();
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


        try {
            boxBlue.search(arr, key);
        } catch (BoxBlueDeviceNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }


    }
}
