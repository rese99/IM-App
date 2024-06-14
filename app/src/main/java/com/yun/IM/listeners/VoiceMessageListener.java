package com.yun.IM.listeners;

import com.yun.IM.databinding.ItemContainerReceivedMessageBinding;
import com.yun.IM.databinding.ItemContainerSentMessageBinding;
import com.yun.IM.models.ChatMessage;

public interface VoiceMessageListener {
    void onSentVoiceMessageClick(ChatMessage chatMessage, ItemContainerSentMessageBinding binding);
    void onReceivedVoiceMessageClick(ChatMessage chatMessage, ItemContainerReceivedMessageBinding binding);
}
