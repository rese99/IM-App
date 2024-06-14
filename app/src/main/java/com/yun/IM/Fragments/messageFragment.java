package com.yun.IM.Fragments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.yun.IM.utilites.Constants.SearchKey;
import static com.yun.IM.utilites.Constants.conversations;
import static com.yun.IM.utilites.Constants.dbAdapter;
import static com.yun.IM.utilites.Constants.friends;
import static com.yun.IM.utilites.Constants.users;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import com.yanzhenjie.recyclerview.OnItemMenuClickListener;
import com.yanzhenjie.recyclerview.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.SwipeMenuItem;
import com.yun.IM.Application.MyApplication;
import com.yun.IM.R;
import com.yun.IM.activities.AddFriendsActivity;
import com.yun.IM.activities.ChatActivity;
import com.yun.IM.activities.UserInfoActivity;
import com.yun.IM.adapters.RecentConversionsAdapter;
import com.yun.IM.adapters.SearchAdapter;
import com.yun.IM.databinding.FragmentMessageBinding;
import com.yun.IM.listeners.ConversionListener;
import com.yun.IM.listeners.UserListener;
import com.yun.IM.models.ChatMessage;
import com.yun.IM.models.Friends;
import com.yun.IM.models.Message;
import com.yun.IM.models.User;
import com.yun.IM.netWork.WebSocketService;
import com.yun.IM.utilites.Constants;
import com.yun.IM.utilites.PreferenceManager;
import com.yun.IM.utilites.ScreenUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class messageFragment extends Fragment implements ConversionListener, UserListener {
    public static RecentConversionsAdapter conversionsAdapter;
    private SearchAdapter searchAdapter;
    private SwipeMenuItem item1;
    private SwipeMenuCreator creator;
    private OnItemMenuClickListener onItemMenuClickListener;
    private WebSocketService webSocketService;
    private PreferenceManager preferenceManager;
    private Context context;
    private FragmentMessageBinding binding;
    private List DataList = new ArrayList();
    private int DataName;
    private String[] starArray = {"联系人", "消息", "群组"};
    private WebSocketService.WebSocketCallback webSocketCallback = new WebSocketService.WebSocketCallback() {
        @Override
        public void onMessage(Message message) {
            UpdateDatabase(message);
            Collections.sort(conversations, Comparator.comparing(obj -> obj.dateObject));
            requireActivity().runOnUiThread(() -> {
                conversionsAdapter.notifyDataSetChanged();
                binding.conversationsRecyclerView.smoothScrollToPosition(0);
                binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
            });
        }
    };

    public static void UpdateDatabase(Message message) {
        boolean flag = false;
        for (ChatMessage chatMessage : conversations) {
            if (chatMessage.conversionId.equals(message.senderId) || chatMessage.conversionId.equals(message.receiverId)) {
                chatMessage.message = message.message;
                chatMessage.dateObject = Long.parseLong(message.timestamp);
                chatMessage.message_type = message.message_type;
                flag = true;
                break;
            }
        }
        if (!flag) {
            for (Friends friend : friends) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.conversionImage = friend.image;
                chatMessage.conversionName = friend.name;
                chatMessage.message = message.message;
                chatMessage.dateObject = Long.parseLong(message.timestamp);
                chatMessage.message_type = message.message_type;
                chatMessage.AES = friend.aes;
                if (friend.id.equals(message.senderId)) {
                    chatMessage.conversionId = message.senderId;
                    conversations.add(chatMessage);
                    break;
                }
                if (friend.userId.equals(message.senderId)) {
                    chatMessage.conversionId = message.receiverId;
                    conversations.add(chatMessage);
                    break;
                }
            }
        }
    }

    private void LoadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.context = getActivity();
        preferenceManager = new PreferenceManager(context);
        MyApplication myApplication = (MyApplication) getActivity().getApplication();
        preferenceManager.putString(Constants.KEY_AVAILABILITY, "1");
        binding = FragmentMessageBinding.inflate(getLayoutInflater());
        webSocketService = myApplication.getWebSocketService();
        if (webSocketService != null) {
            webSocketService.setWebSocketCallback(webSocketCallback);
        }
        init();
        setListener();
        initSpinner();
        LoadUserDetails();
        return binding.getRoot();
    }

    private void init() {
        searchAdapter = new SearchAdapter(preferenceManager.getString(Constants.KEY_USER_ID), DataList, this, this);
        binding.searchRecycleView.setAdapter(searchAdapter);
        conversionsAdapter = new RecentConversionsAdapter(conversations, this);
        createItem();
        binding.conversationsRecyclerView.setSwipeMenuCreator(creator);
        binding.conversationsRecyclerView.setOnItemMenuClickListener(onItemMenuClickListener);
        binding.conversationsRecyclerView.setAdapter(conversionsAdapter);
        if (conversations.size() > 0) {
            Collections.sort(conversations, Comparator.comparing(obj -> obj.dateObject));
            conversionsAdapter.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);
        }
        binding.progressBar.setVisibility(View.GONE);
        binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void setListener() {
        PopupMenu popupMenu = new PopupMenu(context, binding.imageAdd);
        popupMenu.getMenuInflater().inflate(R.menu.message_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.item1) {
                return false;
            } else if (item.getItemId() == R.id.item2) {
                Intent intent = new Intent(context, AddFriendsActivity.class);
                startActivity(intent);
                return false;
            } else {
                binding.bottomPanel.show();
                return false;
            }
        });
        binding.imageAdd.setOnClickListener(v -> popupMenu.show());
        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    SearchKey = s.toString();
                } else {
                    SearchKey = null;
                }
                SearchView(DataName);
            }
        });
    }

    private void createItem() {
        creator = (leftMenu, rightMenu, position) -> {
            item1 = new SwipeMenuItem(getActivity());
            item1.setBackground(R.color.RED);
            item1.setWidth(ScreenUtil.dip2px(getActivity(), 70));
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
            friends.id = conversations.get(adapterPosition).conversionId;
            friends.lastMessage = null;
            friends.dateObject = conversations.get(adapterPosition).dateObject;
            friends.message_type = Constants.Text_MESSAGE;
            dbAdapter.update(friends, Constants.KEY_FRIENDS);
            conversations.remove(adapterPosition);
            conversionsAdapter.notifyItemRemoved(adapterPosition);
        };
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && null != preferenceManager) {
            if (webSocketService != null) {
                webSocketService.setWebSocketCallback(webSocketCallback);
            }
            conversionsAdapter.notifyDataSetChanged();
        } else if (null != preferenceManager) {
            preferenceManager.putString(Constants.KEY_AVAILABILITY, "0");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        preferenceManager.putString(Constants.KEY_AVAILABILITY, "0");
    }

    private void initSpinner() {
        ArrayAdapter<String> starAdapter = new ArrayAdapter<>(context, R.layout.item_select, starArray);
        starAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spinner.setAdapter(starAdapter);
        binding.spinner.setSelection(0);
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DataName = position;
                SearchView(DataName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void SearchView(int dataName) {
        DataList.clear();
        if (SearchKey != null) {
            switch (dataName) {
                case 0:
                    SearchAdapter.dataName = 0;
                    for (int i = 0; i < users.size(); i++) {
                        if (users.get(i).email.contains(SearchKey) || users.get(i).name.contains(SearchKey)) {
                            DataList.add(users.get(i));
                        }
                    }
                    break;
                case 1:
                    SearchAdapter.dataName = 1;
                    if (null != dbAdapter.queryAllDataForKey(SearchKey, Constants.KEY_COLLECTION_CHAT)) {
                        for (Message message : dbAdapter.queryAllDataForKey(SearchKey, Constants.KEY_COLLECTION_CHAT)) {
                            DataList.add(message);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    @Override
    public void onUserClicked(User user, boolean flag) {
        Intent intent = new Intent(context, UserInfoActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}