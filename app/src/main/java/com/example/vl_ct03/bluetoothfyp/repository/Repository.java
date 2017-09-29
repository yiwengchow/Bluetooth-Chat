package com.example.vl_ct03.bluetoothfyp.repository;

import android.bluetooth.BluetoothSocket;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;

import com.example.vl_ct03.bluetoothfyp.model.BluetoothMessage;
import com.example.vl_ct03.bluetoothfyp.model.FileMessage;
import com.example.vl_ct03.bluetoothfyp.model.MessagingAdapter;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by VL-CT03 on 2/8/2017.
 */
public class Repository {
    private static Repository repository = new Repository();

    private ObservableArrayList<BluetoothMessage> unseenMessages = new ObservableArrayList<>();
    private HashMap<String, MessagingAdapter> messagingAdapterHashMap = new HashMap<>();
    private HashMap<String, BluetoothSocket> bluetoothSocketHashMap = new HashMap<>();
    private HashMap<String, ObjectOutputStream> objectOutputStreamHash = new HashMap<>();
    private HashMap<String, ReentrantLock> reentrantLockHashMap = new HashMap<>();
    private HashMap<String, FileMessage> fileMessageHashMap = new HashMap<>();
    private HashMap<String, Boolean> opponentGameReadyHashMap = new HashMap<>();
    private HashMap<String, Boolean> userGameReadyHashMap = new HashMap<>();
    private HashMap<String, int[]> moveHashMap = new HashMap<>();
    private HashMap<String, Integer> playerNumberHashMap = new HashMap<>();
    private HashMap<String, Integer> numberOfUnseenMessagesHashMap = new HashMap<>();
    private HashMap<String, ArrayList<BluetoothMessage>> bluetoothMessageHashMap = new HashMap<>();
    private ObservableList.OnListChangedCallback messageListener = null;

    public static Repository getInstance(){
        return repository;
    }

    public ObservableArrayList<BluetoothMessage> getUnseenMessages() {
        return unseenMessages;
    }

    public HashMap<String, ObjectOutputStream> getObjectOutputStreamHash() {
        return objectOutputStreamHash;
    }

    public HashMap<String, ReentrantLock> getReentrantLockHashMap() {
        return reentrantLockHashMap;
    }

    public HashMap<String, MessagingAdapter> getMessagingAdapterHashMap() {
        return messagingAdapterHashMap;
    }

    public HashMap<String, FileMessage> getFileMessageHashMap() {
        return fileMessageHashMap;
    }

    public ObservableList.OnListChangedCallback getMessageListener() {
        return messageListener;
    }

    public void setMessageListener(ObservableList.OnListChangedCallback messageListener) {
        this.messageListener = messageListener;
    }

    public HashMap<String, Boolean> getOpponentGameReadyHashMap() {
        return opponentGameReadyHashMap;
    }

    public HashMap<String, Boolean> getUserGameReadyHashMap() {
        return userGameReadyHashMap;
    }

    public void setUserIsReady(String address, boolean flag){
        Repository.getInstance().getUserGameReadyHashMap().remove(address);
        Repository.getInstance().getUserGameReadyHashMap().put(address, flag);
    }

    public void setOpponentIsReady(String address, boolean flag){
        Repository.getInstance().getOpponentGameReadyHashMap().remove(address);
        Repository.getInstance().getOpponentGameReadyHashMap().put(address, flag);
    }

    public HashMap<String, int[]> getMoveHashMap() {
        return moveHashMap;
    }

    public HashMap<String, Integer> getPlayerNumberHashMap() {
        return playerNumberHashMap;
    }

    public HashMap<String, BluetoothSocket> getBluetoothSocketHashMap() {
        return bluetoothSocketHashMap;
    }

    public HashMap<String, Integer> getNumberOfUnseenMessagesHashMap() {
        return numberOfUnseenMessagesHashMap;
    }

    public HashMap<String, ArrayList<BluetoothMessage>> getBluetoothMessageHashMap() {
        return bluetoothMessageHashMap;
    }
}
