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

import java.net.URLEncoder;
import java.util.List;

public final class MainActivityV14 extends Activity {
    private LeadStore store;
    private DeliveryStore delivery;
    private LinearLayout telegramTab;
    private LinearLayout resourcesTab;
    private TextView status;
    private TextView selectedSummary;
    private CheckBox sites;
    private CheckBox presentations;
    private EditText channels;
    private EditText sitePortfolio;
    private EditText presentationPortfolio;
    private Spinner lookback;
    private LinearLayout resultsContainer;
    private EditText resourceQuery;
    private int renderedCount = -1;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() {
            updateStatus();
            renderResultsIfNeeded();
            uiHandler.postDelayed(this, 1600L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new LeadStore(this);
        delivery = new DeliveryStore(this);
        setTitle("Universal Lead Radar v14");
        setContentView(buildUi());
        loadSettings();
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
        renderResults(true);
        uiHandler.removeCallbacks(autoRefresh);
        uiHandler.postDelayed(autoRefresh, 500L);
    }

    @Override protected void onPause() {
        uiHandler.removeCallbacks(autoRefresh);
        super.onPause();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(14), dp(14), dp(20));
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("Universal Lead Radar v14");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(title);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        Button tg = new Button(this);
        tg.setText("1 · TELEGRAM");
        tg.setOnClickListener(v -> showTab(true));
        Button resources = new Button(this);
        resources.setText("2 · БИРЖИ / РЕСУРСЫ");
        resources.setOnClickListener(v -> showTab(false));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tabs.addView(tg, half);
        tabs.addView(resources, half);
        page.addView(tabs, fullWidth());

        telegramTab = buildTelegramTab();
        resourcesTab = buildResourcesTab();
        page.addView(telegramTab, fullWidth());
        page.addView(resourcesTab, fullWidth());
        showTab(true);
        return scroll;
    }

    private LinearLayout buildTelegramTab() {
        LinearLayout root = column();

        TextView intro = new TextView(this);
        intro.setText("Только нативные Telegram-заказы. Репосты, которые ведут на Kwork, YouDo, Avito и другие биржи, здесь исключаются, чтобы не было дублей со второй вкладкой. Автоотправки нет.");
        intro.setTextSize(14);
        intro.setPadding(0, dp(10), 0, dp(10));
        root.addView(intro);

        status = new TextView(this);
        status.setTextSize(15);
        root.addView(status);

        root.addView(label("Что искать"));
        sites = new CheckBox(this);
        sites.setText("Лендинг / простой сайт — без Tilda и WordPress");
        root.addView(sites);
        presentations = new CheckBox(this);
        presentations.setText("Презентации / слайды / PowerPoint / PDF / pitch deck");
        root.addView(presentations);

        root.addView(label("Период"));
        lookback = new Spinner(this);
        String[] periods = {"24 часа", "3 дня", "7 дней", "14 дней"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lookback.setAdapter(adapter);
        root.addView(lookback, fullWidth());

        root.addView(label("Telegram-каналы"));
        channels = new EditText(this);
        channels.setMinLines(8);
        channels.setGravity(android.view.Gravity.TOP);
        root.addView(channels, fullWidth());

        root.addView(label("Портфолио сайтов"));
        sitePortfolio = new EditText(this);
        root.addView(sitePortfolio, fullWidth());

        root.addView(label("Портфолио презентаций"));
        presentationPortfolio = new EditText(this);
        root.addView(presentationPortfolio, fullWidth());

        Button start = new Button(this);
        start.setText("ЗАПУСТИТЬ СВЕЖИЙ ПОИСК");
        start.setOnClickListener(v -> startManualSearch());
        root.addView(start, buttonParams());

        Button stop = new Button(this);
        stop.setText("ОСТАНОВИТЬ ПОИСК");
        stop.setOnClickListener(v -> stopManualSearch());
        root.addView(stop, buttonParams());

        Button next = new Button(this);
        next.setText("СЛЕДУЮЩИЙ ОТМЕЧЕННЫЙ · СКОПИРОВАТЬ ОТКЛИК + ОТКРЫТЬ TELEGRAM");
        next.setOnClickListener(v -> openNextSelected());
        root.addView(next, buttonParams());

        Button clear = new Button(this);
        clear.setText("ОЧИСТИТЬ РЕЗУЛЬТАТЫ");
        clear.setOnClickListener(v -> {
            if (store.running()) {
                Toast.makeText(this, "Сначала останови поиск", Toast.LENGTH_SHORT).show();
                return;
            }
            store.clearPreviewHistory();
            delivery.clear();
            renderedCount = -1;
            renderResults(true);
            updateStatus();
        });
        root.addView(clear, buttonParams());

        selectedSummary = new TextView(this);
        selectedSummary.setTextSize(15);
        selectedSummary.setTypeface(Typeface.DEFAULT_BOLD);
        selectedSummary.setPadding(0, dp(14), 0, dp(8));
        root.addView(selectedSummary);

        resultsContainer = column();
        root.addView(resultsContainer, fullWidth());
        return root;
    }

    private LinearLayout buildResourcesTab() {
        LinearLayout root = column();
        TextView intro = new TextView(this);
        intro.setText("Поиск по внешним площадкам для презентаций. Здесь находятся Kwork, YouDo, Avito и другие биржи. Они специально не дублируются во вкладке Telegram. Вход выполняй на самой площадке в браузере — пароль в Lead Radar вводить не нужно.");
        intro.setTextSize(14);
        intro.setPadding(0, dp(10), 0, dp(8));
        root.addView(intro);

        root.addView(label("Что искать"));
        resourceQuery = new EditText(this);
        resourceQuery.setText("презентация PowerPoint слайды pitch deck");
        root.addView(resourceQuery, fullWidth());

        Button searchAll = new Button(this);
        searchAll.setText("ИСКАТЬ СРАЗУ ПО ВСЕМ РЕСУРСАМ");
        searchAll.setOnClickListener(v -> openCrossSiteSearch());
        root.addView(searchAll, buttonParams());

        Button copy = new Button(this);
        copy.setText("СКОПИРОВАТЬ ОТКЛИК ДЛЯ ПРЕЗЕНТАЦИИ");
        copy.setOnClickListener(v -> copyToClipboard(marketplaceReply()));
        root.addView(copy, buttonParams());

        root.addView(label("Площадки"));
        addResource(root, "KWORK · ПРОЕКТЫ", "https://kwork.ru/projects");
        addResource(root, "YOUDO · ОТКРЫТЫЕ ЗАДАНИЯ", "https://youdo.com/tasks-all-opened-all");
        addResource(root, "AVITO · ПОИСК", "https://www.avito.ru/rossiya/vakansii?q=%D0%BF%D1%80%D0%B5%D0%B7%D0%B5%D0%BD%D1%82%D0%B0%D1%86%D0%B8%D1%8F");
        addResource(root, "FREELANCE.RU · ПРОЕКТЫ", "https://freelance.ru/projects/");
        addResource(root, "FL.RU · ПРОЕКТЫ", "https://www.fl.ru/projects/");
        addResource(root, "HABR FREELANCE · ЗАДАЧИ", "https://freelance.habr.com/tasks");
        addResource(root, "WORKZILLA", "https://work-zilla.com/");

        TextView note = new TextView(this);
        note.setText("Эта версия не хранит логины и пароли и не нажимает финальную кнопку «Откликнуться» автоматически. Площадка открывается в браузере с твоей сессией; текст отклика можно скопировать из приложения. Для автоматизации конкретного аккаунта нужен отдельный адаптер под каждую площадку и вход внутри её страницы, а не передача пароля боту.");
        note.setTextSize(13);
        note.setPadding(0, dp(12), 0, dp(12));
        root.addView(note);
        return root;
    }

    private void addResource(LinearLayout root, String name, String url) {
        Button b = new Button(this);
        b.setText(name);
        b.setOnClickListener(v -> openUrl(url, "Не удалось открыть площадку"));
        root.addView(b, compactButtonParams());
    }

    private void showTab(boolean telegram) {
        if (telegramTab != null) telegramTab.setVisibility(telegram ? View.VISIBLE : View.GONE);
        if (resourcesTab != null) resourcesTab.setVisibility(telegram ? View.GONE : View.VISIBLE);
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
        if (store.running()) return;
        if (!sites.isChecked() && !presentations.isChecked()) {
            Toast.makeText(this, "Выбери хотя бы одно направление", Toast.LENGTH_SHORT).show();
            return;
        }
        if (channels.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Список каналов пуст", Toast.LENGTH_SHORT).show();
            return;
        }
        saveSettings();
        requestNotificationsIfNeeded();
        delivery.clear();
        renderedCount = -1;
        Intent i = new Intent(this, ScannerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i); else startService(i);
        Toast.makeText(this, "Поиск v14 запущен", Toast.LENGTH_LONG).show();
    }

    private void stopManualSearch() {
        if (!store.running()) return;
        store.requestStop();
        Intent i = new Intent(this, ScannerService.class);
        i.setAction(ScannerService.ACTION_STOP);
        startService(i);
        updateStatus();
    }

    private void updateStatus() {
        if (status == null) return;
        StringBuilder s = new StringBuilder();
        s.append("Период: ").append(periodLabel(store.lookbackDays())).append("\n");
        s.append("Поиск: ").append(store.running() ? "ИДЁТ" : "НЕ ЗАПУЩЕН").append("\n");
        if (store.totalChannels() > 0) s.append("Источники: ").append(store.checkedChannels()).append(" / ").append(store.totalChannels()).append("\n");
        s.append("Прочитано постов: ").append(store.diagnosticPosts()).append("\n");
        s.append("Кандидатов по теме: ").append(store.diagnosticCandidates()).append("\n");
        s.append("Без прямого Telegram-контакта: ").append(store.diagnosticNoContact()).append("\n");
        s.append("Ошибок чтения: ").append(store.diagnosticErrors()).append("\n");
        s.append("Найдено нативных Telegram-заявок: ").append(store.runFound());
        status.setText(s.toString());
        updateSelectedSummary();
    }

    private void renderResultsIfNeeded() {
        List<LeadRecord> records = store.resultRecords();
        if (records.size() != renderedCount) renderResults(records); else updateSelectedSummary();
    }

    private void renderResults(boolean force) {
        List<LeadRecord> records = store.resultRecords();
        if (force || records.size() != renderedCount) renderResults(records); else updateSelectedSummary();
    }

    private void renderResults(List<LeadRecord> records) {
        if (resultsContainer == null) return;
        resultsContainer.removeAllViews();
        renderedCount = records.size();
        if (records.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(store.running() ? "Идёт поиск…" : "Пока заявок нет.");
            resultsContainer.addView(empty);
        } else {
            for (LeadRecord r : records) resultsContainer.addView(buildLeadCard(r), fullWidth());
        }
        updateSelectedSummary();
    }

    private View buildLeadCard(LeadRecord r) {
        LinearLayout card = column();
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackgroundColor(0x0D000000);
        LinearLayout.LayoutParams cp = fullWidth();
        cp.bottomMargin = dp(12);
        card.setLayoutParams(cp);

        CheckBox choose = new CheckBox(this);
        choose.setText("ХОЧУ НАПИСАТЬ ЭТОМУ ЗАКАЗЧИКУ");
        choose.setTypeface(Typeface.DEFAULT_BOLD);
        choose.setChecked(store.isSelected(r.id));
        choose.setOnCheckedChangeListener((b, checked) -> {
            store.setSelected(r.id, checked);
            updateSelectedSummary();
        });
        card.addView(choose);

        boolean sent = delivery.isSent(r.id);
        String budget = r.budget == null || r.budget.trim().isEmpty() ? "не указан" : r.budget.trim();
        TextView info = new TextView(this);
        info.setText((sent ? "✅ ОТПРАВЛЕНО\n" : "")
                + "[" + LeadStore.categoryName(r.category) + "]\n"
                + "Telegram: " + LeadStore.contactUrl(r.username) + "\n"
                + "Бюджет: " + budget + "\n"
                + "Исходный пост: " + r.postUrl + "\n\n" + r.task + "\n\nОТКЛИК:\n" + r.message);
        info.setTextSize(14);
        info.setTextIsSelectable(true);
        card.addView(info);

        Button open = new Button(this);
        open.setText("СКОПИРОВАТЬ ОТКЛИК + ОТКРЫТЬ TELEGRAM");
        open.setOnClickListener(v -> {
            copyToClipboard(r.message);
            openUrl(LeadStore.contactUrl(r.username), "Нет Telegram-контакта");
        });
        card.addView(open, compactButtonParams());

        Button sentButton = new Button(this);
        sentButton.setText(sent ? "СНЯТЬ ОТМЕТКУ «ОТПРАВЛЕНО»" : "ОТМЕТИТЬ КАК ОТПРАВЛЕНО");
        sentButton.setOnClickListener(v -> {
            delivery.setSent(r.id, !delivery.isSent(r.id));
            if (delivery.isSent(r.id)) store.setSelected(r.id, false);
            renderResults(true);
        });
        card.addView(sentButton, compactButtonParams());

        Button post = new Button(this);
        post.setText("ОТКРЫТЬ ИСХОДНЫЙ ПОСТ");
        post.setOnClickListener(v -> openUrl(r.postUrl, "Нет исходного поста"));
        card.addView(post, compactButtonParams());
        return card;
    }

    private void openNextSelected() {
        for (LeadRecord r : store.resultRecords()) {
            if (!store.isSelected(r.id) || delivery.isSent(r.id)) continue;
            copyToClipboard(r.message);
            openUrl(LeadStore.contactUrl(r.username), "Нет Telegram-контакта");
            return;
        }
        Toast.makeText(this, "Нет неотправленных отмеченных заявок", Toast.LENGTH_LONG).show();
    }

    private String marketplaceReply() {
        String link = store.presentationPortfolioUrl();
        return "Здравствуйте! Увидела ваш запрос на презентацию. Могу взять задачу в работу: помочь со структурой, логикой материала и оформить финальную презентацию в PowerPoint/PDF. Если задача ещё актуальна, пришлите материалы, объём и желаемый срок.\n\nПортфолио презентаций: " + link;
    }

    private void openCrossSiteSearch() {
        try {
            String q = resourceQuery == null ? "презентация" : resourceQuery.getText().toString().trim();
            if (q.isEmpty()) q = "презентация";
            String scope = "(site:kwork.ru/projects OR site:youdo.com/tasks OR site:avito.ru OR site:freelance.ru/projects OR site:fl.ru/projects OR site:freelance.habr.com/tasks OR site:work-zilla.com) ";
            openUrl("https://yandex.ru/search/?text=" + URLEncoder.encode(scope + q, "UTF-8"), "Не удалось открыть поиск");
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось сформировать поиск", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSelectedSummary() {
        if (selectedSummary == null) return;
        int selected = 0;
        int sent = 0;
        for (LeadRecord r : store.resultRecords()) {
            if (delivery.isSent(r.id)) sent++;
            if (store.isSelected(r.id) && !delivery.isSent(r.id)) selected++;
        }
        selectedSummary.setText("Отмечено к отправке: " + selected + " · Уже отправлено: " + sent);
    }

    private void copyToClipboard(String message) {
        if (message == null || message.trim().isEmpty()) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Отклик", message));
        Toast.makeText(this, "Отклик скопирован", Toast.LENGTH_SHORT).show();
    }

    private void openUrl(String url, String error) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Toast.makeText(this, error, Toast.LENGTH_SHORT).show(); }
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

    private void requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
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
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = fullWidth();
        p.topMargin = dp(9);
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
