package com.yun.IM.models;

public class ChatMessage {
    public String senderId;
    public String receiverId;
    public String message;
    public String dateTime;
    public long dateObject;
    public String conversionId;
    public String conversionName;
    public String conversionImage;
    public String message_type;
    public String AES;
    private boolean sending;
    private boolean sendError;

    @Override
    public String toString() {
        return "ChatMessage{" + "senderId='" + senderId + '\'' + ", receiverId='" + receiverId + '\'' + ", message='" + message + '\'' + ", dateTime='" + dateTime + '\'' + ", dateObject=" + dateObject + ", conversionId='" + conversionId + '\'' + ", conversionName='" + conversionName + '\'' + ", conversionImage='" + conversionImage + '\'' + '}';
    }

    public boolean isSending() {
        return sending;
    }

    public void setSending(boolean sending) {
        this.sending = sending;
    }

    public boolean isSendError() {
        return sendError;
    }

    public void setSendError(boolean sendError) {
        this.sendError = sendError;
    }
}
