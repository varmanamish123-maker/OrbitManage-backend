package com.orbit.portfolio.model;

import jakarta.persistence.*;

@Entity
@Table(name = "portfolios")
public class Portfolio extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String name;
    private String description;

    protected Portfolio() {}

    public Portfolio(User user, String name, String description) {
        this.user = user;
        this.name = name;
        this.description = description;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}