package com.thecase.dmqueue;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InstagramAccessibilityService extends AccessibilityService {
    private static final long PENDING_TTL_MS = 90_000L;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!"com.instagram.android".contentEquals(event.getPackageName())) return;

        SharedPreferences p = getSharedPreferences("the_case_dm", MODE_PRIVATE);
        if (!p.getBoolean("pending_paste", false)) return;

        long at = p.getLong("pending_at", 0L);
        if (at <= 0L || System.currentTimeMillis() - at > PENDING_TTL_MS) {
            p.edit().putBoolean("pending_paste", false).putString("pending_stage","timeout").apply();
            return;
        }

        String message = p.getString("pending_message", "");
        if (message == null || message.isEmpty()) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        AccessibilityNodeInfo editor = bestEditor(root);
        if (editor != null) {
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message);
            boolean ok = editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            if (ok) {
                p.edit()
                        .putBoolean("pending_paste", false)
                        .putString("pending_stage","inserted")
                        .putLong("last_paste_at",System.currentTimeMillis())
                        .apply();
                Intent done = new Intent("com.thecase.dmqueue.PASTE_DONE");
                done.setPackage(getPackageName());
                sendBroadcast(done);
                return;
            }
        }

        String stage = p.getString("pending_stage", "open");
        if (!"clicked_message".equals(stage)) {
            AccessibilityNodeInfo button = findMessageButton(root);
            if (button != null && clickNode(button)) {
                p.edit().putString("pending_stage","clicked_message").apply();
            }
        }
    }

    private AccessibilityNodeInfo bestEditor(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> all = new ArrayList<>();
        collect(root, all);
        AccessibilityNodeInfo best = null;
        int bestScore = -999;
        int editableCount = 0;

        for (AccessibilityNodeInfo n : all) {
            if (!n.isEditable()) continue;
            editableCount++;
            int score = 0;
            String blob = nodeText(n);
            if (containsAny(blob, "message", "сообщ", "напис", "direct", "chat", "write")) score += 10;
            String cls = String.valueOf(n.getClassName()).toLowerCase(Locale.ROOT);
            if (cls.contains("edittext")) score += 4;
            if (n.isFocused()) score += 3;
            if (n.isVisibleToUser()) score += 2;
            if (score > bestScore) { bestScore = score; best = n; }
        }

        if (bestScore >= 8) return best;
        String stage = getSharedPreferences("the_case_dm", MODE_PRIVATE).getString("pending_stage","open");
        if ("clicked_message".equals(stage) && editableCount == 1) return best;
        return null;
    }

    private AccessibilityNodeInfo findMessageButton(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> all = new ArrayList<>();
        collect(root, all);
        for (AccessibilityNodeInfo n : all) {
            String blob = nodeText(n);
            if (containsAny(blob, "message", "сообщение", "написать", "написати", "mensagem", "mensaje", "nachricht")) {
                if (n.isClickable() || hasClickableParent(n)) return n;
            }
        }
        return null;
    }

    private boolean clickNode(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        for (int i=0;i<5 && cur!=null;i++) {
            if (cur.isClickable() && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            cur = cur.getParent();
        }
        return false;
    }

    private boolean hasClickableParent(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur=n;
        for(int i=0;i<5 && cur!=null;i++) {
            if(cur.isClickable()) return true;
            cur=cur.getParent();
        }
        return false;
    }

    private void collect(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> out) {
        if (n == null) return;
        out.add(n);
        for (int i=0;i<n.getChildCount();i++) collect(n.getChild(i), out);
    }

    private String nodeText(AccessibilityNodeInfo n) {
        StringBuilder sb=new StringBuilder();
        if(n.getText()!=null) sb.append(n.getText()).append(' ');
        if(n.getContentDescription()!=null) sb.append(n.getContentDescription()).append(' ');
        if(android.os.Build.VERSION.SDK_INT>=26 && n.getHintText()!=null) sb.append(n.getHintText()).append(' ');
        if(n.getViewIdResourceName()!=null) sb.append(n.getViewIdResourceName());
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String s,String... needles) {
        for(String n:needles) if(s.contains(n)) return true;
        return false;
    }

    @Override
    public void onInterrupt() {}
}
