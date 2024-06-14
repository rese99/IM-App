package com.yun.IM.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yun.IM.databinding.ItemContainerRecentConversionBinding;
import com.yun.IM.listeners.ConversionListener;
import com.yun.IM.models.ChatMessage;
import com.yun.IM.models.User;
import com.yun.IM.utilites.Constants;

import java.util.List;

public class RecentConversionsAdapter extends RecyclerView.Adapter<RecentConversionsAdapter.ConversionViewHolder> {
    private final List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;

    public RecentConversionsAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
    }


    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    private Bitmap getConversionImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversionBinding binding;

        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(ChatMessage chatMessage) {
            switch (chatMessage.message_type){
                case Constants.AUDIO_MESSAGE:
                    binding.textRecentMessage.setText("[语音消息]");
                    break;
                case Constants.FILE_MESSAGE:
                    binding.textRecentMessage.setText("[文件消息]");
                    break;
                case Constants.IMAGE_MESSAGE:
                    binding.textRecentMessage.setText("[图片消息]");
                    break;
                default:
                    binding.textRecentMessage.setText(chatMessage.message);
            }
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.id = chatMessage.conversionId;
                user.name = chatMessage.conversionName;
                user.image = chatMessage.conversionImage;
                user.AES=chatMessage.AES;
                conversionListener.onConversionClicked(user);
            });
        }
    }
}
