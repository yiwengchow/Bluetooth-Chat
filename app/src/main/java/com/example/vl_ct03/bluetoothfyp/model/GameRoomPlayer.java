package com.example.vl_ct03.bluetoothfyp.model;

/**
 * Created by PC on 27/05/2017.
 */

public class GameRoomPlayer {
    private String name;
    private String address;
    private boolean ready;

    public GameRoomPlayer(String name, String address){
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress(){
        return address;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }
}
