package com.example.flexbid.model;

public class NotificationMessage {
    private String title;
    private String content;
    private String receiver; // user email or ID
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getReceiver() {
		return receiver;
	}
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	public NotificationMessage(String title, String content, String receiver) {
		super();
		this.title = title;
		this.content = content;
		this.receiver = receiver;
	}

    // Constructors, Getters and Setters
}
