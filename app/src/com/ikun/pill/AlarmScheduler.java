package com.ikun.pill;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

/** 闹钟调度 */
public class AlarmScheduler {

    public static PendingIntent pending(Context c, Med m, String time) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.putExtra("medId", m.id);
        i.putExtra("name", m.name);
        i.putExtra("dose", m.dose);
        i.putExtra("time", time);
        i.putExtra("cat", m.cat);
        int code = Math.abs((m.id + "|" + time).hashCode()) % 100000;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, code, i, flags);
    }

    /** 点状态栏上的闹钟图标时打开应用 */
    private static PendingIntent showIntent(Context c, Med m, String time) {
        Intent i = new Intent(c, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(c, Math.abs((m.id + "|show|" + time).hashCode()) % 100000, i, flags);
    }

    /** 计算下一个该响的时间点 */
    public static long nextAt(String time) {
        int h = 8, mi = 0;
        String[] p = time.split(":");
        try {
            h = Integer.parseInt(p[0].trim());
            mi = Integer.parseInt(p[1].trim());
        } catch (Exception ignored) { }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, mi);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 2000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal.getTimeInMillis();
    }

    public static void cancelAll(Context c, List<Med> meds) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (Med m : meds) {
            for (String t : m.times) {
                am.cancel(pending(c, m, t));
            }
        }
    }

    public static void scheduleAll(Context c, List<Med> meds) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        cancelAll(c, meds);
        for (Med m : meds) {
            if (!m.enabled) continue;
            for (String t : m.times) scheduleOne(c, m, t, 0);
        }
    }

    public static void scheduleOne(Context c, Med m, String time, int extraMinutes) {
        scheduleAt(c, m, time, nextAt(time) + extraMinutes * 60000L);
    }

    /** 在指定时刻响（稍后 10 分钟用这个，不能用 nextAt + 10） */
    public static void scheduleAt(Context c, Med m, String time, long at) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pending(c, m, time);
        try {
            boolean exact = true;
            if (Build.VERSION.SDK_INT >= 31) exact = am.canScheduleExactAlarms();
            if (exact) {
                // setAlarmClock 是系统闹钟级别：状态栏出现闹钟图标，Doze / 国产 ROM 的省电策略都不能推迟它
                PendingIntent show = showIntent(c, m, time);
                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(at, show);
                am.setAlarmClock(info, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (Throwable e) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (Throwable e2) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        }
    }
}
