package com.example.vl_ct03.bluetoothfyp.model;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.controller.BluetoothActivity;
import com.example.vl_ct03.bluetoothfyp.controller.StorageUtility;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

public class MessagingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<BluetoothMessage> bluetoothMessages;
    private HashMap<BluetoothMessage, RecyclerView.ViewHolder> viewHolderHashMap = new HashMap<>();
    private Callback callback;
    private FileMessage currentFileMessage = null;

    public MessagingAdapter(ArrayList<BluetoothMessage> bluetoothMessages, Callback callback){
        this.bluetoothMessages = bluetoothMessages;
        this.callback = callback;
    }

    private class leftFileViewHolder extends RecyclerView.ViewHolder{
        RelativeLayout imageLayoutContainer;
        RelativeLayout imageLayout;
        TextView imageText;
        TextView imageDate;
        ImageView imageView;
        ProgressBar imageProgress;

        private leftFileViewHolder(View view){
            super(view);

            imageLayoutContainer = (RelativeLayout) view.findViewById(R.id.left_message_file_container);
            imageLayout = (RelativeLayout) view.findViewById(R.id.left_message_file_layout);
            imageProgress = (ProgressBar) view.findViewById(R.id.left_message_file_progressBar);
            imageView = (ImageView) view.findViewById(R.id.left_message_file_view);
            imageText = (TextView) view.findViewById(R.id.left_message_file_text);
            imageDate = (TextView) view.findViewById(R.id.left_message_file_date);

            imageProgress.setProgress(0);
        }
    }

    private class rightFileViewHolder extends RecyclerView.ViewHolder{
        RelativeLayout imageLayoutContainer;
        RelativeLayout imageLayout;
        TextView imageText;
        TextView imageDate;
        ImageView imageView;
        ProgressBar imageProgress;

        private rightFileViewHolder(View view){
            super(view);

            imageLayoutContainer = (RelativeLayout) view.findViewById(R.id.right_message_file_container);
            imageLayout = (RelativeLayout) view.findViewById(R.id.right_message_file_layout);
            imageProgress = (ProgressBar) view.findViewById(R.id.right_message_file_progressBar);
            imageView = (ImageView) view.findViewById(R.id.right_message_file_view);
            imageText = (TextView) view.findViewById(R.id.right_message_file_text);
            imageDate = (TextView) view.findViewById(R.id.right_message_file_date);

            imageProgress.setProgress(0);
        }
    }

    private class leftTextViewHolder extends RecyclerView.ViewHolder{
        private RelativeLayout textLayoutContainer;
        private TextView textText;
        private TextView textDate;

        private leftTextViewHolder(View view){
            super(view);

            textLayoutContainer = (RelativeLayout) view.findViewById(R.id.message_text_container);
            textText = (TextView) view.findViewById(R.id.message_text_text);
            textDate = (TextView) view.findViewById(R.id.message_text_date);
        }
    }

    private class rightTextViewHolder extends RecyclerView.ViewHolder{
        private RelativeLayout textLayoutContainer;
        private TextView textText;
        private TextView textDate;

        private rightTextViewHolder(View view){
            super(view);

            textLayoutContainer = (RelativeLayout) view.findViewById(R.id.message_text_container);
            textText = (TextView) view.findViewById(R.id.message_text_text);
            textDate = (TextView) view.findViewById(R.id.message_text_date);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (bluetoothMessages.get(position).getClass().isAssignableFrom(TextMessage.class)) {
            if (bluetoothMessages.get(position).getSenderAddress().equals(
                    android.provider.Settings.Secure.getString(BluetoothActivity.getInstance().getContentResolver(), "bluetooth_address"))) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if (bluetoothMessages.get(position).getSenderAddress().equals(
                    android.provider.Settings.Secure.getString(BluetoothActivity.getInstance().getContentResolver(), "bluetooth_address"))) {
                return 2;
            } else {
                return 3;
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType){
            case 0:
                return new rightTextViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_text_message_right,parent,false));
            case 1:
                return new leftTextViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_text_message_left,parent,false));
            case 2:
                return new rightFileViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_file_message_right,parent,false));
            case 3:
                return new leftFileViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_file_message_left,parent,false));
        }

        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
        );

        try {
            final RelativeLayout progressBar = (RelativeLayout) ((Activity) holder.itemView.getContext()).findViewById(R.id.messaging_status_layout);
            int[] barSize = new int[2];
            progressBar.getLocationOnScreen(barSize);

            if (position == 0) {
                params.setMargins(0, barSize[1], 0, 0);
            } else {
                params.setMargins(0, 10, 0, 0);
            }

            if (holder.getClass().isAssignableFrom(rightTextViewHolder.class)) {
                rightTextViewHolder rightTextViewHolder = (rightTextViewHolder) holder;
                TextMessage textMessage = (TextMessage) bluetoothMessages.get(position);

                rightTextViewHolder.textText.setText(textMessage.getMessage());
                rightTextViewHolder.textDate.setText(textMessage.getStringDate());

                rightTextViewHolder.textLayoutContainer.setLayoutParams(params);

            } else if (holder.getClass().isAssignableFrom(leftTextViewHolder.class)) {
                leftTextViewHolder leftTextViewHolder = (leftTextViewHolder) holder;
                TextMessage textMessage = (TextMessage) bluetoothMessages.get(position);

                leftTextViewHolder.textText.setText(textMessage.getMessage());
                leftTextViewHolder.textDate.setText(textMessage.getStringDate());

                leftTextViewHolder.textLayoutContainer.setLayoutParams(params);

            } else if (holder.getClass().isAssignableFrom(rightFileViewHolder.class)) {
                final rightFileViewHolder rightFileViewHolder = (rightFileViewHolder) holder;
                final FileMessage fileMessage = (FileMessage) bluetoothMessages.get(position);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                BluetoothActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                if (fileMessage.getProgress() == 100) {
                    Picasso.with(BluetoothActivity.getInstance()).load(new File(fileMessage.getPath()))
                            .placeholder(R.mipmap.folder)
                            .resize(displayMetrics.widthPixels / 2, displayMetrics.heightPixels / 2)
                            .centerInside().into(rightFileViewHolder.imageView, callback);

                    rightFileViewHolder.imageLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String type = URLConnection.guessContentTypeFromName(fileMessage.getType());

                            if (type != null) {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.parse("file://" + fileMessage.getPath()), type);
                                BluetoothActivity.getInstance().startActivity(intent);
                            }
                        }
                    });
                } else if (fileMessage.getProgress() < 100 && !fileMessage.isFailed()) {
                    Picasso.with(BluetoothActivity.getInstance()).load(R.mipmap.receiving)
                            .resize(displayMetrics.widthPixels / 2, displayMetrics.heightPixels / 2)
                            .centerInside().into(rightFileViewHolder.imageView, callback);
                } else if (fileMessage.isFailed()) {
                    Picasso.with(BluetoothActivity.getInstance()).load(R.mipmap.error)
                            .resize(displayMetrics.widthPixels / 4, displayMetrics.heightPixels / 4)
                            .centerInside().into(rightFileViewHolder.imageView, callback);
                }

                rightFileViewHolder.imageText.setText(fileMessage.getFileName() + "\n\n" + fileMessage.getMessage());
                rightFileViewHolder.imageDate.setText(fileMessage.getStringDate());

                if (fileMessage.getProgress() == 100 || fileMessage.isFailed()) {
                    rightFileViewHolder.imageProgress.setVisibility(View.INVISIBLE);
                } else {
                    rightFileViewHolder.imageProgress.setVisibility(View.VISIBLE);
                }

                viewHolderHashMap.put(bluetoothMessages.get(position), rightFileViewHolder);

                rightFileViewHolder.imageLayoutContainer.setLayoutParams(params);

            } else if (holder.getClass().isAssignableFrom(leftFileViewHolder.class)) {
                final leftFileViewHolder leftFileViewHolder = (leftFileViewHolder) holder;
                final FileMessage fileMessage = (FileMessage) bluetoothMessages.get(position);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                BluetoothActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                if (fileMessage.getProgress() == 100) {
                    Picasso.with(BluetoothActivity.getInstance()).load(new File(fileMessage.getPath()))
                            .placeholder(R.mipmap.folder)
                            .resize(displayMetrics.widthPixels / 2, displayMetrics.heightPixels / 2)
                            .centerInside().into(leftFileViewHolder.imageView, callback);

                    leftFileViewHolder.imageLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String type = URLConnection.guessContentTypeFromName(fileMessage.getType());

                            if (type != null) {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.parse("file://" + fileMessage.getPath()), type);
                                BluetoothActivity.getInstance().startActivity(intent);
                            }
                        }
                    });
                } else if (fileMessage.getProgress() < 100 && !fileMessage.isFailed()) {
                    Picasso.with(BluetoothActivity.getInstance()).load(R.mipmap.receiving)
                            .resize(displayMetrics.widthPixels / 2, displayMetrics.heightPixels / 2)
                            .centerInside().into(leftFileViewHolder.imageView, callback);
                } else if (fileMessage.isFailed()) {
                    Picasso.with(BluetoothActivity.getInstance()).load(R.mipmap.error)
                            .resize(displayMetrics.widthPixels / 4, displayMetrics.heightPixels / 4)
                            .centerInside().into(leftFileViewHolder.imageView, callback);
                }

                leftFileViewHolder.imageText.setText(fileMessage.getFileName() + "\n\n" + fileMessage.getMessage());
                leftFileViewHolder.imageDate.setText(fileMessage.getStringDate());

                if (fileMessage.getProgress() == 100 || fileMessage.isFailed()) {
                    leftFileViewHolder.imageProgress.setVisibility(View.INVISIBLE);
                } else {
                    leftFileViewHolder.imageProgress.setVisibility(View.VISIBLE);
                }

                viewHolderHashMap.put(bluetoothMessages.get(position), leftFileViewHolder);

                leftFileViewHolder.imageLayoutContainer.setLayoutParams(params);
            }
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return bluetoothMessages.size();
    }

    public void setProgress(int progress) {
        currentFileMessage = (FileMessage) bluetoothMessages.get(bluetoothMessages.size()-1);

            try {
                RecyclerView.ViewHolder viewHolder = viewHolderHashMap.get(currentFileMessage);

                if (viewHolder.getClass().isAssignableFrom(leftFileViewHolder.class)) {
                    leftFileViewHolder leftFileViewHolder = (leftFileViewHolder) viewHolderHashMap.get(currentFileMessage);
                    leftFileViewHolder.imageProgress.setProgress(progress);
                } else {
                    rightFileViewHolder rightFileViewHolder = (rightFileViewHolder) viewHolderHashMap.get(currentFileMessage);
                    rightFileViewHolder.imageProgress.setProgress(progress);
                }

                Log.d("setted the","progress heer");

                currentFileMessage.setRead(true);
                currentFileMessage.setProgress(progress);

                if (progress == 100){
                    currentFileMessage = null;
                }
            }
            catch(NullPointerException e){
                e.printStackTrace();
            }
    }

    public void itemChanged(){
        BluetoothActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    notifyItemChanged(bluetoothMessages.size()-1);
                }
                catch (NullPointerException e){
                    e.printStackTrace();
                }
            }
        });
    }
}
