package com.example.farme.model;

public class Message {
    private String  id;
    private String  senderId;
    private String  text;
    private long    createdAt;
    private boolean read;
    private String  type; // text, image

    public Message() {}

    public String  getId()        { return id; }
    public String  getSenderId()  { return senderId; }
    public String  getText()      { return text; }
    public long    getCreatedAt() { return createdAt; }
    public boolean isRead()       { return read; }
    public String  getType()      { return type; }

    public void setId(String id)             { this.id = id; }
    public void setSenderId(String sid)      { this.senderId = sid; }
    public void setText(String text)         { this.text = text; }
    public void setCreatedAt(long time)      { this.createdAt = time; }
    public void setRead(boolean read)        { this.read = read; }
    public void setType(String type)         { this.type = type; }

    public boolean isImage() { return "image".equals(type); }
}