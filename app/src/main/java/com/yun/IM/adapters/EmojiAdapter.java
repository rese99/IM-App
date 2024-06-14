package com.yun.IM.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yun.IM.databinding.ItemEmojiBinding;
import com.yun.IM.models.Emoji;

import java.util.List;

public class EmojiAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Emoji> emojiList;

    public EmojiAdapter(List<Emoji> emojiList) {
        this.emojiList = emojiList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Item_emoji(ItemEmojiBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((Item_emoji) holder).setData(emojiList.get(position));
    }

    @Override
    public int getItemCount() {
        return emojiList.size();
    }

    static class Item_emoji extends RecyclerView.ViewHolder {

        private final ItemEmojiBinding binding;

        public Item_emoji(ItemEmojiBinding itemEmojiBinding) {
            super(itemEmojiBinding.getRoot());
            binding = itemEmojiBinding;
        }

        void setData(Emoji emoji) {
            binding.emoji1.setText(emoji.emoji1);
            binding.emoji2.setText(emoji.emoji2);
            binding.emoji3.setText(emoji.emoji3);
            binding.emoji4.setText(emoji.emoji4);
            binding.emoji5.setText(emoji.emoji5);
            binding.emoji6.setText(emoji.emoji6);
            binding.emoji7.setText(emoji.emoji7);
            binding.emoji8.setText(emoji.emoji8);
            binding.emoji9.setText(emoji.emoji9);
            binding.emoji10.setText(emoji.emoji10);
        }
    }
}
