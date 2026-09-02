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

public final class MainActivity extends Activity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new LeadStore(this);
        setTitle("Universal Lead Radar v6");
        setContentView(buildUi());
        loadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        updateResults();
        uiHandler.removeCallbacks(autoRefresh);
        uiHandler.postDelayed(autoRefresh, 800L);
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
        title.setText("Universal Telegram Lead Radar v6");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("РУЧНОЙ РЕЖИМ · 100 ИСТОЧНИКОВ. Никакой автоотправки.\n"
                + "Один запуск проходит выбранные каналы и листает историю назад до выбранной даты. "
                + "Ищем только создание презентаций и создание лендингов / простых сайтов; Tilda и WordPress исключены.");
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

        root.addView(label("За какой период проверить историю каждого канала"));
        lookback = new Spinner(this);
        String[] periods = {"24 часа", "3 дня", "7 дней", "14 дней"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lookback.setAdapter(periodAdapter);
        root.addView(lookback, fullWidth());

        TextView periodHelp = new TextView(this);
        periodHelp.setText("По умолчанию — 7 дней. Бот листает публичную историю канала назад до границы выбранного периода, затем переходит к следующему каналу.");
        periodHelp.setTextSize(13);
        periodHelp.setPadding(0, dp(4), 0, dp(6));
        root.addView(periodHelp);

        root.addView(label("Публичные Telegram-каналы"));
        TextView channelHelp = new TextView(this);
        channelHelp.setText("Стартовый каталог v6 содержит 100 источников: сначала профильные дизайн/фриланс-каналы, затем более широкие каналы удалённой работы. "
                + "Список можно редактировать. Один ручной запуск проходит его один раз и останавливается.");
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

        Button refresh = new Button(this);
        refresh.setText("ОБНОВИТЬ РЕЗУЛЬТАТЫ НА ЭКРАНЕ");
        refresh.setOnClickListener(v -> {
            updateStatus();
            updateResults();
        });
        root.addView(refresh, buttonParams());

        Button openPost = new Button(this);
        openPost.setText("ОТКРЫТЬ ИСХОДНЫЙ ПОСТ ПОСЛЕДНЕЙ ЗАЯВКИ");
        openPost.setOnClickListener(v -> {
            String url = store.lastPreviewPost();
            if (url == null || url.isEmpty()) {
                Toast.makeText(this, "Пока нет найденных заявок", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
        root.addView(openPost, buttonParams());

        Button openContact = new Button(this);
        openContact.setText("ОТКРЫТЬ TELEGRAM-КОНТАКТ ПОСЛЕДНЕЙ ЗАЯВКИ");
        openContact.setOnClickListener(v -> {
            String user = store.lastPreviewUser();
            if (user == null || user.isEmpty()) {
                Toast.makeText(this, "Пока нет найденного контакта", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/" + user)));
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
            Toast.makeText(this, "Результаты очищены. Эти заявки можно найти заново.", Toast.LENGTH_SHORT).show();
        });
        root.addView(clear, buttonParams());

        root.addView(label("Найденные заявки"));
        TextView manualHint = new TextView(this);
        manualHint.setText("В каждой заявке есть кликабельный Telegram-контакт, исходный пост, бюджет (если указан) и подготовленный текст. "
                + "Ничего не отправляется автоматически.");
        manualHint.setTextSize(13);
        root.addView(manualHint);

        results = new TextView(this);
        results.setTextSize(14);
        results.setTextIsSelectable(true);
        results.setAutoLinkMask(Linkify.WEB_URLS);
        results.setMovementMethod(LinkMovementMethod.getInstance());
        results.setPadding(dp(10), dp(10), dp(10), dp(18));
        root.addView(results, fullWidth());

        TextView note = new TextView(this);
        note.setText("Нет расписания, периодических запусков, автоотправки и автозапуска после перезагрузки. "
                + "Поиск работает только после твоего нажатия. Чем больше каналов и глубже период, тем дольше один проход — приложение его искусственно по времени не обрывает.");
        note.setTextSize(13);
        note.setPadding(0, dp(14), 0, dp(20));
        root.addView(note);

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
            Toast.makeText(this, "Поиск уже идёт. Дождись завершения или нажми «Остановить».", Toast.LENGTH_LONG).show();
            return;
        }
        if (!sites.isChecked() && !presentations.isChecked()) {
            Toast.makeText(this, "Выбери хотя бы одно направление поиска", Toast.LENGTH_SHORT).show();
            return;
        }
        if (channels.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Список каналов пуст", Toast.LENGTH_SHORT).show();
            return;
        }
        saveSettings();
        requestNotificationsIfNeeded();
        Intent i = new Intent(this, ScannerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i); else startService(i);
        Toast.makeText(this, "Ручной поиск запущен за " + periodLabel(store.lookbackDays())
                + ". После полного прохода он остановится сам.", Toast.LENGTH_LONG).show();
        uiHandler.postDelayed(this::updateStatus, 500L);
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
        boolean running = store.running();
        int checked = store.checkedChannels();
        int total = store.totalChannels();
        String current = store.currentChannel();
        StringBuilder s = new StringBuilder();
        s.append("Режим: ТОЛЬКО РУЧНОЙ · АВТООТПРАВКИ НЕТ\n");
        s.append("Период: ").append(periodLabel(store.lookbackDays())).append("\n");
        s.append("Поиск: ").append(running ? "ИДЁТ" : "НЕ ЗАПУЩЕН").append("\n");
        if (total > 0) s.append("Проверено каналов: ").append(checked).append(" / ").append(total).append("\n");
        else s.append("Каналов в текущем списке: ").append(countChannels(store.channels())).append("\n");
        if (running && current != null && !current.isEmpty()) s.append("Сейчас: @").append(current).append("\n");
        s.append("Найдено за текущий запуск: ").append(store.runFound()).append("\n");
        s.append("Всего сохранено: ").append(store.previewCount());
        status.setText(s.toString());
    }

    private void updateResults() {
        if (results == null) return;
        String history = store.previewHistory();
        results.setText(history == null || history.isEmpty()
                ? "Пока ничего не найдено. Выбери период и нажми «Запустить один ручной поиск»."
                : history);
        Linkify.addLinks(results, Linkify.WEB_URLS);
    }

    private int selectedLookbackDays() {
        int pos = lookback == null ? 2 : lookback.getSelectedItemPosition();
        if (pos == 0) return 1;
        if (pos == 1) return 3;
        if (pos == 3) return 14;
        return 7;
    }

    private void setLookbackSelection(int days) {
        if (lookback == null) return;
        int pos = days == 1 ? 0 : days == 3 ? 1 : days == 14 ? 3 : 2;
        lookback.setSelection(pos);
    }

    private String periodLabel(int days) {
        if (days == 1) return "24 часа";
        if (days == 3) return "3 дня";
        if (days == 14) return "14 дней";
        return "7 дней";
    }

    private int countChannels(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0;
        java.util.HashSet<String> unique = new java.util.HashSet<>();
        String[] pieces = raw.split("[\\n,; ]+");
        for (String piece : pieces) {
            String c = LeadScanner.normalizeChannel(piece);
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
