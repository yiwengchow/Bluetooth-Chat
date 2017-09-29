package com.example.vl_ct03.bluetoothfyp.controller;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.model.BluetoothMessage;
import com.example.vl_ct03.bluetoothfyp.model.MessageBlock;
import com.example.vl_ct03.bluetoothfyp.model.MessageBlockAdapter;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

import static android.app.Activity.RESULT_OK;
import static com.example.vl_ct03.bluetoothfyp.R.string.app_name;

/**
 * Created by PC on 02/04/2017.
 */

public class UserListFragment extends BluetoothFragment {

    final int ADD_DEVICE_REQUEST = 0;
    final int MAKE_DISCOVERABLE_REQUEST = 1;
    final int ON_BLUETOOTH_FOR_DISCOVERABLE_REQUEST = 2;
    final int discoverableDuration = 300;

    ArrayList<MessageBlock> mMessageBlocks = new ArrayList<>();
    MessageBlockAdapter mMessageBlockAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_list, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.findItem(R.id.make_discoverable).setVisible(true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        BluetoothActivity.getInstance().getSupportActionBar().setHomeButtonEnabled(false);
        BluetoothActivity.getInstance().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        BluetoothActivity.getInstance().getSupportActionBar().setDisplayShowHomeEnabled(false);
        BluetoothActivity.getInstance().getSupportActionBar().setTitle(app_name);

        FloatingActionButton addButton = (FloatingActionButton) view.findViewById(R.id.addButton);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBt, ADD_DEVICE_REQUEST);
                } else {
                    ((BluetoothActivity) getActivity()).changeFragment(new AddUserFragment());
                }
            }
        });

        mMessageBlocks.clear();
        mMessageBlockAdapter = new MessageBlockAdapter(mMessageBlocks);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.user_recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mMessageBlockAdapter);

        for (Object object : StorageUtility.readFromFile(StorageUtility.getMessageBlocksFileName())) {
            mMessageBlocks.add((MessageBlock) object);
        }

        boolean load = false;

        for (MessageBlock messageBlock : mMessageBlocks) {
            if (!Repository.getInstance().getNumberOfUnseenMessagesHashMap().containsKey(messageBlock.getAddress())) {
                load = true;
                break;
            }
            else{
                messageBlock.setUnread(Repository.getInstance().getNumberOfUnseenMessagesHashMap().get(messageBlock.getAddress()));
            }
        }

        if (!load){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Collections.sort(mMessageBlocks);
                    StorageUtility.saveToFile(new ArrayList<Object>(mMessageBlocks), StorageUtility.getMessageBlocksFileName());

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMessageBlockAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }
        else {
            final ProgressDialog progress = new ProgressDialog(getActivity());
            mMessageBlocks.clear();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<Thread> threads = new ArrayList<>();
                    final ArrayList<MessageBlock> messageBlocks = new ArrayList<>();

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setTitle("Loading");
                            progress.setMessage("Loading your messages");
                            progress.setCancelable(false);
                            progress.show();
                        }
                    });

                    for (Object object : StorageUtility.readFromFile(StorageUtility.getMessageBlocksFileName())) {
                        final MessageBlock messageBlock = (MessageBlock) object;

                        threads.add(messageBlock.loadUnread());
                        messageBlocks.add(messageBlock);

                        Thread messageThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<BluetoothMessage> bluetoothMessages = new ArrayList<>();

                                for (Object o : StorageUtility.readFromFile(StorageUtility.getMessagesFileName(messageBlock.getAddress()))) {
                                    bluetoothMessages.add((BluetoothMessage) o);
                                    System.out.println("adding message");
                                }

                                Repository.getInstance().getBluetoothMessageHashMap().put(messageBlock.getAddress(), bluetoothMessages);
                            }
                        });

                        messageThread.start();
                        threads.add(messageThread);
                    }

                    for (Thread thread : threads) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    Collections.sort(messageBlocks);
                    StorageUtility.saveToFile(new ArrayList<Object>(messageBlocks), StorageUtility.getMessageBlocksFileName());

                    for (MessageBlock messageBlock : messageBlocks) {
                        mMessageBlocks.add(messageBlock);
                        Repository.getInstance().getNumberOfUnseenMessagesHashMap().put(messageBlock.getAddress(), messageBlock.getUnread());
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mMessageBlockAdapter.notifyDataSetChanged();

                            progress.dismiss();
                        }
                    }, 400);
                }
            }).start();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.make_discoverable:
                if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, discoverableDuration);
                    startActivityForResult(discoverableIntent, MAKE_DISCOVERABLE_REQUEST);
                } else {
                    Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBt, ON_BLUETOOTH_FOR_DISCOVERABLE_REQUEST);
                }
                break;
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().invalidateOptionsMenu();

        setMenuVisibility(true);
    }

    @Override
    public void onPause() {
        super.onPause();

        setMenuVisibility(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ADD_DEVICE_REQUEST:
                if (resultCode == RESULT_OK) {
                    ((BluetoothActivity) getActivity()).changeFragment(new AddUserFragment());
                } else {
                    Toast.makeText(getActivity(), "You need to have bluetooth enabled to display users", Toast.LENGTH_SHORT).show();
                }
                break;

            case MAKE_DISCOVERABLE_REQUEST:
                if (resultCode == discoverableDuration) {
                    Toast.makeText(getActivity(), "Discoverable enabled for " + discoverableDuration + " seconds", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Discoverable not enabled", Toast.LENGTH_SHORT).show();
                }
                break;

            case ON_BLUETOOTH_FOR_DISCOVERABLE_REQUEST:
                if (resultCode == RESULT_OK) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, discoverableDuration);
                    startActivity(discoverableIntent);
                } else {
                    Toast.makeText(getActivity(), "You need to turn on bluetooth to be discoverable", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public void receiveMessage(BluetoothMessage bluetoothMessage) {
        boolean exist = false;

        for (final MessageBlock messageBlock : mMessageBlocks) {
            if (messageBlock.getAddress().equals(bluetoothMessage.getSenderAddress())) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int oldPosition = mMessageBlocks.indexOf(messageBlock);

                        mMessageBlocks.remove(oldPosition);
                        mMessageBlocks.add(0, messageBlock);

                        mMessageBlockAdapter.notifyItemMoved(oldPosition, 0);

                        messageBlock.addUnread();
                        mMessageBlockAdapter.notifyItemChanged(0);
                    }
                });
                exist = true;
                break;
            }
        }

        if (!exist) {
            MessageBlock messageBlock = new MessageBlock(bluetoothMessage.getSenderName(), bluetoothMessage.getSenderAddress());
            mMessageBlocks.add(0, messageBlock);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mMessageBlockAdapter.notifyItemInserted(0);
                    mMessageBlockAdapter.notifyItemRangeChanged(1,mMessageBlocks.size());
                }
            });
        }

        StorageUtility.saveToFile(new ArrayList<Object>(mMessageBlocks), StorageUtility.getMessageBlocksFileName());
    }

    @Override
    public void deviceFound(BluetoothDevice bluetoothDevice) {

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

    }
}
