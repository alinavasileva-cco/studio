package com.alina.leadradar;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
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
    private CheckBox sites;
    private CheckBox presentations;
    private CheckBox consulting;
    private EditText channels;
    private EditText scanMinutes;
    private EditText sendPause;
    private EditText profileUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new LeadStore(this);
        setTitle("Universal Lead Radar");
        setContentView(buildUi());
        loadSettings();
        if (store.enabled()) startScannerService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private View buildUi() {
        int pad = dp(18);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Universal Telegram Lead Radar");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Сайты · Презентации · Консалтинг\nИщет только публичные русскоязычные запросы с прямым контактом для отклика. Локального дневного лимита нет.");
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(8), 0, dp(14));
        root.addView(subtitle);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 0, 0, dp(12));
        root.addView(status);

        TextView catLabel = label("Что искать");
        root.addView(catLabel);
        sites = new CheckBox(this); sites.setText("Сайты-визитки / простые лендинги"); root.addView(sites);
        presentations = new CheckBox(this); presentations.setText("Презентации"); root.addView(presentations);
        consulting = new CheckBox(this); consulting.setText("Бизнес-консалтинг / трекинг"); root.addView(consulting);

        root.addView(label("Публичные Telegram-каналы (по одному на строке)"));
        channels = new EditText(this);
        channels.setMinLines(6);
        channels.setGravity(android.view.Gravity.TOP);
        root.addView(channels, fullWidth());

        root.addView(label("Проверять каждые N минут (минимум 2)"));
        scanMinutes = new EditText(this);
        scanMinutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(scanMinutes, fullWidth());

        root.addView(label("Техническая пауза между отправками, секунд (минимум 30)"));
        sendPause = new EditText(this);
        sendPause.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(sendPause, fullWidth());

        root.addView(label("Ссылка, которую добавлять в сообщение"));
        profileUrl = new EditText(this);
        profileUrl.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(profileUrl, fullWidth());

        Button accessibility = new Button(this);
        accessibility.setText("1. Включить Accessibility для Lead Radar");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibility, buttonParams());

        Button start = new Button(this);
        start.setText("2. Сохранить и запустить автоматический режим");
        start.setOnClickListener(v -> {
            saveSettings();
            store.setEnabled(true);
            requestNotificationsIfNeeded();
            startScannerService();
            updateStatus();
            Toast.makeText(this, "Lead Radar запущен", Toast.LENGTH_SHORT).show();
        });
        root.addView(start, buttonParams());

        Button stop = new Button(this);
        stop.setText("Остановить");
        stop.setOnClickListener(v -> {
            store.setEnabled(false);
            Intent i = new Intent(this, ScannerService.class);
            i.setAction(ScannerService.ACTION_STOP);
            startService(i);
            updateStatus();
        });
        root.addView(stop, buttonParams());

        Button retry = new Button(this);
        retry.setText("Повторить текущую ожидающую отправку");
        retry.setOnClickListener(v -> AutoSendAccessibilityService.requestProcess(this));
        root.addView(retry, buttonParams());

        TextView note = new TextView(this);
        note.setText("Автоотправка выполняется только если в публичном посте есть явный запрос на исполнителя и прямой @username для связи. Вакансии в штат, самореклама исполнителей, англоязычные посты и сложные маркетплейсы отсекаются. Ограничения/антиспам Telegram приложение не обходит.");
        note.setTextSize(13);
        note.setPadding(0, dp(14), 0, dp(20));
        root.addView(note);

        return scroll;
    }

    private void loadSettings() {
        sites.setChecked(store.sitesEnabled());
        presentations.setChecked(store.presentationsEnabled());
        consulting.setChecked(store.consultingEnabled());
        channels.setText(store.channels());
        scanMinutes.setText(String.valueOf(store.scanMinutes()));
        sendPause.setText(String.valueOf(store.sendPauseSeconds()));
        profileUrl.setText(store.profileUrl());
        updateStatus();
    }

    private void saveSettings() {
        store.setCategories(sites.isChecked(), presentations.isChecked(), consulting.isChecked());
        store.setChannels(channels.getText().toString());
        store.setScanMinutes(parseInt(scanMinutes.getText().toString(), 5));
        store.setSendPauseSeconds(parseInt(sendPause.getText().toString(), 90));
        store.setProfileUrl(profileUrl.getText().toString());
    }

    private void updateStatus() {
        if (status == null) return;
        String accessibility = isAccessibilityEnabled() ? "ВКЛ" : "ВЫКЛ";
        String engine = store.enabled() ? "РАБОТАЕТ" : "ОСТАНОВЛЕН";
        String pending = store.hasPending() ? "\nОжидает отправки: @" + store.pendingUser() : "";
        status.setText("Поиск: " + engine + "\nAccessibility: " + accessibility + "\nОтправлено: " + store.sentCount() + pending);
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
