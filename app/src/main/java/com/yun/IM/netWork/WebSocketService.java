package com.yun.IM.netWork;

import static com.yun.IM.utilites.Constants.AUDIO_DIR;
import static com.yun.IM.utilites.Constants.AUDIO_FILE;
import static com.yun.IM.utilites.Constants.AUDIO_MESSAGE;
import static com.yun.IM.utilites.Constants.AUDIO_TIME;
import static com.yun.IM.utilites.Constants.CALLBACK_FRIEND;
import static com.yun.IM.utilites.Constants.IMAGE_MESSAGE;
import static com.yun.IM.utilites.Constants.REQUEST_FRIEND;
import static com.yun.IM.utilites.Constants.Text_MESSAGE;
import static com.yun.IM.utilites.Constants.WS;
import static com.yun.IM.utilites.Constants.chatMessages;
import static com.yun.IM.utilites.Constants.dbAdapter;
import static com.yun.IM.utilites.Constants.executorService;
import static com.yun.IM.utilites.Constants.friends;
import static com.yun.IM.utilites.Constants.userId;
import static com.yun.IM.utilites.Constants.users;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.gson.Gson;
import com.yun.IM.Fragments.contactsFragment;
import com.yun.IM.Fragments.messageFragment;
import com.yun.IM.R;
import com.yun.IM.activities.ChatActivity;
import com.yun.IM.activities.FriendRequestActivity;
import com.yun.IM.activities.MainActivity;
import com.yun.IM.activities.SignInActivity;
import com.yun.IM.adapters.ChatAdapter;
import com.yun.IM.adapters.UsersAdapter;
import com.yun.IM.models.Friends;
import com.yun.IM.models.Message;
import com.yun.IM.models.User;
import com.yun.IM.utilites.Constants;
import com.yun.IM.utilites.DBAdapter;
import com.yun.IM.utilites.Encrypt;
import com.yun.IM.utilites.PreferenceManager;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketService extends Service {
    private static final String TAG = "websocket";
    Gson gson = new Gson();
    private PreferenceManager preferenceManager;
    private HashMap<Integer, Message> messageHashMap = new HashMap<>();
    private HashMap<Integer, String> KeyHashMap = new HashMap<>();
    private WebSocket webSocket;
    private WebSocketCallback webSocketCallback;
    private int reconnectTimeout = 5000;
    private boolean connected = false;
    private Handler handler = new Handler();
    private volatile boolean isRunning = true;
    private volatile boolean Heart = false;
    private boolean IsSend = true;
    private PowerManager.WakeLock wakeLock;
    private Thread thread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WebSocketService.class.getName());
        wakeLock.acquire();
        AUDIO_DIR = this.getDir("Audio", Context.MODE_PRIVATE);
        preferenceManager = new PreferenceManager(getApplicationContext());
        dbAdapter = new DBAdapter(getApplicationContext());
        dbAdapter.open(preferenceManager.getString(Constants.KEY_USER_ID));
        executorService = new ThreadPoolExecutor(5, 20, 2, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
        connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        close();
        dbAdapter.close();
        executorService.shutdown();
        startService(new Intent(this, WebSocketService.class));
        super.onDestroy();
    }


    private void connect() {
        IsSend = true;
        OkHttpClient client = new OkHttpClient.Builder().writeTimeout(20000, TimeUnit.MILLISECONDS).readTimeout(20000, TimeUnit.MILLISECONDS).retryOnConnectionFailure(true).build();
        Request request = new Request.Builder().url(WS + preferenceManager.getString(Constants.KEY_USER_ID)).addHeader("token", preferenceManager.getString(Constants.KEY_TOKEN)).build();
        webSocket = client.newWebSocket(request, new WebSocketHandler());
    }

    public boolean send(Message message, String AES) {
        if (connected) {
            message.message = Encrypt.encrypt(message, AES);
            webSocket.send(message.json());
            message.message = Encrypt.decrypt(message.message, AES);
            executorService.execute(() -> {
                switch (message.message_type) {
                    case AUDIO_MESSAGE:
                        message.message = AUDIO_FILE.getPath() + "time:" + AUDIO_TIME;
                        break;
                    case Text_MESSAGE:
                        break;
                }
                dbAdapter.insert(message, Constants.KEY_COLLECTION_CHAT);
            });
            return true;
        } else {
            int size = ChatAdapter.getItemPosition();
            messageHashMap.put(size, message);
            KeyHashMap.put(size, AES);
            retrySend();
            return false;
        }
    }


    public void sendPong(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }


    public void close() {
        if (webSocket != null) {
            boolean shutDownFlag = webSocket.close(1000, "manual close");
            webSocket = null;
        }
    }

    private void reconnect() {
        handler.postDelayed(() -> {
            if (!connected) {
                connect();
            }
        }, reconnectTimeout);
    }

    private void Heart() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (Heart) {
                    sendPong("ping");
                } else {
                    reconnect();
                    timer.cancel();
                }
                Heart = false;
            }
        }, 500, 60000);
    }

    private void retrySend() {
        if (thread == null) {
            thread = new Thread(() -> {
                while (isRunning) {
                    if (connected && null != messageHashMap) {
                        Iterator<Map.Entry<Integer, Message>> iterator = messageHashMap.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Integer, Message> entry = iterator.next();
                            int position = entry.getKey();
                            Message message = entry.getValue();
                            if (send(message, KeyHashMap.get(position))) {
                                messageFragment.UpdateDatabase(message);
                                chatMessages.get(position).setSending(false);
                                handler.post(() -> {
                                    ChatActivity.chatAdapter.notifyItemChanged(position);
                                    ChatActivity.swipeRecyclerView.smoothScrollToPosition(chatMessages.size());
                                });
                                dbAdapter.update(new Friends(message.senderId, message.receiverId, message.message, Long.parseLong(message.timestamp), message.message_type), Constants.KEY_FRIENDS);
                                iterator.remove();
                            }
                        }
                        break;
                    }
                }
            });
            thread.start();
            new Handler().postDelayed(() -> {
                isRunning = false;
                if (messageHashMap != null) {
                    Iterator<Map.Entry<Integer, Message>> iterator = messageHashMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<Integer, Message> entry = iterator.next();
                        int position = entry.getKey();
                        chatMessages.get(position).setSending(false);
                        chatMessages.get(position).setSendError(true);
                        ChatActivity.chatAdapter.notifyItemChanged(position);
                        ChatActivity.swipeRecyclerView.smoothScrollToPosition(chatMessages.size());
                        iterator.remove();
                        thread = null;
                    }
                }
            }, 15000);
        }
    }

    public void setWebSocketCallback(WebSocketCallback webSocketCallback) {
        this.webSocketCallback = webSocketCallback;
    }

    private void sendNotification(Message message) {
        Intent intent;
        if (!message.message_type.equals(REQUEST_FRIEND)) {
            User user = new User();
            user.id = message.senderId;
            UnOnlineGetFriends(message);
            for (int i = 0; i < friends.size(); i++) {
                if (friends.get(i).id.equals(message.senderId)) {
                    user.name = friends.get(i).name;
                    user.image = friends.get(i).image;
                    user.AES = friends.get(i).aes;
                    break;
                }
            }
            intent = new Intent(getApplicationContext(), ChatActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra(Constants.KEY_USER, user);
        } else {
            intent = new Intent(getApplicationContext(), FriendRequestActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        String changId = "chat_message";
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_MUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), changId);
        builder.setSmallIcon(R.mipmap.icon);
        builder.setContentTitle("新消息");
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText("你有一条新消息"));
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "新消息通知";
            String channelDescription = "This notification channel is used for chat message notification";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(changId, channelName, importance);
            channel.setDescription(channelDescription);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        notificationManagerCompat.notify(Constants.KEY_NOTIFICATION_ID, builder.build());
    }

    private void request_friend(Message message) {
        OkHttpUtils.get().addParams("userId", message.senderId).url(Constants.KEY_URL + "getUser").build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e) {

            }

            @Override
            public void onResponse(Call call, String s) {
                boolean flag = true;
                User user = gson.fromJson(s, User.class);
                Friends friends = new Friends();
                friends.userId = message.receiverId;
                friends.id = message.senderId;
                friends.name = user.name;
                friends.image = user.image;
                friends.email = user.email;
                friends.studs = "1";
                friends.friend_message = message.message;
                friends.aes = user.AES;
                for (int i = 0; i < Constants.friends.size(); i++) {
                    if (Constants.friends.get(i).id.equals(friends.id)) {
                        Constants.friends.get(i).friend_message = message.message;
                        dbAdapter.update(friends, Constants.KEY_FRIENDS);
                        flag = false;
                    }
                }
                if (flag) {
                    Constants.friends.add(friends);
                    dbAdapter.insert(friends, Constants.KEY_FRIENDS);
                }
            }
        });
    }

    private void UnOnlineGetFriends(Message message) {
        if (friends.size() == 0) {
            friends = dbAdapter.queryAllData(preferenceManager.getString(Constants.KEY_USER_ID), Constants.KEY_FRIENDS);
            if (null != friends) {
                messageFragment.UpdateDatabase(message);
                for (int i = 0; i < friends.size(); i++) {
                    if (friends.get(i).studs.equals("0")) {
                        User user = new User();
                        user.name = friends.get(i).name;
                        user.email = friends.get(i).email;
                        user.image = friends.get(i).image;
                        user.id = friends.get(i).id;
                        users.add(user);
                        userId.add(user.id);
                    }
                }
            }
        }
    }

    public interface WebSocketCallback {
        void onMessage(Message message);
    }

    public class LocalBinder extends Binder {
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    private class WebSocketHandler extends WebSocketListener {

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            Heart = true;
            Heart();
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            connected = true;
            executorService.execute(() -> {
                if (IsSend) {
                    sendPong("Local");
                    IsSend = false;
                }
                switch (text) {
                    case "pong":
                        Heart = true;
                        break;
                    case "BACK":
                        Logout();
                        break;
                    default:
                        Message message = gson.fromJson(text, Message.class);
                        message.message = Encrypt.decrypt(message.message, preferenceManager.getString(Constants.KEY_AES));
                        switch (message.message_type) {
                            case CALLBACK_FRIEND:
                                dbAdapter.insert(message, Constants.KEY_COLLECTION_CHAT);
                                for (Friends friend:friends){
                                    if (friend.id.equals(message.senderId)){
                                        friend.studs="0";
                                        friend.lastMessage=message.message;
                                        users.add(new User(friend.name,friend.image,friend.email,friend.id,friend.aes));
                                        messageFragment.UpdateDatabase(message);
                                        dbAdapter.update(friend, Constants.KEY_FRIENDS);
                                        break;
                                    }
                                }
                                handler.post(() -> {
                                    contactsFragment.adapter.notifyDataSetChanged();
                                    messageFragment.conversionsAdapter.notifyDataSetChanged();
                                });
                                break;
                            case REQUEST_FRIEND:
                                dbAdapter.insert(message, Constants.KEY_COLLECTION_CHAT);
                                request_friend(message);
                                handler.post(() -> {
                                    MainActivity.visible();
                                    UsersAdapter.ButtonViewHolder.visible();
                                });
                                break;
                            case AUDIO_MESSAGE:
                                executorService.execute(() -> {
                                    try {
                                        AUDIO_FILE = new File(AUDIO_DIR, message.senderId + System.currentTimeMillis());
                                        AUDIO_FILE.createNewFile();
                                        AUDIO_TIME = Integer.parseInt(message.message.split("time:")[1]);
                                        byte[] AudioMessage = Base64.decode(message.message.split("time")[0], Base64.DEFAULT);
                                        message.message = AUDIO_FILE.getPath() + "time:" + AUDIO_TIME;
                                        dbAdapter.insert(message, Constants.KEY_COLLECTION_CHAT);
                                        dbAdapter.update(new Friends(message.receiverId, message.senderId, message.message, Long.parseLong(message.timestamp), message.message_type), Constants.KEY_FRIENDS);
                                        if (webSocketCallback != null) {
                                            webSocketCallback.onMessage(message);
                                        }
                                        FileOutputStream fileOutputStream = new FileOutputStream(AUDIO_FILE);
                                        fileOutputStream.write(AudioMessage);
                                        fileOutputStream.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                                break;
                            case IMAGE_MESSAGE:
                            case Text_MESSAGE:
                                executorService.execute(() -> {
                                    dbAdapter.insert(message, Constants.KEY_COLLECTION_CHAT);
                                    dbAdapter.update(new Friends(message.receiverId, message.senderId, message.message, Long.parseLong(message.timestamp), message.message_type), Constants.KEY_FRIENDS);
                                });
                                if (webSocketCallback != null) {
                                    webSocketCallback.onMessage(message);
                                }
                                break;
                        }
                        if (preferenceManager.getString(Constants.KEY_AVAILABILITY) != null && preferenceManager.getString(Constants.KEY_AVAILABILITY).equals("0")) {
                            sendNotification(message);
                        }
                }
            });
        }


        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            connected = false;
            reconnect();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, Throwable t, Response response) {
            Log.e(TAG, "onFailure: ", t);
            try {
                if (response != null && response.body().string().contains("1001")) {
                    Logout();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            connected = false;
            reconnect();
        }

        private void Logout() {
            close();
            preferenceManager.clear();
            Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Constants.KEY_RETRY_LOGIN = true;
        }
    }
}
