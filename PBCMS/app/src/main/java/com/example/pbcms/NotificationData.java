package com.example.pbcms;

public class NotificationData {
    private String title;
    private String message;
    private long timestamp;
    private String senderId;
    private String type;

    public NotificationData() {
        this("", "", 0L, "", "general");
    }

    public NotificationData(String title, String message, long timestamp, String senderId, String type) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.type = type;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
