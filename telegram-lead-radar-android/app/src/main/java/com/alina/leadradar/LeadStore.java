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

    private static final String DEFAULT_CHANNELS =
            "designhunters\n"
            + "workasap\n"
            + "jun_hi_vacancies\n"
            + "designwork_vacansii\n"
            + "designs_job\n"
            + "design_vacancy\n"
            + "design_jobs_uxui\n"
            + "fordesigner\n"
            + "dsgn_vacancies\n"
            + "zakaz_design\n"
            + "jun_hi\n"
            + "forallmedia\n"
            + "designer_vacancies\n"
            + "junior_designers\n"
            + "vakansii_dizaynerov\n"
            + "dprofilejob\n"
            + "dsgnworkers\n"
            + "vakansii_dlya_dizaynera\n"
            + "workindesign\n"
            + "designs_squad\n"
            + "vakansii_design\n"
            + "visionisland\n"
            + "frilans\n"
            + "jun_jobs\n"
            + "designizer\n"
            + "vacancies_dsgn\n"
            + "designodromo\n"
            + "designkziskra\n"
            + "jobaem\n"
            + "design_freevacancies\n"
            + "board_axolotl\n"
            + "design_careers\n"
            + "zakazi_designers\n"
            + "jobs_designer\n"
            + "freelance_design_work\n"
            + "webdesign_jobs\n"
            + "rabota_design\n"
            + "rabotadesign\n"
            + "uiux_jobs\n"
            + "uxui_jobs\n"
            + "designer_ru\n"
            + "kadrof_work\n"
            + "vacanciesrus\n"
            + "getclient\n"
            + "freelancetaverna\n"
            + "digitaltender\n"
            + "dnative_job\n"
            + "mediajobs_ru\n"
            + "dddwork\n"
            + "edujobs\n"
            + "edmarketclubjob\n"
            + "dl_marketplace\n"
            + "jobpower\n"
            + "digital_hr\n"
            + "perezvonyu\n"
            + "normrabota\n"
            + "theyseeku\n"
            + "naudalenkebro\n"
            + "rabotka_zdes\n"
            + "well_paid_job\n"
            + "chooseajob\n"
            + "stage_first\n"
            + "easy_wrk\n"
            + "yuniorapp\n"
            + "digitalbroccoli\n"
            + "frwork3\n"
            + "freeworkfeed\n"
            + "remoteit\n"
            + "evacuatejobs\n"
            + "from_home\n"
            + "freetasks\n"
            + "hmoffice\n"
            + "remotejobss\n"
            + "udalenkabezz\n"
            + "zapwork\n"
            + "rabota_udalennaya_vakansii_tg\n"
            + "freelancce\n"
            + "maroset\n"
            + "workoo\n"
            + "distantsiya\n"
            + "noexperience\n"
            + "frilanser_vacansii\n"
            + "udafrii\n"
            + "koteyka_freelancer\n"
            + "talentedpeoples\n"
            + "vacancysmm\n"
            + "work_from_lina\n"
            + "vacanciesmarketing\n"
            + "proffreelancee\n"
            + "workzavr\n"
            + "jobi_jobs\n"
            + "udalendwork\n"
            + "freelance_vacancii\n"
            + "distantwork_freelance_vacancies\n"
            + "udalenkamoya\n"
            + "remote_tgworks\n"
            + "poisk_udalenka\n"
            + "vacansii_na_udalenke\n"
            + "on_remote\n"
            + "udfril";

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
