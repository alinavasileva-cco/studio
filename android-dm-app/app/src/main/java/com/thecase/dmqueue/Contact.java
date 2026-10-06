package com.thecase.dmqueue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class Contact {
    public String id = UUID.randomUUID().toString();
    public String instagram = "";
    public String brand = "";
    public String message = "";
    public String language = "";
    public String note = "";
    public String status = "new";
    public long importedAt = System.currentTimeMillis();
    public long updatedAt = System.currentTimeMillis();

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("instagram", instagram);
        o.put("brand", brand);
        o.put("message", message);
        o.put("language", language);
        o.put("note", note);
        o.put("status", status);
        o.put("importedAt", importedAt);
        o.put("updatedAt", updatedAt);
        return o;
    }

    public static Contact fromJson(JSONObject o) {
        Contact c = new Contact();
        c.id = o.optString("id", UUID.randomUUID().toString());
        c.instagram = o.optString("instagram", "");
        c.brand = o.optString("brand", "");
        c.message = o.optString("message", "");
        c.language = o.optString("language", "");
        c.note = o.optString("note", "");
        c.status = o.optString("status", "new");
        c.importedAt = o.optLong("importedAt", System.currentTimeMillis());
        c.updatedAt = o.optLong("updatedAt", c.importedAt);
        return c;
    }
}
