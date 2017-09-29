package com.example.vl_ct03.bluetoothfyp.model;

import android.support.annotation.NonNull;

import com.example.vl_ct03.bluetoothfyp.controller.StorageUtility;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * Created by PC on 08/04/2017.
 */

public class MessageBlock implements Serializable, Comparable<MessageBlock> {
    private String mName;
    private String mAddress;

    private int mCount;

    public MessageBlock(String name, String address) {
        this.mName = name;
        this.mAddress = address;

        try {
            loadUnread().join();
        } catch (InterruptedException e) {

        }
    }

    public void addUnread(){
        ++mCount;
    }

    public Thread loadUnread(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mCount = 0;

                ArrayList<Object> objects = StorageUtility.readFromFile(StorageUtility.getMessagesFileName(getAddress()));
                Collections.reverse(objects);

                for (Object object : objects) {
                    if (!((BluetoothMessage) object).getRead()) {
                        addUnread();
                    } else {
                        break;
                    }
                }
            }
        });

        thread.start();

        return thread;
    }

    private Date getLastDate() {
        ArrayList<BluetoothMessage> bluetoothMessages = new ArrayList<>();

        for (Object o : StorageUtility.readFromFile(StorageUtility.getMessagesFileName(getAddress()))){
            bluetoothMessages.add((BluetoothMessage) o);
        }

        if (bluetoothMessages.size() > 0) {
            return bluetoothMessages.get(bluetoothMessages.size() - 1).getDate();
        }
        else{
            return null;
        }

    }

    public void setUnread(int num){
        mCount = num;
    }

    public int getUnread(){
        return mCount;
    }


    public String getName() {
        return mName;
    }


    @Override
    public int compareTo(@NonNull MessageBlock messageBlock) {
        if (getLastDate() != null) {
            if (messageBlock.getLastDate() != null) {
                return -(getLastDate().compareTo(messageBlock.getLastDate()));
            } else {
                return -1;
            }
        } else {
            if (messageBlock.getLastDate() == null) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    public String getAddress() {
        return mAddress;
    }
}
