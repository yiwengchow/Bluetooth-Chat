package com.example.vl_ct03.bluetoothfyp.controller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.model.BluetoothMessage;
import com.example.vl_ct03.bluetoothfyp.model.BtDeviceAdapter;
import com.example.vl_ct03.bluetoothfyp.model.MessageBlock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by PC on 02/04/2017.
 */

public class AddUserFragment extends BluetoothFragment implements Serializable {
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private List<String> mMessageBlocks = new ArrayList<>();
    private BtDeviceAdapter mBtDeviceAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_add_user, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        BluetoothActivity.getInstance().getSupportActionBar().setHomeButtonEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setDisplayShowHomeEnabled(true);

        getActivity().getWindow().setBackgroundDrawableResource(android.R.color.white);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.PairedRecyclerView);

        mBtDeviceAdapter = new BtDeviceAdapter(mDevices);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mBtDeviceAdapter);

        for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            mDevices.add(device);
        }

        for (Object object : StorageUtility.readFromFile(StorageUtility.getMessageBlocksFileName())) {
            MessageBlock messageBlock = (MessageBlock) object;

            mMessageBlocks.add(messageBlock.getAddress());
        }

        List<BluetoothDevice> deviceClone = new ArrayList<>(mDevices);

        for (BluetoothDevice bluetoothDevice : deviceClone) {
            if (mMessageBlocks.contains(bluetoothDevice.getAddress())) {
                mDevices.remove(bluetoothDevice);
            }
        }

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.add_user_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mDevices.clear();

                for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
                    if (!mMessageBlocks.contains(device.getAddress())) {
                        mDevices.add(device);
                    }
                }

                mBtDeviceAdapter.notifyDataSetChanged();

                if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                }

                BluetoothAdapter.getDefaultAdapter().startDiscovery();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
        }

        return false;
    }

    @Override
    public void onPause(){
        super.onPause();

        if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });

        if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }

        BluetoothAdapter.getDefaultAdapter().startDiscovery();

    }

    @Override
    public void receiveMessage(BluetoothMessage bluetoothMessage) {

    }

    @Override
    public void deviceFound(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice != null) {
            if (!bluetoothDevice.getName().equals("") && !mDevices.contains(bluetoothDevice) && !mMessageBlocks.contains(bluetoothDevice.getAddress())) {
                mDevices.add(bluetoothDevice);
                mBtDeviceAdapter.notifyItemInserted(mDevices.size());
            }
        }
    }

    @Override
    public void deviceConnected(BluetoothDevice bluetoothDevice) {

    }

    @Override
    public void deviceDisconnected(BluetoothDevice bluetoothDevice) {

    }

    @Override
    public void bluetoothOn() {

    }

    @Override
    public void bluetoothOff() {

    }

    @Override
    public void discoveryFinished() {
        swipeRefreshLayout.setRefreshing(false);
    }
}
