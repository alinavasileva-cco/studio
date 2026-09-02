package com.alina.leadradar;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AutoSendAccessibilityService extends AccessibilityService {
    private static volatile AutoSendAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean attemptInProgress = new AtomicBoolean(false);
    private long lastLaunchAt = 0L;
    private LeadStore store;

    public static void requestProcess(Context context) {
        AutoSendAccessibilityService s = instance;
        if (s != null) s.processPending();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        store = new LeadStore(this);
        handler.postDelayed(this::processPending, 800L);
    }

    private void processPending() {
        if (store == null) store = new LeadStore(this);
        if (!store.hasPending()) return;
        long now = System.currentTimeMillis();
        if (now - lastLaunchAt < 12_000L) return;

        String user = store.pendingUser();
        String message = store.pendingMessage();
        if (user == null || user.isEmpty() || message == null || message.isEmpty()) return;

        lastLaunchAt = now;
        store.bumpPendingAttempt();
        String url = "https://t.me/" + user + "?text=" + Uri.encode(message);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String telegramPackage = findTelegramPackage();
        if (telegramPackage != null) intent.setPackage(telegramPackage);
        try {
            startActivity(intent);
            handler.postDelayed(this::attemptSend, 1800L);
            handler.postDelayed(this::attemptSend, 3500L);
            handler.postDelayed(this::attemptSend, 6000L);
        } catch (Exception ignored) {
            // Scanner keeps the lead pending and will retry later.
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || !isTelegramPackage(String.valueOf(event.getPackageName()))) return;
        if (store == null) store = new LeadStore(this);
        if (!store.hasPending()) return;
        handler.removeCallbacks(this::attemptSend);
        handler.postDelayed(this::attemptSend, 700L);
    }

    private void attemptSend() {
        if (store == null || !store.hasPending()) return;
        if (!attemptInProgress.compareAndSet(false, true)) return;
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;
            String expected = store.pendingMessage();
            if (!hasExpectedDraft(root, expected)) return;

            AccessibilityNodeInfo send = findSendNode(root);
            if (send == null) return;
            AccessibilityNodeInfo clickable = clickableNode(send);
            if (clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                store.markPendingSent();
                handler.postDelayed(this::processPending, 1500L);
            }
        } finally {
            attemptInProgress.set(false);
        }
    }

    private boolean hasExpectedDraft(AccessibilityNodeInfo node, String expected) {
        if (node == null) return false;
        CharSequence text = node.getText();
        if (node.isEditable() && text != null && expected != null) {
            String actual = text.toString().trim();
            String marker = expected.substring(0, Math.min(28, expected.length())).trim();
            if (!marker.isEmpty() && actual.contains(marker)) return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null && hasExpectedDraft(child, expected)) return true;
        }
        return false;
    }

    private AccessibilityNodeInfo findSendNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        String text = node.getText() == null ? "" : node.getText().toString().toLowerCase(Locale.ROOT);
        String desc = node.getContentDescription() == null ? "" : node.getContentDescription().toString().toLowerCase(Locale.ROOT);
        String id = node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName().toLowerCase(Locale.ROOT);

        boolean semanticSend = text.equals("отправить") || text.equals("send")
                || desc.equals("отправить") || desc.equals("send")
                || desc.contains("отправить сообщение") || desc.contains("send message")
                || id.contains("send_button") || id.contains("message_panel_send") || id.endsWith("/send");
        if (semanticSend && (node.isClickable() || (node.getParent() != null && node.getParent().isClickable()))) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findSendNode(child);
            if (found != null) return found;
        }
        return null;
    }

    private AccessibilityNodeInfo clickableNode(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        for (int i = 0; i < 4 && current != null; i++) {
            if (current.isClickable()) return current;
            current = current.getParent();
        }
        return null;
    }

    private String findTelegramPackage() {
        String[] candidates = {"org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram"};
        PackageManager pm = getPackageManager();
        for (String pkg : candidates) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return null;
    }

    private boolean isTelegramPackage(String pkg) {
        return "org.telegram.messenger".equals(pkg)
                || "org.telegram.messenger.web".equals(pkg)
                || "org.thunderdog.challegram".equals(pkg);
    }

    @Override public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
