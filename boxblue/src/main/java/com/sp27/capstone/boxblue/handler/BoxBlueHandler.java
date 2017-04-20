package com.sp27.capstone.boxblue.handler;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.util.Arrays;

import static com.sp27.capstone.boxblue.constants.BoxBlueMessageConstants.MESSAGE_READ;
import static com.sp27.capstone.boxblue.constants.BoxBlueMessageConstants.MESSAGE_TOAST;
import static com.sp27.capstone.boxblue.constants.BoxBlueMessageConstants.MESSAGE_WRITE;

/**
 * Created by crejaud on 4/4/17.
 */

public class BoxBlueHandler extends Handler {

    Context mCtx;

    public BoxBlueHandler(Context ctx) {
        mCtx = ctx;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_WRITE:
                byte[][] writeBufArr = (byte[][]) msg.obj;
                for (int i = 0; i < writeBufArr.length; i++) {
                    byte[] writeBuf = writeBufArr[i];
                    // construct a string from the buffer
                    Toast.makeText(mCtx, Arrays.toString(writeBuf),
                            Toast.LENGTH_SHORT).show();
                }
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                Toast.makeText(mCtx,new String(readBuf),Toast.LENGTH_LONG).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(mCtx, msg.getData().getString("toast"),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
