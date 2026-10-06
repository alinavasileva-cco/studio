package com.thecase.dmqueue;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContactStore {
    private static final String PREFS = "the_case_dm";
    private static final String KEY_CONTACTS = "contacts";
    private final SharedPreferences prefs;

    public ContactStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Contact> load() {
        ArrayList<Contact> out = new ArrayList<>();
        String raw = prefs.getString(KEY_CONTACTS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) out.add(Contact.fromJson(o));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public void save(List<Contact> contacts) {
        JSONArray arr = new JSONArray();
        for (Contact c : contacts) {
            try { arr.put(c.toJson()); } catch (Exception ignored) {}
        }
        prefs.edit().putString(KEY_CONTACTS, arr.toString()).apply();
    }

    public Contact findById(List<Contact> contacts, String id) {
        for (Contact c : contacts) if (c.id.equals(id)) return c;
        return null;
    }

    public Contact findByHandle(List<Contact> contacts, String handle) {
        String h = normalizeHandle(handle);
        for (Contact c : contacts) {
            if (normalizeHandle(c.instagram).equalsIgnoreCase(h)) return c;
        }
        return null;
    }

    public int importContacts(List<Contact> contacts, List<Contact> incoming, boolean updateExisting) {
        int changed = 0;
        for (Contact n : incoming) {
            Contact existing = findByHandle(contacts, n.instagram);
            if (existing == null) {
                contacts.add(n);
                changed++;
            } else if (updateExisting) {
                if (!n.brand.isEmpty()) existing.brand = n.brand;
                if (!n.message.isEmpty()) existing.message = n.message;
                if (!n.language.isEmpty()) existing.language = n.language;
                if (!n.note.isEmpty()) existing.note = n.note;
                existing.status = "new";
                existing.updatedAt = System.currentTimeMillis();
                changed++;
            }
        }
        save(contacts);
        return changed;
    }

    public static String normalizeHandle(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("@")) s = s.substring(1);
        s = s.replace("https://", "").replace("http://", "");
        if (s.startsWith("www.")) s = s.substring(4);
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("instagram.com/")) s = s.substring("instagram.com/".length());
        else if (lower.startsWith("www.instagram.com/")) s = s.substring("www.instagram.com/".length());
        int q = s.indexOf('?'); if (q >= 0) s = s.substring(0, q);
        int hash = s.indexOf('#'); if (hash >= 0) s = s.substring(0, hash);
        int slash = s.indexOf('/'); if (slash >= 0) s = s.substring(0, slash);
        return s.trim();
    }

    public void clear() {
        prefs.edit().remove(KEY_CONTACTS).apply();
    }

    public SharedPreferences prefs() {
        return prefs;
    }
}
