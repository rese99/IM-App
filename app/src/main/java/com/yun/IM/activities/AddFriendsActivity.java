package com.yun.IM.activities;

import static com.yun.IM.utilites.Constants.dbAdapter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yun.IM.Application.MyApplication;
import com.yun.IM.adapters.UsersAdapter;
import com.yun.IM.databinding.ActivityAddFriendsBinding;
import com.yun.IM.listeners.UserListener;
import com.yun.IM.models.Friends;
import com.yun.IM.models.Message;
import com.yun.IM.models.User;
import com.yun.IM.netWork.WebSocketService;
import com.yun.IM.utilites.Constants;
import com.yun.IM.utilites.PreferenceManager;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;

public class AddFriendsActivity extends AppCompatActivity implements UserListener {
    private ActivityAddFriendsBinding binding;
    private Gson gson = new Gson();
    private UsersAdapter adapter;
    private PreferenceManager preferenceManager;
    private List<User> users = new ArrayList<>();
    private WebSocketService webSocketService;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddFriendsBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        MyApplication myApplication = (MyApplication) getApplication();
        webSocketService = myApplication.getWebSocketService();
        init();
        setListeners();
        setContentView(binding.getRoot());
    }

    private void init() {
        adapter = new UsersAdapter(users, this, false, 1);
        binding.conversationsRecyclerView.setAdapter(adapter);
    }

    private void setListeners() {
        binding.imageFind.setOnClickListener(v -> {
            loading(true);
            OkHttpUtils
                    .get()
                    .url(Constants.KEY_URL + "getUsers")
                    .addParams(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                    .addParams("key", binding.findFriends.getText().toString().trim())
                    .build()
                    .execute(new StringCallback() {
                        @Override
                        public void onError(Call call, Exception e) {
                            loading(false);
                            showToast("请求出错");
                        }

                        @Override
                        public void onResponse(Call call, String s) {
                            loading(false);
                            if (null != s) {
                                users = gson.fromJson(s, new TypeToken<List<User>>() {
                                }.getType());
                                adapter.setUsers(users);
                                adapter.notifyDataSetChanged();
                                binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
                            } else {
                                showToast("没有找到该账户信息");
                            }
                        }
                    });
        });
        binding.sure.setOnClickListener(v -> {
            boolean flags = true;
            Message message = new Message();
            message.message = binding.addFriendsMessage.getText().toString();
            message.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
            message.receiverId = user.id;
            message.timestamp = String.valueOf(new Date().getTime());
            message.message_type = Constants.REQUEST_FRIEND;
            Friends friends = new Friends();
            friends.userId = message.senderId;
            friends.id = message.receiverId;
            friends.name = user.name;
            friends.image = user.image;
            friends.email = user.email;
            friends.studs = "1";
            friends.aes = user.AES;
            binding.lineBottom.setVisibility(View.GONE);
            for (int i = 0; i < Constants.friends.size(); i++) {
                if (Constants.friends.get(i).id.equals(friends.id)) {
                    flags = false;
                    break;
                }
            }
            if (flags) {
                Constants.friends.add(friends);
                dbAdapter.insert(friends, Constants.KEY_FRIENDS);
            }
            webSocketService.send(message, user.AES);
        });
        binding.back.setOnClickListener(v -> {
            binding.lineBottom.setVisibility(View.GONE);
        });
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUserClicked(User user, boolean flag) {
        if (!flag) {
            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
            intent.putExtra(Constants.KEY_USER, user);
            startActivity(intent);
        } else {
            binding.lineBottom.setVisibility(View.VISIBLE);
            this.user = user;
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}