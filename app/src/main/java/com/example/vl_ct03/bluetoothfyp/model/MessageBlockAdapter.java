package com.example.vl_ct03.bluetoothfyp.model;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.controller.BluetoothActivity;
import com.example.vl_ct03.bluetoothfyp.controller.ConversationFragment;
import com.example.vl_ct03.bluetoothfyp.controller.StorageUtility;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.util.ArrayList;

/**
 * Created by PC on 03/05/2017.
 */

public class MessageBlockAdapter extends RecyclerView.Adapter<MessageBlockAdapter.MyViewHolder> {

    private ArrayList<MessageBlock> messageBlocks;

    public class MyViewHolder extends RecyclerView.ViewHolder{
        private RelativeLayout layout;
        private TextView name;
        private TextView date;
        private TextView message;
        private TextView messageUnread;

        private MyViewHolder(View view){
            super(view);
            layout = (RelativeLayout) view.findViewById(R.id.messageLayout);
            name = (TextView) view.findViewById(R.id.messageName);
            date = (TextView) view.findViewById(R.id.messageDate);
            message = (TextView) view.findViewById(R.id.messageText);
            messageUnread = (TextView) view.findViewById(R.id.messageUnread);
        }
    }

    public MessageBlockAdapter(ArrayList<MessageBlock> messageBlocks){
        this.messageBlocks = messageBlocks;
    }

    @Override
    public MyViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_message_block,parent,false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MessageBlockAdapter.MyViewHolder holder, final int position) {
        final MessageBlock messageBlock = messageBlocks.get(position);
        holder.name.setText(messageBlock.getName());
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConversationFragment conversationFragment = ConversationFragment.getInstance();
                conversationFragment.initialize(messageBlocks.get(position).getName(),
                        messageBlocks.get(position).getAddress());

                BluetoothActivity.getInstance().changeFragment(conversationFragment);
            }
        });

        holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothActivity.getInstance());
                builder.setTitle("Remove message")
                        .setMessage("Are you sure?")
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Repository.getInstance().getNumberOfUnseenMessagesHashMap().remove(messageBlocks.get(position).getAddress());
                                Repository.getInstance().getBluetoothMessageHashMap().remove(messageBlocks.get(position).getAddress());

                                messageBlocks.remove(messageBlocks.get(position));

                                StorageUtility.saveToFile(new ArrayList<Object>(messageBlocks), StorageUtility.getMessageBlocksFileName());
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, messageBlocks.size());
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                return true;
            }
        });

        ArrayList<Object> objects = StorageUtility.readANumberFromFile(StorageUtility.getMessagesFileName(messageBlock.getAddress()), 1);

        if (objects.size() > 0) {
            BluetoothMessage bluetoothMessage = (BluetoothMessage) objects.get(objects.size() - 1);

            if (messageBlock.getUnread() > 0) {
                holder.messageUnread.setText(String.valueOf(messageBlock.getUnread()));
            }
            else{
                holder.messageUnread.setText("");
            }

            if (bluetoothMessage != null) {
                if (bluetoothMessage.getClass().isAssignableFrom(TextMessage.class)) {
                    TextMessage textMessage = (TextMessage) bluetoothMessage;
                    holder.message.setText(textMessage.getMessage());
                    holder.date.setText(textMessage.getStringDate());

                } else {
                    FileMessage fileMessage = (FileMessage) bluetoothMessage;
                    holder.message.setText("[File" +
                            "] " + fileMessage.getMessage());
                    holder.date.setText(fileMessage.getStringDate());
                }
            }
        }
        else{
            holder.message.setText("");
            holder.date.setText("");
            holder.messageUnread.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return messageBlocks.size();
    }
}
