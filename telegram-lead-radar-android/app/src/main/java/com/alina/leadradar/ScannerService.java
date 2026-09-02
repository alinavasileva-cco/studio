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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ScannerService extends Service {
    public static final String ACTION_STOP = "com.alina.leadradar.STOP";
    private static final String CHANNEL_ID = "lead_radar_scanner";
    private static final int NOTIFICATION_ID = 42;

    private final AtomicBoolean workerStarted = new AtomicBoolean(false);
    private volatile boolean running = true;
    private LeadStore store;

    @Override
    public void onCreate() {
        super.onCreate();
        store = new LeadStore(this);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, notification("Запуск поиска заявок…"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            store.setEnabled(false);
            running = false;
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        store.setEnabled(true);
        if (workerStarted.compareAndSet(false, true)) {
            Thread t = new Thread(this::workerLoop, "lead-radar-scanner");
            t.setDaemon(true);
            t.start();
        }
        return START_STICKY;
    }

    private void workerLoop() {
        LeadScanner scanner = new LeadScanner();
        while (running && store.enabled()) {
            try {
                if (!store.previewMode() && store.hasPending()) {
                    long age = System.currentTimeMillis() - store.pendingSince();
                    if (age > 15_000L) {
                        updateNotification("Ожидаю отправку в @" + store.pendingUser());
                        AutoSendAccessibilityService.requestProcess(this);
                    }
                    sleep(10_000L);
                    continue;
                }

                if (!store.previewMode()) {
                    long minPauseMs = store.sendPauseSeconds() * 1000L;
                    long sinceLastSend = System.currentTimeMillis() - store.lastSendAt();
                    if (store.lastSendAt() > 0 && sinceLastSend < minPauseMs) {
                        long left = Math.max(1, (minPauseMs - sinceLastSend) / 1000L);
                        updateNotification("Следующая отправка через ~" + left + " сек.");
                        sleep(Math.min(10_000L, minPauseMs - sinceLastSend));
                        continue;
                    }
                }

                List<String> channels = parseChannels(store.channels());
                updateNotification(store.previewMode()
                        ? "ТЕСТ v3: разбираю задания в " + channels.size() + " каналах…"
                        : "Проверяю " + channels.size() + " Telegram-каналов…");

                if (store.previewMode()) {
                    int found = runPreviewScan(scanner, channels);
                    updateNotification(found == 0
                            ? "ТЕСТ v3: новых релевантных заданий нет"
                            : "ТЕСТ v3: найдено заданий: " + found + ". Ничего не отправлено");
                    sleep(store.scanMinutes() * 60_000L);
                } else {
                    boolean queued = runAutoScan(scanner, channels);
                    if (!queued) {
                        updateNotification("Новых подходящих заявок нет. Отправлено: " + store.sentCount());
                        sleep(store.scanMinutes() * 60_000L);
                    } else {
                        sleep(10_000L);
                    }
                }
            } catch (Throwable ignored) {
                updateNotification("Временная ошибка. Повторю автоматически");
                sleep(30_000L);
            }
        }
        stopSelf();
    }

    private int runPreviewScan(LeadScanner scanner, List<String> channels) {
        int found = 0;
        for (String channel : channels) {
            if (!running || !store.enabled()) break;
            try {
                List<Lead> leads = scanner.scanChannel(channel, store);
                for (int i = leads.size() - 1; i >= 0; i--) {
                    Lead lead = leads.get(i);
                    if (store.wasSent(lead.dedupKey) || store.wasPreviewed(lead.dedupKey)) continue;
                    String message = MessageComposer.compose(lead, store.profileUrl());
                    store.addPreview(lead, message);
                    found++;
                    if (found >= 15) return found;
                }
            } catch (Exception ignored) {
                updateNotification("ТЕСТ: не удалось проверить @" + channel + "; продолжаю");
            }
        }
        return found;
    }

    private boolean runAutoScan(LeadScanner scanner, List<String> channels) {
        for (String channel : channels) {
            if (!running || !store.enabled() || store.hasPending()) break;
            try {
                List<Lead> leads = scanner.scanChannel(channel, store);
                for (int i = leads.size() - 1; i >= 0; i--) {
                    Lead lead = leads.get(i);
                    if (store.wasSent(lead.dedupKey)) continue;
                    String message = MessageComposer.compose(lead, store.profileUrl());
                    store.setPending(lead, message);
                    updateNotification("Найдена заявка: " + categoryName(lead.category) + " → @" + lead.username);
                    AutoSendAccessibilityService.requestProcess(this);
                    return true;
                }
            } catch (Exception ignored) {
                updateNotification("Не удалось проверить @" + channel + "; продолжаю");
            }
        }
        return false;
    }

    private List<String> parseChannels(String raw) {
        if (raw == null) return Collections.emptyList();
        String[] pieces = raw.split("[\\n,; ]+");
        List<String> result = new ArrayList<>();
        for (String piece : pieces) {
            String c = LeadScanner.normalizeChannel(piece);
            if (!c.isEmpty() && !result.contains(c)) result.add(c);
        }
        return result;
    }

    private String categoryName(Lead.Category c) {
        if (c == Lead.Category.SITE) return "сайт";
        if (c == Lead.Category.PRESENTATION) return "презентация";
        if (c == Lead.Category.AI) return "AI / ИИ";
        return "консалтинг";
    }

    private void sleep(long ms) {
        try { Thread.sleep(Math.max(1000L, ms)); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Lead Radar", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Поиск и обработка заявок");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification notification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        String title = store != null && store.previewMode() ? "Lead Radar v3 — ТЕСТ" : "Lead Radar работает";
        return b.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification(text));
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { running = false; workerStarted.set(false); super.onDestroy(); }
}
