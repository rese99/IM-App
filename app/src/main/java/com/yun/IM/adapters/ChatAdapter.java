package com.yun.IM.adapters;

import static com.yun.IM.utilites.Constants.chatMessages;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yun.IM.databinding.ItemContainerReceivedMessageBinding;
import com.yun.IM.databinding.ItemContainerSentMessageBinding;
import com.yun.IM.databinding.ItemReceivedImageBinding;
import com.yun.IM.databinding.ItemSentImageBinding;
import com.yun.IM.listeners.VoiceMessageListener;
import com.yun.IM.models.ChatMessage;
import com.yun.IM.utilites.Constants;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int SENT_MESSAGE = 1;
    public static final int SENT_AUDIO = 2;
    public static final int SENT_IMAGE = 3;
    public static final int SENT_FILE = 4;
    public static final int RECEIVED_MESSAGE = 5;
    public static final int RECEIVED_AUDIO = 6;
    public static final int RECEIVED_IMAGE = 7;
    public static final int RECEIVED_FILE = 8;
    private final String senderId;
    private final VoiceMessageListener voiceMessageListener;
    private Bitmap receiverProfileImage;


    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId, VoiceMessageListener voiceMessageListener) {
        Constants.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
        this.voiceMessageListener = voiceMessageListener;
    }

    public static int getItemPosition() {
        return chatMessages.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case SENT_AUDIO:
            case SENT_MESSAGE:
                return new SentMessageViewHolder(ItemContainerSentMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            case SENT_FILE:
            case SENT_IMAGE:
                return new SentImageViewHolder(ItemSentImageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            case RECEIVED_IMAGE:
                return new ReceivedImageViewHolder(ItemReceivedImageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            case RECEIVED_FILE:
            case RECEIVED_AUDIO:
            default:
                return new ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case SENT_AUDIO:
            case SENT_MESSAGE:
                ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
                break;
            case SENT_FILE:
                break;
            case SENT_IMAGE:
                ((SentImageViewHolder) holder).setData(chatMessages.get(position));
                break;
            case RECEIVED_AUDIO:
            case RECEIVED_MESSAGE:
                ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position), receiverProfileImage);
                break;
            case RECEIVED_FILE:
                break;
            case RECEIVED_IMAGE:
                ((ReceivedImageViewHolder) holder).setData(chatMessages.get(position));
                break;
        }

    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).senderId.equals(senderId)) {
            switch (chatMessages.get(position).message_type) {
                case Constants.AUDIO_MESSAGE:
                    return SENT_AUDIO;
                case Constants.FILE_MESSAGE:
                    return SENT_FILE;
                case Constants.IMAGE_MESSAGE:
                    return SENT_IMAGE;
                default:
                    return SENT_MESSAGE;
            }
        } else {
            switch (chatMessages.get(position).message_type) {
                case Constants.AUDIO_MESSAGE:
                    return RECEIVED_AUDIO;
                case Constants.FILE_MESSAGE:
                    return RECEIVED_FILE;
                case Constants.IMAGE_MESSAGE:
                    return RECEIVED_IMAGE;
                default:
                    return RECEIVED_MESSAGE;
            }
        }
    }

    private Bitmap getImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            if (chatMessage.message_type.equals(Constants.AUDIO_MESSAGE)) {
                int num = Integer.parseInt(chatMessage.message.split("time:")[1]);
                binding.dotsImageView.setVisibility(View.VISIBLE);
                binding.textMessage.setText(String.valueOf(num));
                binding.textMessage.setMinWidth(Math.max(196, num / 4 * 196));
                binding.textMessage.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                binding.textMessage.setOnClickListener(v -> voiceMessageListener.onReceivedVoiceMessageClick(chatMessage, binding));
            } else {
                binding.textMessage.setText(chatMessage.message);
            }
            binding.textDateTime.setText(chatMessage.dateTime);
            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage) {
            if (chatMessage.message_type.equals(Constants.AUDIO_MESSAGE)) {
                int num = Integer.parseInt(chatMessage.message.split("time:")[1]);
                binding.dotsImageView.setVisibility(View.VISIBLE);
                binding.textMessage.setMinWidth(Math.max(196, num / 4 * 196));
                binding.textMessage.setText(String.valueOf(num));
                binding.textMessage.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
                binding.textMessage.setOnClickListener(v -> voiceMessageListener.onSentVoiceMessageClick(chatMessage, binding));
            } else {
                binding.textMessage.setText(chatMessage.message);
            }
            binding.textDateTime.setText(chatMessage.dateTime);

            if (chatMessage.isSending()) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
            if (chatMessage.isSendError()) {
                binding.sendError.setVisibility(View.VISIBLE);
            } else {
                binding.sendError.setVisibility(View.GONE);
            }
        }
    }

    class SentImageViewHolder extends RecyclerView.ViewHolder {
        private ItemSentImageBinding binding;

        public SentImageViewHolder(ItemSentImageBinding itemSentImageBinding) {
            super(itemSentImageBinding.getRoot());
            binding = itemSentImageBinding;
        }

        void setData(ChatMessage chatMessage) {
            binding.image.setImageBitmap(getImage(chatMessage.message));
            binding.textDateTime.setText(chatMessage.dateTime);
            if (chatMessage.isSending()) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
            if (chatMessage.isSendError()) {
                binding.sendError.setVisibility(View.VISIBLE);
            } else {
                binding.sendError.setVisibility(View.GONE);
            }
        }
    }

    class ReceivedImageViewHolder extends RecyclerView.ViewHolder {
        private ItemReceivedImageBinding binding;

        public ReceivedImageViewHolder(ItemReceivedImageBinding itemReceivedImageBinding) {
            super(itemReceivedImageBinding.getRoot());
            binding = itemReceivedImageBinding;
        }

        void setData(ChatMessage chatMessage) {
            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
            binding.image.setImageBitmap(getImage(chatMessage.message));
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }

}
