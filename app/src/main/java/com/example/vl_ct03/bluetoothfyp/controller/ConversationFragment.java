package com.example.vl_ct03.bluetoothfyp.controller;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vl_ct03.bluetoothfyp.R;
import com.example.vl_ct03.bluetoothfyp.model.BluetoothMessage;
import com.example.vl_ct03.bluetoothfyp.model.FileMessage;
import com.example.vl_ct03.bluetoothfyp.model.MessagingAdapter;
import com.example.vl_ct03.bluetoothfyp.model.TextMessage;
import com.example.vl_ct03.bluetoothfyp.repository.Repository;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.sql.Date;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static android.app.Activity.RESULT_OK;

public class ConversationFragment extends BluetoothFragment implements Serializable {

    private final int SEND_FILE = 101;

    private String mReceiverName = "";
    private String mReceiverAddress = "";

    private int mFirstVisibleItemPos = 0;
    private boolean mAutoScroll = false;
    private boolean mTypedMessage = false;
    private boolean mIsPaired = false;
    private boolean mIsSearching = false;
    private boolean mPairConnect = false;

    private BluetoothSocket mBluetoothSocket;
    private RecyclerView mRecyclerView;
    private MessagingAdapter mMessagingAdapter = null;
    private Thread mConnectingThread;
    private ReentrantLock mReentrantLock;
    private ArrayList<BluetoothMessage> mBluetoothMessages;

    private TextView mStatusText;
    private RelativeLayout mStatusLayout;

    public static ConversationFragment getInstance() {
        return new ConversationFragment();
    }

    public void initialize(String name, String address) {
        this.mReceiverName = name;
        this.mReceiverAddress = address;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conversation, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        FloatingActionButton messagingButton = (FloatingActionButton) view.findViewById(R.id.messagingButton);
        EditText messageBox = (EditText) view.findViewById(R.id.messagingMessageBox);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.messaging_recyclerView);
        mStatusLayout = (RelativeLayout) view.findViewById(R.id.messaging_status_layout);
        mStatusText = (TextView) view.findViewById(R.id.messaging_status);

        BluetoothActivity.getInstance().getSupportActionBar().setHomeButtonEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setDisplayShowHomeEnabled(true);
        BluetoothActivity.getInstance().getSupportActionBar().setTitle(mReceiverName);

        getActivity().getWindow().setBackgroundDrawableResource(R.mipmap.message_background);

        mBluetoothMessages = Repository.getInstance().getBluetoothMessageHashMap().get(mReceiverAddress);
        readMessages();

        mStatusLayout.bringToFront();
        mStatusLayout.setVisibility(View.INVISIBLE);
        mStatusText.setText(R.string.connecting_status);

        mMessagingAdapter = new MessagingAdapter(mBluetoothMessages, new Callback() {
            @Override
            public void onSuccess() {
                if (mAutoScroll) {
                    mRecyclerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mRecyclerView.smoothScrollToPosition(mBluetoothMessages.size() - 1);
                        }
                    }, 100);
                }
            }

            @Override
            public void onError() {

            }
        });

        Repository.getInstance().getMessagingAdapterHashMap().put(mReceiverAddress, mMessagingAdapter);
        checkConnection();

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mMessagingAdapter);

        mStatusLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    if (!mIsPaired) {
                        if (mIsSearching) {
                            mStatusText.setText(R.string.not_paired_status);
                            Toast.makeText(getActivity(), "Canceled discovery", Toast.LENGTH_SHORT).show();
                            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                            mIsSearching = false;
                        } else {
                            mStatusText.setText(R.string.searching_status);
                            BluetoothAdapter.getDefaultAdapter().startDiscovery();
                            mIsSearching = true;
                        }
                    }
                }
            }
        });

        messageBox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mTypedMessage = true;
                return false;
            }
        });

        messagingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkConnection()) {
                    TextView messageText = (TextView) view.findViewById(R.id.messagingMessageBox);
                    String message = messageText.getText().toString();

                    if (!mReentrantLock.isLocked()) {
                        if (!message.equals("")) {
                            messageText.setText("");

                            final TextMessage textMessage = new TextMessage(mReceiverName, BluetoothAdapter.getDefaultAdapter().getName(), mReceiverAddress,
                                    android.provider.Settings.Secure.getString(BluetoothActivity.getInstance().getContentResolver(), "bluetooth_address"),
                                    message, new Date(System.currentTimeMillis()));

                            new UserSendTask(mBluetoothSocket).sendMessage(textMessage);

                            mBluetoothMessages.add(textMessage);
                            mMessagingAdapter.notifyItemInserted(mBluetoothMessages.size() - 1);
                            mRecyclerView.scrollToPosition(mBluetoothMessages.size() - 1);

                            textMessage.setRead(true);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    StorageUtility.addMessage(textMessage);
                                }
                            }).start();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Please wait until file is sent", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), mReceiverName + " is not connected, please wait and try again...", Toast.LENGTH_SHORT).show();
                    checkConnection();
                }
            }
        });

        mRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (mTypedMessage) {
                    mTypedMessage = false;
                    if (mAutoScroll) {
                        mRecyclerView.smoothScrollToPosition(mBluetoothMessages.size() - 1);
                    }
                }
            }
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                if (layoutManager.findLastCompletelyVisibleItemPosition() == mBluetoothMessages.size() - 1) {
                    mAutoScroll = true;

                    mFirstVisibleItemPos = layoutManager.findFirstVisibleItemPosition();
                } else {
                    if (mFirstVisibleItemPos != layoutManager.findFirstVisibleItemPosition()) {
                        mAutoScroll = false;
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        setMenuVisibility(true);

        checkConnection();

//        readMessages();
        mRecyclerView.scrollToPosition(mBluetoothMessages.size()-1);
    }

    @Override
    public void onPause() {
        super.onPause();

        setMenuVisibility(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Repository.getInstance().getMessagingAdapterHashMap().remove(mReceiverAddress);

        System.out.println("destroyedhere");
        if (mConnectingThread != null && mConnectingThread.isAlive()) {
            mConnectingThread.interrupt();
        }

        if (BluetoothAdapter.getDefaultAdapter().isDiscovering()){
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.findItem(R.id.send_image).setVisible(true);
        menu.findItem(R.id.send_file).setVisible(true);
        menu.findItem(R.id.send_game).setVisible(true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }

        if (!checkConnection()) {
            Toast.makeText(getActivity(), mReceiverName + " is not connected, please wait and try again...", Toast.LENGTH_SHORT).show();
        } else {
            if (mReentrantLock != null && !mReentrantLock.isLocked()) {
                switch (item.getItemId()) {
                    case R.id.send_image:

                        Intent imageIntent = new Intent();
                        imageIntent.setAction(Intent.ACTION_GET_CONTENT);
                        imageIntent.setType("image/*");
                        startActivityForResult(imageIntent, SEND_FILE);

                        return true;

                    case R.id.send_file:
                        Intent fileIntent = new Intent();
                        fileIntent.setAction(Intent.ACTION_GET_CONTENT);
                        fileIntent.setType("*/*");
                        startActivityForResult(fileIntent, SEND_FILE);

                        return true;

                    case R.id.send_game:
                        GameRoomFragment gameRoomFragment = GameRoomFragment.getInstance();
                        gameRoomFragment.initialize(mBluetoothSocket);
                        ((BluetoothActivity) getActivity()).changeFragment(gameRoomFragment);

                        return true;
                }
            } else {
                Toast.makeText(getActivity(), "Please wait until file is sent or received", Toast.LENGTH_SHORT).show();
            }
        }

        return false;
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            final Uri returnUri = intent.getData();

            ContentResolver cR = getActivity().getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();

            final String type = mime.getExtensionFromMimeType(cR.getType(returnUri));
            String getFileName = null;

            if (returnUri.getScheme().equals("content")) {
                Cursor cursor = getActivity().getContentResolver().query(returnUri, null, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        getFileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            }
            if (getFileName == null) {
                getFileName = returnUri.getPath();
                int cut = getFileName.lastIndexOf('/');
                if (cut != -1) {
                    getFileName = getFileName.substring(cut + 1);
                }
            }

            final String filename = getFileName;

            try {
                File sourceFile = new File(Environment.getExternalStorageDirectory(), "BluetoothChat/images");

                if (!sourceFile.exists()) {
                    sourceFile.mkdirs();
                }

                String checkFileName = null;
                int count = 1;

                while(true) {
                    if (checkFileName == null){
                        checkFileName = filename;
                    }

                    File file = new File(sourceFile.getPath(), checkFileName);

                    if (file.exists()) {
                        checkFileName = filename;
                        String[] splitName = checkFileName.split("\\.");
                        splitName[0] += String.format(" (%d)", count++);
                        checkFileName = splitName[0] + "." + splitName[1];
                    }
                    else{
                        file.createNewFile();
                        break;
                    }
                }

                final File file = new File(sourceFile.getPath(), checkFileName);

                ParcelFileDescriptor parcelFileDescriptor = getActivity().getContentResolver().openFileDescriptor(returnUri, "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

                FileInputStream fileInputStream = new FileInputStream(fileDescriptor);

                final FileChannel inputChannel = fileInputStream.getChannel();
                final FileChannel outputChannel = new FileOutputStream(file).getChannel();

                inputChannel.transferTo(0, inputChannel.size(), outputChannel);
                inputChannel.close();
                outputChannel.close();

                switch (requestCode) {
                    case SEND_FILE:
                        final AlertDialog.Builder imageDialog = new AlertDialog.Builder(getActivity());
                        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_file_send, null);
                        imageDialog.setView(view);

                        final Dialog dialog = imageDialog.create();
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                        final ImageView imageView = (ImageView) view.findViewById(R.id.image_dialog_image);
                        final TextView imageName = (TextView) view.findViewById(R.id.image_dialog_file_name);
                        final EditText dialogText = (EditText) view.findViewById(R.id.image_dialog_text);

                        imageDialog.setCancelable(false);

                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        BluetoothActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        if (options.outWidth != -1 && options.outHeight != -1) {
                            // This is an image file.
                            Picasso.with(BluetoothActivity.getInstance()).load(new File(file.getPath()))
                                    .placeholder(R.mipmap.folder)
                                    .resize(displayMetrics.widthPixels, displayMetrics.heightPixels / 2)
                                    .centerInside().into(imageView);
                        } else {
                            // This is not an image file.
                            Picasso.with(BluetoothActivity.getInstance()).load(R.mipmap.folder)
                                    .resize(displayMetrics.widthPixels / 2, displayMetrics.heightPixels / 2)
                                    .centerInside().into(imageView);
                        }

                        imageName.setText(file.getName());

                        imageDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                file.delete();
                                dialog.dismiss();
                            }
                        });

                        imageDialog.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (checkConnection()) {
                                    mAutoScroll = true;

                                    dialog.dismiss();

                                    final FileMessage fileMessage = new FileMessage(filename, mReceiverName, BluetoothAdapter.getDefaultAdapter().getName(), mReceiverAddress,
                                            android.provider.Settings.Secure.getString(BluetoothActivity.getInstance().getContentResolver(), "bluetooth_address"),
                                            dialogText.getText().toString(), new Date(System.currentTimeMillis()), file.getPath(), type);

                                    fileMessage.setRead(true);
                                    mBluetoothMessages.add(fileMessage);
                                    mMessagingAdapter.notifyItemInserted(mBluetoothMessages.size() - 1);
                                    mTypedMessage = true;

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            StorageUtility.addMessage(fileMessage);
                                        }
                                    }).start();

                                    Repository.getInstance().getFileMessageHashMap().put(mReceiverAddress, fileMessage);
                                    new UserSendTask(mBluetoothSocket).sendFile(fileMessage, file);
                                } else {
                                    Toast.makeText(getActivity(), mReceiverName + " is not connected, please wait and try again...", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        imageDialog.show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkConnection() {
        try {
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                if (mStatusLayout.getVisibility() == View.INVISIBLE) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int[] locationArray = new int[2];
                            mStatusText.setText(R.string.turn_on_bluetooth_status);
                            mStatusLayout.setBackgroundResource(android.R.color.holo_red_dark);
                            mStatusLayout.getLocationOnScreen(locationArray);
                            mStatusLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down));
                            mStatusLayout.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    mStatusText.setText(R.string.turn_on_bluetooth_status);
                    mStatusLayout.setBackgroundResource(android.R.color.holo_red_dark);
                }

                return false;
            }

            mIsPaired = true;

            // check if socket existed
            for (BluetoothSocket socket : Repository.getInstance().getBluetoothSocketHashMap().values()) {
                if (socket.getRemoteDevice().getAddress().equals(mReceiverAddress)) {
                    mBluetoothSocket = socket;
                    mReentrantLock = Repository.getInstance().getReentrantLockHashMap().get(mReceiverAddress);

                    if (mConnectingThread != null && mConnectingThread.isAlive()) {
                        mConnectingThread.interrupt();
                    }

                    if (mStatusLayout.getVisibility() == View.VISIBLE) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    int[] locationArray = new int[2];
                                    mStatusText.setText(R.string.connected_status);
                                    mStatusLayout.setBackgroundResource(android.R.color.holo_green_light);
                                    mStatusLayout.getLocationOnScreen(locationArray);
                                    mStatusLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up));
                                    mStatusLayout.setVisibility(View.INVISIBLE);
                                }
                            });
                        }

                    }

                    return true;
                }
            }

            mBluetoothSocket = null;

            // show the drop down notice
            if (mStatusLayout.getVisibility() == View.INVISIBLE) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int[] locationArray = new int[2];
                        mStatusText.setText(R.string.connecting_status);
                        mStatusLayout.setBackgroundResource(android.R.color.holo_red_dark);
                        mStatusLayout.getLocationOnScreen(locationArray);
                        mStatusLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down));
                        mStatusLayout.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mStatusText.setText(R.string.connecting_status);
                        mStatusLayout.setBackgroundResource(android.R.color.holo_red_dark);
                    }
                });
            }

            // check if is bonded
            for (BluetoothDevice bluetoothDevice : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
                if (bluetoothDevice.getAddress().equals(mReceiverAddress)) {
                    // found device is paired
                    if (mConnectingThread == null || !mConnectingThread.isAlive()) {
                        connect(bluetoothDevice);
                    }
                    return false;
                }
            }

            mIsPaired = false;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mStatusText.setText(R.string.not_paired_status);
                    mStatusLayout.setBackgroundResource(android.R.color.holo_red_dark);
                }
            });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void connect(final BluetoothDevice bluetoothDevice) {
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE && mPairConnect) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BluetoothSocket socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.nameUUIDFromBytes("Yiwengbluetoothfyp".getBytes()));
                        socket.connect();

                        Repository.getInstance().getBluetoothSocketHashMap().put(mReceiverAddress, socket);
                        Repository.getInstance().getObjectOutputStreamHash().put(mReceiverAddress, new ObjectOutputStream(socket.getOutputStream()));
                        Repository.getInstance().getReentrantLockHashMap().put(mReceiverAddress, new ReentrantLock());
                        Repository.getInstance().getOpponentGameReadyHashMap().put(mReceiverAddress, false);
                        Repository.getInstance().getUserGameReadyHashMap().put(mReceiverAddress, false);
                        Repository.getInstance().getPlayerNumberHashMap().put(mReceiverAddress, 1);

                        new UserReceiveTask(socket);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                checkConnection();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();

                        mPairConnect = false;
                    }
                }
            }).start();
        } else {
            if (mConnectingThread == null || !mConnectingThread.isAlive()) {
                mConnectingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (!mConnectingThread.isInterrupted()) {
                                if ((BluetoothAdapter.getDefaultAdapter().getBondedDevices().contains(bluetoothDevice) || mIsSearching)  && BluetoothAdapter.getDefaultAdapter().getBondedDevices().contains(bluetoothDevice)) {
                                    try {
                                        final BluetoothSocket socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.nameUUIDFromBytes("Yiwengbluetoothfyp".getBytes()));

                                        try {
                                            socket.connect();

                                            Repository.getInstance().getBluetoothSocketHashMap().put(mReceiverAddress, socket);
                                            Repository.getInstance().getObjectOutputStreamHash().put(mReceiverAddress, new ObjectOutputStream(socket.getOutputStream()));
                                            Repository.getInstance().getReentrantLockHashMap().put(mReceiverAddress, new ReentrantLock());
                                            Repository.getInstance().getOpponentGameReadyHashMap().put(mReceiverAddress, false);
                                            Repository.getInstance().getUserGameReadyHashMap().put(mReceiverAddress, false);
                                            Repository.getInstance().getPlayerNumberHashMap().put(mReceiverAddress, 1);

                                            new UserReceiveTask(socket);

                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    checkConnection();
                                                }
                                            });

                                            break;
                                        }
                                        catch (IOException e){
                                            socket.close();

                                            if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE){
                                                break;
                                            }
                                        }
                                    } catch (IOException e) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                checkConnection();
                                            }
                                        });
                                    }
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            checkConnection();
                                        }
                                    });
                                }
                                else {
                                    break;
                                }
                            }
                            checkConnection();
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                });
                mConnectingThread.start();
                Log.d("started thread", "already");
            }
        }
    }

    private void readMessages() {
        Repository.getInstance().getNumberOfUnseenMessagesHashMap().put(mReceiverAddress, 0);

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean rewrite = false;

                for (int i = mBluetoothMessages.size() - 1; i >= 0; i--) {
                    if (!mBluetoothMessages.get(i).getRead()) {
                        rewrite = true;
                        mBluetoothMessages.get(i).setRead(true);
                    }
                    else{
                        break;
                    }
                }

                if (rewrite) {
                    StorageUtility.saveToFile(new ArrayList<Object>(mBluetoothMessages), StorageUtility.getMessagesFileName(mReceiverAddress));
                }
            }
        }).start();
    }

    public String getAddress() {
        return mReceiverAddress;
    }

    @Override
    void receiveMessage(final BluetoothMessage bluetoothMessage) {
        if (bluetoothMessage.getSenderAddress().equals(mReceiverAddress)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessagingAdapter.notifyItemInserted(mBluetoothMessages.size());

                    if (mAutoScroll) {
                        mRecyclerView.scrollToPosition(mBluetoothMessages.size() - 1);
                    }
                }
            });
        }
    }

    @Override
    void deviceFound(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice.getAddress() != null && bluetoothDevice.getAddress().equals(mReceiverAddress)) {
            if (mIsSearching) {
                mIsSearching = false;
                mPairConnect = true;
                connect(bluetoothDevice);
            }
        }
    }

    @Override
    void deviceConnected(BluetoothDevice bluetoothDevice) {
    }

    @Override
    void deviceDisconnected(BluetoothDevice bluetoothDevice) {
    }

    @Override
    void bluetoothOn() {
        checkConnection();
    }

    @Override
    void bluetoothOff() {
        checkConnection();
    }

    @Override
    public void discoveryFinished() {
        if (mIsSearching) {
            if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            }
            BluetoothAdapter.getDefaultAdapter().startDiscovery();
        }
    }
}