package com.example.flexbid.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity
public class SellerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "sellerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<SellerPortfolioItem> portfolioItems = new ArrayList<>();

    // Constructors
    public SellerProfile() {}

    public SellerProfile(User user) {
        this.user = user;
    }

    public SellerProfile(Integer id, User user, List<SellerPortfolioItem> portfolioItems) {
        this.id = id;
        this.user = user;
        this.portfolioItems = portfolioItems;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<SellerPortfolioItem> getPortfolioItems() {
        return portfolioItems;
    }

    public void setPortfolioItems(List<SellerPortfolioItem> portfolioItems) {
        this.portfolioItems = portfolioItems;
    }
}
