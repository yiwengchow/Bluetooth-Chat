package com.example.vl_ct03.bluetoothfyp.controller;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;

import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by PC on 03/05/2017.
 */

public class AcceptTask extends AsyncTask<Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... voids) {
        while (true) {
            try {
                if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    BluetoothServerSocket bluetoothServerSocketSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("", UUID.nameUUIDFromBytes("Yiwengbluetoothfyp".getBytes()));
                    BluetoothSocket bluetoothSocket = bluetoothServerSocketSocket.accept();
                    String address = bluetoothSocket.getRemoteDevice().getAddress();

                    Repository.getInstance().getBluetoothSocketHashMap().put(address, bluetoothSocket);
                    Repository.getInstance().getObjectOutputStreamHash().put(address, new ObjectOutputStream(bluetoothSocket.getOutputStream()));
                    Repository.getInstance().getReentrantLockHashMap().put(address, new ReentrantLock());
                    Repository.getInstance().getOpponentGameReadyHashMap().put(address, false);
                    Repository.getInstance().getUserGameReadyHashMap().put(address, false);
                    Repository.getInstance().getPlayerNumberHashMap().put(address, 2);

                    new UserReceiveTask(bluetoothSocket);

                    try {
                        ConversationFragment conversationFragment = (ConversationFragment) BluetoothActivity.getInstance().getCurrentFragment();
                        conversationFragment.checkConnection();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                    }

                    bluetoothServerSocketSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
