package com.yun.IM.adapters;

import static com.yun.IM.utilites.Constants.SearchKey;
import static com.yun.IM.utilites.Constants.users;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yun.IM.databinding.ItemContainerRecentConversionBinding;
import com.yun.IM.listeners.ConversionListener;
import com.yun.IM.listeners.UserListener;
import com.yun.IM.models.Message;
import com.yun.IM.models.User;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static int dataName;
    private final List DataList;
    private final String userId;
    private final UserListener listener;
    private final ConversionListener conversionListener;

    public SearchAdapter(String userId, List DataList, UserListener listener, ConversionListener conversionListener) {
        this.DataList = DataList;
        this.userId = userId;
        this.listener = listener;
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (dataName) {
            case 0:
                return new FriendsViewHolder(ItemContainerRecentConversionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            case 1:
                return new MessageViwHolder(ItemContainerRecentConversionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            default:
                return null;
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (dataName) {
            case 0:
                ((FriendsViewHolder) holder).setData((User) DataList.get(position));
                break;
            case 1:
                ((MessageViwHolder) holder).setData((Message) DataList.get(position));
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return DataList.size();
    }

    private Bitmap getConversionImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void setHighLineText(String text, String keyword, TextView textView) {
        if (!TextUtils.isEmpty(keyword) && !TextUtils.isEmpty(text)) {
            if (text.toLowerCase().contains(keyword.toLowerCase())) {
                int start = 0;
                if (text.contains(keyword)) {
                    start = text.indexOf(keyword);
                } else {
                    start = text.toLowerCase().indexOf(keyword.toLowerCase());
                }
                SpannableStringBuilder styled = new SpannableStringBuilder(text);
                styled.setSpan(new ForegroundColorSpan(Color.RED), start, start + keyword.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setText(styled);
            } else {
                textView.setText(text);
            }
        }
    }

    class FriendsViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversionBinding binding;

        FriendsViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(User user) {
            binding.imageProfile.setImageBitmap(getConversionImage(user.image));
            setHighLineText(user.name,SearchKey,binding.textName);
            setHighLineText(user.email,SearchKey,binding.textRecentMessage);
            binding.getRoot().setOnClickListener(v -> listener.onUserClicked(user, true));
        }
    }

    class MessageViwHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversionBinding binding;

        MessageViwHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(Message message) {
            if (message.senderId.equals(userId)) {
                for (User user : users) {
                    if (user.id.equals(message.receiverId)) {
                        binding.imageProfile.setImageBitmap(getConversionImage(user.image));
                        binding.textName.setText(user.name);
                        binding.getRoot().setOnClickListener(v -> conversionListener.onConversionClicked(user));
                        break;
                    }
                }
            } else {
                for (User user : users) {
                    if (user.id.equals(message.senderId)) {
                        binding.imageProfile.setImageBitmap(getConversionImage(user.image));
                        binding.textName.setText(user.name);
                        binding.getRoot().setOnClickListener(v -> conversionListener.onConversionClicked(user));
                        break;
                    }
                }
            }
            setHighLineText(message.message,SearchKey,binding.textRecentMessage);
        }
    }
}
