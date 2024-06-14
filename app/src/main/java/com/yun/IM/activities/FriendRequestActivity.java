package com.yun.IM.activities;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.yun.IM.utilites.Constants.dbAdapter;
import static com.yun.IM.utilites.Constants.friends;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.recyclerview.OnItemMenuClickListener;
import com.yanzhenjie.recyclerview.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.SwipeMenuItem;
import com.yun.IM.Application.MyApplication;
import com.yun.IM.Fragments.contactsFragment;
import com.yun.IM.Fragments.messageFragment;
import com.yun.IM.R;
import com.yun.IM.adapters.UsersAdapter;
import com.yun.IM.databinding.ActivityFriendRequestBinding;
import com.yun.IM.listeners.UserListener;
import com.yun.IM.models.Friends;
import com.yun.IM.models.Message;
import com.yun.IM.models.User;
import com.yun.IM.netWork.WebSocketService;
import com.yun.IM.utilites.Constants;
import com.yun.IM.utilites.PreferenceManager;
import com.yun.IM.utilites.ScreenUtil;
import com.zhy.http.okhttp.OkHttpUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FriendRequestActivity extends AppCompatActivity implements UserListener {
    private ActivityFriendRequestBinding binding;
    private UsersAdapter adapter;
    private SwipeMenuItem item1;
    private SwipeMenuCreator creator;
    private OnItemMenuClickListener onItemMenuClickListener;
    private PreferenceManager preferenceManager;
    private List<User> users;
    private WebSocketService webSocketService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFriendRequestBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(this);
        MyApplication myApplication = (MyApplication) getApplication();
        webSocketService = myApplication.getWebSocketService();
        users = new ArrayList<>();
        init();
        binding.progressBar.setVisibility(View.GONE);
        binding.userRecyclerView.setVisibility(View.VISIBLE);
        setContentView(binding.getRoot());
    }

    private void init() {
        for (int i = 0; i < friends.size(); i++) {
            if (friends.get(i).studs.equals("1")) {
                User user = new User();
                user.image = friends.get(i).image;
                user.id = friends.get(i).id;
                user.name = friends.get(i).name;
                user.email = friends.get(i).friend_message;
                user.AES = friends.get(i).aes;
                users.add(user);
            }
        }
        adapter = new UsersAdapter(users, this, false, 2);
        createItem();
        binding.userRecyclerView.setSwipeMenuCreator(creator);
        binding.userRecyclerView.setOnItemMenuClickListener(onItemMenuClickListener);
        binding.userRecyclerView.setAdapter(adapter);
    }

    private void createItem() {
        creator = (leftMenu, rightMenu, position) -> {
            item1 = new SwipeMenuItem(getApplicationContext());
            item1.setBackground(R.color.RED);
            item1.setWidth(ScreenUtil.dip2px(getApplicationContext(), 70));
            item1.setHeight(MATCH_PARENT);
            item1.setText("删除");
            item1.setTextSize(18);
            item1.setTextColor(Color.WHITE);
            rightMenu.addMenuItem(item1);
        };
        onItemMenuClickListener = (menuBridge, adapterPosition) -> {
            menuBridge.closeMenu();
            Friends friends = new Friends();
            friends.userId = preferenceManager.getString(Constants.KEY_USER_ID);
            friends.id = users.get(adapterPosition).id;
            dbAdapter.deleteOneData(friends, Constants.KEY_FRIENDS);
            users.remove(adapterPosition);
            adapter.notifyItemRemoved(adapterPosition);
        };
    }

    @Override
    public void onUserClicked(User user, boolean flag) {
        Friends friends = new Friends();
        friends.userId = preferenceManager.getString(Constants.KEY_USER_ID);
        friends.id = user.id;
        friends.studs = "0";
        dbAdapter.update(friends, Constants.KEY_FRIENDS);
        for (int i = 0; i < Constants.friends.size(); i++) {
            if (Constants.friends.get(i).id.equals(user.id)) {
                Constants.friends.get(i).studs = "0";
                user.email = Constants.friends.get(i).email;
                break;
            }
        }
        new Thread(() -> {
            try {
                OkHttpUtils.get().url(Constants.KEY_URL + "AddUser").addParams("userid", friends.userId).addParams("id", friends.id).build().execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        Constants.users.add(user);
        Constants.userId.add(user.id);
        messageFragment.UpdateDatabase(new Message("我们已成功添加好友，快来和我聊天吧", friends.userId, user.id, String.valueOf(new Date().getTime()), Constants.Text_MESSAGE));
        Toast.makeText(this, "已成功和" + user.name + "成为好友", Toast.LENGTH_SHORT).show();
        webSocketService.send(new Message("我们已成功添加好友，快来和我聊天吧", friends.userId, user.id, String.valueOf(new Date().getTime()), Constants.CALLBACK_FRIEND), user.AES);
//        ChatMessage message = new ChatMessage();
//        message.message = "我们已成功添加好友，快来和我聊天吧";
//        message.conversionId = user.id;
//        message.message_type = Constants.Text_MESSAGE;
//        message.AES = user.AES;
//        message.dateObject = new Date().getTime();
//        message.conversionImage = user.image;
//        message.conversionName = user.name;
//        conversations.add(message);
        messageFragment.conversionsAdapter.notifyDataSetChanged();
        contactsFragment.adapter.notifyDataSetChanged();

    }
}