package com.example.vl_ct03.bluetoothfyp.controller;

import android.Manifest;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.ObservableList;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.widget.Toast;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.model.BluetoothMessage;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class BluetoothActivity extends AppCompatActivity {

    private final int MY_PERMISSIONS_REQUEST = 100;
    private static BluetoothActivity mBluetoothActivity;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            BluetoothFragment bluetoothFragment = getCurrentFragment();

            if (bluetoothFragment != null) {
                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        bluetoothFragment.deviceConnected(bluetoothDevice);
                        System.out.println("device connected lol");
                        break;

                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        bluetoothFragment.deviceDisconnected(bluetoothDevice);
                        break;

                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                            bluetoothFragment.bluetoothOn();
                        } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                            bluetoothFragment.bluetoothOff();
                        }
                        break;

                    case BluetoothDevice.ACTION_FOUND:
                        bluetoothFragment.deviceFound(bluetoothDevice);
                        break;

                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        bluetoothFragment.discoveryFinished();
                        break;
                }
            }
        }
    };

    public static BluetoothActivity getInstance() {
        return mBluetoothActivity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mBluetoothActivity = this;

        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        String address = getIntent().getStringExtra("notification_address");

        if (address != null){
            NotificationUtility.removeNotification(NotificationUtility.gameNotificationIds.get(address));

            BluetoothSocket bluetoothSocket = Repository.getInstance().getBluetoothSocketHashMap().get(address);

            if (bluetoothSocket != null) {
                GameRoomFragment gameRoomFragment = new GameRoomFragment();
                gameRoomFragment.initialize(bluetoothSocket);

                changeFragment(gameRoomFragment);
            }
            else{
                changeFragment(new UserListFragment());
                Toast.makeText(this, "Lost connection", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            changeFragment(new UserListFragment());
        }

        if (Repository.getInstance().getMessageListener() != null){
            Repository.getInstance().getUnseenMessages().removeOnListChangedCallback(Repository.getInstance().getMessageListener());
        }

        Repository.getInstance().setMessageListener(new ObservableList.OnListChangedCallback<ObservableList<BluetoothMessage>>() {
            @Override
            public void onChanged(ObservableList<BluetoothMessage> bluetoothMessages) {
            }

            @Override
            public void onItemRangeChanged(ObservableList<BluetoothMessage> bluetoothMessages, int i, int i1) {
            }

            @Override
            public void onItemRangeInserted(ObservableList<BluetoothMessage> bluetoothMessages, int i, int i1) {
                BluetoothMessage bluetoothMessage = bluetoothMessages.get(i);
                StorageUtility.addMessage(bluetoothMessage);

                if (!Repository.getInstance().getBluetoothMessageHashMap().containsKey(bluetoothMessage.getSenderAddress())) {
                    ArrayList<BluetoothMessage> messages = new ArrayList<>();

                    for (Object o : StorageUtility.readFromFile(StorageUtility.getMessagesFileName(bluetoothMessage.getSenderAddress()))) {
                        BluetoothMessage message = (BluetoothMessage) o;
                        messages.add(message);
                    }

                    Repository.getInstance().getBluetoothMessageHashMap().put(bluetoothMessage.getSenderAddress(), messages);
                }
                else{
                    Repository.getInstance().getBluetoothMessageHashMap().get(bluetoothMessage.getSenderAddress()).add(bluetoothMessage);
                }

                if (!Repository.getInstance().getNumberOfUnseenMessagesHashMap().containsKey(bluetoothMessage.getSenderAddress())){
                    int count = 0;

                    ArrayList<BluetoothMessage> messages = Repository.getInstance().getBluetoothMessageHashMap().get(bluetoothMessage.getSenderAddress());
                    Collections.reverse(messages);

                    for (BluetoothMessage message : messages) {
                        if (!message.getRead()){
                            count++;
                        }
                        else{
                            break;
                        }
                    }

                    Repository.getInstance().getNumberOfUnseenMessagesHashMap().put(bluetoothMessage.getSenderAddress(), count);
                }
                else{
                    if (!bluetoothMessage.getRead()) {
                        int messages = Repository.getInstance().getNumberOfUnseenMessagesHashMap().get(bluetoothMessage.getSenderAddress());
                        Repository.getInstance().getNumberOfUnseenMessagesHashMap().put(bluetoothMessage.getSenderAddress(), ++messages);
                    } else {
                        Repository.getInstance().getNumberOfUnseenMessagesHashMap().put(bluetoothMessage.getSenderAddress(), 0);
                    }
                }

                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    BluetoothFragment bluetoothFragment = (BluetoothFragment) fragment;
                    if (bluetoothFragment != null && (bluetoothFragment.isVisible())) {
                        bluetoothFragment.receiveMessage(bluetoothMessage);
                    }
                }
            }

            @Override
            public void onItemRangeMoved(ObservableList<BluetoothMessage> bluetoothMessages, int i, int i1, int i2) {
            }

            @Override
            public void onItemRangeRemoved(ObservableList<BluetoothMessage> bluetoothMessages, int i, int i1) {
            }
        });

        Repository.getInstance().getUnseenMessages().addOnListChangedCallback(Repository.getInstance().getMessageListener());

        Intent intent = new Intent(this, FileService.class);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.make_discoverable).setVisible(false);
        menu.findItem(R.id.send_file).setVisible(false);
        menu.findItem(R.id.send_image).setVisible(false);
        menu.findItem(R.id.send_game).setVisible(false);

        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    new AcceptTask().execute();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            finish();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        NotificationUtility.removeNotification(0);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(broadcastReceiver);
    }

    public BluetoothFragment getCurrentFragment() {
        BluetoothFragment bluetoothFragment = null;

        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment.isMenuVisible()) {
                bluetoothFragment = (BluetoothFragment) fragment;
                return bluetoothFragment;
            }
        }

        return bluetoothFragment;
    }

    public void changeFragment(BluetoothFragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
//                .setCustomAnimations(android.R.anim.fade_in,android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(findViewById(R.id.bluetooth_container).getId(), fragment, fragment.getClass().getSimpleName())
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();

    }
}

