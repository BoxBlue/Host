package com.sp27.capstone.boxblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import com.sp27.capstone.boxblue.connection.BoxBlueClientReceiver;
import com.sp27.capstone.boxblue.connection.BoxBlueClientThread;
import com.sp27.capstone.boxblue.constants.BoxBlueDataTransferType;
import com.sp27.capstone.boxblue.constants.BoxBlueStorageType;
import com.sp27.capstone.boxblue.exception.BoxBlueDeviceNotFoundException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BoxBlue {
    private BluetoothAdapter mmBluetoothAdapter;
    private BluetoothDevice mmDevice;
    private Handler mmHandler;
    private BoxBlueClientReceiver mmBoxBlueClientReceiver;
    public static final String TAG = "BoxBlue";

    // CONSTANTS
    final int numBytesInHeader = 6;
    final int maxPayloadLength = 255;
    final int maxPacketLength = 301;


    private final Set<String> rpiHardwareAddress = new HashSet<>();
    private String rpiAddress;
    private String rpiName;

    private void addRpiMacIds() {
        rpiHardwareAddress.add("B8:27:EB:9B:8B:74");
        rpiHardwareAddress.add("B8:27:EB:9A:6E:5E");
    }



    private Set<BluetoothDevice> getSetOfPairedDevices() {
        return mmBluetoothAdapter.getBondedDevices();
    }

    private Set<BluetoothDevice> getSetOfPairedBoxBlueDevices() {
        Set<BluetoothDevice> pairedBoxBlueDevices = new HashSet<>();

        for (BluetoothDevice device : getSetOfPairedDevices()) {
            String deviceName = device.getName();               // Device Name
            String deviceHardwareAddress = device.getAddress(); // Device MAC address

            Log.d(TAG, deviceName + " " + deviceHardwareAddress);

            if (rpiHardwareAddress.contains(deviceHardwareAddress)) {
                Log.d(TAG, deviceName + " " + deviceHardwareAddress + " added.");
                pairedBoxBlueDevices.add(device);
            }

        }

        return pairedBoxBlueDevices;
    }

    public BoxBlue(BluetoothAdapter bluetoothAdapter, Handler handler, String deviceMAC, String deviceName) {
        // add rpi mac ids
        addRpiMacIds();

        mmBluetoothAdapter = bluetoothAdapter;
        mmHandler = handler;
        mmBoxBlueClientReceiver = new BoxBlueClientReceiver();
        mmBoxBlueClientReceiver.setIntendedDeviceMAC(deviceMAC);
        mmBoxBlueClientReceiver.setIntendedDeviceName(deviceName);
        this.rpiAddress = deviceMAC;
        this.rpiName = deviceName;
    }


    private void applyHeader(byte[] message, int currentIndex, byte id, byte functionType, byte storageType, byte sequence, byte payloadLength) {
        // Apply magic
        message[currentIndex++] = (byte) 0xFA;

        // Apply ID
        message[currentIndex++] = id;

        // Apply function type
        message[currentIndex++] = functionType;

        // Apply storage type
        message[currentIndex++] = storageType;

        // Apply sequence
        message[currentIndex++] = sequence;

        // Apply payload length
        message[currentIndex] = payloadLength;
    }

    private byte[] intArrayToByteArray(int[] array) {
        Log.d(TAG, "int array: " + Arrays.toString(array));

        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(array);

        Log.d(TAG, "byte array: " + Arrays.toString(byteBuffer.array()));

        return byteBuffer.array();
    }

    //call in onStart
    public void registerClientReceiver(final Context context) {
        context.registerReceiver(mmBoxBlueClientReceiver, mmBoxBlueClientReceiver.getFilter());
    }

    //call in onStop
    public void unRegisterClientReceiver(final Context context) {
        context.unregisterReceiver(mmBoxBlueClientReceiver);
    }

    public void connect() {
        if (mmBoxBlueClientReceiver.getSocket() != null) {
            try {
                mmBoxBlueClientReceiver.getSocket().connect();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            for (BluetoothDevice device : mmBluetoothAdapter.getBondedDevices()) {
                if (device.getName().equals(this.rpiName) || device.getAddress().equals(this.rpiAddress)) {
                    mmBoxBlueClientReceiver.connectSocket(device);
                    mmDevice = device;
                    return;
                }
            }
            mmBluetoothAdapter.startDiscovery();
        }
    }

    public void search(int[] array, int key) throws BoxBlueDeviceNotFoundException {
        // this is the total payloadArr
        int[] payloadArr = new int[array.length + 1];
        // add key
        payloadArr[0] = key;
        // add terminator
        //payloadArr[payloadArr.length - 1] = '\n';
        // copy array to [1, payloadArr.length - 1]
        System.arraycopy(array, 0, payloadArr, 1, array.length);

        // this is the total payload array in bytes
        byte[] totalPayloadInBytes = intArrayToByteArray(payloadArr);

        // how many messages should be sent
        // this implementation because if it is a multiple of 255, we want to send an extra packet of size 0
        int numMessages = totalPayloadInBytes.length / maxPayloadLength + 1;
        //int numMessages = totalPayloadInBytes.length % maxPayloadLength == 0 ? totalPayloadInBytes.length / maxPayloadLength : totalPayloadInBytes.length / maxPayloadLength + 1;

        // choose random id for byte
        byte id = (byte) new Random().nextInt();

        // TODO try to make total message as 2D array
        byte[][] totalMessage = new byte[numMessages][];
        Log.d(TAG, "Number of messages: " + numMessages);
        for(int i = 0; i < numMessages; i++) {
            int payloadSize = totalPayloadInBytes.length - i * maxPayloadLength >= maxPayloadLength ? maxPayloadLength : totalPayloadInBytes.length - i * maxPayloadLength;
            //Log.d(TAG, "Payload size: " + payloadSize);
            byte[] msg = new byte[payloadSize + numBytesInHeader];

            // apply header
            applyHeader(msg,
                    0,
                    id,
                    BoxBlueDataTransferType.SEARCH.getTransferTypeId(),
                    BoxBlueStorageType.NULL.getStorageTypeId(),
                    (byte) i,
                    (byte) payloadSize);

            // copy payload at [i * maxPacketLength, i * maxPacketLength + payloadSize] to msg at [6, 6 + payloadSize]
            System.arraycopy(totalPayloadInBytes, i * maxPayloadLength, msg, numBytesInHeader, payloadSize);

            Log.d(TAG, "message " + i + ": " + Arrays.toString(msg));

            totalMessage[i] = msg;
        }

        byte[] messageTemp = new byte[7];
        messageTemp[0] = (byte) 0xFA;
        messageTemp[1] = 1;
        messageTemp[2] = 2;
        messageTemp[3] = 3;
        messageTemp[4] = 4;
        messageTemp[5] = 1;
        messageTemp[6] = 6;

        Log.d(TAG, "temp message byte array: " + Arrays.toString(messageTemp));

        if (mmDevice == null) mmDevice = mmBoxBlueClientReceiver.getDevice();
        if (mmDevice == null) {
            throw new BoxBlueDeviceNotFoundException("No device. Make sure to call registerClientReceiver() and then connect() from your BoxBlue instance.");
        }
        else {
            // initialize client thread
            BoxBlueClientThread boxBlueClientThread = new BoxBlueClientThread(mmDevice,
                    BoxBlueDataTransferType.SEARCH,
                    totalMessage,
                    mmHandler,
                    mmBluetoothAdapter,
                    mmBoxBlueClientReceiver.getSocket()
            );

            // start thread which connects to boxblue and sends the data then reads the data
            boxBlueClientThread.start();
        }
    }

    public void sort(int[] array)  throws BoxBlueDeviceNotFoundException {

        // this is the total payload array in bytes
        byte[] totalPayloadInBytes = intArrayToByteArray(array);

        // how many messages should be sent
        // this implementation because if it is a multiple of 255, we want to send an extra packet of size 0
        int numMessages = totalPayloadInBytes.length / maxPayloadLength + 1;
        //int numMessages = totalPayloadInBytes.length % maxPayloadLength == 0 ? totalPayloadInBytes.length / maxPayloadLength : totalPayloadInBytes.length / maxPayloadLength + 1;

        // choose random id for byte
        byte id = (byte) new Random().nextInt();

        // TODO try to make total message as 2D array
        byte[][] totalMessage = new byte[numMessages][];
        Log.d(TAG, "Number of messages: " + numMessages);
        for(int i = 0; i < numMessages; i++) {
            int payloadSize = totalPayloadInBytes.length - i * maxPayloadLength >= maxPayloadLength ? maxPayloadLength : totalPayloadInBytes.length - i * maxPayloadLength;
            //Log.d(TAG, "Payload size: " + payloadSize);
            byte[] msg = new byte[payloadSize + numBytesInHeader];

            // apply header
            applyHeader(msg,
                    0,
                    id,
                    BoxBlueDataTransferType.SORT.getTransferTypeId(),
                    BoxBlueStorageType.NULL.getStorageTypeId(),
                    (byte) i,
                    (byte) payloadSize);

            // copy payload at [i * maxPacketLength, i * maxPacketLength + payloadSize] to msg at [6, 6 + payloadSize]
            System.arraycopy(totalPayloadInBytes, i * maxPayloadLength, msg, numBytesInHeader, payloadSize);

            Log.d(TAG, "message " + i + ": " + Arrays.toString(msg));

            totalMessage[i] = msg;
        }

        if (mmDevice == null) mmDevice = mmBoxBlueClientReceiver.getDevice();
        if (mmDevice == null) {
            throw new BoxBlueDeviceNotFoundException("No device. Make sure to call registerClientReceiver() and then connect() from your BoxBlue instance.");
        }
        else {
            // initialize client thread
            BoxBlueClientThread boxBlueClientThread = new BoxBlueClientThread(mmDevice,
                    BoxBlueDataTransferType.SORT,
                    totalMessage,
                    mmHandler,
                    mmBluetoothAdapter,
                    mmBoxBlueClientReceiver.getSocket()
            );

            // start thread which connects to boxblue and sends the data then reads the data
            boxBlueClientThread.start();
        }
    }

    public void storeImage(byte[] totalPayloadInBytes) throws BoxBlueDeviceNotFoundException {
        // how many messages should be sent
        // this implementation because if it is a multiple of 255, we want to send an extra packet of size 0
        int numMessages = totalPayloadInBytes.length / maxPayloadLength + 1;
        //int numMessages = totalPayloadInBytes.length % maxPayloadLength == 0 ? totalPayloadInBytes.length / maxPayloadLength : totalPayloadInBytes.length / maxPayloadLength + 1;

        // choose random id for byte
        byte id = (byte) new Random().nextInt();

        // TODO try to make total message as 2D array
        byte[][] totalMessage = new byte[numMessages][];
        Log.d(TAG, "Number of messages: " + numMessages);
        for(int i = 0; i < numMessages; i++) {
            int payloadSize = totalPayloadInBytes.length - i * maxPayloadLength >= maxPayloadLength ? maxPayloadLength : totalPayloadInBytes.length - i * maxPayloadLength;
            //Log.d(TAG, "Payload size: " + payloadSize);
            byte[] msg = new byte[payloadSize + numBytesInHeader];

            // apply header
            applyHeader(msg,
                    0,
                    id,
                    BoxBlueDataTransferType.TRANSFER_DATA.getTransferTypeId(),
                    BoxBlueStorageType.IMAGE.getStorageTypeId(),
                    (byte) i,
                    (byte) payloadSize);

            // copy payload at [i * maxPacketLength, i * maxPacketLength + payloadSize] to msg at [6, 6 + payloadSize]
            System.arraycopy(totalPayloadInBytes, i * maxPayloadLength, msg, numBytesInHeader, payloadSize);

            Log.d(TAG, "message " + i + ": " + Arrays.toString(msg));

            totalMessage[i] = msg;
        }

        if (mmDevice == null) mmDevice = mmBoxBlueClientReceiver.getDevice();
        if (mmDevice == null) {
            throw new BoxBlueDeviceNotFoundException("No device. Make sure to call registerClientReceiver() and then connect() from your BoxBlue instance.");
        }
        else {
            // initialize client thread
            BoxBlueClientThread boxBlueClientThread = new BoxBlueClientThread(mmDevice,
                    BoxBlueDataTransferType.TRANSFER_DATA,
                    totalMessage,
                    mmHandler,
                    mmBluetoothAdapter,
                    mmBoxBlueClientReceiver.getSocket()
            );

            // start thread which connects to boxblue and sends the data then reads the data
            boxBlueClientThread.start();
        }
    }



}
