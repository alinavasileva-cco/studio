package com.alina.leadradar;

public final class Lead {
    public enum Category { SITE, PRESENTATION, CONSULTING }

    public final Category category;
    public final String channel;
    public final String postUrl;
    public final String text;
    public final String username;
    public final String budget;

    public Lead(Category category, String channel, String postUrl, String text, String username, String budget) {
        this.category = category;
        this.channel = channel;
        this.postUrl = postUrl;
        this.text = text;
        this.username = username;
        this.budget = budget;
    }
}
