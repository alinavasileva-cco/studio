package com.alina.leadradar;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

public final class LeadStore {
    private static final String PREFS = "lead_radar";
    private static final int RULES_VERSION = 6;
    private static final String RESULTS_FILE = "manual_lead_results.txt";

    private static final String OLD_V5_CHANNELS =
            "zakaz_design\nDesigns_squad\nDesigns_job\ndesignwork_vacansii\njobaem\npro_zayavki\nGetClient\n"
            + "dsgn_vacancies\ndesign_crate\nzakazi_designers\nvakansiidesign\nvakansii_dlya_dizaynera\ndsgnworkers";

    /*
     * 100 public sources. Profiled design/freelance sources are first, broader
     * digital/remote aggregators follow. A dead/private/renamed source is simply
     * skipped by ScannerService and never stops the full manual pass.
     */
    private static final String DEFAULT_CHANNELS =
            "workzavr\n"
            + "designer_ru\n"
            + "designhunters\n"
            + "designwork_vacansii\n"
            + "zakaz_design\n"
            + "zakazi_designers\n"
            + "designizer\n"
            + "design_jobs_uxui\n"
            + "vakansii_dlya_dizaynera\n"
            + "design_freevacancies\n"
            + "jobs_designer\n"
            + "comeindesign\n"
            + "vakansii_design\n"
            + "dprofilejob\n"
            + "design_careers\n"
            + "vacancies_dsgn\n"
            + "webdesign_jobs\n"
            + "rabota_design\n"
            + "design_vacancy\n"
            + "dsgnworkers\n"
            + "fordesigner\n"
            + "designer_vacancies\n"
            + "junior_designers\n"
            + "jun_hi_vacancies\n"
            + "Visionisland\n"
            + "vakansii_dizaynerov\n"
            + "workindesign\n"
            + "design_crate\n"
            + "Designs_squad\n"
            + "Designs_job\n"
            + "free_Design1\n"
            + "freelance_design_work\n"
            + "GetJob_design\n"
            + "DesignHub_Jobs\n"
            + "designslot\n"
            + "rabota_designer\n"
            + "job4designer\n"
            + "designer_jobs\n"
            + "rost_design\n"
            + "yotolabdesigner\n"
            + "searchdesignerr\n"
            + "Jobs_for_designers\n"
            + "bbe_jobs\n"
            + "local_talent\n"
            + "designer_ru_work\n"
            + "uiux_jobs\n"
            + "webfrl\n"
            + "vdhl_good\n"
            + "perezvonyu\n"
            + "FrWork\n"
            + "theyseeku\n"
            + "xCareers\n"
            + "prjctr\n"
            + "dna325\n"
            + "workoo\n"
            + "recruit\n"
            + "vacanciesrus\n"
            + "chooseajob\n"
            + "Well_paid_Job\n"
            + "zapwork\n"
            + "DobbyFreeLance\n"
            + "stage_1st\n"
            + "normrabota\n"
            + "freelancefeed\n"
            + "workathomerus\n"
            + "digital_hr\n"
            + "digital_human\n"
            + "mediajobs_ru\n"
            + "digitaltender\n"
            + "Remoteit\n"
            + "naudalenkebro\n"
            + "stage_first\n"
            + "vezdeworker\n"
            + "antirabstvo\n"
            + "findbestpartner\n"
            + "distantsiya\n"
            + "finfreelance\n"
            + "dnative_job\n"
            + "rueventjob\n"
            + "freelancetaverna\n"
            + "projectperhour\n"
            + "remote_ru\n"
            + "kadrof_work\n"
            + "rabotaforyou\n"
            + "promopoisk\n"
            + "founderjob\n"
            + "workasap\n"
            + "worklis\n"
            + "polyaluzjob\n"
            + "divanjobs\n"
            + "remote_tgworks\n"
            + "frwork3\n"
            + "proffreelancee\n"
            + "hmoffice\n"
            + "udalendwork\n"
            + "udafrii\n"
            + "jobforjunior\n"
            + "evacuatejobs\n"
            + "huggabletalents\n"
            + "theblueprintcareer";

    private final Context appContext;
    private final SharedPreferences p;

    public LeadStore(Context context) {
        appContext = context.getApplicationContext();
        p = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateToV6();
    }

    private void migrateToV6() {
        if (p.getInt("rules_version", 0) >= RULES_VERSION) return;

        String existing = p.getString("channels", "");
        String normalizedExisting = existing == null ? "" : existing.trim();
        int existingCount = countLines(normalizedExisting);
        boolean looksLikeOldDefault = normalizedExisting.isEmpty()
                || normalizedExisting.equalsIgnoreCase(OLD_V5_CHANNELS.trim())
                || existingCount <= 13;
        String channelsToUse = looksLikeOldDefault ? DEFAULT_CHANNELS : normalizedExisting;

        p.edit()
                .putInt("rules_version", RULES_VERSION)
                .putString("channels", channelsToUse)
                .putInt("lookback_days", 7)
                .putBoolean("sites_enabled", true)
                .putBoolean("presentations_enabled", true)
                .putBoolean("running", false)
                .putInt("checked_channels", 0)
                .putInt("total_channels", 0)
                .putInt("run_found", 0)
                .putInt("preview_count", 0)
                .remove("previewed_posts")
                .remove("last_preview_post")
                .remove("last_preview_user")
                .remove("last_preview_message")
                .remove("last_preview_text")
                .apply();
        try { resultsFile().delete(); } catch (Exception ignored) {}
    }

    public boolean wasSent(String key) { return false; }

    public boolean wasPreviewed(String key) {
        return new HashSet<>(p.getStringSet("previewed_posts", new HashSet<>())).contains(key);
    }

    public synchronized void addPreview(Lead lead, String message) {
        Set<String> previewed = new HashSet<>(p.getStringSet("previewed_posts", new HashSet<>()));
        previewed.add(lead.dedupKey);

        String budget = lead.budget == null || lead.budget.trim().isEmpty() ? "не указан" : lead.budget.trim();
        String original = cleanExcerpt(lead.text, 900);
        String contactUrl = "https://t.me/" + lead.username;
        String entry = "[" + categoryName(lead.category) + "]\n"
                + "Контакт: " + contactUrl
                + "\nБюджет: " + budget
                + "\nИсходный пост: " + lead.postUrl
                + "\n\nЗАДАЧА:\n" + original
                + "\n\nПОДГОТОВЛЕННЫЙ ОТКЛИК:\n" + message;

        String old = readResultsFile();
        String combined = old.isEmpty() ? entry : entry + "\n\n━━━━━━━━━━━━━━━━━━\n\n" + old;
        writeResultsFile(combined);

        p.edit()
                .putStringSet("previewed_posts", previewed)
                .putString("last_preview_post", lead.postUrl)
                .putString("last_preview_user", lead.username)
                .putString("last_preview_message", message)
                .putString("last_preview_text", original)
                .putInt("preview_count", p.getInt("preview_count", 0) + 1)
                .putInt("run_found", p.getInt("run_found", 0) + 1)
                .apply();
    }

    public String previewHistory() { return readResultsFile(); }
    public String lastPreviewPost() { return p.getString("last_preview_post", ""); }
    public String lastPreviewUser() { return p.getString("last_preview_user", ""); }
    public String lastPreviewMessage() { return p.getString("last_preview_message", ""); }
    public int previewCount() { return p.getInt("preview_count", 0); }

    public void clearPreviewHistory() {
        try { resultsFile().delete(); } catch (Exception ignored) {}
        p.edit()
                .remove("previewed_posts")
                .remove("last_preview_post")
                .remove("last_preview_user")
                .remove("last_preview_message")
                .remove("last_preview_text")
                .putInt("preview_count", 0)
                .putInt("run_found", 0)
                .apply();
    }

    public String channels() { return p.getString("channels", DEFAULT_CHANNELS); }
    public void setChannels(String channels) {
        p.edit().putString("channels", channels == null ? "" : channels.trim()).apply();
    }

    public int lookbackDays() {
        int d = p.getInt("lookback_days", 7);
        return (d == 1 || d == 3 || d == 7 || d == 14) ? d : 7;
    }

    public void setLookbackDays(int days) {
        int d = (days == 1 || days == 3 || days == 7 || days == 14) ? days : 7;
        p.edit().putInt("lookback_days", d).apply();
    }

    public String profileUrl() {
        return p.getString("profile_url", "https://alinavasileva-cco.github.io/studio/");
    }

    public void setProfileUrl(String url) {
        p.edit().putString("profile_url", url == null ? "" : url.trim()).apply();
    }

    public boolean sitesEnabled() { return p.getBoolean("sites_enabled", true); }
    public boolean presentationsEnabled() { return p.getBoolean("presentations_enabled", true); }
    public boolean aiEnabled() { return false; }
    public boolean consultingEnabled() { return false; }

    public void setCategories(boolean sites, boolean presentations) {
        p.edit()
                .putBoolean("sites_enabled", sites)
                .putBoolean("presentations_enabled", presentations)
                .apply();
    }

    public synchronized void beginRun(int totalChannels) {
        p.edit()
                .putBoolean("running", true)
                .putInt("checked_channels", 0)
                .putInt("total_channels", totalChannels)
                .putInt("run_found", 0)
                .putString("current_channel", "")
                .putLong("run_started_at", System.currentTimeMillis())
                .remove("run_finished_at")
                .apply();
    }

    public void setProgress(int checked, int total, String currentChannel) {
        p.edit()
                .putInt("checked_channels", checked)
                .putInt("total_channels", total)
                .putString("current_channel", currentChannel == null ? "" : currentChannel)
                .apply();
    }

    public synchronized void finishRun() {
        p.edit()
                .putBoolean("running", false)
                .putString("current_channel", "")
                .putLong("run_finished_at", System.currentTimeMillis())
                .apply();
    }

    public void requestStop() { p.edit().putBoolean("running", false).apply(); }
    public boolean running() { return p.getBoolean("running", false); }
    public int checkedChannels() { return p.getInt("checked_channels", 0); }
    public int totalChannels() { return p.getInt("total_channels", 0); }
    public int runFound() { return p.getInt("run_found", 0); }
    public String currentChannel() { return p.getString("current_channel", ""); }

    private File resultsFile() { return new File(appContext.getFilesDir(), RESULTS_FILE); }

    private String readResultsFile() {
        File file = resultsFile();
        if (!file.exists()) return "";
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toString("UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private void writeResultsFile(String text) {
        try (FileOutputStream out = new FileOutputStream(resultsFile(), false)) {
            out.write(text.getBytes("UTF-8"));
            out.flush();
        } catch (Exception ignored) {}
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

    private static int countLines(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0;
        return raw.trim().split("[\\n,; ]+").length;
    }
}
