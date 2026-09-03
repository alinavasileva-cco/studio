package com.alina.leadradar;

import org.json.JSONObject;

public final class LeadRecord {
    public final String id;
    public final Lead.Category category;
    public final String username;
    public final String postUrl;
    public final String budget;
    public final String task;
    public final String message;

    public LeadRecord(String id, Lead.Category category, String username, String postUrl,
                      String budget, String task, String message) {
        this.id = id == null ? "" : id;
        this.category = category;
        this.username = username == null ? "" : username;
        this.postUrl = postUrl == null ? "" : postUrl;
        this.budget = budget == null ? "" : budget;
        this.task = task == null ? "" : task;
        this.message = message == null ? "" : message;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("category", category == null ? "" : category.name());
            o.put("username", username);
            o.put("postUrl", postUrl);
            o.put("budget", budget);
            o.put("task", task);
            o.put("message", message);
        } catch (Exception ignored) {}
        return o;
    }

    public static LeadRecord fromJson(JSONObject o) {
        if (o == null) return null;
        try {
            Lead.Category c = Lead.Category.valueOf(o.optString("category", ""));
            return new LeadRecord(
                    o.optString("id", ""), c, o.optString("username", ""),
                    o.optString("postUrl", ""), o.optString("budget", ""),
                    o.optString("task", ""), o.optString("message", "")
            );
        } catch (Exception e) {
            return null;
        }
    }
}
