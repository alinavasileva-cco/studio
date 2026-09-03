package com.alina.leadradar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ScannerService extends Service {
    public static final String ACTION_STOP = "com.alina.leadradar.STOP";
    private static final String CHANNEL_ID = "lead_radar_scanner";
    private static final int NOTIFICATION_ID = 42;
    private final AtomicBoolean workerStarted = new AtomicBoolean(false);
    private volatile boolean keepRunning = true;
    private LeadStore store;

    @Override public void onCreate() {
        super.onCreate();
        store = new LeadStore(this);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, notification("Подготовка свежего поиска…", true));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            keepRunning = false;
            store.requestStop();
            updateNotification("Поиск остановлен вручную", false);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (workerStarted.compareAndSet(false, true)) {
            keepRunning = true;
            Thread t = new Thread(this::runOneManualPass, "lead-radar-v12");
            t.setDaemon(true);
            t.start();
        }
        return START_NOT_STICKY;
    }

    private void runOneManualPass() {
        LeadScannerV12 scanner = new LeadScannerV12();
        List<String> channels = parseChannels(store.channels());
        store.beginRun(channels.size());
        int checked = 0;
        try {
            for (String channel : channels) {
                if (!keepRunning || !store.running()) break;
                store.setProgress(checked, channels.size(), channel);
                updateNotification("Проверено " + checked + " из " + channels.size()
                        + " · постов " + store.diagnosticPosts()
                        + " · найдено " + store.runFound()
                        + " · @" + channel, true);
                try {
                    List<Lead> leads = scanner.scanChannel(channel, store);
                    for (int i = leads.size() - 1; i >= 0; i--) {
                        if (!keepRunning || !store.running()) break;
                        Lead lead = leads.get(i);
                        if (lead.category != Lead.Category.SITE && lead.category != Lead.Category.PRESENTATION) continue;
                        if (store.wasPreviewed(lead.dedupKey)) continue;
                        String message = MessageComposer.compose(
                                lead,
                                store.profileUrl(),
                                store.presentationPortfolioUrl()
                        );
                        if (message == null || message.trim().isEmpty()) continue;
                        store.addPreview(lead, message);
                    }
                } catch (Exception ignored) {
                    store.addReadError();
                }
                checked++;
                store.setProgress(checked, channels.size(), "");
            }
        } finally {
            boolean stopped = checked < channels.size() && !store.running();
            int found = store.runFound();
            store.finishRun();
            String text = (stopped ? "Поиск остановлен. " : "Поиск завершён. ")
                    + "Проверено " + checked + " из " + channels.size()
                    + ", постов " + store.diagnosticPosts()
                    + ", найдено " + found
                    + ", ошибок чтения " + store.diagnosticErrors();
            updateNotification(text, false);
            stopForeground(true);
            stopSelf();
        }
    }

    private List<String> parseChannels(String raw) {
        List<String> r = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return r;
        Set<String> seen = new HashSet<>();
        for (String p : raw.split("[\\n,; ]+")) {
            String c = LeadScannerV12.normalizeChannel(p);
            if (!c.isEmpty() && seen.add(c.toLowerCase(java.util.Locale.ROOT))) r.add(c);
        }
        return r;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(
                    CHANNEL_ID,
                    "Lead Radar — ручной поиск",
                    NotificationManager.IMPORTANCE_LOW
            );
            c.setDescription("Ручной поиск Telegram-заказов");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification(String text, boolean ongoing) {
        Intent open = new Intent(this, MainActivityV12.class);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setContentTitle("Lead Radar v12")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentIntent(pi)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .build();
    }

    private void updateNotification(String text, boolean ongoing) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, notification(text, ongoing));
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        keepRunning = false;
        workerStarted.set(false);
        super.onDestroy();
    }
}
