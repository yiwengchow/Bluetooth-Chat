package com.example.vl_ct03.bluetoothfyp.model;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.controller.BluetoothActivity;
import com.example.vl_ct03.bluetoothfyp.controller.StorageUtility;

import java.util.List;

/**
 * Created by PC on 02/04/2017.
 */

public class BtDeviceAdapter extends RecyclerView.Adapter<BtDeviceAdapter.MyViewHolder> {

    private List<BluetoothDevice> bluetoothDevices;
    private BluetoothActivity bluetoothActivity = BluetoothActivity.getInstance();

    public class MyViewHolder extends RecyclerView.ViewHolder{
        private TextView title;
        private RelativeLayout layout;

        private MyViewHolder(View view){
            super(view);
            title = (TextView) view.findViewById(R.id.Bt_device_title);
            layout = (RelativeLayout) view.findViewById(R.id.Bt_device_layout);
        }
    }

    public BtDeviceAdapter(List<BluetoothDevice> bluetoothDevices){
        this.bluetoothDevices = bluetoothDevices;
    }

    @Override
    public MyViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_row,parent,false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        BluetoothDevice bluetoothDevice = bluetoothDevices.get(position);
        holder.title.setText(bluetoothDevice.getName());
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageUtility.addMessageBlock(new MessageBlock(bluetoothDevices.get(position).getName(),
                        bluetoothDevices.get(position).getAddress()));

                bluetoothActivity.onBackPressed();
            }
        });
    }

    @Override
    public int getItemCount() {
        return bluetoothDevices.size();
    }


}



