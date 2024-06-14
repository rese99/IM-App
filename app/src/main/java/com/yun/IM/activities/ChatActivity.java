package com.yun.IM.activities;

import static com.yun.IM.utilites.Constants.AUDIO_DIR;
import static com.yun.IM.utilites.Constants.AUDIO_FILE;
import static com.yun.IM.utilites.Constants.AUDIO_TIME;
import static com.yun.IM.utilites.Constants.KEY_PERMISSIONS;
import static com.yun.IM.utilites.Constants.conversations;
import static com.yun.IM.utilites.Constants.dbAdapter;
import static com.yun.IM.utilites.Constants.emojiList;
import static com.yun.IM.utilites.Constants.executorService;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.yanzhenjie.recyclerview.SwipeRecyclerView;
import com.yun.IM.Application.MyApplication;
import com.yun.IM.Fragments.messageFragment;
import com.yun.IM.adapters.ChatAdapter;
import com.yun.IM.adapters.EmojiAdapter;
import com.yun.IM.databinding.ActivityChatBinding;
import com.yun.IM.databinding.ItemContainerReceivedMessageBinding;
import com.yun.IM.databinding.ItemContainerSentMessageBinding;
import com.yun.IM.listeners.VoiceMessageListener;
import com.yun.IM.models.ChatMessage;
import com.yun.IM.models.Friends;
import com.yun.IM.models.Message;
import com.yun.IM.models.User;
import com.yun.IM.netWork.WebSocketService;
import com.yun.IM.utilites.Constants;
import com.yun.IM.utilites.DialogManager;
import com.yun.IM.utilites.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.EasyPermissions;

public class ChatActivity extends AppCompatActivity implements VoiceMessageListener {
    private static final int DISTANCE_Y_CANCEL = 50;
    public static SwipeRecyclerView swipeRecyclerView;
    public static ChatAdapter chatAdapter;
    private static ActivityChatBinding binding;
    int audioSource = MediaRecorder.AudioSource.MIC;
    int sampleRateInHz = 44100;
    int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecord;
    private byte[] buffer;
    private FileOutputStream outputStream;
    private ByteArrayOutputStream stream;
    private WebSocketService webSocketService;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private PreferenceManager preferenceManager;
    private final WebSocketService.WebSocketCallback webSocketCallback = message -> {
        runOnUiThread(() -> {
            messageFragment.UpdateDatabase(message);
            MessageHandle(false, message);
        });
    };
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            Message message = new Message();
                            message.message = encodeImage(bitmap);
                            message.message_type = Constants.IMAGE_MESSAGE;
                            MessageHandle(true, message);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );
    private final ActivityResultLauncher<Intent> takePhoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null && result.getData().getExtras() != null) {
                        Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                        Message message = new Message();
                        message.message = encodeImage(bitmap);
                        message.message_type = Constants.IMAGE_MESSAGE;
                        MessageHandle(true, message);
                    }
                }
            }
    );
    private long StartRecordTime, StopRecordTime;
    private DialogManager dialogManager;
    private volatile boolean IsRecorder;
    private AudioTrack audioTrack;
    private int bufferSizeInBytes;
    private int bufferSize;
    private AnimationDrawable anim;
    private EmojiAdapter emojiAdapter;

    /**
     * emoji输入
     *
     * @param text
     */
    public static void show(String text) {
        text = binding.inputMessage.getText().toString() + text;
        binding.inputMessage.setText(text);
        binding.inputMessage.requestFocus();
        binding.inputMessage.setSelection(text.length());
    }

    public void MessageHandle(boolean flag, Message message) {
        ChatMessage chatMessage = new ChatMessage();
        if (flag) {
            message.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
            message.receiverId = receiverUser.id;
            message.timestamp = String.valueOf(new Date().getTime());
            if (webSocketService.send(message, receiverUser.AES)) {
                messageFragment.UpdateDatabase(message);
                dbAdapter.update(new Friends(message.senderId, message.receiverId, message.message, Long.parseLong(message.timestamp), message.message_type), Constants.KEY_FRIENDS);
            } else {
                chatMessage.setSending(true);
            }
        }
        chatMessage.senderId = message.senderId;
        chatMessage.receiverId = message.receiverId;
        chatMessage.message = message.message;
        chatMessage.dateTime = getReadableDateTime(message.timestamp);
        chatMessage.dateObject = Long.parseLong(message.timestamp);
        chatMessage.message_type = message.message_type;
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
        binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size());
        binding.chatRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication myApplication = (MyApplication) getApplication();
        webSocketService = myApplication.getWebSocketService();
        dialogManager = new DialogManager(this);
        webSocketService.setWebSocketCallback(webSocketCallback);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        loadReceiverDetails();
        setListeners();
        init();
        GetLocalMessages();
        swipeRecyclerView = binding.chatRecyclerView;
        setContentView(binding.getRoot());
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        emojiAdapter = new EmojiAdapter(emojiList);
        chatAdapter = new ChatAdapter(chatMessages, getBitmapFromEncodeString(receiverUser.image), preferenceManager.getString(Constants.KEY_USER_ID), this);
        binding.chatRecyclerView.setAdapter(chatAdapter);
        binding.emojiView.setAdapter(emojiAdapter);
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_OUT_MONO, audioFormat);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, AudioFormat.CHANNEL_OUT_MONO, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
    }

    private void GetLocalMessages() {
        List<Message> messages = dbAdapter.queryAllData(preferenceManager.getString(Constants.KEY_USER_ID), receiverUser.id, Constants.KEY_COLLECTION_CHAT);
        if (messages != null) {
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.senderId = messages.get(i).senderId;
                chatMessage.receiverId = messages.get(i).receiverId;
                chatMessage.message = messages.get(i).message;
                chatMessage.dateTime = getReadableDateTime(messages.get(i).timestamp);
                chatMessage.dateObject = Long.parseLong(messages.get(i).timestamp);
                chatMessage.message_type = messages.get(i).message_type;
                chatMessages.add(chatMessage);
            }
            Collections.sort(chatMessages, Comparator.comparing(obj -> obj.dateObject));
            chatAdapter.notifyDataSetChanged();
            binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size());
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
    }

    private void sendMessage() {
        Message message = new Message();
        message.message = binding.inputMessage.getText().toString();
        message.message_type = Constants.Text_MESSAGE;
        MessageHandle(true, message);
        binding.inputMessage.setText(null);
    }

    private Bitmap getBitmapFromEncodeString(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.send.setOnClickListener(v -> sendMessage());
        binding.voice.setOnClickListener(v -> {
            binding.voice.setVisibility(View.GONE);
            binding.inputMessage.setVisibility(View.GONE);
            binding.keyboard.setVisibility(View.VISIBLE);
            binding.voiceMessage.setVisibility(View.VISIBLE);
            binding.fragmentMenu.setVisibility(View.GONE);
            binding.fragmentEmoji.setVisibility(View.GONE);

        });
        binding.keyboard.setOnClickListener(v -> {
            binding.keyboard.setVisibility(View.GONE);
            binding.voiceMessage.setVisibility(View.GONE);
            binding.voice.setVisibility(View.VISIBLE);
            binding.inputMessage.setVisibility(View.VISIBLE);
        });
        binding.imageInfo.setOnClickListener(v -> {
            chatMessages.clear();
            chatAdapter.notifyDataSetChanged();
            dbAdapter.deleteMessage(preferenceManager.getString(Constants.KEY_USER_ID), receiverUser.id, Constants.KEY_COLLECTION_CHAT);
            for (int i = 0; i < conversations.size(); i++) {
                if (conversations.get(i).conversionId.equals(receiverUser.id)) {
                    conversations.remove(i);
                    break;
                }
            }
            Friends friends = new Friends();
            friends.userId = preferenceManager.getString(Constants.KEY_USER_ID);
            friends.id = receiverUser.id;
            friends.lastMessage = null;
            friends.dateObject = new Date().getTime();
            friends.message_type = Constants.Text_MESSAGE;
            dbAdapter.update(friends, Constants.KEY_FRIENDS);
        });
        binding.face.setOnClickListener(v -> {
            binding.fragmentMenu.setVisibility(View.GONE);
            binding.fragmentEmoji.setVisibility(View.VISIBLE);
            binding.inputMessage.requestFocus();
            binding.inputMessage.setSelection(binding.inputMessage.getText().length());
        });
        binding.add.setOnClickListener(v -> {
            binding.keyboard.setVisibility(View.GONE);
            binding.voiceMessage.setVisibility(View.GONE);
            binding.fragmentEmoji.setVisibility(View.GONE);
            binding.fragmentMenu.setVisibility(View.VISIBLE);
            binding.voice.setVisibility(View.VISIBLE);
            binding.inputMessage.setVisibility(View.VISIBLE);
            binding.inputMessage.requestFocus();
            binding.inputMessage.setSelection(binding.inputMessage.getText().length());
        });
        binding.voiceMessage.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    IsRecorder = true;
                    startRecord();
                    stopPlaying();
                    dialogManager.showRecordingDialog();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (wantToCancel(x, y)) {
                        dialogManager.wantToCancel();
                    } else {
                        dialogManager.recording();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    stopRecord();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    RecordError();
                    break;
                default:
                    break;
            }
            return true;
        });
        binding.inputMessage.setOnClickListener(v -> {
            binding.fragmentMenu.setVisibility(View.GONE);
            binding.fragmentEmoji.setVisibility(View.GONE);
        });
        binding.inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    binding.add.setVisibility(View.VISIBLE);
                    binding.send.setVisibility(View.GONE);
                } else {
                    binding.add.setVisibility(View.GONE);
                    binding.send.setVisibility(View.VISIBLE);
                }
            }
        });
        binding.photo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        binding.camera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            takePhoto.launch(intent);
        });
        binding.backSpace.setOnClickListener(v -> {
            int keyCode = KeyEvent.KEYCODE_DEL;
            KeyEvent keyEventDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            KeyEvent keyEventUp = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
            binding.inputMessage.onKeyDown(keyCode, keyEventDown);
            binding.inputMessage.onKeyUp(keyCode, keyEventUp);
        });
        binding.chatRecyclerView.setOnTouchListener((v, event) -> {
            if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
                binding.fragmentEmoji.setVisibility(View.GONE);
                binding.fragmentMenu.setVisibility(View.GONE);
            }
            return false;
        });
    }

    private String getReadableDateTime(String longtime) {
        long timestamp = Long.parseLong(longtime);
        Date date = new Date(timestamp);
        return new SimpleDateFormat("MMMM dd, yyyy - HH:mm", Locale.getDefault()).format(date);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webSocketService.setWebSocketCallback(webSocketCallback);
        preferenceManager.putString(Constants.KEY_AVAILABILITY, "1");
    }

    @Override
    protected void onPause() {
        super.onPause();
        preferenceManager.putString(Constants.KEY_AVAILABILITY, "0");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RecordError();
        audioTrack.release();
    }

    /**
     * 录音按钮位置监听
     *
     * @param x
     * @param y
     * @return
     */
    private boolean wantToCancel(int x, int y) {
        if (x < 0 || x > binding.voiceMessage.getWidth()) {// 判断是否在左边，右边，上边，下边
            return true;
        }
        if (y < -DISTANCE_Y_CANCEL || y > binding.voiceMessage.getHeight() + DISTANCE_Y_CANCEL) {
            return true;
        }
        return false;
    }

    /**
     * 开始录音
     */
    private void startRecord() {
        executorService.submit(() -> {
            try {
                AUDIO_FILE = new File(AUDIO_DIR, receiverUser.id + System.currentTimeMillis());
                AUDIO_FILE.createNewFile();
                if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    EasyPermissions.requestPermissions(ChatActivity.this, "获取应用所需权限", KEY_PERMISSIONS, Manifest.permission.RECORD_AUDIO);
                } else {
                    audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
                    buffer = new byte[bufferSizeInBytes];
                    StartRecordTime = System.currentTimeMillis();
                    audioRecord.startRecording();
                    outputStream = new FileOutputStream(AUDIO_FILE);
                    stream = new ByteArrayOutputStream();
                    int bytesRead = 0;
                    while (IsRecorder) {
                        bytesRead = audioRecord.read(buffer, 0, bufferSizeInBytes);
                        if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION || bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                            break;
                        }
                        outputStream.write(buffer, 0, bytesRead);
                        stream.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 停止录音
     */
    private void stopRecord() {
        IsRecorder = false;
        audioRecord.stop();
        audioRecord.release();
        if (dialogManager.IsShow()) {
            AUDIO_FILE.delete();
            dialogManager.dimissDialog();
        } else {
            StopRecordTime = System.currentTimeMillis();
            AUDIO_TIME = (int) ((StopRecordTime - StartRecordTime) / 1000);
            if (AUDIO_TIME < 1) {
                dialogManager.tooShort();
                AUDIO_FILE.delete();
            } else if (AUDIO_TIME > 60) {
                dialogManager.tooLong();
                AUDIO_FILE.delete();
            } else {
                String base64String = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT) + "time:" + AUDIO_TIME;
                Message message = new Message();
                message.message = base64String;
                message.message_type = Constants.AUDIO_MESSAGE;
                MessageHandle(true, message);
                dialogManager.dimissDialog();
            }
        }
    }

    /**
     * 权限获取
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 错误处理
     */
    private void RecordError() {
        IsRecorder = false;
        binding.voiceMessage.setText("按住说话");
        if (audioRecord != null) {
            audioRecord.release();
        }
        dialogManager.dimissDialog();
    }

    /**
     * 播放录音
     *
     * @param filePath
     */
    private void playAudio(String filePath) {
        File file = new File(filePath);
        byte[] buffer = new byte[bufferSize];
        try {
            FileInputStream stream = new FileInputStream(file);
            audioTrack.play();
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                audioTrack.write(buffer, 0, bytesRead);
            }
            animClean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止播放
     */
    private void stopPlaying() {
        if (audioTrack != null) {
            audioTrack.stop();
            animClean();
        }
    }

    @Override
    public void onBackPressed() {
        stopPlaying();
        animClean();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onSentVoiceMessageClick(ChatMessage chatMessage, ItemContainerSentMessageBinding binding) {
        animClean();
        anim = (AnimationDrawable) binding.dotsImageView.getBackground();
        executorService.execute(() -> {
            if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                stopPlaying();
            } else {
                String filePath = chatMessage.message.split("time")[0];
                anim.start();
                playAudio(filePath);
            }
        });
    }

    @Override
    public void onReceivedVoiceMessageClick(ChatMessage chatMessage, ItemContainerReceivedMessageBinding binding) {
        animClean();
        anim = (AnimationDrawable) binding.dotsImageView.getBackground();
        executorService.execute(() -> {
            if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                stopPlaying();
            } else {
                String filePath = chatMessage.message.split("time")[0];
                anim.start();
                playAudio(filePath);
            }
        });
    }

    private void animClean() {
        if (anim != null) {
            runOnUiThread(() -> {
                anim.selectDrawable(0);
                anim.stop();
            });
        }
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth;
        int previewHeight;
        if (bitmap.getWidth() <= 600) {
            previewWidth = bitmap.getWidth();
            previewHeight = bitmap.getHeight();
        } else {
            previewWidth = 600;
            previewHeight = (int) ((float) bitmap.getHeight() / bitmap.getWidth() * 600);
        }
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
}