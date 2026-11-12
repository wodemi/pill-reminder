package com.ikun.pill;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 闹钟到点 / 通知按钮回调 */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        if (intent == null) return;
        Store.setLastFire(c, System.currentTimeMillis());
        String what = intent.getStringExtra("do");
        long medId = intent.getLongExtra("medId", 0);
        String name = intent.getStringExtra("name");
        String dose = intent.getStringExtra("dose");
        String cat = intent.getStringExtra("cat");
        String time = intent.getStringExtra("time");
        if (time == null) time = "08:00";
        if (name == null) name = "";
        if (dose == null) dose = "";

        Med m = new Med(medId, name, dose, new java.util.ArrayList<String>());
        m.times.add(time);
        if (cat != null) m.cat = cat;

        if ("mark".equals(what)) {
            String today = Store.today();
            if (!Store.isTaken(c, medId, time, today)) {
                Store.setTaken(c, medId, time, today, true);
                Store.adjustStock(c, medId, -1);
            }
            Notify.cancel(c, medId, time);
            return;
        }
        if ("snooze".equals(what)) {
            Notify.cancel(c, medId, time);
            AlarmScheduler.scheduleAt(c, m, time, System.currentTimeMillis() + 10 * 60000L);
            return;
        }

        Notify.show(c, m, time);

        // 全屏响铃：闹钟触发的广播允许拉起 Activity
        try {
            android.content.Intent ai = new android.content.Intent(c, AlarmActivity.class);
            ai.putExtra("medId", medId);
            ai.putExtra("name", name);
            ai.putExtra("dose", dose);
            ai.putExtra("time", time);
            ai.putExtra("cat", m.cat);
            ai.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
            c.startActivity(ai);
        } catch (Throwable ignored) { }

        // 排明天同一时间
        AlarmScheduler.scheduleOne(c, m, time, 0);
    }
}
