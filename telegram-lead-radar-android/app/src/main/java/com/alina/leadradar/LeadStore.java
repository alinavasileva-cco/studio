package com.alina.leadradar;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LeadStore {
    private static final String PREFS = "lead_radar";
    private static final int RULES_VERSION = 11;
    private static final String RESULTS_FILE = "manual_lead_results_v11.json";
    private static final String OLD_RESULTS_FILE = "manual_lead_results.txt";
    private static final String DEFAULT_PRESENTATION_PORTFOLIO =
            "https://drive.google.com/drive/folders/1e7F1UzYrb0wCvmVzVuNK_htp1Kd5JHQf";

    private static final String DEFAULT_CHANNELS =
            "rueventjob\n"
            + "mskeventjob\n"
            + "freelancetaverna\n"
            + "zakaz_design\n"
            + "zakazi_designers\n"
            + "Designs_job\n"
            + "design_freevacancies\n"
            + "SearchDesignerr\n"
            + "free_Design1\n"
            + "jobs_designer\n"
            + "webdesign_jobs\n"
            + "rabota_design\n"
            + "job4designer\n"
            + "designerworkchat\n"
            + "vakansii_dlya_dizaynera\n"
            + "designer_work\n"
            + "workindesign\n"
            + "designizer\n"
            + "rabotadesign\n"
            + "GetJob_design\n"
            + "vsedizaineri\n"
            + "pro_zayavki\n"
            + "design_crate\n"
            + "Designs_squad\n"
            + "dsgnworkers\n"
            + "GetClient\n"
            + "job_webdesign\n"
            + "search_zakaz\n"
            + "design_vacancy\n"
            + "dsgn_vacancies\n"
            + "design_jobs_uxui\n"
            + "designodromo\n"
            + "dprofilejob\n"
            + "vacancies_dsgn\n"
            + "designer_vacancies\n"
            + "fordesigner\n"
            + "design_careers\n"
            + "designhunters\n"
            + "designer_ru_work\n"
            + "designer_ru\n"
            + "freelance_vacancii\n"
            + "frilanser_vacansii\n"
            + "proffreelancee\n"
            + "ALLW0RK\n"
            + "freetasks\n"
            + "freeworkfeed\n"
            + "jobaem\n"
            + "job_developer\n"
            + "jobs_for_it";

    private final Context appContext;
    private final SharedPreferences p;

    public LeadStore(Context context) {
        appContext = context.getApplicationContext();
        p = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateToV11();
    }

    private void migrateToV11() {
        if (p.getInt("rules_version", 0) >= RULES_VERSION) return;
        String existingChannels = p.getString("channels", DEFAULT_CHANNELS);
        if (existingChannels == null || existingChannels.trim().isEmpty()) existingChannels = DEFAULT_CHANNELS;
        String siteProfile = p.getString("profile_url", "https://alinavasileva-cco.github.io/studio/");
        if (siteProfile == null) siteProfile = "";
        p.edit()
                .putInt("rules_version", RULES_VERSION)
                .putString("channels", existingChannels)
                .putString("profile_url", siteProfile)
                .putString("presentation_portfolio_url", DEFAULT_PRESENTATION_PORTFOLIO)
                .putInt("lookback_days", 3)
                .putBoolean("sites_enabled", true)
                .putBoolean("presentations_enabled", true)
                .putBoolean("running", false)
                .putInt("checked_channels", 0)
                .putInt("total_channels", 0)
                .putInt("run_found", 0)
                .putInt("preview_count", 0)
                .putInt("diag_posts", 0)
                .putInt("diag_blocks", 0)
                .putInt("diag_candidates", 0)
                .putInt("diag_no_contact", 0)
                .putInt("diag_errors", 0)
                .remove("previewed_posts")
                .remove("selected_ids")
                .remove("last_preview_post")
                .remove("last_preview_user")
                .remove("last_preview_message")
                .remove("last_preview_text")
                .apply();
        try { new File(appContext.getFilesDir(), OLD_RESULTS_FILE).delete(); } catch (Exception ignored) {}
        try { resultsFile().delete(); } catch (Exception ignored) {}
    }

    public boolean wasSent(String key) { return false; }

    public boolean wasPreviewed(String key) {
        return new HashSet<>(p.getStringSet("previewed_posts", new HashSet<>())).contains(key);
    }

    public synchronized void addPreview(Lead lead, String message) {
        Set<String> previewed = new HashSet<>(p.getStringSet("previewed_posts", new HashSet<>()));
        previewed.add(lead.dedupKey);

        List<LeadRecord> list = readRecords();
        String budget = lead.budget == null ? "" : lead.budget.trim();
        String original = cleanExcerpt(lead.text, 1800);
        list.add(0, new LeadRecord(
                lead.dedupKey,
                lead.category,
                lead.username,
                lead.postUrl,
                budget,
                original,
                message == null ? "" : message.trim()
        ));
        writeRecords(list);

        p.edit()
                .putStringSet("previewed_posts", previewed)
                .putString("last_preview_post", lead.postUrl)
                .putString("last_preview_user", lead.username)
                .putString("last_preview_message", message == null ? "" : message)
                .putString("last_preview_text", original)
                .putInt("preview_count", list.size())
                .putInt("run_found", p.getInt("run_found", 0) + 1)
                .apply();
    }

    public synchronized List<LeadRecord> resultRecords() {
        return new ArrayList<>(readRecords());
    }

    public synchronized void setSelected(String id, boolean selected) {
        if (id == null || id.isEmpty()) return;
        Set<String> ids = new HashSet<>(p.getStringSet("selected_ids", new HashSet<>()));
        if (selected) ids.add(id); else ids.remove(id);
        p.edit().putStringSet("selected_ids", ids).apply();
    }

    public boolean isSelected(String id) {
        if (id == null || id.isEmpty()) return false;
        return new HashSet<>(p.getStringSet("selected_ids", new HashSet<>())).contains(id);
    }

    public int selectedCount() {
        return new HashSet<>(p.getStringSet("selected_ids", new HashSet<>())).size();
    }

    public synchronized void addDiagnostics(int posts, int blocks, int candidates, int noContact) {
        p.edit()
                .putInt("diag_posts", p.getInt("diag_posts", 0) + Math.max(0, posts))
                .putInt("diag_blocks", p.getInt("diag_blocks", 0) + Math.max(0, blocks))
                .putInt("diag_candidates", p.getInt("diag_candidates", 0) + Math.max(0, candidates))
                .putInt("diag_no_contact", p.getInt("diag_no_contact", 0) + Math.max(0, noContact))
                .apply();
    }

    public void addReadError() { p.edit().putInt("diag_errors", p.getInt("diag_errors", 0) + 1).apply(); }
    public int diagnosticPosts() { return p.getInt("diag_posts", 0); }
    public int diagnosticBlocks() { return p.getInt("diag_blocks", 0); }
    public int diagnosticCandidates() { return p.getInt("diag_candidates", 0); }
    public int diagnosticNoContact() { return p.getInt("diag_no_contact", 0); }
    public int diagnosticErrors() { return p.getInt("diag_errors", 0); }

    public String previewHistory() {
        StringBuilder out = new StringBuilder();
        for (LeadRecord r : readRecords()) {
            if (out.length() > 0) out.append("\n\n━━━━━━━━━━━━━━━━━━\n\n");
            out.append("[").append(categoryName(r.category)).append("]\n")
                    .append("Telegram-контакт: ").append(contactUrl(r.username)).append("\n")
                    .append("Бюджет: ").append(r.budget == null || r.budget.isEmpty() ? "не указан" : r.budget).append("\n")
                    .append("Исходный пост: ").append(r.postUrl).append("\n\n")
                    .append("ЗАДАЧА:\n").append(r.task).append("\n\n")
                    .append("ПОДГОТОВЛЕННЫЙ ОТКЛИК:\n").append(r.message);
        }
        return out.toString();
    }

    public String lastPreviewPost() { return p.getString("last_preview_post", ""); }
    public String lastPreviewUser() { return p.getString("last_preview_user", ""); }
    public String lastPreviewMessage() { return p.getString("last_preview_message", ""); }
    public int previewCount() { return p.getInt("preview_count", 0); }

    public static String contactUrl(String username) {
        if (username == null || username.trim().isEmpty()) return "";
        String u = username.trim();
        while (u.startsWith("@")) u = u.substring(1);
        return "https://t.me/" + u;
    }

    public synchronized void clearPreviewHistory() {
        try { resultsFile().delete(); } catch (Exception ignored) {}
        p.edit()
                .remove("previewed_posts")
                .remove("selected_ids")
                .remove("last_preview_post")
                .remove("last_preview_user")
                .remove("last_preview_message")
                .remove("last_preview_text")
                .putInt("preview_count", 0)
                .putInt("run_found", 0)
                .apply();
    }

    public String channels() { return p.getString("channels", DEFAULT_CHANNELS); }
    public void setChannels(String channels) { p.edit().putString("channels", channels == null ? "" : channels.trim()).apply(); }

    public int lookbackDays() {
        int d = p.getInt("lookback_days", 3);
        return (d == 1 || d == 3 || d == 7 || d == 14) ? d : 3;
    }

    public void setLookbackDays(int days) {
        int d = (days == 1 || days == 3 || days == 7 || days == 14) ? days : 3;
        p.edit().putInt("lookback_days", d).apply();
    }

    public String profileUrl() { return p.getString("profile_url", "https://alinavasileva-cco.github.io/studio/"); }
    public void setProfileUrl(String url) { p.edit().putString("profile_url", url == null ? "" : url.trim()).apply(); }
    public String presentationPortfolioUrl() { return p.getString("presentation_portfolio_url", DEFAULT_PRESENTATION_PORTFOLIO); }
    public void setPresentationPortfolioUrl(String url) { p.edit().putString("presentation_portfolio_url", url == null ? "" : url.trim()).apply(); }

    public boolean sitesEnabled() { return p.getBoolean("sites_enabled", true); }
    public boolean presentationsEnabled() { return p.getBoolean("presentations_enabled", true); }
    public boolean aiEnabled() { return false; }
    public boolean consultingEnabled() { return false; }

    public void setCategories(boolean sites, boolean presentations) {
        p.edit().putBoolean("sites_enabled", sites).putBoolean("presentations_enabled", presentations).apply();
    }

    public synchronized void beginRun(int totalChannels) {
        clearPreviewHistory();
        p.edit()
                .putBoolean("running", true)
                .putInt("checked_channels", 0)
                .putInt("total_channels", totalChannels)
                .putInt("run_found", 0)
                .putInt("preview_count", 0)
                .putInt("diag_posts", 0)
                .putInt("diag_blocks", 0)
                .putInt("diag_candidates", 0)
                .putInt("diag_no_contact", 0)
                .putInt("diag_errors", 0)
                .putString("current_channel", "")
                .putLong("run_started_at", System.currentTimeMillis())
                .remove("run_finished_at")
                .apply();
    }

    public void setProgress(int checked, int total, String currentChannel) {
        p.edit().putInt("checked_channels", checked).putInt("total_channels", total)
                .putString("current_channel", currentChannel == null ? "" : currentChannel).apply();
    }

    public synchronized void finishRun() {
        p.edit().putBoolean("running", false).putString("current_channel", "")
                .putLong("run_finished_at", System.currentTimeMillis()).apply();
    }

    public void requestStop() { p.edit().putBoolean("running", false).apply(); }
    public boolean running() { return p.getBoolean("running", false); }
    public int checkedChannels() { return p.getInt("checked_channels", 0); }
    public int totalChannels() { return p.getInt("total_channels", 0); }
    public int runFound() { return p.getInt("run_found", 0); }
    public String currentChannel() { return p.getString("current_channel", ""); }

    private File resultsFile() { return new File(appContext.getFilesDir(), RESULTS_FILE); }

    private List<LeadRecord> readRecords() {
        List<LeadRecord> list = new ArrayList<>();
        File f = resultsFile();
        if (!f.exists()) return list;
        try {
            String raw = readFile(f);
            if (raw == null || raw.trim().isEmpty()) return list;
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                LeadRecord r = LeadRecord.fromJson(o);
                if (r != null) list.add(r);
            }
        } catch (Exception ignored) {}
        return list;
    }

    private void writeRecords(List<LeadRecord> list) {
        try {
            JSONArray a = new JSONArray();
            for (LeadRecord r : list) a.put(r.toJson());
            try (FileOutputStream out = new FileOutputStream(resultsFile(), false)) {
                out.write(a.toString().getBytes("UTF-8"));
                out.flush();
            }
        } catch (Exception ignored) {}
    }

    private String readFile(File file) {
        try (FileInputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toString("UTF-8");
        } catch (Exception ignored) { return ""; }
    }

    private static String cleanExcerpt(String text, int limit) {
        if (text == null) return "";
        String s = text.replaceAll("\\s+", " ").trim();
        return s.length() <= limit ? s : s.substring(0, limit - 1).trim() + "…";
    }

    public static String categoryName(Lead.Category c) {
        if (c == Lead.Category.SITE) return "САЙТ / ЛЕНДИНГ";
        if (c == Lead.Category.PRESENTATION) return "ПРЕЗЕНТАЦИЯ";
        return "ИСКЛЮЧЕНО";
    }
}
