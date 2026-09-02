package com.alina.leadradar;

public final class Lead {
    public enum Category { SITE, PRESENTATION, AI, CONSULTING }

    public final Category category;
    public final String channel;
    public final String postUrl;
    public final String text;
    public final String username;
    public final String budget;
    public final String dedupKey;

    public Lead(Category category, String channel, String postUrl, String text, String username, String budget, String dedupKey) {
        this.category = category;
        this.channel = channel;
        this.postUrl = postUrl;
        this.text = text;
        this.username = username;
        this.budget = budget;
        this.dedupKey = dedupKey;
    }
}
