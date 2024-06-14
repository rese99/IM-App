package com.yun.IM.activities;

import static com.yun.IM.utilites.Constants.dbAdapter;
import static com.yun.IM.utilites.Constants.friends;
import static com.yun.IM.utilites.Constants.users;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;

import com.yun.IM.databinding.ActivityUserInfoBinding;
import com.yun.IM.models.Friends;
import com.yun.IM.models.User;
import com.yun.IM.utilites.Constants;

public class UserInfoActivity extends AppCompatActivity {

    private ActivityUserInfoBinding binding;
    private User user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityUserInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListener();
    }
    private Bitmap getImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    private void init(){
        user = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.imageProfile.setImageBitmap(getImage(user.image));
        binding.UserEmail.setText(user.email);
        binding.UserName.setText(user.name);
    }
    private void setListener(){
        binding.Chat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(Constants.KEY_USER, user);
            startActivity(intent);
        });
        binding.Remove.setOnClickListener(v -> {
            for (Friends friend:friends) {
                if (friend.id.equals(user.id)){
                    friends.remove(friend);
                    dbAdapter.deleteOneData(friend,Constants.KEY_FRIENDS);
                    break;
                }
            }
            for (User user1:users){
                if (user1.id.equals(user.id)){
                    users.remove(user1);
                    break;
                }
            }
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }

}