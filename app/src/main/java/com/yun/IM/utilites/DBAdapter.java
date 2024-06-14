package com.yun.IM.utilites;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.yun.IM.models.Friends;
import com.yun.IM.models.Message;

import java.util.ArrayList;
import java.util.List;

public class DBAdapter {
    private static final int DB_VERSION = 1;
    private Context context;
    private SQLiteDatabase db;
    private DBOpenHelper dbOpenHelper;

    public DBAdapter(Context _context) {
        context = _context.getApplicationContext();
    }

    /**
     * Close the database
     */
    public void close() {
        if (db != null) {
            db.close();
            db = null;
        }
    }

    /**
     * Open the database
     */
    public void open(String DB_NAME) {
        if (DB_NAME != null && !DB_NAME.equals("") && !DB_NAME.equals("null") && context != null) {
            dbOpenHelper = new DBOpenHelper(context, DB_NAME + ".db", null, DB_VERSION);
            db = dbOpenHelper.getReadableDatabase();
            while (true) {
                dbOpenHelper = new DBOpenHelper(context, DB_NAME + ".db", null, DB_VERSION);
                db = dbOpenHelper.getReadableDatabase();
                if (db != null) {
                    break;
                }
            }
        }

    }

    public void insert(Message message, String DB_TABLE) {
        ContentValues newValues = new ContentValues();
        newValues.put(Constants.KEY_MESSAGE, message.message);
        newValues.put(Constants.KEY_SENDER_ID, message.senderId);
        newValues.put(Constants.KEY_RECEIVER_ID, message.receiverId);
        newValues.put(Constants.KEY_TIMESTAMP, message.timestamp);
        newValues.put(Constants.MESSAGE_TYPE, message.message_type);
        db.insert(DB_TABLE, null, newValues);
    }

    public void insert(Friends friends, String DB_TABLE) {
        ContentValues newValues = new ContentValues();
        newValues.put(Constants.KEY_USER_ID, friends.userId);
        newValues.put(Constants.KEY_NAME, friends.name);
        newValues.put(Constants.KEY_EMAIL, friends.email);
        newValues.put(Constants.KEY_IMAGE, friends.image);
        newValues.put(Constants.KEY_ID, friends.id);
        newValues.put(Constants.KEY_LAST_MESSAGE, friends.lastMessage);
        newValues.put(Constants.KEY_TIMESTAMP, friends.dateObject);
        newValues.put(Constants.FRIEND_STUDS, friends.studs);
        newValues.put(Constants.FRIEND_REQUEST_MESSAGE, friends.friend_message);
        newValues.put(Constants.KEY_AES, friends.aes);
        db.insert(DB_TABLE, null, newValues);
    }

    public void update(Friends friends, String DB_TABLE) {
        if (friends.message_type == null) {
            friends.message_type = Constants.Text_MESSAGE;
        }
        switch (friends.message_type) {
            case Constants.AUDIO_MESSAGE:
                friends.lastMessage = "[语音消息]";
                break;
            case Constants.FILE_MESSAGE:
                friends.lastMessage = "[文件消息]";
                break;
            case Constants.IMAGE_MESSAGE:
                friends.lastMessage = "[动画表情]";
                break;
            default:
                break;
        }
        ContentValues values = new ContentValues();
        values.put(Constants.KEY_LAST_MESSAGE, friends.lastMessage);
        values.put(Constants.KEY_TIMESTAMP, friends.dateObject);
        if (friends.studs != null) {
            values.put(Constants.FRIEND_STUDS, friends.studs);
        }
        if (friends.friend_message != null) {
            values.put(Constants.FRIEND_REQUEST_MESSAGE, friends.friend_message);
        }
        db.update(DB_TABLE, values, Constants.KEY_USER_ID + " = '" + friends.userId + "' AND " + Constants.KEY_ID + " = '" + friends.id + "'", null);
    }

    public List<Message> queryAllData(String senderId, String receiverId, String DB_TABLE) {
        String condition1 = Constants.KEY_SENDER_ID + " = '" + senderId + "' AND " + Constants.KEY_RECEIVER_ID + " = '" + receiverId + "'";
        String condition2 = Constants.KEY_SENDER_ID + " = '" + receiverId + "' AND " + Constants.KEY_RECEIVER_ID + " = '" + senderId + "'";
        String condition = condition1 + " OR " + condition2;
        Cursor results = db.query(DB_TABLE, new String[]{Constants.KEY_MESSAGE, Constants.KEY_SENDER_ID, Constants.KEY_RECEIVER_ID, Constants.KEY_TIMESTAMP, Constants.MESSAGE_TYPE}, condition, null, null, null, null);
        return ConvertToMessage(results);
    }

    public List<Message> queryAllDataForKey(String key, String DB_TABLE) {
        String condition = Constants.KEY_MESSAGE + " LIKE ? ";
        String[] selectionArgs = new String[]{"%" + key + "%"};
        Cursor results = db.query(DB_TABLE, new String[]{Constants.KEY_MESSAGE, Constants.KEY_SENDER_ID, Constants.KEY_RECEIVER_ID, Constants.KEY_TIMESTAMP, Constants.MESSAGE_TYPE},condition , selectionArgs, null, null, null);
        return ConvertToMessage(results);
    }

    public List<Friends> queryAllData(String userId, String DB_TABLE) {
        Cursor results = db.query(DB_TABLE, new String[]{Constants.KEY_USER_ID, Constants.KEY_NAME, Constants.KEY_EMAIL, Constants.KEY_IMAGE, Constants.KEY_ID, Constants.KEY_LAST_MESSAGE, Constants.KEY_TIMESTAMP, Constants.FRIEND_STUDS, Constants.FRIEND_REQUEST_MESSAGE, Constants.KEY_AES}, Constants.KEY_USER_ID + " = " + "'" + userId + "'", null, null, null, null);
        return ConvertToFriends(results);
    }

    public void deleteOneData(Friends friends, String DB_TABLE) {
        String condition = Constants.KEY_USER_ID + " = '" + friends.userId + "' AND " + Constants.KEY_ID + " = '" + friends.id + "'";
        db.delete(DB_TABLE, condition, null);
    }

    public void deleteMessage(String senderId, String receiverId, String DB_TABLE) {
        String condition1 = Constants.KEY_SENDER_ID + " = '" + senderId + "' AND " + Constants.KEY_RECEIVER_ID + " = '" + receiverId + "'";
        String condition2 = Constants.KEY_SENDER_ID + " = '" + receiverId + "' AND " + Constants.KEY_RECEIVER_ID + " = '" + senderId + "'";
        String condition = condition1 + " OR " + condition2;
        db.delete(DB_TABLE, condition, null);
    }

    @SuppressLint("Range")
    private List<Message> ConvertToMessage(Cursor cursor) {
        int resultCounts = cursor.getCount();
        if (resultCounts == 0 || !cursor.moveToFirst()) {
            return null;
        }
        List<Message> messageList = new ArrayList<>();
        for (int i = 0; i < resultCounts; i++) {
            Message message = new Message();
            message.message = cursor.getString(cursor.getColumnIndex(Constants.KEY_MESSAGE));
            message.senderId = cursor.getString(cursor.getColumnIndex(Constants.KEY_SENDER_ID));
            message.receiverId = cursor.getString(cursor.getColumnIndex(Constants.KEY_RECEIVER_ID));
            message.timestamp = cursor.getString(cursor.getColumnIndex(Constants.KEY_TIMESTAMP));
            message.message_type = cursor.getString(cursor.getColumnIndex(Constants.MESSAGE_TYPE));
            messageList.add(message);
            cursor.moveToNext();
        }
        return messageList;
    }

    @SuppressLint("Range")
    private List<Friends> ConvertToFriends(Cursor cursor) {
        int resultCounts = cursor.getCount();
        if (resultCounts == 0 || !cursor.moveToFirst()) {
            return null;
        }
        List<Friends> friendsList = new ArrayList<>();
        for (int i = 0; i < resultCounts; i++) {
            Friends friends = new Friends();
            friends.userId = cursor.getString(cursor.getColumnIndex(Constants.KEY_USER_ID));
            friends.name = cursor.getString(cursor.getColumnIndex(Constants.KEY_NAME));
            friends.email = cursor.getString(cursor.getColumnIndex(Constants.KEY_EMAIL));
            friends.image = cursor.getString(cursor.getColumnIndex(Constants.KEY_IMAGE));
            friends.id = cursor.getString(cursor.getColumnIndex(Constants.KEY_ID));
            friends.lastMessage = cursor.getString(cursor.getColumnIndex(Constants.KEY_LAST_MESSAGE));
            friends.dateObject = Long.parseLong(cursor.getString(cursor.getColumnIndex(Constants.KEY_TIMESTAMP)));
            friends.studs = cursor.getString(cursor.getColumnIndex(Constants.FRIEND_STUDS));
            friends.friend_message = cursor.getString(cursor.getColumnIndex(Constants.FRIEND_REQUEST_MESSAGE));
            friends.aes = cursor.getString(cursor.getColumnIndex(Constants.KEY_AES));
            friendsList.add(friends);
            cursor.moveToNext();
        }
        return friendsList;
    }

    @SuppressLint("Range")
    public void printTableSchema(String tableName) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        while (cursor.moveToNext()) {
            int cid = cursor.getInt(cursor.getColumnIndex("cid"));
            String name = cursor.getString(cursor.getColumnIndex("name"));
            String type = cursor.getString(cursor.getColumnIndex("type"));
            int notnull = cursor.getInt(cursor.getColumnIndex("notnull"));
            String dfltValue = cursor.getString(cursor.getColumnIndex("dflt_value"));
            int pk = cursor.getInt(cursor.getColumnIndex("pk"));
            System.out.println("[CID: " + cid + "], [Name: " + name + "], [DataType: " + type + "], [NotNull: " + notnull + "], [Default Value: " + dfltValue + "], [Primary Key: " + pk + "]");
        }
    }

    /**
     * 静态Helper类，用于建立、更新和打开数据库
     */
    public static class DBOpenHelper extends SQLiteOpenHelper {
        private static final String DB_CREATE_Chat = "create table " + Constants.KEY_COLLECTION_CHAT + " (" + Constants.KEY_MESSAGE + " text, " + Constants.KEY_SENDER_ID + " text," + Constants.KEY_RECEIVER_ID + " text," + Constants.KEY_TIMESTAMP + " text," + Constants.MESSAGE_TYPE + " text);";
        private static final String DB_create_Friends = "create table " + Constants.KEY_FRIENDS + " (" + Constants.KEY_USER_ID + " text, " + Constants.KEY_NAME + " text," + Constants.KEY_EMAIL + " text," + Constants.KEY_IMAGE + " text," + Constants.KEY_ID + " text," + Constants.KEY_LAST_MESSAGE + " text," + Constants.KEY_TIMESTAMP + " text," + Constants.FRIEND_STUDS + " text," + Constants.FRIEND_REQUEST_MESSAGE + " text," + Constants.KEY_AES + " text);";

        public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase _db) {
            _db.execSQL(DB_CREATE_Chat);
            _db.execSQL(DB_create_Friends);
        }

        @Override
        public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion) {

        }
    }
}
