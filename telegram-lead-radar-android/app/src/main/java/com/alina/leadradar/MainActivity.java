package com.alina.leadradar;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private LeadStore store;
    private TextView status;
    private TextView previewHistory;
    private CheckBox sites;
    private CheckBox presentations;
    private CheckBox previewMode;
    private EditText channels;
    private EditText scanMinutes;
    private EditText sendPause;
    private EditText profileUrl;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() {
            updateStatus();
            updatePreviewHistory();
            uiHandler.postDelayed(this, 3000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new LeadStore(this);
        setTitle("Universal Lead Radar v4");
        setContentView(buildUi());
        loadSettings();
        if (store.enabled()) startScannerService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        updatePreviewHistory();
        uiHandler.removeCallbacks(autoRefresh);
        uiHandler.postDelayed(autoRefresh, 1000L);
    }

    @Override
    protected void onPause() {
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
        title.setText("Universal Telegram Lead Radar v4");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("ТОЛЬКО: создание презентаций + создание лендингов / простых сайтов.\nИсключено: Tilda, WordPress, AI/ИИ, логотипы, айдентика, фирменный стиль и обычные вакансии.");
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(8), 0, dp(14));
        root.addView(subtitle);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 0, 0, dp(12));
        root.addView(status);

        previewMode = new CheckBox(this);
        previewMode.setText("ТЕСТОВЫЙ РЕЖИМ — искать и показывать, НИЧЕГО НЕ ОТПРАВЛЯТЬ");
        previewMode.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(previewMode);

        root.addView(label("Что искать"));
        sites = new CheckBox(this);
        sites.setText("Создание лендинга / простого сайта — НЕ Tilda и НЕ WordPress");
        root.addView(sites);
        presentations = new CheckBox(this);
        presentations.setText("Создание / оформление презентации, PowerPoint / PDF / pitch deck");
        root.addView(presentations);

        root.addView(label("Публичные Telegram-каналы (расширенный профильный пул)"));
        channels = new EditText(this);
        channels.setMinLines(8);
        channels.setGravity(android.view.Gravity.TOP);
        root.addView(channels, fullWidth());

        root.addView(label("Проверять каждые N минут (минимум 2)"));
        scanMinutes = new EditText(this);
        scanMinutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(scanMinutes, fullWidth());

        root.addView(label("Пауза между автоотправками, секунд (только авто-режим, минимум 30)"));
        sendPause = new EditText(this);
        sendPause.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(sendPause, fullWidth());

        root.addView(label("Ссылка на портфолио"));
        profileUrl = new EditText(this);
        profileUrl.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(profileUrl, fullWidth());

        Button accessibility = new Button(this);
        accessibility.setText("1. Проверить / включить Accessibility для Lead Radar");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibility, buttonParams());

        Button start = new Button(this);
        start.setText("2. Сохранить и запустить поиск");
        start.setOnClickListener(v -> {
            saveSettings();
            store.setEnabled(true);
            if (store.previewMode()) store.clearPending();
            requestNotificationsIfNeeded();
            startScannerService();
            updateStatus();
            Toast.makeText(this, store.previewMode() ? "Тестовый поиск запущен — отправки отключены" : "Автоматический режим запущен", Toast.LENGTH_LONG).show();
        });
        root.addView(start, buttonParams());

        Button stop = new Button(this);
        stop.setText("Остановить поиск");
        stop.setOnClickListener(v -> {
            store.setEnabled(false);
            Intent i = new Intent(this, ScannerService.class);
            i.setAction(ScannerService.ACTION_STOP);
            startService(i);
            updateStatus();
        });
        root.addView(stop, buttonParams());

        Button refresh = new Button(this);
        refresh.setText("Обновить найденные заявки на экране");
        refresh.setOnClickListener(v -> {
            updateStatus();
            updatePreviewHistory();
        });
        root.addView(refresh, buttonParams());

        Button openLast = new Button(this);
        openLast.setText("Открыть исходный пост последней заявки");
        openLast.setOnClickListener(v -> {
            String url = store.lastPreviewPost();
            if (url == null || url.isEmpty()) {
                Toast.makeText(this, "Пока нет найденных заявок", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
        root.addView(openLast, buttonParams());

        Button clearHistory = new Button(this);
        clearHistory.setText("Очистить тестовую историю и проверить заново");
        clearHistory.setOnClickListener(v -> {
            store.clearPreviewHistory();
            updatePreviewHistory();
            Toast.makeText(this, "История очищена — можно повторить проверку", Toast.LENGTH_SHORT).show();
        });
        root.addView(clearHistory, buttonParams());

        Button retry = new Button(this);
        retry.setText("Повторить ожидающую автоотправку");
        retry.setOnClickListener(v -> {
            if (store.previewMode()) Toast.makeText(this, "В тестовом режиме отправка отключена", Toast.LENGTH_SHORT).show();
            else AutoSendAccessibilityService.requestProcess(this);
        });
        root.addView(retry, buttonParams());

        root.addView(label("Последние релевантные заявки и подготовленные сообщения"));
        previewHistory = new TextView(this);
        previewHistory.setTextSize(14);
        previewHistory.setTextIsSelectable(true);
        previewHistory.setAutoLinkMask(Linkify.WEB_URLS);
        previewHistory.setMovementMethod(LinkMovementMethod.getInstance());
        previewHistory.setPadding(dp(10), dp(10), dp(10), dp(16));
        root.addView(previewHistory, fullWidth());

        TextView note = new TextView(this);
        note.setText("v4 использует расширенный набор профильных каналов и пропускает только конкретные заказы на презентации или создание сайта/лендинга. Если в задаче требуется Tilda, WordPress, логотип, айдентика, фирменный стиль или это штатная/долгосрочная вакансия — заявка отбрасывается.");
        note.setTextSize(13);
        note.setPadding(0, dp(14), 0, dp(20));
        root.addView(note);

        return scroll;
    }

    private void loadSettings() {
        sites.setChecked(store.sitesEnabled());
        presentations.setChecked(store.presentationsEnabled());
        previewMode.setChecked(store.previewMode());
        channels.setText(store.channels());
        scanMinutes.setText(String.valueOf(store.scanMinutes()));
        sendPause.setText(String.valueOf(store.sendPauseSeconds()));
        profileUrl.setText(store.profileUrl());
        updateStatus();
        updatePreviewHistory();
    }

    private void saveSettings() {
        store.setCategories(sites.isChecked(), presentations.isChecked());
        store.setPreviewMode(previewMode.isChecked());
        store.setChannels(channels.getText().toString());
        store.setScanMinutes(parseInt(scanMinutes.getText().toString(), 5));
        store.setSendPauseSeconds(parseInt(sendPause.getText().toString(), 90));
        store.setProfileUrl(profileUrl.getText().toString());
    }

    private void updateStatus() {
        if (status == null) return;
        String accessibility = isAccessibilityEnabled() ? "ВКЛ" : "ВЫКЛ";
        String engine = store.enabled() ? "РАБОТАЕТ" : "ОСТАНОВЛЕН";
        String mode = store.previewMode() ? "ТЕСТ — НЕ ОТПРАВЛЯЕТ" : "АВТООТПРАВКА";
        String pending = !store.previewMode() && store.hasPending() ? "\nОжидает отправки: @" + store.pendingUser() : "";
        status.setText("Поиск: " + engine + "\nРежим: " + mode + "\nAccessibility: " + accessibility
                + "\nНайдено в тесте: " + store.previewCount() + "\nОтправлено: " + store.sentCount() + pending);
    }

    private void updatePreviewHistory() {
        if (previewHistory == null) return;
        String history = store.previewHistory();
        previewHistory.setText(history == null || history.isEmpty()
                ? "Пока релевантных заявок нет. После запуска результаты появятся здесь автоматически."
                : history);
        Linkify.addLinks(previewHistory, Linkify.WEB_URLS);
    }

    private boolean isAccessibilityEnabled() {
        ComponentName expected = new ComponentName(this, AutoSendAccessibilityService.class);
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            ComponentName actual = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(actual)) return true;
        }
        return false;
    }

    private void startScannerService() {
        Intent i = new Intent(this, ScannerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i); else startService(i);
    }

    private void requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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

    private int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception ignored) { return fallback; }
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
