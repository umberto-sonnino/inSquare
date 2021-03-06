package com.nsqre.insquare.Activities;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.transition.Explode;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.nsqre.insquare.Fragments.MainContent.MapFragment;
import com.nsqre.insquare.Message.Message;
import com.nsqre.insquare.Message.MessageAdapter;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Services.ChatService;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.Analytics.AnalyticsApplication;
import com.nsqre.insquare.Utilities.Photo.helpers.DocumentHelper;
import com.nsqre.insquare.Utilities.Photo.imgurmodel.ImageResponse;
import com.nsqre.insquare.Utilities.Photo.imgurmodel.Upload;
import com.nsqre.insquare.Utilities.Photo.services.UploadService;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * This activity lets the user chat in a Square, using a socket.io chat
 */
public class ChatActivity extends AppCompatActivity implements MessageAdapter.ChatMessageClickListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ChatActivity";
    
    private static final long TYPING_TIMER_LENGTH = 600;
    public static final int RECENT_MESSAGES_NUM = 50;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private EditText chatEditText;

    private Socket mSocket;

    /**
     * The InSquareProfile of the current user
     * @see InSquareProfile
     */
    private InSquareProfile mProfile;

    private Menu mMenu;

    private Square mSquare;
    private String mSquareId;
    private String mSquareName;
    private String mUsername;
    private String mUserId;

    private Toolbar toolbar;
    private TextView toolbarName;
    private TextView toolbarCircleInitials;
    private ImageView toolbarCircle;
    private int toolbarCircleColor;
    private String toolbarInitials;

    private boolean isScrolled;

    private Tracker mTracker;
    private Locale format;

    private HashMap<String, ArrayList<Message>> outgoingMessages;

    private Upload upload; // Upload object containging image and meta data
    private File chosenFile; //chosen file from intent

    private ChatActivity ca;
    public final static int FILE_PICK = 1001;

    private LinkedList<Integer> positions;

    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1;

    /**
     * Receives the notification from the ChatService that some message has been sent, so it updates the view
     * @see ChatService
     */
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Log.d(TAG, "onReceive: messaggio inviato con chatservice");
                Message m = (Message) intent.getParcelableExtra("messageSent");
                int position = positions.getFirst();
                positions.remove(positions.getFirst());
                Message messageFromAdapter = messageAdapter.getMessage(position);
                messageFromAdapter.setTime();
                messageFromAdapter.setId(m.getId());
                messageAdapter.notifyDataSetChanged();
                //Toast.makeText(getApplicationContext(), "notificato", Toast.LENGTH_SHORT).show();
            }
            catch (Exception e) {}
        }
    };
    private ImageButton sendButton;

    //TODO aggiungere slider "nuovi messaggi" se sto guardando messaggi vecchi, risolvere problema download messaggi
    /**
     * Initializes the socket.io components, downloads the messages present in the chat, tries to send messages in the outgoing list and eventually puts to zero the
     * notification counter for this chat
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        positions = new LinkedList<>();
        //FOTO
        //ButterKnife.bind(this);
        ca = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.logo_icon);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(getString(R.string.app_name),
                            icon, Color.parseColor("#D32F2F"));
            setTaskDescription(taskDesc);
        }
        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        format = getResources().getConfiguration().locale;

        isScrolled = false;

        messageAdapter = new MessageAdapter(getApplicationContext());
        messageAdapter.setOnClickListener(this);

        setContentView(R.layout.activity_chat);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Explode explode = new Explode();
            explode.setDuration(200);

            getWindow().setEnterTransition(explode);
            getWindow().setExitTransition(explode);
        }

        sendButton = (ImageButton) findViewById(R.id.chat_send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSend();
            }
        });

        //Foto
        ImageButton uploadImage = (ImageButton) findViewById(R.id.chat_foto_button);
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertPhotoWrapper();
                //chooseFileIntent();
            }
        });


        recyclerView = (RecyclerView) findViewById(R.id.message_list);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);
        //recyclerView.addItemDecoration(new DividerItemDecoration(this, null));

        chatEditText = (EditText) findViewById(R.id.message_text);
        chatEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                        Drawable originalDrawable = sendButton.getBackground();
                        Drawable wrappedDrawable = DrawableCompat.wrap(originalDrawable);
                        ColorStateList color;

                        if(s.length() > 0)
                        {
                            color = ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent);
                            sendButton.setClickable(true);
                        }else
                        {
                            sendButton.setClickable(false);
                            color = ContextCompat.getColorStateList(getApplicationContext(), R.color.colorPrimaryLight);
                        }

                        DrawableCompat.setTintList(wrappedDrawable, color);
                        sendButton.setBackground(wrappedDrawable);
                    }
                }
        );

        // Recuperiamo i dati passati dalla BottomNavActivity
        Intent intent = getIntent();

        mSquare = (Square) intent.getParcelableExtra(MapFragment.SQUARE_TAG);
        Log.d(TAG, mSquare.toString());

        toolbarInitials = intent.getStringExtra(BottomNavActivity.INITIALS_TAG);
        toolbarCircleColor = intent.getIntExtra(BottomNavActivity.INITIALS_COLOR_TAG, 0);

        mSquareId = mSquare.getId();
        mSquareName = mSquare.getName();

        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        if(sharedPreferences.contains(mSquareId)) {
            sharedPreferences.edit().remove(mSquareId).apply();
            sharedPreferences.edit().putInt("squareCount", sharedPreferences.getInt("squareCount",0) - 1).apply();
        }

        outgoingMessages = mProfile.getOutgoingMessages();

        if (outgoingMessages.keySet().contains(mSquareId)) {
            for (Message m : outgoingMessages.get(mSquareId)) {
                if (!messageAdapter.contains(m))
                    addMessage(m);
            }
        }
        setupToolbar();

        final View rootView = this.getWindow().getDecorView(); // this = activity
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (isScrolled) {
                    isScrolled = false;
                    return;
                }
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getHeight();
                int heightDifference = screenHeight - (r.bottom - r.top);

                if (heightDifference > screenHeight / 3) {
                    recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                    isScrolled = true;
                }
            }
        });
    }

    /**
     * Sets up the toolbar for the ChatActivity
     */
    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }else
        {
            Log.d(TAG, "setupToolbar: it was null!");
        }

        toolbarName = (TextView) findViewById(R.id.chat_toolbar_square_name);
        toolbarCircle = (ImageView) findViewById(R.id.chat_toolbar_square_circle);
        toolbarCircleInitials = (TextView) findViewById(R.id.chat_square_initials);

        toolbarName.setText(mSquareName);
        toolbarCircleInitials.setText(toolbarInitials);
        // Cambia colore
        ColorStateList color = ContextCompat.getColorStateList(getApplicationContext(), toolbarCircleColor);
        final Drawable originalDrawable = toolbarCircle.getBackground();
        final Drawable wrappedDrawable = DrawableCompat.wrap(originalDrawable);
        DrawableCompat.setTintList(wrappedDrawable, color);
        toolbarCircle.setBackground(wrappedDrawable);
        
        toolbarName.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        int xOffset = (int) toolbarName.getX();
                        Toast message = Toast.makeText(ChatActivity.this, mSquareName, Toast.LENGTH_SHORT);
                        message.setGravity(Gravity.TOP, 0, toolbar.getHeight());
                        message.show();
                        return true;
                    }
                }
        );
    }

    /**
     * Checks if the app has permissions. If not it requests them
     */
    private void insertPhotoWrapper() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionsNeeded.add("WRITE Storage");
        if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE))
            permissionsNeeded.add("READ Storage");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);
                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                                }
                            }
                        });
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            }
            return;
        }

        chooseFileIntent();
    }

    /**
     * Opens a dialog to let the user choose the image to send
     */
    private void chooseFileIntent(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        this.startActivityForResult(intent, FILE_PICK);
    }

    /**
     * Shows a certain message on the screen with an OK and a Cancel buttons.
     * @param message the message to show
     * @param okListener the listener for the OK button
     */
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * If the user has correctly chosen an image, calls uploadImage() to upload it
     * @see #uploadImage()
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case FILE_PICK:
                Uri returnUri;

                if (resultCode != RESULT_OK) {
                    return;
                }

                returnUri = data.getData();
                String filePath = DocumentHelper.getPath(this, returnUri);
                //Safety check to prevent null pointer exception
                if (filePath == null || filePath.isEmpty()) return;
                chosenFile = new File(filePath);
                uploadImage();
                break;
        }
    }


    private boolean addPermission(List<String> permissionsList, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
                // Check for Rationale Option
                if (!shouldShowRequestPermissionRationale(permission))
                    return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
            {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    chooseFileIntent();
                } else {
                    // Permission Denied
                    Toast.makeText(this, R.string.chat_permission_fail, Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    /**
     * Creates a Volley request to download the messages present in a particular square, then it adds the results to the
     * view
     * @param quantity
     */
    private void getRecentMessages(int quantity) {

        sendButton.setClickable(false);
        final String q = new Integer(quantity).toString();

        VolleyManager.getInstance().getRecentMessages("true", q, mSquareId,
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        if (object == null) {
                            Toast.makeText(ChatActivity.this, R.string.chat_recent_msg_fail, Toast.LENGTH_SHORT).show();
                        } else {

                            ArrayList<Message> messages = (ArrayList<Message>) object;
                            for (Message m : messages) {
                                addMessage(m);
                            }
                            sendButton.setClickable(true);
                        }
                    }

                    @Override
                    public void responsePOST(Object object) {
                        // Vuoto -- GET Request
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // Vuoto -- GET Request
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        // Vuoto -- GET Request
                    }
                });
    }


    /**
     * Initializes some values and emits an event so the server knows the user is connected to the chat
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        sharedPreferences.edit().putString("actualSquare", mSquareId).apply();

        try {
            String url = getString(R.string.socket);
            mSocket = IO.socket(url);

            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);

            mSocket.on("sendMessage", onSendMessage);
            mSocket.on("newMessage", onNewMessage);
            mSocket.on("ping", onPing);

            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // Get Messaggi recenti
        getRecentMessages(RECENT_MESSAGES_NUM);

        mUsername = InSquareProfile.getUsername();
        mUserId = InSquareProfile.getUserId();

        JSONObject data = new JSONObject();

        try {
            data.put("room", mSquareId);
            data.put("username", mUsername);
            data.put("userid", mUserId);
            data.put("message", mUsername + " joined");
        } catch(JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("addUser", data);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        registerReceiver(messageReceiver, new IntentFilter(ChatService.NOTIFICATION));
    }

    /**
     * Receiver for the event of deletion of the square that shows an appropriate alert
     * and prevents the user from sending other messages.
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("event");
            Log.d("messageReceiver", "Got message: " + message);
            if("deletion".equals(intent.getStringExtra("action"))) {
                if(mSquareId.equals(intent.getStringExtra("squareId"))) {
                    messageAdapter.clear();
                    messageAdapter.notifyDataSetChanged();
                    findViewById(R.id.removed_text).setVisibility(View.VISIBLE);
                    chatEditText.setFocusable(false);
                }
            }
        }
    };

    /**
     * Turns off the connection to socket
     */
    @Override
    protected void onPause() {
        super.onPause();
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("sendMessage", onSendMessage);
        mSocket.off("newMessage", onNewMessage);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        sharedPreferences.edit().remove("actualSquare").apply();

        unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Adds a message to the view
     * @param m The message you want to add
     */
    private void addMessage(Message m) {
        if(m.getId() != null && messageAdapter.contains(m)) {
            return;
        } else {
            messageAdapter.addItem(m);
        }
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
        isScrolled = true;
    }

    /**
     * Attempts to send a message through ChatService, if the message is valid
     * @see ChatService
     */
    private void attemptSend() {
        // [START message_event]
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Send Message")
                .build());
        // [END message_event]

        if(mUsername == null) 
        {
            return;
        }
        String message = chatEditText.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            chatEditText.requestFocus();
            return;
        }

        chatEditText.setText("");

        Message m = new Message(message, mUsername, mUserId, format);
        mProfile.addOutgoing(mSquareId, m, getApplicationContext());
        Intent intent = new Intent(this, ChatService.class);
        intent.putExtra("squareid", mSquareId);
        intent.putExtra("message", m);
        startService(intent);
        addMessage(m);
        positions.add(messageAdapter.getItemCount()-1);
    }

    /**
     * Attempts to send a message through ChatService, if the message is valid
     * @see ChatService
     */
    private void attemptSendFoto(String fotoURL) {
        // [START message_event]
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Send Foto")
                .build());
        // [END message_event]

        if(mUsername == null)
        {
            return;
        }
        String message = fotoURL;
        if (TextUtils.isEmpty(message)) {
            chatEditText.requestFocus();

            return;
        }

        chatEditText.setText("");

        Message m = new Message(message, mUsername, mUserId, format);
        mProfile.addOutgoing(mSquareId, m, getApplicationContext());
        Intent intent = new Intent(this, ChatService.class);
        intent.putExtra("squareid", mSquareId);
        intent.putExtra("message", m);
        startService(intent);
        addMessage(m);
        positions.add(messageAdapter.getItemCount()-1);
    }


    /**
     * Notifies if the connection to the socket has failed
     */
    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, getString(R.string.error_connect));
                    //Toast.makeText(getApplicationContext(), getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    /**
     * Receives the event from socket for a new message to display and it displays it
     * @see #addMessage(Message)
     */
    private Emitter.Listener onNewMessage = new Emitter.Listener()
    {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username = "";
                    String message = "";
                    String userId = "";
                    String messageId = "";
                    String date = "";
                    boolean userSpot = false;

                    try {
                        username = data.getString("username");
                        message = data.getString("contents");
                        userId = data.getString("userid");
                        messageId = data.getString("msg_id");
                        date = data.getString("date");
                        userSpot = data.getBoolean("userSpot");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    addMessage(new Message(messageId, message, username, userId, date, userSpot, format));

                }
            });
        }
    };

    /**
     * Receives the event from socket for a new message sent and it displays it
     * @see #addMessage(Message)
     */
    private Emitter.Listener onSendMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    String room = "";
                    String username = "";
                    String message = "";
                    String userId = "";
                    String messageId = "";
                    String date = "";
                    boolean userSpot = false;

                    try {
                        room = data.getString("room");
                        username = data.getString("username");
                        message = data.getString("contents");
                        userId = data.getString("userid");
                        messageId = data.getString("msg_id");
                        date = data.getString("date");
                        userSpot = data.getBoolean("userSpot");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    addMessage(new Message(messageId, message, username, userId, date, userSpot, format));
                }
            });
        }
    };

    /**
     * Receives an event from socket to keep the connection alive(to not let it timeout)
     */
    private Emitter.Listener onPing = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    mSocket.emit("pong", data);
                }
            });
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_chat_actions, menu);

        mMenu = menu;

        //SHARE

       //menu.findItem(R.id.share_action).setIcon(R.drawable.ic_share_white_48dp);

//        if (mProfile.favouriteSquaresList.contains(mSquare))
        if(InSquareProfile.isFav(mSquare.getId()))
            menu.findItem(R.id.favourite_square_action).setIcon(R.drawable.ic_favorite_white_24dp);
        else menu.findItem(R.id.favourite_square_action).setIcon(R.drawable.ic_favorite_border_white_24dp);

        return true;
    }

    /**
     * Manages the option menu items
     * @param item The item selected
     * @see #favouriteSquare(int, Square)
     * @see VolleyManager
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                break;
            case R.id.menu_entry_feedback:
                // [START feedback_event]
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Feedback")
                        .build());
                // [END feedback_event]
                final Dialog d = new Dialog(this);
                d.setContentView(R.layout.dialog_feedback);
                d.setTitle("Feedback");
                d.setCancelable(true);
                d.show();

                final EditText feedbackEditText = (EditText) d.findViewById(R.id.dialog_feedbacktext);

                final String feedback = feedbackEditText.getText().toString().trim();
                final String activity = this.getClass().getSimpleName();

                Button confirm = (Button) d.findViewById(R.id.dialog_feedback_confirm_button);
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        VolleyManager.getInstance().postFeedback(
                                feedback,
                                InSquareProfile.getUserId(),
                                activity,
                                new VolleyManager.VolleyResponseListener() {
                                    @Override
                                    public void responseGET(Object object) {
                                        // Vuoto - POST Request
                                    }

                                    @Override
                                    public void responsePOST(Object object) {
                                        if (object == null) {
                                            Toast.makeText(ChatActivity.this, R.string.chat_feedback_fail, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(ChatActivity.this, R.string.chat_feedback_success, Toast.LENGTH_SHORT).show();
                                            d.dismiss();
                                        }
                                    }

                                    @Override
                                    public void responsePATCH(Object object) {
                                        // Vuoto - POST Request
                                    }

                                    @Override
                                    public void responseDELETE(Object object) {
                                        // Vuoto - POST Request
                                    }
                                }
                        );
                    }
                });
                break;
            case R.id.favourite_square_action:
                if(InSquareProfile.isFav(mSquare.getId()))
                {
                    favouriteSquare(Request.Method.DELETE, mSquare);
                } else {
                    favouriteSquare(Request.Method.POST, mSquare);
                }
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Creates a Volley request to put/remove a square in/from the favourite squares list, then calls updateList
     * @param method The volley method you want to use(POST to add, DELETE to remove
     * @param square The square you want to add/remove
     */
    public void favouriteSquare(final int method, final Square square) {

        VolleyManager.getInstance().handleFavoriteSquare(method, square.getId(), InSquareProfile.getUserId(),
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responsePOST(Object object) {
                        if (object == null) {
                            //La richiesta e' fallita
                            Log.d(TAG, "responsePOST - non sono riuscito ad inserire il fav " + square.toString());
                        } else {
                            InSquareProfile.addFav(square);
                            mMenu.findItem(R.id.favourite_square_action).setIcon(R.drawable.ic_favorite_white_24dp);
                        }
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        InSquareProfile.removeFav(square.getId());
                        mMenu.findItem(R.id.favourite_square_action).setIcon(R.drawable.ic_favorite_border_white_24dp);
                    }
                });
    }

    @Override
    public void onItemClick(int position, View v) {
        // TODO implementare onclick behavior per i messaggi nella chat
        Log.d(TAG, "onItemClick: I've just clicked item " + position);
    }


    /**
     * Uploads the image on Imgur
     * @see #createUpload(File)
     * @see UploadService
     */
    public void uploadImage() {
    /*
      Create the @Upload object
     */
        if (chosenFile == null) return;
        createUpload(chosenFile);

    /*
      Start upload
     */
        new UploadService(this).Execute(upload, new UiCallback());

    }

    /**
     * Creates an Upload object out of a File one
     * @param image The image the user wants to upload
     */
    private void createUpload(File image) {
        Log.d("createUpload", "createUpload");
        upload = new Upload();

        upload.image = image;
    }

    private void clearInput() {
        Log.d("clearInput", "clearInput");
        //uploadImage.setImageResource(R.drawable.ic_add_a_photo_black_48dp);
    }

    /**
     * Manages the callbacks for the upload of a photo
     */
    private class UiCallback implements Callback<ImageResponse> {

        @Override
        public void success(ImageResponse imageResponse, Response response) {
            attemptSendFoto(imageResponse.data.link);
            Log.d("success", imageResponse.data.link);
            clearInput();
        }

        @Override
        public void failure(RetrofitError error) {
            Log.d("failure", "failure");
            //Assume we have no connection, since error is null
            if (error == null) {
                Log.d("ERROR UiCallback", "ERROR UiCallback");
            }
        }
    }

    //SHARE
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        //showMessage(getString(R.string.google_play_services_error));
    }
}
