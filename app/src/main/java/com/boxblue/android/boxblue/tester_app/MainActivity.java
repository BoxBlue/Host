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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.boxblue.android.boxblue.R;
import com.sp27.capstone.boxblue.BoxBlue;
import com.sp27.capstone.boxblue.exception.BoxBlueDeviceNotFoundException;
import com.sp27.capstone.boxblue.handler.BoxBlueHandler;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_ENABLE_BT = 1; // request code for enabling bluetooth
    static final int REQUEST_COARSE_LOCATION = 2; // request code for enanbling coarse location
    static final int REQUEST_PICTURE = 3; // request code for taking a picture

    private int arr_size = 10000;
    private int[] sorted_arr = new int[arr_size];
    private int[] unsorted_arr = new int[arr_size];
    private int key = 0;

    private Button searchButton, sortButton, pictureButton, connectBtn;
    private TextView connectionStatusView;

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

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                Toast.makeText(this, "Permission access failed?", Toast.LENGTH_SHORT).show();

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_COARSE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        // Enable Bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            // disable search button temporarily
            searchButton.setEnabled(false);
            sortButton.setEnabled(false);
            pictureButton.setEnabled(false);
            connectBtn.setEnabled(false);
        } else {
            // initialize handler with context of this activity
            mHandler = new BoxBlueHandler(this,connectionStatusView);
            //boxBlue = new BoxBlue(mBluetoothAdapter, mHandler, "B8:27:EB:9A:6E:5E", "raspberrypi");
            boxBlue = new BoxBlue(mBluetoothAdapter, mHandler, "B8:27:EB:9B:8B:74", "raspberrypi");
            if (!boxBlue.isRegistered()) {
                boxBlue.registerClientReceiver(this);
                Log.d(TAG, "Starting connection");
                boxBlue.connect();
                Log.d(TAG, "Connect called");
            }
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
        sortButton = (Button) findViewById(R.id.sort_button);
        pictureButton = (Button) findViewById(R.id.picture_button);
        connectBtn = (Button)findViewById(R.id.try_connect_btn);
        connectionStatusView = (TextView)findViewById(R.id.status_view);
        connectionStatusView.setText("Disconnected");

        Random rand = new Random();


        // populate sorted array with random numbers
        for (int i = 0; i < arr_size; i++) {
            sorted_arr[i] = i;
            unsorted_arr[i] = rand.nextInt();
        }


        long nanoStart = SystemClock.elapsedRealtimeNanos();
        Arrays.binarySearch(sorted_arr,0);
        long nanoEnd = SystemClock.elapsedRealtimeNanos();
        long time = nanoEnd - nanoStart;
        Log.d("TIME","elapsed for local search = " + time);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search(sorted_arr, key);
            }
        });
        sortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sort(unsorted_arr);
            }
        });
        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take a picture with the phone
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePicture, REQUEST_PICTURE);
            }
        });
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boxBlue.connect();
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
                boxBlue = new BoxBlue(mBluetoothAdapter, mHandler, "B8:27:EB:9A:6E:5E", "raspberrypi");
                boxBlue.registerClientReceiver(this);
                boxBlue.connect();
                // enable search button
                searchButton.setEnabled(true);
                sortButton.setEnabled(true);
                connectBtn.setEnabled(true);
            }
        }
        else if (requestCode == REQUEST_PICTURE) {
            if (resultCode == RESULT_OK) {
                Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] imageInBytes = stream.toByteArray();
                try {
                    boxBlue.storeImage(imageInBytes);
                } catch (BoxBlueDeviceNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"WE GOOOODOOODODODOD");
            }
        }
    }

    private void search(int[] arr, int key) {
        try {
            boxBlue.search(arr, key);
        } catch (BoxBlueDeviceNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sort(int[] arr) {
        try {
            boxBlue.sort(arr);
        } catch (BoxBlueDeviceNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
