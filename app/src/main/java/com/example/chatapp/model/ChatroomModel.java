package com.example.chatapp.model;

import com.google.firebase.Timestamp;

import java.util.List;

public class ChatroomModel {
    String chatroomId;
    List<String> userIds;
    List<Boolean> request;
    Timestamp lastMessageTimestamp;
    String lastMessageSenderId;
    String lastMessage;

    public ChatroomModel() {
    }

    public ChatroomModel(String chatroomId, List<String> userIds, List<Boolean> request, Timestamp lastMessageTimestamp, String lastMessageSenderId) {
        this.chatroomId = chatroomId;
        this.userIds = userIds;
        this.request = request;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getchatroomId() {
        return chatroomId;
    }

    public void setchatroomId(String chatroomId) {
        this.chatroomId = chatroomId;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public List<Boolean> getRequest() {
        return request;
    }

    public void setRequest(List<Boolean> request) {
        this.request = request;
    }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
