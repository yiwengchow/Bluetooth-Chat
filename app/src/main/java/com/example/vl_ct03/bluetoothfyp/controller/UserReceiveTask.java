package com.example.vl_ct03.bluetoothfyp.controller;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.util.Log;

import com.example.vl_ct03.bluetoothfyp.model.FileMessage;
import com.example.vl_ct03.bluetoothfyp.model.TextMessage;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Date;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by PC on 04/04/2017.
 */

public class UserReceiveTask {

    private BluetoothSocket bluetoothSocket;
    private String address;
    private ObjectInputStream ois;

    public enum Message {MESSAGE, FILE, PROGRESS, READY, MOVE, LEAVE_GAME, STILL_INGAME}

    public UserReceiveTask(BluetoothSocket bluetoothSocket) {
        try {
            this.bluetoothSocket = bluetoothSocket;
            this.address = bluetoothSocket.getRemoteDevice().getAddress();

            ois = new ObjectInputStream(bluetoothSocket.getInputStream());
            listenToSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void listenToSocket() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        try {
                            Message message = (Message) ois.readObject();

                            switch (message) {
                                case MESSAGE:
                                    receivedMessage();
                                    break;
                                case FILE:
                                    receivedImage();
                                    break;
                                case PROGRESS:
                                    receiveFileProgress();
                                    break;
                                case READY:
                                    receiveReady();
                                    break;
                                case MOVE:
                                    receiveMove();
                                    break;
                                case LEAVE_GAME:
                                    receiveLeaveRoom();
                                    break;
                                case STILL_INGAME:
                                    receiveStillInGame();
                                    break;
                            }
                        } catch (ClassCastException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    try {
                        bluetoothSocket.close();

                        Repository.getInstance().getBluetoothSocketHashMap().remove(address);
                        Repository.getInstance().getObjectOutputStreamHash().remove(address);
                        Repository.getInstance().getReentrantLockHashMap().remove(address);
                        Repository.getInstance().getOpponentGameReadyHashMap().remove(address);
                        Repository.getInstance().getUserGameReadyHashMap().remove(address);
                        Repository.getInstance().getMoveHashMap().remove(address);
                        Repository.getInstance().getPlayerNumberHashMap().remove(address);

                        try {
                            ConversationFragment conversationFragment = (ConversationFragment) BluetoothActivity.getInstance().getCurrentFragment();
                            conversationFragment.checkConnection();
                        } catch (NullPointerException e1) {
                            e.printStackTrace();
                        } catch (ClassCastException e1) {
                            e.printStackTrace();
                        }

                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void receivedMessage() {
        try {
            TextMessage textMessage = (TextMessage) ois.readObject();

            if (BluetoothActivity.getInstance().getCurrentFragment() != null &&
                    BluetoothActivity.getInstance().getCurrentFragment().getClass().isAssignableFrom(ConversationFragment.class)) {

                ConversationFragment conversationFragment = (ConversationFragment) BluetoothActivity.getInstance().getCurrentFragment();

                if (conversationFragment.getAddress().equals(address)) {
                    textMessage.setRead(true);
                }
                else{
                    textMessage.setRead(false);
                }
            } else {
                textMessage.setRead(false);
            }

            Repository.getInstance().getUnseenMessages().add(textMessage);

            if (BluetoothActivity.getInstance().getCurrentFragment() == null) {
                NotificationUtility.displayUnreadNotifications();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void receivedImage() {
        ReentrantLock reentrantLock = Repository.getInstance().getReentrantLockHashMap().get(address);

        if (reentrantLock != null && reentrantLock.tryLock()) {
            try {
                FileOutputStream fos;
                Double fileSize;

                byte[] bytes;
                byte[] chunk = new byte[1024];
                Double readByte = 0.00;
                Double oldPercentage = 0.00;

                final FileMessage fileMessage = (FileMessage) ois.readObject();
                fileMessage.setRead(false);
                fileSize = (double) ois.readInt();

                bytes = new byte[fileSize.intValue()];

                File sourceFile = new File(Environment.getExternalStorageDirectory(), "BluetoothChat/media");

                if (!sourceFile.exists()) {
                    sourceFile.mkdirs();
                }

                String checkFileName = null;
                int count = 1;

                while(true) {
                    if (checkFileName == null){
                        checkFileName = fileMessage.getFileName();
                    }

                    File fileCheck = new File(sourceFile.getPath(), checkFileName);

                    if (fileCheck.exists()) {
                        checkFileName = fileMessage.getFileName();
                        String[] splitName = checkFileName.split("\\.");
                        splitName[0] += String.format(" (%d)", count++);
                        checkFileName = splitName[0] + "." + splitName[1];
                    }
                    else{
                        fileCheck.createNewFile();
                        fileMessage.setFileName(checkFileName);
                        break;
                    }
                }

                final File file = new File(sourceFile.getPath(), checkFileName);
                fileMessage.setPath(file.getPath());

                if (BluetoothActivity.getInstance().getCurrentFragment() != null &&
                        BluetoothActivity.getInstance().getCurrentFragment().getClass().isAssignableFrom(ConversationFragment.class)) {

                    ConversationFragment conversationFragment = (ConversationFragment) BluetoothActivity.getInstance().getCurrentFragment();

                    if (conversationFragment.getAddress().equals(address)) {
                        fileMessage.setRead(true);
                    }
                    else{
                        fileMessage.setRead(false);
                    }
                } else {
                    fileMessage.setRead(false);
                }

                Repository.getInstance().getUnseenMessages().add(fileMessage);
                Repository.getInstance().getFileMessageHashMap().put(address, fileMessage);

                if (BluetoothActivity.getInstance().getCurrentFragment() == null) {
                    NotificationUtility.displayUnreadNotifications();
                }

                while (readByte < fileSize) {
                    int read = ois.read(chunk);
                    for (int i = 0; i < read; i++) {
                        bytes[readByte.intValue() + i] = chunk[i];
                    }
                    readByte += read;

                    Double percentage = (readByte / fileSize) * 100;

                    if (percentage.intValue() != oldPercentage.intValue()) {
                        oldPercentage = percentage;
                        new UserSendTask(bluetoothSocket).sendProgress(percentage.intValue());

                        try {
                            Repository.getInstance().getMessagingAdapterHashMap().get(address)
                                    .setProgress(percentage.intValue());
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }


                fos = new FileOutputStream(file);
                fos.write(bytes, 0, bytes.length);

                fos.flush();
                fos.close();

                fileMessage.setProgress(100);
                StorageUtility.updateProgress(fileMessage);
                Repository.getInstance().getFileMessageHashMap().remove(address);

                if (Repository.getInstance().getMessagingAdapterHashMap().containsKey(address)) {
                    Repository.getInstance().getMessagingAdapterHashMap().get(address)
                            .itemChanged();
                }

            } catch (IOException e) {
                e.printStackTrace();

                FileMessage fileMessage = Repository.getInstance().getFileMessageHashMap().get(address);

                fileMessage.setFailed(true);
                StorageUtility.updateProgress(fileMessage);
                Repository.getInstance().getFileMessageHashMap().remove(address);

                if (Repository.getInstance().getMessagingAdapterHashMap().containsKey(address)) {
                    Repository.getInstance().getMessagingAdapterHashMap().get(address)
                            .itemChanged();
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    private void receiveFileProgress() {
        try {
            int progress = ois.readInt();

            Log.d("name is", bluetoothSocket.getRemoteDevice().getName());

            if (Repository.getInstance().getMessagingAdapterHashMap().containsKey(address)) {
                Repository.getInstance().getMessagingAdapterHashMap().get(address)
                        .setProgress(progress);
            }

            Log.d("progress", String.valueOf(progress));

            FileMessage fileMessage = Repository.getInstance().getFileMessageHashMap().get(address);

            if (fileMessage != null && progress == 100) {
                fileMessage.setProgress(100);
                StorageUtility.updateProgress(fileMessage);
                Repository.getInstance().getFileMessageHashMap().remove(address);

                if (Repository.getInstance().getMessagingAdapterHashMap().containsKey(address)) {
                    Repository.getInstance().getMessagingAdapterHashMap().get(address)
                            .itemChanged();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void receiveReady() {
        BluetoothFragment bluetoothFragment = BluetoothActivity.getInstance().getCurrentFragment();

        Repository.getInstance().setOpponentIsReady(address, !Repository.getInstance().getOpponentGameReadyHashMap().get(address));

        if (bluetoothFragment != null && bluetoothFragment.getClass().isAssignableFrom(GameRoomFragment.class)) {
            final GameRoomFragment gameRoomFragment = (GameRoomFragment) bluetoothFragment;

            if (gameRoomFragment.getAddress().equals(address)) {
                gameRoomFragment.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gameRoomFragment.ready();
                    }
                });
            }
            else{
                if (Repository.getInstance().getOpponentGameReadyHashMap().get(address)){
                    NotificationUtility.displayGameNotification(address);
                }
                else{
                    NotificationUtility.removeNotification(NotificationUtility.gameNotificationIds.get(address));
                }
            }
        }
        else{
            if (Repository.getInstance().getOpponentGameReadyHashMap().get(address)){
                NotificationUtility.displayGameNotification(address);
            }
            else{
                NotificationUtility.removeNotification(NotificationUtility.gameNotificationIds.get(address));
            }
        }
    }

    private void receiveMove() {
        System.out.println("movereceived");
        try {
            final int r = ois.readInt();
            final int c = ois.readInt();

            BluetoothFragment bluetoothFragment = BluetoothActivity.getInstance().getCurrentFragment();

            if (bluetoothFragment != null && bluetoothFragment.getClass().isAssignableFrom(GameFragment.class)) {
                final GameFragment gameFragment = (GameFragment) bluetoothFragment;

                System.out.println("gamefragment");

                if (gameFragment.getAddress().equals(address)) {
                    gameFragment.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("gamemove");
                            gameFragment.opponentMove(r, c);
                        }
                    });
                }

                return;
            }

            Repository.getInstance().getMoveHashMap().put(address, new int[]{r,c});

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveLeaveRoom(){
        BluetoothFragment bluetoothFragment = BluetoothActivity.getInstance().getCurrentFragment();

        if (bluetoothFragment != null && bluetoothFragment.getClass().isAssignableFrom(GameFragment.class)) {
            final GameFragment gameFragment = (GameFragment) bluetoothFragment;

            if (gameFragment.getAddress().equals(address)) {
                gameFragment.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gameFragment.leaveGame();
                    }
                });
            }
        }
    }

    private void receiveStillInGame(){
        BluetoothFragment bluetoothFragment = BluetoothActivity.getInstance().getCurrentFragment();

        if (bluetoothFragment != null && bluetoothFragment.getClass().isAssignableFrom(GameFragment.class)) {
            final GameFragment gameFragment = (GameFragment) bluetoothFragment;

            if (gameFragment.getAddress().equals(address)) {
                gameFragment.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gameFragment.stillInGame();
                    }
                });
            }
        }
    }
}
