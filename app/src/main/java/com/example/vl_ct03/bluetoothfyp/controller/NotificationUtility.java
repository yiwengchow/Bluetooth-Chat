package com.example.vl_ct03.bluetoothfyp.controller;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.model.BluetoothMessage;
import com.example.vl_ct03.bluetoothfyp.model.FileMessage;
import com.example.vl_ct03.bluetoothfyp.model.MessageBlock;
import com.example.vl_ct03.bluetoothfyp.model.TextMessage;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by PC on 26/05/2017.
 */

public class NotificationUtility extends StorageUtility{

    private static int count = 0;
    public static HashMap<String, Integer> gameNotificationIds = new HashMap<>();

    public static void displayUnreadNotifications() {
        int messageCount = 0;
        int nameCount = 0;

        HashMap<MessageBlock, Integer> messageAmounts = new HashMap<>();
        HashMap<String, String> lastMessage = new HashMap<>();

        ArrayList<MessageBlock> messageBlocks= new ArrayList<>();

        for (Object o : readFromFile(getMessageBlocksFileName())) {
            messageBlocks.add((MessageBlock) o);
        }

        for (MessageBlock messageBlock : messageBlocks) {
            ArrayList<Object> messages = readFromFile(getMessagesFileName(messageBlock.getAddress()));

            boolean counted = false;

            if (messages != null) {
                int count = 0;

                for (int i = messages.size() - 1; i >= 0; i--) {
                    if (!((BluetoothMessage) messages.get(i)).getRead()) {
                        count++;
                        messageCount++;
                        counted = true;
                    } else {
                        break;
                    }
                }

                if (counted) {
                    nameCount++;
                    messageAmounts.put(messageBlock, count);
                    lastMessage.put(messageBlock.getAddress(), (messages.get(messages.size() - 1).getClass().isAssignableFrom(TextMessage.class)
                            ? ((TextMessage) messages.get(messages.size() - 1)).getMessage() : "[File] " + ((FileMessage) messages.get(messages.size() - 1)).getMessage()));
                }
            }
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(BluetoothActivity.getInstance())
                        .setSmallIcon(R.mipmap.icon)
                        .setContentTitle(String.format("%d messages from %d people", messageCount, nameCount))
                        .setContentText("New messages received!")
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_ALL);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        for (MessageBlock messageBlock : messageAmounts.keySet()) {
            inboxStyle.addLine(messageBlock.getName() + " "
                    + (messageAmounts.get(messageBlock) > 1 ? String.format("(%s messages)", messageAmounts.get(messageBlock)) : "")
                    + String.format(":\t")
                    + lastMessage.get(messageBlock.getAddress()));
        }

        mBuilder.setStyle(inboxStyle);

        Intent resultIntent = BluetoothActivity.getInstance().getPackageManager().getLaunchIntentForPackage(BluetoothActivity.getInstance().getPackageName());

        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        BluetoothActivity.getInstance(),
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) BluetoothActivity.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, mBuilder.build());
    }

    public static void displayGameNotification(String address){
        String name = Repository.getInstance().getBluetoothSocketHashMap().get(address).getRemoteDevice().getName();

        gameNotificationIds.put(address, ++count);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(BluetoothActivity.getInstance())
                        .setSmallIcon(R.mipmap.icon)
                        .setContentTitle(String.format("You have a game request!"))
                        .setContentText(name + " would like to play with you")
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_ALL);

        Intent resultIntent = BluetoothActivity.getInstance().getPackageManager().getLaunchIntentForPackage(BluetoothActivity.getInstance().getPackageName());
        resultIntent.putExtra("notification_address",address);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        BluetoothActivity.getInstance(),
                        count,
                        resultIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) BluetoothActivity.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(count, mBuilder.build());
    }

    public static void removeNotification(int id) {
        NotificationManager mNotificationManager =
                (NotificationManager) BluetoothActivity.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(id);
    }
}
