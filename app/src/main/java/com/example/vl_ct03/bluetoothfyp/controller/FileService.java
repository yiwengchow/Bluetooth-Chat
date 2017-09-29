package com.example.vl_ct03.bluetoothfyp.controller;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.example.vl_ct03.bluetoothfyp.model.FileMessage;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.io.IOException;

/**
 * Created by PC on 19/05/2017.
 */

public class FileService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        for (String address: Repository.getInstance().getFileMessageHashMap().keySet()) {
            FileMessage fileMessage = Repository.getInstance().getFileMessageHashMap().get(address);
            fileMessage.setFailed(true);
            StorageUtility.updateProgress(fileMessage);
        }
    }
}
