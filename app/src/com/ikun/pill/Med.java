package com.ikun.pill;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 一条药物记录 */
public class Med {
    public long id;
    public String name = "";
    public String dose = "";
    public String note = "";
    public String cat = "";
    public List<String> times = new ArrayList<>();
    public boolean enabled = true;
    public int stock = -1;
    public int left = -1;

    public Med() {}

    public Med(long id, String name, String dose, List<String> times) {
        this.id = id;
        this.name = name;
        this.dose = dose;
        this.times = times;
    }

    public boolean trackStock() { return stock > 0; }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("dose", dose);
        o.put("note", note);
        o.put("cat", cat);
        o.put("enabled", enabled);
        o.put("stock", stock);
        o.put("left", left);
        JSONArray a = new JSONArray();
        for (String t : times) a.put(t);
        o.put("times", a);
        return o;
    }

    public static Med fromJson(JSONObject o) {
        Med m = new Med();
        m.id = o.optLong("id", 0);
        m.name = o.optString("name", "");
        m.dose = o.optString("dose", "");
        m.note = o.optString("note", "");
        m.cat = o.optString("cat", "");
        m.enabled = o.optBoolean("enabled", true);
        m.stock = o.optInt("stock", -1);
        m.left = o.optInt("left", -1);
        JSONArray a = o.optJSONArray("times");
        if (a != null) {
            for (int i = 0; i < a.length(); i++) m.times.add(a.optString(i));
        }
        return m;
    }
}
