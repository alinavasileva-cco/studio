package com.alina.leadradar;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class MainActivityV11 extends Activity {
    private LeadStore store;
    private TextView status;
    private TextView selectedSummary;
    private CheckBox sites;
    private CheckBox presentations;
    private EditText channels;
    private EditText sitePortfolio;
    private EditText presentationPortfolio;
    private Spinner lookback;
    private LinearLayout resultsContainer;
    private int renderedCount = -1;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() {
            updateStatus();
            renderResultsIfNeeded();
            uiHandler.postDelayed(this, 1800L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new LeadStore(this);
        setTitle("Universal Lead Radar v11");
        setContentView(buildUi());
        loadSettings();
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
        renderResults(true);
        uiHandler.removeCallbacks(autoRefresh);
        uiHandler.postDelayed(autoRefresh, 600L);
    }

    @Override protected void onPause() {
        uiHandler.removeCallbacks(autoRefresh);
        super.onPause();
    }

    private View buildUi() {
        int pad = dp(18);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Universal Telegram Lead Radar v11");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("РУЧНОЙ ПОИСК. НИЧЕГО НЕ ОТПРАВЛЯЕТСЯ АВТОМАТИЧЕСКИ.\n"
                + "Каждая найденная заявка теперь отдельная карточка. Ты сама ставишь галочку, кому хочешь написать. "
                + "Для презентаций в отклик автоматически добавляется отдельное портфолио презентаций.");
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(8), 0, dp(14));
        root.addView(subtitle);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 0, 0, dp(12));
        root.addView(status);

        root.addView(label("Что искать"));
        sites = new CheckBox(this);
        sites.setText("Создание лендинга / простого сайта — НЕ Tilda и НЕ WordPress");
        root.addView(sites);
        presentations = new CheckBox(this);
        presentations.setText("Создание / оформление презентации, PowerPoint / PDF / pitch deck");
        root.addView(presentations);

        root.addView(label("За какой период перечитать историю"));
        lookback = new Spinner(this);
        String[] periods = {"24 часа", "3 дня", "7 дней", "14 дней"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lookback.setAdapter(adapter);
        root.addView(lookback, fullWidth());

        root.addView(label("Telegram-каналы с заказами"));
        channels = new EditText(this);
        channels.setMinLines(9);
        channels.setGravity(android.view.Gravity.TOP);
        root.addView(channels, fullWidth());

        root.addView(label("Портфолио сайтов"));
        sitePortfolio = new EditText(this);
        sitePortfolio.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(sitePortfolio, fullWidth());

        root.addView(label("Портфолио презентаций"));
        presentationPortfolio = new EditText(this);
        presentationPortfolio.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(presentationPortfolio, fullWidth());

        Button start = new Button(this);
        start.setText("ЗАПУСТИТЬ СВЕЖИЙ РУЧНОЙ ПОИСК");
        start.setOnClickListener(v -> startManualSearch());
        root.addView(start, buttonParams());

        Button stop = new Button(this);
        stop.setText("ОСТАНОВИТЬ ТЕКУЩИЙ ПОИСК");
        stop.setOnClickListener(v -> stopManualSearch());
        root.addView(stop, buttonParams());

        Button openSelected = new Button(this);
        openSelected.setText("ОТКРЫТЬ ПЕРВОГО ОТМЕЧЕННОГО И СКОПИРОВАТЬ ОТКЛИК");
        openSelected.setOnClickListener(v -> openFirstSelected());
        root.addView(openSelected, buttonParams());

        Button clear = new Button(this);
        clear.setText("ОЧИСТИТЬ РЕЗУЛЬТАТЫ");
        clear.setOnClickListener(v -> {
            if (store.running()) {
                Toast.makeText(this, "Сначала останови текущий поиск", Toast.LENGTH_SHORT).show();
                return;
            }
            store.clearPreviewHistory();
            renderedCount = -1;
            updateStatus();
            renderResults(true);
        });
        root.addView(clear, buttonParams());

        root.addView(label("Найденные заявки текущего запуска"));
        selectedSummary = new TextView(this);
        selectedSummary.setTextSize(15);
        selectedSummary.setTypeface(Typeface.DEFAULT_BOLD);
        selectedSummary.setPadding(0, dp(4), 0, dp(8));
        root.addView(selectedSummary);

        TextView hint = new TextView(this);
        hint.setText("Галочка ничего не отправляет. Она только помечает заявки, которым ты решила написать. "
                + "В каждой карточке можно открыть контакт, скопировать подготовленный отклик или открыть исходный пост.");
        hint.setTextSize(13);
        hint.setPadding(0, 0, 0, dp(8));
        root.addView(hint);

        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultsContainer, fullWidth());
        return scroll;
    }

    private void loadSettings() {
        sites.setChecked(store.sitesEnabled());
        presentations.setChecked(store.presentationsEnabled());
        channels.setText(store.channels());
        sitePortfolio.setText(store.profileUrl());
        presentationPortfolio.setText(store.presentationPortfolioUrl());
        setLookbackSelection(store.lookbackDays());
        updateStatus();
        renderResults(true);
    }

    private void saveSettings() {
        store.setCategories(sites.isChecked(), presentations.isChecked());
        store.setChannels(channels.getText().toString());
        store.setProfileUrl(sitePortfolio.getText().toString());
        store.setPresentationPortfolioUrl(presentationPortfolio.getText().toString());
        store.setLookbackDays(selectedLookbackDays());
    }

    private void startManualSearch() {
        if (store.running()) {
            Toast.makeText(this, "Поиск уже идёт", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!sites.isChecked() && !presentations.isChecked()) {
            Toast.makeText(this, "Выбери хотя бы одно направление", Toast.LENGTH_SHORT).show();
            return;
        }
        if (channels.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Список источников пуст", Toast.LENGTH_SHORT).show();
            return;
        }
        saveSettings();
        requestNotificationsIfNeeded();
        renderedCount = -1;
        Intent i = new Intent(this, ScannerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i); else startService(i);
        Toast.makeText(this, "Свежий поиск v11 запущен", Toast.LENGTH_LONG).show();
        uiHandler.postDelayed(() -> { updateStatus(); renderResults(true); }, 800L);
    }

    private void stopManualSearch() {
        if (!store.running()) {
            Toast.makeText(this, "Сейчас поиск не запущен", Toast.LENGTH_SHORT).show();
            return;
        }
        store.requestStop();
        Intent i = new Intent(this, ScannerService.class);
        i.setAction(ScannerService.ACTION_STOP);
        startService(i);
        updateStatus();
    }

    private void updateStatus() {
        if (status == null) return;
        StringBuilder s = new StringBuilder();
        s.append("Режим: ТОЛЬКО РУЧНОЙ · АВТООТПРАВКИ НЕТ\n");
        s.append("Период: ").append(periodLabel(store.lookbackDays())).append("\n");
        s.append("Поиск: ").append(store.running() ? "ИДЁТ" : "НЕ ЗАПУЩЕН").append("\n");
        int total = store.totalChannels();
        if (total > 0) s.append("Проверено источников: ").append(store.checkedChannels()).append(" / ").append(total).append("\n");
        else s.append("Источников в списке: ").append(countChannels(store.channels())).append("\n");
        if (store.running() && !store.currentChannel().isEmpty()) s.append("Сейчас: @").append(store.currentChannel()).append("\n");
        s.append("Реально прочитано постов: ").append(store.diagnosticPosts()).append("\n");
        s.append("Разобрано блоков/задач: ").append(store.diagnosticBlocks()).append("\n");
        s.append("Кандидатов по теме: ").append(store.diagnosticCandidates()).append("\n");
        s.append("Без прямого TG-контакта: ").append(store.diagnosticNoContact()).append("\n");
        s.append("Ошибок чтения каналов: ").append(store.diagnosticErrors()).append("\n");
        s.append("Найдено подходящих: ").append(store.runFound());
        status.setText(s.toString());
        updateSelectedSummary();
    }

    private void renderResultsIfNeeded() {
        List<LeadRecord> records = store.resultRecords();
        if (records.size() != renderedCount) renderResults(records);
        else updateSelectedSummary();
    }

    private void renderResults(boolean force) {
        List<LeadRecord> records = store.resultRecords();
        if (force || records.size() != renderedCount) renderResults(records);
        else updateSelectedSummary();
    }

    private void renderResults(List<LeadRecord> records) {
        resultsContainer.removeAllViews();
        renderedCount = records.size();
        updateSelectedSummary();

        if (records.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(store.running()
                    ? "Идёт поиск. Подходящие заявки будут появляться здесь по мере проверки каналов."
                    : "Пока подходящих заявок нет.");
            empty.setTextSize(14);
            empty.setPadding(dp(10), dp(10), dp(10), dp(18));
            resultsContainer.addView(empty);
            return;
        }

        for (LeadRecord r : records) resultsContainer.addView(buildLeadCard(r), fullWidth());
    }

    private View buildLeadCard(LeadRecord r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams cp = fullWidth();
        cp.bottomMargin = dp(14);
        card.setLayoutParams(cp);
        card.setBackgroundColor(0x0D000000);

        CheckBox choose = new CheckBox(this);
        choose.setText("ХОЧУ ОТПРАВИТЬ ЭТОМУ ЗАКАЗЧИКУ");
        choose.setTypeface(Typeface.DEFAULT_BOLD);
        choose.setChecked(store.isSelected(r.id));
        choose.setOnCheckedChangeListener((buttonView, isChecked) -> {
            store.setSelected(r.id, isChecked);
            updateSelectedSummary();
        });
        card.addView(choose);

        TextView info = new TextView(this);
        String budget = r.budget == null || r.budget.trim().isEmpty() ? "не указан" : r.budget.trim();
        info.setText("[" + LeadStore.categoryName(r.category) + "]\n"
                + "Telegram: " + LeadStore.contactUrl(r.username) + "\n"
                + "Бюджет: " + budget + "\n"
                + "Исходный пост: " + r.postUrl + "\n\n"
                + "ЗАДАЧА:\n" + r.task);
        info.setTextSize(14);
        info.setTextIsSelectable(true);
        info.setPadding(0, dp(6), 0, dp(8));
        card.addView(info);

        TextView reply = new TextView(this);
        reply.setText("ПОДГОТОВЛЕННЫЙ ОТКЛИК:\n" + r.message);
        reply.setTextSize(14);
        reply.setTextIsSelectable(true);
        reply.setPadding(0, dp(4), 0, dp(8));
        card.addView(reply);

        Button contact = new Button(this);
        contact.setText("ОТКРЫТЬ TELEGRAM-КОНТАКТ");
        contact.setOnClickListener(v -> openUrl(LeadStore.contactUrl(r.username), "Нет Telegram-контакта"));
        card.addView(contact, compactButtonParams());

        Button copy = new Button(this);
        copy.setText("СКОПИРОВАТЬ ОТКЛИК");
        copy.setOnClickListener(v -> copyToClipboard(r.message));
        card.addView(copy, compactButtonParams());

        Button post = new Button(this);
        post.setText("ОТКРЫТЬ ИСХОДНЫЙ ПОСТ");
        post.setOnClickListener(v -> openUrl(r.postUrl, "Нет исходного поста"));
        card.addView(post, compactButtonParams());

        return card;
    }

    private void openFirstSelected() {
        List<LeadRecord> records = store.resultRecords();
        for (LeadRecord r : records) {
            if (!store.isSelected(r.id)) continue;
            copyToClipboard(r.message);
            openUrl(LeadStore.contactUrl(r.username), "Нет Telegram-контакта");
            return;
        }
        Toast.makeText(this, "Сначала отметь хотя бы одну заявку галочкой", Toast.LENGTH_LONG).show();
    }

    private void updateSelectedSummary() {
        if (selectedSummary != null) {
            selectedSummary.setText("Отмечено к ручной отправке: " + store.selectedCount());
        }
    }

    private void copyToClipboard(String message) {
        if (message == null || message.trim().isEmpty()) {
            Toast.makeText(this, "Нет подготовленного отклика", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Отклик", message));
        Toast.makeText(this, "Отклик скопирован", Toast.LENGTH_SHORT).show();
    }

    private void openUrl(String url, String emptyMessage) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, emptyMessage, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private int selectedLookbackDays() {
        int pos = lookback == null ? 1 : lookback.getSelectedItemPosition();
        return pos == 0 ? 1 : pos == 1 ? 3 : pos == 3 ? 14 : 7;
    }

    private void setLookbackSelection(int days) {
        lookback.setSelection(days == 1 ? 0 : days == 3 ? 1 : days == 14 ? 3 : 2);
    }

    private String periodLabel(int days) {
        return days == 1 ? "24 часа" : days == 3 ? "3 дня" : days == 14 ? "14 дней" : "7 дней";
    }

    private int countChannels(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0;
        java.util.HashSet<String> unique = new java.util.HashSet<>();
        for (String piece : raw.split("[\\n,; ]+")) {
            String c = LeadScannerV11.normalizeChannel(piece);
            if (!c.isEmpty()) unique.add(c.toLowerCase(java.util.Locale.ROOT));
        }
        return unique.size();
    }

    private void requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(15);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, dp(12), 0, dp(4));
        return v;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = fullWidth();
        p.topMargin = dp(10);
        return p;
    }

    private LinearLayout.LayoutParams compactButtonParams() {
        LinearLayout.LayoutParams p = fullWidth();
        p.topMargin = dp(4);
        return p;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
