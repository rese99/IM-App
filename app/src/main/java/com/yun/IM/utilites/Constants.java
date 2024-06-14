package com.yun.IM.utilites;

import com.yun.IM.models.ChatMessage;
import com.yun.IM.models.Emoji;
import com.yun.IM.models.Friends;
import com.yun.IM.models.User;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class Constants {
    //    好友
    public static final String FRIEND_REQUEST_MESSAGE = "friend_message";
    public static final String FRIEND_STUDS = "studs";
    public static final String KEY_FRIENDS = "friends";
    public static final String KEY_LAST_MESSAGE = "lastMessage";
    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_AES = "aes";
    //    信息类型
    public static final String MESSAGE_TYPE = "message_type";
    public static final String REQUEST_FRIEND = "request_friend";
    public static final String CALLBACK_FRIEND = "call_friend";
    public static final String Text_MESSAGE = "text";
    public static final String IMAGE_MESSAGE = "image";
    public static final String AUDIO_MESSAGE = "audio";
    public static final String FILE_MESSAGE = "file";
    //    通知
    public static final int KEY_NOTIFICATION_ID = 96728453;
    //    权限ID
    public static final int KEY_PERMISSIONS = 123;
    //    个人信息储存
    public static final String KEY_PREFERENCE_NAME = "IMChatPreference";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_USER = "user";
    public static final String KEY_TOKEN = "token";
    //    聊天信息数据库
    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    //    窗口检测
    public static final String KEY_AVAILABILITY = "availability";
    //    密码匹配
    public static final String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,16}$";
    //    邮箱匹配
    public static final String email = "^[a-z0-9A-Z]+[- | a-z0-9A-Z . _]+@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-z]{2,}$";
    //    服务器url
    public static final String WS = "ws://110.41.132.226:1314/WebSocket/";
    public static final String KEY_URL = "http://110.41.132.226:1314/";
    //    静态词条
    public static final String SignUp = "用户注册";
    public static final String forget = "找回密码";
    public static final String ShowError = "连接超时";
    //    AES加密
    public static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    public static final String KEY_ALGORITHM = "AES";
    //    语音文件储存
    public static File AUDIO_DIR = null;
    public static File AUDIO_FILE = null;
    public static int AUDIO_TIME = 0;
    //    重新登录
    public static boolean KEY_RETRY_LOGIN = false;
    //    静态变量
    public static DBAdapter dbAdapter;
    public static List<Friends> friends = new ArrayList<>();
    public static List<User> users = new ArrayList<>();
    public static HashSet<String> userId = new HashSet<>();
    public static List<ChatMessage> conversations = new ArrayList<>();
    public static List<ChatMessage> chatMessages = new ArrayList<>();
    public static List<Emoji> emojiList = new ArrayList<>();
    //    线程池
    public static ExecutorService executorService;
    //    静态词汇
    public static String SearchKey;
    //    更新检测
    public static boolean isCheck = false;

    public static void clean() {
        if (friends != null) {
            friends.clear();
        }
        if (users != null) {
            users.clear();
        }
        if (userId != null) {
            userId.clear();
        }
        if (conversations != null) {
            conversations.clear();
        }
        if (chatMessages != null) {
            chatMessages.clear();
        }
    }
}
