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
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
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

public final class MainActivityV8 extends Activity {
    private LeadStore store;
    private TextView status;
    private TextView results;
    private CheckBox sites;
    private CheckBox presentations;
    private EditText channels;
    private EditText profileUrl;
    private Spinner lookback;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() {
            updateStatus();
            updateResults();
            uiHandler.postDelayed(this, 2500L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new LeadStore(this);
        setTitle("Universal Lead Radar v8");
        setContentView(buildUi());
        loadSettings();
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
        updateResults();
        uiHandler.removeCallbacks(autoRefresh);
        uiHandler.postDelayed(autoRefresh, 800L);
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
        title.setText("Universal Telegram Lead Radar v8");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("РУЧНОЙ ПОИСК БЫСТРЫХ ЗАКАЗОВ.\n"
                + "Только создание презентаций и создание лендингов / простых сайтов. "
                + "В результат попадает только заявка с прямым Telegram-контактом: @username или t.me/username. "
                + "Kwork, YouDo и любые другие внешние биржи как контакт запрещены.");
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

        root.addView(label("За какой период проверить историю"));
        lookback = new Spinner(this);
        String[] periods = {"24 часа", "3 дня", "7 дней", "14 дней"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lookback.setAdapter(adapter);
        root.addView(lookback, fullWidth());

        TextView periodHelp = new TextView(this);
        periodHelp.setText("По умолчанию 7 дней. Каждый публичный канал листается назад до выбранной даты.");
        periodHelp.setTextSize(13);
        root.addView(periodHelp);

        root.addView(label("Telegram-каналы с заказами"));
        TextView channelHelp = new TextView(this);
        channelHelp.setText("Возвращены удачные старые Telegram-источники. Потоки, ведущие только на Kwork/YouDo, удалены. "
                + "Даже если в другом канале встретится внешняя биржа, такая заявка не попадёт в результат без прямого Telegram-контакта. Список можно редактировать.");
        channelHelp.setTextSize(13);
        root.addView(channelHelp);

        channels = new EditText(this);
        channels.setMinLines(10);
        channels.setGravity(android.view.Gravity.TOP);
        root.addView(channels, fullWidth());

        root.addView(label("Ссылка на портфолио"));
        profileUrl = new EditText(this);
        profileUrl.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(profileUrl, fullWidth());

        Button start = new Button(this);
        start.setText("ЗАПУСТИТЬ ОДИН РУЧНОЙ ПОИСК");
        start.setOnClickListener(v -> startManualSearch());
        root.addView(start, buttonParams());

        Button stop = new Button(this);
        stop.setText("ОСТАНОВИТЬ ТЕКУЩИЙ ПОИСК");
        stop.setOnClickListener(v -> stopManualSearch());
        root.addView(stop, buttonParams());

        Button openPost = new Button(this);
        openPost.setText("ОТКРЫТЬ ИСХОДНЫЙ ПОСТ ПОСЛЕДНЕЙ ЗАЯВКИ");
        openPost.setOnClickListener(v -> openUrl(store.lastPreviewPost(), "Пока нет найденных заявок"));
        root.addView(openPost, buttonParams());

        Button openContact = new Button(this);
        openContact.setText("ОТКРЫТЬ TELEGRAM-КОНТАКТ ПОСЛЕДНЕЙ ЗАЯВКИ");
        openContact.setOnClickListener(v -> {
            String user = store.lastPreviewUser();
            String url = LeadStore.contactUrl(user);
            openUrl(url, "Пока нет прямого Telegram-контакта");
        });
        root.addView(openContact, buttonParams());

        Button copyMessage = new Button(this);
        copyMessage.setText("СКОПИРОВАТЬ ОТКЛИК ПОСЛЕДНЕЙ ЗАЯВКИ");
        copyMessage.setOnClickListener(v -> {
            String message = store.lastPreviewMessage();
            if (message == null || message.isEmpty()) {
                Toast.makeText(this, "Пока нет подготовленного отклика", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Отклик", message));
            Toast.makeText(this, "Отклик скопирован. Отправляешь его только ты сама.", Toast.LENGTH_LONG).show();
        });
        root.addView(copyMessage, buttonParams());

        Button clear = new Button(this);
        clear.setText("ОЧИСТИТЬ СОХРАНЁННЫЕ РЕЗУЛЬТАТЫ");
        clear.setOnClickListener(v -> {
            if (store.running()) {
                Toast.makeText(this, "Сначала останови текущий поиск", Toast.LENGTH_SHORT).show();
                return;
            }
            store.clearPreviewHistory();
            updateStatus();
            updateResults();
        });
        root.addView(clear, buttonParams());

        root.addView(label("Найденные быстрые заказы"));
        TextView hint = new TextView(this);
        hint.setText("Каждый результат обязан содержать прямой Telegram-контакт, исходный Telegram-пост, бюджет если указан и подготовленный отклик. Внешние биржи не показываются.");
        hint.setTextSize(13);
        root.addView(hint);

        results = new TextView(this);
        results.setTextSize(14);
        results.setTextIsSelectable(true);
        results.setAutoLinkMask(Linkify.WEB_URLS);
        results.setMovementMethod(LinkMovementMethod.getInstance());
        results.setPadding(dp(10), dp(10), dp(10), dp(18));
        root.addView(results, fullWidth());

        return scroll;
    }

    private void loadSettings() {
        sites.setChecked(store.sitesEnabled());
        presentations.setChecked(store.presentationsEnabled());
        channels.setText(store.channels());
        profileUrl.setText(store.profileUrl());
        setLookbackSelection(store.lookbackDays());
        updateStatus();
        updateResults();
    }

    private void saveSettings() {
        store.setCategories(sites.isChecked(), presentations.isChecked());
        store.setChannels(channels.getText().toString());
        store.setProfileUrl(profileUrl.getText().toString());
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
        Intent i = new Intent(this, ScannerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i); else startService(i);
        Toast.makeText(this, "Ручной поиск заявок с Telegram-контактами запущен", Toast.LENGTH_LONG).show();
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
        s.append("Контакт: ТОЛЬКО TELEGRAM\n");
        s.append("Период: ").append(periodLabel(store.lookbackDays())).append("\n");
        s.append("Поиск: ").append(store.running() ? "ИДЁТ" : "НЕ ЗАПУЩЕН").append("\n");
        int total = store.totalChannels();
        if (total > 0) s.append("Проверено источников: ").append(store.checkedChannels()).append(" / ").append(total).append("\n");
        else s.append("Источников в списке: ").append(countChannels(store.channels())).append("\n");
        if (store.running() && !store.currentChannel().isEmpty()) s.append("Сейчас: @").append(store.currentChannel()).append("\n");
        s.append("Найдено за запуск: ").append(store.runFound()).append("\n");
        s.append("Всего сохранено: ").append(store.previewCount());
        status.setText(s.toString());
    }

    private void updateResults() {
        if (results == null) return;
        String history = store.previewHistory();
        results.setText(history == null || history.isEmpty()
                ? "Пока подходящих заявок с прямым Telegram-контактом нет. Выбери период и запусти поиск."
                : history);
        Linkify.addLinks(results, Linkify.WEB_URLS);
    }

    private void openUrl(String url, String emptyMessage) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, emptyMessage, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private int selectedLookbackDays() {
        int pos = lookback == null ? 2 : lookback.getSelectedItemPosition();
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
            String c = LeadScannerV8.normalizeChannel(piece);
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
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = fullWidth();
        p.topMargin = dp(10);
        return p;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
