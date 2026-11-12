package com.ikun.pill;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 开机 / 更新后重新排闹钟（否则重启手机提醒就失效了） */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Notify.ensureChannel(c);
        AlarmScheduler.scheduleAll(c, Store.load(c));
        if (Store.guard(c)) GuardService.start(c);
    }
}
