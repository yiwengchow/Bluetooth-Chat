package com.example.vl_ct03.bluetoothfyp.model;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.controller.BluetoothActivity;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.util.ArrayList;

/**
 * Created by PC on 27/05/2017.
 */

public class GameRoomAdapter extends RecyclerView.Adapter<GameRoomAdapter.MyViewHolder> {

    private ArrayList<GameRoomPlayer> players = new ArrayList<>();

    public class MyViewHolder extends RecyclerView.ViewHolder{

        private TextView playerText;

        public MyViewHolder(View itemView) {
            super(itemView);
            playerText = (TextView) itemView.findViewById(R.id.game_room_list_text);
        }
    }

    public GameRoomAdapter(ArrayList<GameRoomPlayer> players){
        this.players = players;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.game_room_list_row,parent,false);

        return new GameRoomAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.playerText.setText(players.get(position).getName());

        if (players.get(position).isReady()){
            holder.playerText.setBackgroundResource(R.drawable.ready);
            holder.playerText.setTextColor(BluetoothActivity.getInstance().getResources().getColor(android.R.color.black));
        }
        else{
            holder.playerText.setBackgroundResource(R.drawable.message_right);
            holder.playerText.setTextColor(BluetoothActivity.getInstance().getResources().getColor(android.R.color.white));
        }
    }

    @Override
    public int getItemCount() {
        return players.size();
    }
}
