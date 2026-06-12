package com.example.flexbid.model;

import java.util.Set;

import jakarta.persistence.Transient;

public class RegisterRequest {
	 private String username;
	    private String email;
	    private Long contact;
	    private String password;
	    private Set<String> roles;
  
	    @Transient
	    private String confirmPassword;

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

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public Set<String> getRoles() {
			return roles;
		}

		public void setRoles(Set<String> roles) {
			this.roles = roles;
		}

		public String getConfirmPassword() {
			return confirmPassword;
		}

		public void setConfirmPassword(String confirmPassword) {
			this.confirmPassword = confirmPassword;
		}
}
