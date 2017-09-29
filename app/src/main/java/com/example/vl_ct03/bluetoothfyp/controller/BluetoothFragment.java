package com.example.vl_ct03.bluetoothfyp.controller;

import android.bluetooth.BluetoothDevice;
import android.support.v4.app.Fragment;

import com.example.vl_ct03.bluetoothfyp.model.BluetoothMessage;

/**
 * Created by PC on 06/04/2017.
 */

public abstract class BluetoothFragment extends Fragment {
    abstract void receiveMessage(BluetoothMessage bluetoothMessage);

    abstract void deviceFound(BluetoothDevice bluetoothDevice);

    abstract void deviceConnected(BluetoothDevice bluetoothDevice);

    abstract void deviceDisconnected(BluetoothDevice bluetoothDevice);

    abstract void bluetoothOn();

    abstract void bluetoothOff();

    abstract void discoveryFinished();
}
