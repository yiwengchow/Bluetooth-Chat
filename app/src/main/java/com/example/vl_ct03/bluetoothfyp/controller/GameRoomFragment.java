package com.example.vl_ct03.bluetoothfyp.controller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.model.BluetoothMessage;
import com.example.vl_ct03.bluetoothfyp.model.GameRoomAdapter;
import com.example.vl_ct03.bluetoothfyp.model.GameRoomPlayer;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.util.ArrayList;

public class GameRoomFragment extends BluetoothFragment {

    private ArrayList<GameRoomPlayer> mPlayers = new ArrayList<>();
    private GameRoomAdapter mGameRoomAdapter;
    private BluetoothSocket mBluetoothSocket;
    private String mReceiverAddress;

    public static GameRoomFragment getInstance() {
        return new GameRoomFragment();
    }

    public void initialize(BluetoothSocket bluetoothSocket) {
        this.mBluetoothSocket = bluetoothSocket;
        this.mReceiverAddress = bluetoothSocket.getRemoteDevice().getAddress();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game_room, container, false);

        BluetoothActivity.getInstance().getSupportActionBar().setHomeButtonEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setDisplayShowHomeEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setTitle("TIC-TAC-TOE");

        try {
            NotificationUtility.removeNotification(NotificationUtility.gameNotificationIds.get(mReceiverAddress));
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.game_room_recyclerview);
        final Button readyButton = (Button) view.findViewById(R.id.game_room_ready);
        Button exitButton = (Button) view.findViewById(R.id.game_room_exit);

        GameRoomPlayer user = new GameRoomPlayer(BluetoothAdapter.getDefaultAdapter().getName(),
                android.provider.Settings.Secure.getString(BluetoothActivity.getInstance().getContentResolver(), "bluetooth_address"));

        GameRoomPlayer opponent = new GameRoomPlayer(mBluetoothSocket.getRemoteDevice().getName(),
                mBluetoothSocket.getRemoteDevice().getAddress());

        try{
            user.setReady(Repository.getInstance().getUserGameReadyHashMap().get(mReceiverAddress));
            opponent.setReady(Repository.getInstance().getOpponentGameReadyHashMap().get(mReceiverAddress));

            readyButton.setText(Repository.getInstance().getUserGameReadyHashMap().get(mReceiverAddress) ? "UN_READY" : "READY");

            mPlayers.clear();

            mPlayers.add(user);
            mPlayers.add(opponent);

            mGameRoomAdapter = new GameRoomAdapter(mPlayers);

            readyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!Repository.getInstance().getReentrantLockHashMap().get(mReceiverAddress).isLocked()) {
                        try {
                            new UserSendTask(mBluetoothSocket).sendReady();
                            Repository.getInstance().setUserIsReady(mReceiverAddress, !Repository.getInstance().getUserGameReadyHashMap().get(mReceiverAddress));

                            readyButton.setText(Repository.getInstance().getUserGameReadyHashMap().get(mReceiverAddress) ? "UN-READY" : "READY");

                            if (!checkStartGame()) {
                                for (GameRoomPlayer player : mPlayers) {
                                    if (!player.getAddress().equals(mReceiverAddress)) {
                                        player.setReady(!player.isReady());
                                        mGameRoomAdapter.notifyItemChanged(mPlayers.indexOf(player));
                                        break;
                                    }
                                }
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        Toast.makeText(getActivity(), "Please wait until file is sent or received", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().onBackPressed();
                }
            });

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(mGameRoomAdapter);

            getActivity().getWindow().setBackgroundDrawableResource(R.mipmap.message_background);
        }
        catch (NullPointerException e){
            getActivity().onBackPressed();
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
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

    private boolean checkStartGame() {
        if (Repository.getInstance().getOpponentGameReadyHashMap().get(mReceiverAddress)
                && Repository.getInstance().getUserGameReadyHashMap().get(mReceiverAddress)){

            GameFragment gameFragment = GameFragment.getInstance();
            gameFragment.initialize(mBluetoothSocket);

            Repository.getInstance().setUserIsReady(mReceiverAddress, false);
            Repository.getInstance().setOpponentIsReady(mReceiverAddress, false);

            ((BluetoothActivity) getActivity()).changeFragment(gameFragment);

            System.out.println("true start");
            return true;
        }

        return false;
    }

    public void ready() {
        for (GameRoomPlayer player : mPlayers) {
            if (player.getAddress().equals(mReceiverAddress)) {
                player.setReady(Repository.getInstance().getOpponentGameReadyHashMap().get(mReceiverAddress));
                checkStartGame();

                mGameRoomAdapter.notifyItemChanged(mPlayers.indexOf(player));
            }
        }
    }

    public String getAddress(){
        return mReceiverAddress;
    }

    @Override
    public void onResume() {
        super.onResume();
        setMenuVisibility(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        setMenuVisibility(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBluetoothSocket != null && mBluetoothSocket.isConnected() && Repository.getInstance().getUserGameReadyHashMap().get(mReceiverAddress)){
            Repository.getInstance().setUserIsReady(mReceiverAddress, false);
            new UserSendTask(mBluetoothSocket).sendReady();
        }
    }

    @Override
    public void receiveMessage(BluetoothMessage bluetoothMessage) {

    }

    @Override
    public void deviceFound(BluetoothDevice bluetoothDevice) {

    }

    @Override
    public void deviceConnected(BluetoothDevice bluetoothDevice) {

    }

    @Override
    public void deviceDisconnected(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice.getAddress().equals(mReceiverAddress)) {
            getActivity().onBackPressed();
            Toast.makeText(getActivity(), bluetoothDevice.getName() + " has disconnected", Toast.LENGTH_SHORT).show();
        }
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
