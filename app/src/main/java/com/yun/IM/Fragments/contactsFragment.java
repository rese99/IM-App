package com.yun.IM.Fragments;

import static com.yun.IM.utilites.Constants.users;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.yun.IM.activities.AddFriendsActivity;
import com.yun.IM.activities.ChatActivity;
import com.yun.IM.activities.UserInfoActivity;
import com.yun.IM.adapters.UsersAdapter;
import com.yun.IM.databinding.FragmentContactsBinding;
import com.yun.IM.listeners.UserListener;
import com.yun.IM.models.User;
import com.yun.IM.utilites.Constants;

public class contactsFragment extends Fragment implements UserListener {
    private Context context;
    private FragmentContactsBinding binding;
    public static UsersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.context = getActivity();
        binding = FragmentContactsBinding.inflate(getLayoutInflater());
        loading(true);
        adapter = new UsersAdapter(users, this, true, 0);
        binding.userRecyclerView.setAdapter(adapter);
        loading(false);
        binding.userRecyclerView.setVisibility(View.VISIBLE);
        setListeners();
        return binding.getRoot();
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user, boolean flag) {
        Intent intent = new Intent(context, UserInfoActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }

    private void setListeners() {
        binding.imageAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this.getActivity(), AddFriendsActivity.class);
            startActivity(intent);
        });
    }
}