package com.example.vl_ct03.bluetoothfyp.controller;

import android.bluetooth.BluetoothSocket;

import com.example.vl_ct03.bluetoothfyp.model.FileMessage;
import com.example.vl_ct03.bluetoothfyp.model.TextMessage;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by PC on 04/04/2017.
 */

public class UserSendTask {

    private String address;
    private ObjectOutputStream oos;
    private ReentrantLock lock;

    public UserSendTask(BluetoothSocket bluetoothSocket) {
        this.address = bluetoothSocket.getRemoteDevice().getAddress();
        oos = Repository.getInstance().getObjectOutputStreamHash().get(address);
        lock = Repository.getInstance().getReentrantLockHashMap().get(address);
    }

    public void sendMessage(final TextMessage message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (oos) {
                    try {
                        oos.reset();
                        oos.writeObject(UserReceiveTask.Message.MESSAGE);
                        oos.writeObject(message);
                        oos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void sendFile(final FileMessage message, final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final FileInputStream fis = new FileInputStream(file);
                    final byte[] bytes = new byte[1024];

                    // nid condition
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                lock.lock();
                                oos.reset();
                                oos.writeObject(UserReceiveTask.Message.FILE);
                                oos.writeObject(message);
                                oos.writeInt(fis.available());

                                int count;

                                try {
                                    while ((count = fis.read(bytes)) > 0) {
                                        oos.write(bytes, 0, count);
                                        oos.flush();
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
                                }

                                try {
                                    fis.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                            finally{
                                lock.unlock();
                            }
                        }
                    }).start();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void sendProgress(int progress) {
        try {
            oos.reset();
            oos.writeObject(UserReceiveTask.Message.PROGRESS);
            oos.writeInt(progress);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void sendReady() {
        try {
            oos.reset();
            oos.writeObject(UserReceiveTask.Message.READY);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void sendMove(int r, int c) {
        try {
            oos.reset();
            oos.writeObject(UserReceiveTask.Message.MOVE);
            oos.writeInt(r);
            oos.writeInt(c);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void sendLeaveGame(){
        try{
            oos.reset();
            oos.writeObject(UserReceiveTask.Message.LEAVE_GAME);
            oos.flush();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void sendStillInGame(){
        try{
            oos.reset();
            oos.writeObject(UserReceiveTask.Message.STILL_INGAME);
            oos.flush();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
