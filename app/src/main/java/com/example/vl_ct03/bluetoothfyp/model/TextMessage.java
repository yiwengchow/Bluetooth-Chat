package com.example.vl_ct03.bluetoothfyp.model;

import java.util.Date;

/**
 * Created by yiwen on 07/04/2017.
 */

public class TextMessage extends BluetoothMessage{

    private String message;

    public TextMessage(String receiverName, String senderName, String receiverAddress, String senderAddress, String message, Date date){
        this.receiverName = receiverName;
        this.senderName = senderName;
        this.receiverAddress = receiverAddress;
        this.senderAddress = senderAddress;
        this.message = message;
        this.date = date;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
