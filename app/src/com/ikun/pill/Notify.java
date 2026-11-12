package com.ikun.pill;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** 提醒通知 */
public class Notify {
    public static final String CHANNEL = "med_reminder";

    public static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(
                CHANNEL, "吃药提醒", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("到点提醒你吃药");
        ch.setSound(null, null);
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 400, 200, 400});
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    private static PendingIntent action(Context c, Med m, String time, String what, int reqBase) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.putExtra("do", what);
        i.putExtra("medId", m.id);
        i.putExtra("name", m.name);
        i.putExtra("dose", m.dose);
        i.putExtra("time", time);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, reqBase, i, flags);
    }

    public static void show(Context c, Med m, String time) {
        ensureChannel(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent piOpen = PendingIntent.getActivity(c, 1, open, flags);

        int base = Math.abs((m.id + "|" + time).hashCode()) % 10000;

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(c, CHANNEL);
        } else {
            b = new Notification.Builder(c);
            b.setPriority(Notification.PRIORITY_HIGH);
        }
        b.setSmallIcon(R.drawable.ic_notify);
        b.setContentTitle("该吃药了 · " + (m.name.isEmpty() ? "药" : m.name));
        b.setContentText((m.dose.isEmpty() ? "" : m.dose + " · ") + (m.note.isEmpty() ? "" : m.note + " · ") + time);
        b.setStyle(new Notification.BigTextStyle().bigText(
                (m.name.isEmpty() ? "药" : m.name) + "\n剂量：" + (m.dose.isEmpty() ? "按医嘱" : m.dose)
                        + "\n时间：" + time + "\n\n吃完记得点下面的按钮。"));
        android.content.Intent ai = new android.content.Intent(c, AlarmActivity.class);
        ai.putExtra("medId", m.id);
        ai.putExtra("name", m.name);
        ai.putExtra("dose", m.dose);
        ai.putExtra("time", time);
        ai.putExtra("cat", m.cat);
        ai.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent piAlarm = PendingIntent.getActivity(c, 2, ai, flags);

        b.setContentIntent(piAlarm);
        b.setFullScreenIntent(piAlarm, true);
        b.setOngoing(true);
        b.setAutoCancel(false);
        b.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION));
        b.setVibrate(new long[]{0, 400, 200, 400});
        b.setCategory(Notification.CATEGORY_ALARM);
        b.addAction(new Notification.Action.Builder(
                android.R.drawable.checkbox_on_background, "已服用", action(c, m, time, "mark", base + 1)).build());
        b.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_menu_recent_history, "10 分钟后", action(c, m, time, "snooze", base + 2)).build());

        nm.notify(base, b.build());
    }

    public static void cancel(Context c, long medId, String time) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(Math.abs((medId + "|" + time).hashCode()) % 10000);
    }
}
