package com.example.vl_ct03.bluetoothfyp.controller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.model.BluetoothMessage;
import com.example.vl_ct03.bluetoothfyp.model.TextMessage;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.locks.ReentrantLock;

public class GameFragment extends BluetoothFragment {

    private BluetoothSocket mBluetoothSocket;
    private String mReceiverAddress;
    private ReentrantLock mReentrantLock = new ReentrantLock();
    private Toast moveToast;

    private enum value {EMPTY, X, O}

    private boolean mIsCross;
    private boolean mInGame;
    private int mSize = 3;
    private int mMoveCount = 0;
    private int mWinCount = 0;
    private int mLoseCount = 0;
    private int mDrawCount = 0;

    private FrameLayout[][] mButtons = new FrameLayout[mSize][mSize];
    private value[][] mButtonValues = new value[mSize][mSize];

    private value mPlayerType;

    private TextView mGameWonTextView;
    private TextView mGameLostTextView;
    private TextView mGameDrawTextView;

    public static GameFragment getInstance() {
        return new GameFragment();
    }

    public void initialize(BluetoothSocket bluetoothSocket) {
        this.mBluetoothSocket = bluetoothSocket;
        this.mReceiverAddress = bluetoothSocket.getRemoteDevice().getAddress();

        this.mIsCross = Repository.getInstance().getPlayerNumberHashMap().get(mReceiverAddress) == 1;
        this.mPlayerType = mIsCross ? value.X : value.O;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        Repository.getInstance().getMoveHashMap().remove(mReceiverAddress);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_game, container, false);
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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        BluetoothActivity.getInstance().getSupportActionBar().setHomeButtonEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setDisplayShowHomeEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setTitle("TIC-TAC-TOE");
        getActivity().getWindow().setBackgroundDrawableResource(android.R.color.white);

        FrameLayout button_1 = (FrameLayout) view.findViewById(R.id.b1);
        FrameLayout button_2 = (FrameLayout) view.findViewById(R.id.b2);
        FrameLayout button_3 = (FrameLayout) view.findViewById(R.id.b3);
        FrameLayout button_4 = (FrameLayout) view.findViewById(R.id.b4);
        FrameLayout button_5 = (FrameLayout) view.findViewById(R.id.b5);
        FrameLayout button_6 = (FrameLayout) view.findViewById(R.id.b6);
        FrameLayout button_7 = (FrameLayout) view.findViewById(R.id.b7);
        FrameLayout button_8 = (FrameLayout) view.findViewById(R.id.b8);
        FrameLayout button_9 = (FrameLayout) view.findViewById(R.id.b9);

        mGameWonTextView = (TextView) view.findViewById(R.id.game_won);
        mGameLostTextView = (TextView) view.findViewById(R.id.game_lost);
        mGameDrawTextView = (TextView) view.findViewById(R.id.game_draw);

        moveToast = Toast.makeText(getActivity(), "It's the opponent's turn to move", Toast.LENGTH_SHORT);

        mButtons[0][0] = button_1;
        mButtons[0][1] = button_2;
        mButtons[0][2] = button_3;

        mButtons[1][0] = button_4;
        mButtons[1][1] = button_5;
        mButtons[1][2] = button_6;

        mButtons[2][0] = button_7;
        mButtons[2][1] = button_8;
        mButtons[2][2] = button_9;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                setOnClick(mButtons[r][c]);
                mButtonValues[r][c] = value.EMPTY;
            }
        }

        if (!mIsCross) {
            mReentrantLock.lock();
        }

        resetGame(true);
    }

    private void setOnClick(final FrameLayout button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mReentrantLock.isLocked()) {
                    for (int r = 0; r < 3; r++) {
                        for (int c = 0; c < 3; c++) {
                            if (mButtons[r][c].equals(button)) {
                                if (mButtonValues[r][c].equals(value.EMPTY)) {
                                    if (mIsCross) {
                                        mButtonValues[r][c] = value.X;
                                        flipCard(r, c, value.X);
                                        checkCondition(r, c, value.X);
                                    } else {
                                        mButtonValues[r][c] = value.O;
                                        flipCard(r, c, value.O);
                                        checkCondition(r, c, value.O);
                                    }

                                    mReentrantLock.lock();
                                    new UserSendTask(mBluetoothSocket).sendMove(r, c);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
                else{
                    if (mInGame) {
                        moveToast.show();
                    }
                }
            }
        });
    }

    private void flipCard(int r, int c, value val) {

        final FrameLayout layout = mButtons[r][c];

        Fragment fragment;

        switch (val) {
            case X:
                fragment = FragmentX.newInstance();
                break;
            case O:
                fragment = FragmentO.newInstance();
                break;
            default:
                fragment = FragmentE.newInstance();
                break;
        }

        getChildFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(layout.getId(), fragment)
                .addToBackStack(null)
                .commit();

    }

    public void opponentMove(int r, int c) {
        if (mIsCross) {
            mButtonValues[r][c] = value.O;
            flipCard(r, c, value.O);
            checkCondition(r, c, value.O);
        } else {
            mButtonValues[r][c] = value.X;
            flipCard(r, c, value.X);
            checkCondition(r, c, value.X);
        }

        mReentrantLock.unlock();
    }

    private void checkCondition(int row, int column, value value) {
        mMoveCount++;
        Log.d("checking", "condition");
        // check row

        for (int i = 0; i < mSize; i++) {
            if (mButtonValues[i][column] != value) {
                break;
            }
            if (i == mSize - 1) {
                //win
                gameEnd(value);
            }
        }

        // check column

        for (int i = 0; i < mSize; i++) {
            if (mButtonValues[row][i] != value) {
                break;
            }
            if (i == mSize - 1) {
                //win
                gameEnd(value);
            }
        }

        // check diagonal

        if (row == column) {
            for (int i = 0; i < mSize; i++) {
                if (mButtonValues[i][i] != value) {
                    break;
                }
                if (i == mSize - 1) {
                    //win
                    gameEnd(value);
                }
            }
        }

        // check anti-diag

        if (row + column == mSize - 1) {
            for (int i = 0; i < mSize; i++) {
                if (mButtonValues[i][(mSize - 1) - i] != value) {
                    break;
                }
                if (i == mSize - 1) {
                    //win
                    gameEnd(value);
                }
            }
        }

        //check draw
        if (mMoveCount == ((Double) Math.pow(mSize, 2)).intValue()) {
            gameEnd(GameFragment.value.EMPTY);
        }
    }

    private void gameEnd(value value) {

        if (!value.equals(GameFragment.value.EMPTY)) {
            if (value.equals(mPlayerType)) {
                //player win
                mWinCount++;
                Toast.makeText(getActivity(), "You have WON!!!", Toast.LENGTH_SHORT).show();
                resetGame(false);
            } else {
                //player lose
                mLoseCount++;
                Toast.makeText(getActivity(), "You have LOST!!!", Toast.LENGTH_SHORT).show();
                resetGame(false);
            }
        } else {
            //draw
            mDrawCount++;
            Toast.makeText(getActivity(), "YOU DRAWED!!!", Toast.LENGTH_SHORT).show();
            resetGame(false);
        }
    }

    private void resetGame(boolean first) {
        updateScore();


        mInGame = false;



        if (!first) {
            new CountDownTimer(2000, 1000) {

                boolean lock = false;

                @Override
                public void onTick(long l) {
                    if (!mReentrantLock.isLocked()){
                        mReentrantLock.lock();
                        lock = true;
                    }
                }

                @Override
                public void onFinish() {
                    for (int r = 0; r < 3; r++) {
                        for (int c = 0; c < 3; c++) {
                            flipCard(r, c, value.EMPTY);
                            mButtonValues[r][c] = value.EMPTY;
                        }
                    }

                    if (lock){
                        mReentrantLock.unlock();
                    }

                    mInGame = true;
                }
            }.start();
        }
        else{
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    flipCard(r, c, value.EMPTY);
                    mButtonValues[r][c] = value.EMPTY;
                }
            }
            mInGame = true;
        }

        mMoveCount = 0;
    }

    private void updateScore() {
        mGameWonTextView.setText(String.valueOf(mWinCount));
        mGameLostTextView.setText(String.valueOf(mLoseCount));
        mGameDrawTextView.setText(String.valueOf(mDrawCount));
    }

    public void leaveGame(){
        getActivity().onBackPressed();
        Toast.makeText(getActivity(), mBluetoothSocket.getRemoteDevice().getName() + " has left the game", Toast.LENGTH_SHORT).show();
    }

    public void stillInGame(){
        new UserSendTask(mBluetoothSocket).sendStillInGame();
    }

    public String getAddress(){
        return mReceiverAddress;
    }

    @Override
    public void onResume() {
        super.onResume();

        setMenuVisibility(true);

        if (Repository.getInstance().getMoveHashMap().containsKey(mReceiverAddress)){
            int[] move = Repository.getInstance().getMoveHashMap().get(mReceiverAddress);
            int r = move[0];
            int c = move[1];

            if (mIsCross) {
                mButtonValues[r][c] = value.O;
                flipCard(r, c, value.O);
                checkCondition(r, c, value.O);
            } else {
                mButtonValues[r][c] = value.X;
                flipCard(r, c, value.X);
                checkCondition(r, c, value.X);
            }

            mReentrantLock.unlock();
            Repository.getInstance().getMoveHashMap().remove(mReceiverAddress);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        setMenuVisibility(false);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if (mBluetoothSocket != null && mBluetoothSocket.isConnected()) {
            new UserSendTask(mBluetoothSocket).sendLeaveGame();
        }
    }

    @Override
    public void receiveMessage(final BluetoothMessage bluetoothMessage) {
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
