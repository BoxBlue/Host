package com.sp27.capstone.boxblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import com.sp27.capstone.boxblue.connection.BoxBlueClientThread;
import com.sp27.capstone.boxblue.constants.BoxBlueDataTransferType;
import com.sp27.capstone.boxblue.constants.BoxBlueStorageType;
import com.sp27.capstone.boxblue.exception.BoxBlueDeviceNotFoundException;

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
    public static final String TAG = "BoxBlue";

    private Set<BluetoothDevice> getSetOfPairedDevices() {
        return mmBluetoothAdapter.getBondedDevices();
    }

    private Set<BluetoothDevice> getSetOfPairedBoxBlueDevices() {
        Set<BluetoothDevice> pairedBoxBlueDevices = new HashSet<>();

        for (BluetoothDevice device : getSetOfPairedDevices()) {
            String deviceName = device.getName();               // Device Name
            String deviceHardwareAddress = device.getAddress(); // Device MAC address

            Log.d(TAG, deviceName + " " + deviceHardwareAddress);

            // Getting uuids of the device
            ParcelUuid[] uuids = device.getUuids();

            pairedBoxBlueDevices.add(device);

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

    private void applyHeader(byte[] message, int currentIndex, byte id, byte functionType, byte storageType, byte sequence, byte payloadLength) {
        // Apply magic
        message[currentIndex++] = (byte) R.integer.magic;

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

        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * 4 + 1);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(array);

        Log.d(TAG, "byte array: " + Arrays.toString(byteBuffer.array()));

        return byteBuffer.array();
    }

    public void search(int[] array, int key) {

        // this is the total payloadArr
        int[] payloadArr = new int[array.length + 2];
        // add key
        payloadArr[0] = key;
        // add terminator
        payloadArr[payloadArr.length - 1] = '\n';
        // copy array to [1, payloadArr.length - 2]
        System.arraycopy(array, 0, payloadArr, 1, array.length);

        // CONSTANTS
        final int numBytesInHeader = 6;
        final int maxPayloadLength = 255;

        // this is the total payload array in bytes
        byte[] totalPayloadInBytes = intArrayToByteArray(payloadArr);

        // how many messages should be sent
        int numMessages = totalPayloadInBytes.length % maxPayloadLength == 0 ? totalPayloadInBytes.length / maxPayloadLength : totalPayloadInBytes.length / maxPayloadLength + 1;

        // total message to be sent in bytes
        byte[] totalMessageInBytes = new byte[totalPayloadInBytes.length + numBytesInHeader * numMessages];

        // counters
        int payloadStartIndex = 0, messageStartIndex = 0;

        // choose random id for byte
        byte id = (byte) new Random().nextInt();

        for (int sequence = 0; sequence < numMessages; sequence++) {

            // size of payload
            int sizeOfCurrentPayload = totalPayloadInBytes.length - payloadStartIndex >= maxPayloadLength ? maxPayloadLength : totalPayloadInBytes.length - payloadStartIndex;

            //Log.d(TAG, "size of payload: " + sizeOfCurrentPayload);

            // apply header to byte buffer
            applyHeader(totalMessageInBytes,
                    messageStartIndex,
                    id,
                    BoxBlueDataTransferType.SEARCH.getTransferTypeId(),
                    BoxBlueStorageType.NULL.getStorageTypeId(),
                    (byte) sequence,
                    (byte) sizeOfCurrentPayload);

            // offset message start index by size of header
            messageStartIndex += numBytesInHeader;

            // copy payload into total message in bytes
            System.arraycopy(totalPayloadInBytes, payloadStartIndex,
                    totalMessageInBytes, messageStartIndex, sizeOfCurrentPayload);

            // increment start indices
            payloadStartIndex += sizeOfCurrentPayload;
            messageStartIndex += sizeOfCurrentPayload;
        }

        Log.d(TAG, "message byte array: " + Arrays.toString(totalMessageInBytes));

        // initialize client thread
        BoxBlueClientThread boxBlueClientThread = new BoxBlueClientThread(mmDevice,
                BoxBlueDataTransferType.SEARCH,
                totalMessageInBytes,
                mmHandler,
                mmBluetoothAdapter);

        // start thread which connects to boxblue and sends the data then reads the data
        boxBlueClientThread.start();
    }

    public int[] sort(int[] array) {
        return array;
    }



}
