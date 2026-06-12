package com.example.flexbid.dto;

import java.util.Set;

public class UserDTO {
    private int id;
    private String username;
    private String email;
    private Long contact;
    private Set<String> roles;
    public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	private boolean emailVerified;

    public boolean isEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public UserDTO(int id, String username, String email, Long contact, Set<String> roles, boolean emailverified) {
		// TODO Auto-generated constructor stub
    	this.id=id;
    	this.username=username;
    	this.email=email;
    	this.contact=contact;
    	this.roles=roles;
    	this.emailVerified=emailverified;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Long getContact() {
		return contact;
	}

	public void setContact(Long contact) {
		this.contact = contact;
	}

}
