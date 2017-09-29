package com.example.vl_ct03.bluetoothfyp.model;

import java.util.Date;

/**
 * Created by PC on 16/04/2017.
 */

public class FileMessage extends BluetoothMessage{

    private String fileName;
    private String message;
    private String type;
    private String path;
    private int progress;
    private boolean failed;

    public FileMessage(String fileName, String receiverName, String senderName, String receiverAddress, String senderAddress, String message, Date date, String path, String type){
        this.fileName = fileName;
        this.receiverName = receiverName;
        this.senderName = senderName;
        this.receiverAddress = receiverAddress;
        this.senderAddress = senderAddress;
        this.date = date;
        this.message = message;
        this.path = path;
        this.type = type;

        this.failed = false;
        setProgress(0);
    }

    public String getMessage(){
        return message;
    }

    public String getType() {
        return type;
    }

    public void setPath(String path){
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setProgress(int progress){
        this.progress = progress;
    }

    public int getProgress(){
        return progress;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
