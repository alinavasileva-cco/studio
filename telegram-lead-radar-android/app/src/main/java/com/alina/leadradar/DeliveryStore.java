package com.alina.leadradar;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** Stores only manual workflow state. The app never sends messages itself. */
public final class DeliveryStore {
    private final SharedPreferences p;

    public DeliveryStore(Context context) {
        p = context.getApplicationContext().getSharedPreferences("lead_radar_delivery", Context.MODE_PRIVATE);
    }

    public boolean isSent(String id) {
        if (id == null || id.isEmpty()) return false;
        return new HashSet<>(p.getStringSet("sent_ids", new HashSet<>())).contains(id);
    }

    public void setSent(String id, boolean sent) {
        if (id == null || id.isEmpty()) return;
        Set<String> ids = new HashSet<>(p.getStringSet("sent_ids", new HashSet<>()));
        if (sent) ids.add(id); else ids.remove(id);
        p.edit().putStringSet("sent_ids", ids).apply();
    }

    public void clear() {
        p.edit().remove("sent_ids").apply();
    }
}
