package com.example.vl_ct03.bluetoothfyp.controller;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.model.BluetoothMessage;
import com.example.vl_ct03.bluetoothfyp.model.FileMessage;
import com.example.vl_ct03.bluetoothfyp.model.MessageBlock;
import com.example.vl_ct03.bluetoothfyp.model.TextMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by PC on 23/04/2017.
 */

public class StorageUtility {
    public static final File sourceFile = new File(Environment.getExternalStorageDirectory(), "BluetoothChat");
    public static String getMessageBlocksFileName(){
        return "message_blocks";
    }
    public static String getMessagesFileName(String address){
        return address + "_messages";
    }

    public static void updateProgress(FileMessage fileMessage) {
        String address = fileMessage.getReceiverAddress().equals(android.provider.Settings.Secure.getString(BluetoothActivity.getInstance().getContentResolver(),
                "bluetooth_address"))
                ? fileMessage.getSenderAddress() : fileMessage.getReceiverAddress();

        ArrayList<BluetoothMessage> bluetoothMessages = new ArrayList<>();

        for (Object o : StorageUtility.readFromFile(getMessagesFileName(address))) {
            bluetoothMessages.add((BluetoothMessage) o);
        }

        bluetoothMessages.remove(bluetoothMessages.size() - 1);
        bluetoothMessages.add(fileMessage);

        StorageUtility.saveToFile(new ArrayList<Object>(bluetoothMessages), getMessagesFileName(address));
    }

    public static void addMessage(BluetoothMessage bluetoothMessage) {
        String address = bluetoothMessage.getReceiverAddress().equals(android.provider.Settings.Secure.getString(BluetoothActivity.getInstance().getContentResolver(),
                "bluetooth_address"))
                ? bluetoothMessage.getSenderAddress() : bluetoothMessage.getReceiverAddress();

        String name = bluetoothMessage.getReceiverName().equals(BluetoothAdapter.getDefaultAdapter().getName())
                ? bluetoothMessage.getSenderName() : bluetoothMessage.getReceiverName();

        if (!sourceFile.exists()) {
            System.out.println("does not exist");
            sourceFile.mkdirs();
        }

        ArrayList<BluetoothMessage> bluetoothMessages = new ArrayList<>();

        for (Object o : readFromFile(getMessagesFileName(address))) {
            bluetoothMessages.add((BluetoothMessage) o);
        }

        bluetoothMessages.add(bluetoothMessage);

        saveToFile(new ArrayList<Object>(bluetoothMessages), getMessagesFileName(address));

        MessageBlock messageBlock = null;
        ArrayList<MessageBlock> messageBlockArrayList = new ArrayList<>();

        for (Object object : readFromFile(getMessageBlocksFileName())) {
            messageBlockArrayList.add((MessageBlock) object);
        }

        for (MessageBlock block : messageBlockArrayList) {
            if (block.getAddress().equals(address)) {
                messageBlock = block;
                break;
            }
        }

        if (messageBlock == null) {
            addMessageBlock(new MessageBlock(name,address));
        }
    }

    public static void addMessageBlock(MessageBlock messageBlock) {
        if (!sourceFile.exists()) {
            System.out.println("does not exist");
            sourceFile.mkdirs();
        }

        ArrayList<MessageBlock> messageBlocks = new ArrayList<>();

        for (Object o : readFromFile(getMessageBlocksFileName())) {
            messageBlocks.add((MessageBlock) o);
        }

        messageBlocks.add(messageBlock);
        saveToFile(new ArrayList<Object>(messageBlocks), getMessageBlocksFileName());
    }

    public static void saveToFile(ArrayList<Object> items, String filename) {
        if (!sourceFile.exists()) {
            sourceFile.mkdirs();
        }

        File file = new File(sourceFile.getPath(), filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));

            Collections.reverse(items);

            for (Object item : items) {
                oos.writeObject(item);
            }
            oos.flush();
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Object> readFromFile(String filename) {
        if (!sourceFile.exists()) {
            sourceFile.mkdirs();
        }

        File file = new File(sourceFile.getPath(), filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<Object> items = new ArrayList<>();

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));

            while (true) {
                items.add(ois.readObject());
            }
        } catch (ClassNotFoundException e) {
        } catch (IOException e) {
        } catch (ClassCastException e){
        }

        Collections.reverse(items);

        return items;
    }

    public static ArrayList<Object> readANumberFromFile(String filename, int amount){
        if (!sourceFile.exists()) {
            sourceFile.mkdirs();
        }

        File file = new File(sourceFile.getPath(), filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<Object> items = new ArrayList<>();

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));

            for (int i = 0; i < amount; i++) {
                items.add(ois.readObject());
            }
        } catch (ClassNotFoundException e) {
        } catch (IOException e) {
        } catch (ClassCastException e){
        }

        return items;
    }
}
