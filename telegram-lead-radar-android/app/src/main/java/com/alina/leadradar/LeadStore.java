package com.alina.leadradar;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class LeadStore {
    private static final String PREFS = "lead_radar";
    private static final int RULES_VERSION = 4;
    private static final String V4_CHANNELS =
            "zakaz_design\n" +
            "Designs_squad\n" +
            "Designs_job\n" +
            "designwork_vacansii\n" +
            "jobaem\n" +
            "pro_zayavki\n" +
            "GetClient\n" +
            "dsgn_vacancies\n" +
            "design_crate\n" +
            "zakazi_designers\n" +
            "vakansiidesign\n" +
            "vakansii_dlya_dizaynera\n" +
            "dsgnworkers";

    private final SharedPreferences p;

    public LeadStore(Context context) {
        p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateToV4();
    }

    private void migrateToV4() {
        if (p.getInt("rules_version", 0) >= RULES_VERSION) return;
        p.edit()
                .putInt("rules_version", RULES_VERSION)
                .putString("channels", V4_CHANNELS)
                .putBoolean("sites_enabled", true)
                .putBoolean("presentations_enabled", true)
                .putBoolean("ai_enabled", false)
                .putBoolean("consulting_enabled", false)
                .putBoolean("preview_mode", true)
                .putBoolean("enabled", false)
                .putBoolean("pending", false)
                .remove("preview_history")
                .remove("previewed_posts")
                .remove("last_preview_post")
                .remove("last_preview_user")
                .remove("last_preview_message")
                .remove("last_preview_text")
                .remove("pending_user")
                .remove("pending_message")
                .remove("pending_post")
                .remove("pending_key")
                .remove("pending_category")
                .remove("pending_budget")
                .remove("pending_since")
                .remove("pending_attempts")
                .putInt("preview_count", 0)
                .apply();
    }

    public boolean wasSent(String key) {
        return new HashSet<>(p.getStringSet("sent_posts", new HashSet<>())).contains(key);
    }

    public boolean wasPreviewed(String key) {
        return new HashSet<>(p.getStringSet("previewed_posts", new HashSet<>())).contains(key);
    }

    public synchronized void addPreview(Lead lead, String message) {
        Set<String> previewed = new HashSet<>(p.getStringSet("previewed_posts", new HashSet<>()));
        previewed.add(lead.dedupKey);

        String budget = lead.budget == null || lead.budget.trim().isEmpty() ? "не указан" : lead.budget.trim();
        String original = cleanExcerpt(lead.text, 700);
        String entry = "[" + categoryName(lead.category) + "] @" + lead.username
                + "\nБюджет: " + budget
                + "\nИсходный пост: " + lead.postUrl
                + "\n\nКОНКРЕТНОЕ ЗАДАНИЕ:\n" + original
                + "\n\nСообщение бота:\n" + message;

        String old = p.getString("preview_history", "");
        String combined = old.isEmpty() ? entry : entry + "\n\n────────────\n\n" + old;
        combined = trimHistory(combined, 20);

        p.edit()
                .putStringSet("previewed_posts", previewed)
                .putString("preview_history", combined)
                .putString("last_preview_post", lead.postUrl)
                .putString("last_preview_user", lead.username)
                .putString("last_preview_message", message)
                .putString("last_preview_text", original)
                .putInt("preview_count", p.getInt("preview_count", 0) + 1)
                .apply();
    }

    public String previewHistory() { return p.getString("preview_history", ""); }
    public String lastPreviewPost() { return p.getString("last_preview_post", ""); }
    public int previewCount() { return p.getInt("preview_count", 0); }

    public void clearPreviewHistory() {
        p.edit()
                .remove("preview_history")
                .remove("previewed_posts")
                .remove("last_preview_post")
                .remove("last_preview_user")
                .remove("last_preview_message")
                .remove("last_preview_text")
                .putInt("preview_count", 0)
                .apply();
    }

    public synchronized void setPending(Lead lead, String message) {
        p.edit()
                .putBoolean("pending", true)
                .putString("pending_user", lead.username)
                .putString("pending_message", message)
                .putString("pending_post", lead.postUrl)
                .putString("pending_key", lead.dedupKey)
                .putString("pending_category", lead.category.name())
                .putString("pending_budget", lead.budget == null ? "" : lead.budget)
                .putLong("pending_since", System.currentTimeMillis())
                .putInt("pending_attempts", 0)
                .apply();
    }

    public boolean hasPending() { return p.getBoolean("pending", false); }
    public String pendingUser() { return p.getString("pending_user", ""); }
    public String pendingMessage() { return p.getString("pending_message", ""); }
    public String pendingPost() { return p.getString("pending_post", ""); }
    public String pendingKey() { return p.getString("pending_key", pendingPost()); }
    public long pendingSince() { return p.getLong("pending_since", 0L); }
    public int pendingAttempts() { return p.getInt("pending_attempts", 0); }
    public void bumpPendingAttempt() { p.edit().putInt("pending_attempts", pendingAttempts() + 1).apply(); }

    public synchronized void markPendingSent() {
        String key = pendingKey();
        Set<String> sent = new HashSet<>(p.getStringSet("sent_posts", new HashSet<>()));
        if (key != null && !key.isEmpty()) sent.add(key);
        int count = p.getInt("sent_count", 0) + 1;
        p.edit()
                .putStringSet("sent_posts", sent)
                .putInt("sent_count", count)
                .putLong("last_send_at", System.currentTimeMillis())
                .putBoolean("pending", false)
                .remove("pending_user").remove("pending_message").remove("pending_post").remove("pending_key")
                .remove("pending_category").remove("pending_budget").remove("pending_since").remove("pending_attempts")
                .apply();
    }

    public synchronized void clearPending() {
        p.edit().putBoolean("pending", false)
                .remove("pending_user").remove("pending_message").remove("pending_post").remove("pending_key")
                .remove("pending_category").remove("pending_budget").remove("pending_since").remove("pending_attempts")
                .apply();
    }

    public int sentCount() { return p.getInt("sent_count", 0); }
    public long lastSendAt() { return p.getLong("last_send_at", 0L); }

    public boolean enabled() { return p.getBoolean("enabled", false); }
    public void setEnabled(boolean enabled) { p.edit().putBoolean("enabled", enabled).apply(); }

    public boolean previewMode() { return p.getBoolean("preview_mode", true); }
    public void setPreviewMode(boolean enabled) { p.edit().putBoolean("preview_mode", enabled).apply(); }

    public String channels() { return p.getString("channels", V4_CHANNELS); }
    public void setChannels(String channels) { p.edit().putString("channels", channels).apply(); }

    public int scanMinutes() { return Math.max(2, p.getInt("scan_minutes", 5)); }
    public void setScanMinutes(int minutes) { p.edit().putInt("scan_minutes", Math.max(2, minutes)).apply(); }

    public int sendPauseSeconds() { return Math.max(30, p.getInt("send_pause_seconds", 90)); }
    public void setSendPauseSeconds(int seconds) { p.edit().putInt("send_pause_seconds", Math.max(30, seconds)).apply(); }

    public String profileUrl() {
        return p.getString("profile_url", "https://alinavasileva-cco.github.io/studio/");
    }
    public void setProfileUrl(String url) { p.edit().putString("profile_url", url == null ? "" : url.trim()).apply(); }

    public boolean sitesEnabled() { return p.getBoolean("sites_enabled", true); }
    public boolean presentationsEnabled() { return p.getBoolean("presentations_enabled", true); }
    public boolean aiEnabled() { return false; }
    public boolean consultingEnabled() { return false; }

    public void setCategories(boolean sites, boolean presentations) {
        p.edit()
                .putBoolean("sites_enabled", sites)
                .putBoolean("presentations_enabled", presentations)
                .putBoolean("ai_enabled", false)
                .putBoolean("consulting_enabled", false)
                .apply();
    }

    private static String cleanExcerpt(String text, int limit) {
        if (text == null) return "";
        String s = text.replaceAll("\\s+", " ").trim();
        return s.length() <= limit ? s : s.substring(0, limit - 1).trim() + "…";
    }

    private static String categoryName(Lead.Category c) {
        if (c == Lead.Category.SITE) return "САЙТ / ЛЕНДИНГ";
        if (c == Lead.Category.PRESENTATION) return "ПРЕЗЕНТАЦИЯ";
        return "ИСКЛЮЧЕНО";
    }

    private static String trimHistory(String history, int maxEntries) {
        String separator = "\n\n────────────\n\n";
        String[] entries = history.split(java.util.regex.Pattern.quote(separator));
        if (entries.length <= maxEntries) return history;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < maxEntries; i++) {
            if (i > 0) b.append(separator);
            b.append(entries[i]);
        }
        return b.toString();
    }
}
