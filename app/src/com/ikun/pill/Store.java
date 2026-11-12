package com.ikun.pill;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 本地存储 */
public class Store {
    private static final String PREF = "pill_store";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static List<Med> load(Context c) {
        List<Med> list = new ArrayList<>();
        String raw = sp(c).getString("meds", "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) list.add(Med.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) { }
        return list;
    }

    public static void save(Context c, List<Med> meds) {
        JSONArray a = new JSONArray();
        for (Med m : meds) {
            try { a.put(m.toJson()); } catch (Exception ignored) { }
        }
        sp(c).edit().putString("meds", a.toString()).apply();
    }

    public static long nextId(Context c) {
        long v = sp(c).getLong("nextId", 1);
        sp(c).edit().putLong("nextId", v + 1).apply();
        return v;
    }

    /** 打卡一次，库存 -1；取消打卡 +1 */
    public static void adjustStock(Context c, long medId, int delta) {
        List<Med> all = load(c);
        boolean changed = false;
        for (Med m : all) {
            if (m.id == medId && m.trackStock()) {
                int base = m.left < 0 ? m.stock : m.left;
                m.left = Math.max(0, base + delta);
                changed = true;
            }
        }
        if (changed) save(c, all);
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
    }

    private static String key(long medId, String time, String date) {
        return "done|" + medId + "|" + time + "|" + date;
    }

    public static boolean isTaken(Context c, long medId, String time, String date) {
        return sp(c).getBoolean(key(medId, time, date), false);
    }

    public static void setTaken(Context c, long medId, String time, String date, boolean taken) {
        sp(c).edit().putBoolean(key(medId, time, date), taken).apply();
    }

    /** 最近用过的药（索引），最多 8 个 */
    public static List<Integer> recent(Context c) {
        List<Integer> out = new ArrayList<>();
        String raw = sp(c).getString("recent", "");
        if (raw == null || raw.isEmpty()) return out;
        for (String p : raw.split(",")) {
            try { out.add(Integer.parseInt(p.trim())); } catch (Exception ignored) { }
        }
        return out;
    }

    public static void pushRecent(Context c, int idx) {
        List<Integer> list = recent(c);
        list.remove(Integer.valueOf(idx));
        list.add(0, idx);
        while (list.size() > 8) list.remove(list.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        sp(c).edit().putString("recent", sb.toString()).apply();
    }
    public static void setLastFire(Context c, long ts) {
        sp(c).edit().putLong("lastFire", ts).apply();
    }

    public static long lastFire(Context c) {
        return sp(c).getLong("lastFire", 0L);
    }

    public static boolean onboarded(Context c) {
        return sp(c).getBoolean("onboarded", false);
    }

    public static void setOnboarded(Context c, boolean v) {
        sp(c).edit().putBoolean("onboarded", v).apply();
    }

    public static boolean autoDone(Context c) {
        return sp(c).getBoolean("autoDone", false);
    }

    public static void setAutoDone(Context c, boolean v) {
        sp(c).edit().putBoolean("autoDone", v).apply();
    }

    public static boolean guard(Context c) {
        return sp(c).getBoolean("guard", false);
    }

    public static void setGuard(Context c, boolean on) {
        sp(c).edit().putBoolean("guard", on).apply();
    }

    public static boolean dark(Context c) {
        return sp(c).getBoolean("dark", true);
    }

    public static void setDark(Context c, boolean dark) {
        sp(c).edit().putBoolean("dark", dark).apply();
    }
}
