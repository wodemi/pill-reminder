package com.ikun.pill;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/**
 * 后台守护：一条常驻通知让系统知道「这个应用还要干活」，
 * 避免被国产 ROM 的后台清理掐掉，闹钟到点才拉得起全屏页面。
 */
public class GuardService extends Service {

    public static final String CH = "guard";
    public static final int ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            startForeground(ID, build());
        } catch (Throwable ignored) { }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 每次启动顺手把闹钟重排一遍，防止被杀后调度丢失
        try {
            Notify.ensureChannel(this);
            AlarmScheduler.scheduleAll(this, Store.load(this));
        } catch (Throwable ignored) { }
        return START_STICKY;
    }

    private Notification build() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CH, "后台守护", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("保持提醒可靠，可随时在应用里关掉");
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 3, open, flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, CH);
        } else {
            b = new Notification.Builder(this);
            b.setPriority(Notification.PRIORITY_LOW);
        }
        b.setSmallIcon(R.drawable.ic_notify);
        b.setContentTitle("按时吃药 · 守护中");
        b.setContentText("到点会全屏响铃提醒你");
        b.setContentIntent(pi);
        b.setOngoing(true);
        b.setShowWhen(false);
        return b.build();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    public static void start(Context c) {
        try {
            Intent i = new Intent(c, GuardService.class);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
            else c.startService(i);
        } catch (Throwable ignored) { }
    }

    public static void stop(Context c) {
        try {
            c.stopService(new Intent(c, GuardService.class));
        } catch (Throwable ignored) { }
    }
}
