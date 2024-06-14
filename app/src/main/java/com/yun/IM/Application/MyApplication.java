package com.yun.IM.Application;

import static com.yun.IM.utilites.Constants.dbAdapter;
import static com.yun.IM.utilites.Constants.emojiList;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.yun.IM.models.Emoji;
import com.yun.IM.netWork.WebSocketService;
import com.yun.IM.utilites.Constants;
import com.yun.IM.utilites.DBAdapter;
import com.yun.IM.utilites.PreferenceManager;

public class MyApplication extends Application {
    private WebSocketService webSocketService;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            webSocketService = ((WebSocketService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            webSocketService = null;
        }
    };

    public WebSocketService getWebSocketService() {
        return webSocketService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbAdapter = new DBAdapter(getApplicationContext());
        startService(new Intent(this, WebSocketService.class));
        bindService(new Intent(this, WebSocketService.class), serviceConnection, BIND_AUTO_CREATE);
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        dbAdapter.open(preferenceManager.getString(Constants.KEY_USER_ID));
        String s = "\uD83D\uDE00\uD83D\uDE03\uD83D\uDE04\uD83D\uDE01\uD83D\uDE06\uD83D\uDE05\uD83E\uDD23\uD83D\uDE02\uD83D\uDE42\uD83D\uDE43\uD83D\uDE09\uD83D\uDE0A\uD83D\uDE07\uD83D\uDE15\uD83D\uDE1F\uD83D\uDE41\uD83D\uDE2E\uD83D\uDE2F\uD83D\uDE32\uD83D\uDE31\uD83D\uDE2D\uD83D\uDE22\uD83D\uDE25\uD83D\uDE30\uD83D\uDE28\uD83D\uDE27\uD83D\uDE26\uD83E\uDD7A\uD83D\uDE33\uD83D\uDE16\uD83D\uDE23\uD83D\uDE1E\uD83D\uDE13\uD83D\uDE29\uD83D\uDE2B\uD83E\uDD71\uD83D\uDE24\uD83D\uDE21\uD83D\uDE20\uD83E\uDD2C\uD83E\uDD70\uD83D\uDE0D\uD83E\uDD29\uD83D\uDE1D\uD83E\uDD2A\uD83D\uDE1C\uD83D\uDE1B\uD83D\uDE0B\uD83D\uDE19\uD83D\uDE1A\uD83D\uDE17\uD83D\uDE18\uD83E\uDD11\uD83E\uDD17\uD83E\uDD2D\uD83E\uDD2B\uD83E\uDD14\uD83E\uDD10\uD83E\uDD28\uD83D\uDE10\uD83E\uDD25\uD83D\uDE2C\uD83D\uDE44\uD83D\uDE12\uD83D\uDE0F\uD83D\uDE36\uD83D\uDE11\uD83D\uDE0C\uD83D\uDE14\uD83D\uDE2A\uD83E\uDD24\uD83D\uDE34\uD83D\uDE37\uD83E\uDD12\uD83E\uDD15\uD83E\uDD22\uD83E\uDD73\uD83E\uDD20\uD83E\uDD2F\uD83D\uDE35\uD83E\uDD74\uD83E\uDD27\uD83E\uDD2E\uD83D\uDE0E\uD83E\uDD13\uD83E\uDDD0\uD83D\uDE28\uD83D\uDE2B\uD83D\uDE29\uD83E\uDD2C\uD83D\uDCA9\uD83D\uDC7B\uD83E\uDD21\uD83D\uDC7E\uD83D\uDC7F\uD83D\uDC80\uD83D\uDC79\uD83D\uDE39\uD83D\uDE3F\uD83D\uDC96";
        for (int i = 0; i < s.length(); i++) {
            Emoji emoji = new Emoji();
            emoji.emoji1 = s.substring(i, i + 2);
            emoji.emoji2 = s.substring(i + 2, i + 4);
            emoji.emoji3 = s.substring(i + 4, i + 6);
            emoji.emoji4 = s.substring(i + 6, i + 8);
            emoji.emoji5 = s.substring(i + 8, i + 10);
            emoji.emoji6 = s.substring(i + 10, i + 12);
            emoji.emoji7 = s.substring(i + 12, i + 14);
            emoji.emoji8 = s.substring(i + 14, i + 16);
            emoji.emoji9 = s.substring(i + 16, i + 18);
            emoji.emoji10 = s.substring(i + 18, i + 20);
            i = i + 19;
            emojiList.add(emoji);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unbindService(serviceConnection);
        startService(new Intent(getApplicationContext(), WebSocketService.class));
    }
}
