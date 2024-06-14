package com.yun.IM.adapters;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yun.IM.activities.FriendRequestActivity;
import com.yun.IM.activities.MainActivity;
import com.yun.IM.databinding.ItemContainerButtonBinding;
import com.yun.IM.databinding.ItemContainerUserBinding;
import com.yun.IM.listeners.UserListener;
import com.yun.IM.models.User;
import com.yun.IM.utilites.Constants;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_BUTTON = 0;
    public static final int VIEW_TYPE_USER = 1;
    private final UserListener userListener;
    private final boolean flag;
    private final int num;
    private List<User> users;

    public UsersAdapter(List<User> users, UserListener userListener, boolean flag, int num) {
        this.users = users;
        this.userListener = userListener;
        this.flag = flag;
        this.num = num;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_BUTTON) {
            return new ButtonViewHolder(ItemContainerButtonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        } else {
            return new UserViewHolder(ItemContainerUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (flag) {
            if (getItemViewType(position) == VIEW_TYPE_USER) {
                ((UserViewHolder) holder).setUserData(users.get(position - 1));
            }
        } else {
            ((UserViewHolder) holder).setUserData(users.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return flag ? users.size() + 1 : users.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (flag && position == VIEW_TYPE_BUTTON) {
            return VIEW_TYPE_BUTTON;
        } else {
            return VIEW_TYPE_USER;
        }
    }

    private Bitmap getUserImages(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static class ButtonViewHolder extends RecyclerView.ViewHolder {
        static ItemContainerButtonBinding binding;
        ButtonViewHolder(ItemContainerButtonBinding itemContainerButtonBinding) {
            super(itemContainerButtonBinding.getRoot());
            binding = itemContainerButtonBinding;
            binding.NewFriends.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), FriendRequestActivity.class);
                MainActivity.invisible();
                binding.dotRed.setVisibility(View.GONE);
                v.getContext().startActivity(intent);
            });
            binding.GroupChat.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "点击了群聊", Toast.LENGTH_SHORT).show();
            });
        }

        public static void visible() {
            binding.dotRed.setVisibility(View.VISIBLE);
        }
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        ItemContainerUserBinding binding;

        UserViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }

        void setUserData(User user) {
            boolean flag;
            switch (num){
                case 2:
                    binding.imageText.setText("确认添加");
                    binding.imageText.setBackgroundTintList(ColorStateList.valueOf(Color.BLUE));
                    binding.imageText.setOnClickListener(v -> userListener.onUserClicked(user, true));
                    break;
                case 1:
                    if (Constants.userId.contains(user.id)) {
                        binding.imageText.setText("开始聊天");
                        binding.imageText.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                        flag = false;
                    } else {
                        binding.imageText.setText("添加好友");
                        binding.imageText.setBackgroundTintList(ColorStateList.valueOf(Color.BLUE));
                        flag = true;
                    }
                    binding.imageText.setOnClickListener(v -> userListener.onUserClicked(user, flag));
                    break;
                default:
                    binding.imageText.setVisibility(View.GONE);
                    binding.getRoot().setOnClickListener(v -> userListener.onUserClicked(user, false));
                    break;
            }
            binding.textName.setText(user.name);
            binding.textEmail.setText(user.email);
            binding.imageProfile.setImageBitmap(getUserImages(user.image));
        }
    }
}
