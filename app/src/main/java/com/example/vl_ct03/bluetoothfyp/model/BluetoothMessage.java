package com.example.vl_ct03.bluetoothfyp.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by PC on 16/04/2017.
 */

public class BluetoothMessage implements Serializable{
    String receiverName;
    String senderName;
    String receiverAddress;
    String senderAddress;

    Date date;
    boolean read = false;

    public String getReceiverName(){
        return receiverName;
    }

    public String getSenderName(){
        return senderName;
    }

    public String getReceiverAddress(){
        return receiverAddress;
    }

    public String getSenderAddress(){
        return senderAddress;
    }

    public Date getDate(){
        return date;
    }

    public String getStringDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
        String dateString = sdf.format(date);
        String time = dateString.substring(dateString.length()-5,dateString.length());
        String date = dateString.substring(0,dateString.length()-6);

        return time + "\n" + date;
    }

    public void setRead(boolean bool){
        read = bool;
    }

    public boolean getRead(){
        return read;
    }
}
