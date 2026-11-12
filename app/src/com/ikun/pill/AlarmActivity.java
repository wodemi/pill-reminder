package com.ikun.pill;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 全屏响铃提醒：到点直接弹出来，跟系统闹钟一个体验 */
public class AlarmActivity extends Activity {

    private MediaPlayer player;
    private Ringtone fallback;
    private Vibrator vib;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean stopped = false;

    private long medId;
    private String name = "", dose = "", time = "08:00", cat = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        Intent it = getIntent();
        medId = it.getLongExtra("medId", 0);
        name = safe(it.getStringExtra("name"), "药");
        dose = safe(it.getStringExtra("dose"), "");
        time = safe(it.getStringExtra("time"), "08:00");
        cat = safe(it.getStringExtra("cat"), "");

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        buildUi();
        startAlarm();

        handler.postDelayed(new Runnable() {
            public void run() {
                if (!stopped) {
                    stopAlarm();
                    Toast.makeText(AlarmActivity.this, "响铃 2 分钟自动停了，通知还在", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }, 120000L);
    }

    private static String safe(String s, String def) {
        return s == null || s.trim().isEmpty() ? def : s;
    }

    // ---------- 界面 ----------

    private void buildUi() {
        int tint = Drugs.colorOf(cat);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackground(gradient(tint));
        root.setPadding(dp(28), dp(56), dp(28), dp(40));

        TextView now = new TextView(this);
        now.setText(new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date()));
        now.setTextSize(74);
        now.setTextColor(0xFFFFFFFF);
        now.setTypeface(now.getTypeface(), android.graphics.Typeface.BOLD);
        now.setGravity(Gravity.CENTER);
        root.addView(now);

        TextView date = new TextView(this);
        date.setText(new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date()) + " · 该吃药了");
        date.setTextSize(15);
        date.setTextColor(0xCCFFFFFF);
        date.setGravity(Gravity.CENTER);
        date.setPadding(0, dp(6), 0, 0);
        root.addView(date);

        View sp1 = new View(this);
        root.addView(sp1, new LinearLayout.LayoutParams(1, 0, 1f));

        TextView icon = new TextView(this);
        icon.setText("💊");
        icon.setTextSize(46);
        icon.setGravity(Gravity.CENTER);
        root.addView(icon);

        TextView drug = new TextView(this);
        drug.setText(name);
        drug.setTextSize(29);
        drug.setTextColor(0xFFFFFFFF);
        drug.setTypeface(drug.getTypeface(), android.graphics.Typeface.BOLD);
        drug.setGravity(Gravity.CENTER);
        drug.setPadding(0, dp(14), 0, 0);
        root.addView(drug);

        StringBuilder sub = new StringBuilder();
        if (!dose.isEmpty()) sub.append(dose);
        if (!cat.isEmpty()) {
            if (sub.length() > 0) sub.append("  ·  ");
            sub.append(cat);
        }
        if (sub.length() > 0) {
            TextView d = new TextView(this);
            d.setText(sub.toString());
            d.setTextSize(15);
            d.setTextColor(0xCCFFFFFF);
            d.setGravity(Gravity.CENTER);
            d.setPadding(0, dp(8), 0, 0);
            root.addView(d);
        }

        TextView t = new TextView(this);
        t.setText("计划时间 " + time);
        t.setTextSize(14);
        t.setTextColor(0x99FFFFFF);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(14), 0, 0);
        root.addView(t);

        View sp2 = new View(this);
        root.addView(sp2, new LinearLayout.LayoutParams(1, 0, 1f));

        TextView take = new TextView(this);
        take.setText("✓   已服用");
        take.setTextSize(19);
        take.setTypeface(take.getTypeface(), android.graphics.Typeface.BOLD);
        take.setTextColor(0xFF06210F);
        take.setGravity(Gravity.CENTER);
        take.setBackground(round(0xFF4ADE80, 20));
        take.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doTake(); }
        });
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, dp(66));
        tlp.setMargins(0, dp(10), 0, 0);
        root.addView(take, tlp);

        TextView snooze = new TextView(this);
        snooze.setText("稍后 10 分钟");
        snooze.setTextSize(17);
        snooze.setTextColor(0xFFFFFFFF);
        snooze.setGravity(Gravity.CENTER);
        snooze.setBackground(round(0x33FFFFFF, 20));
        snooze.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doSnooze(); }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, dp(58));
        slp.setMargins(0, dp(12), 0, 0);
        root.addView(snooze, slp);

        TextView close = new TextView(this);
        close.setText("关闭提醒");
        close.setTextSize(14);
        close.setTextColor(0x99FFFFFF);
        close.setGravity(Gravity.CENTER);
        close.setPadding(0, dp(18), 0, 0);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopAlarm();
                finish();
            }
        });
        root.addView(close);

        setContentView(root);
    }

    private GradientDrawable gradient(int tint) {
        int top = blend(0xFF0B1017, tint, 0.22f);
        int bottom = 0xFF070A0F;
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
        return g;
    }

    private static int blend(int base, int over, float alpha) {
        int r = (int) (Color.red(over) * alpha + Color.red(base) * (1 - alpha));
        int g = (int) (Color.green(over) * alpha + Color.green(base) * (1 - alpha));
        int b = (int) (Color.blue(over) * alpha + Color.blue(base) * (1 - alpha));
        return Color.rgb(r, g, b);
    }

    // ---------- 动作 ----------

    private void doTake() {
        stopAlarm();
        String today = Store.today();
        Store.setTaken(this, medId, time, today, true);
        List<Med> all = Store.load(this);
        for (Med m : all) {
            if (m.id == medId && m.trackStock()) {
                int base = m.left < 0 ? m.stock : m.left;
                m.left = Math.max(0, base - 1);
                Store.save(this, all);
                break;
            }
        }
        Notify.cancel(this, medId, time);
        Med m = new Med(medId, name, dose, new java.util.ArrayList<String>());
        m.times.add(time);
        AlarmScheduler.scheduleOne(this, m, time, 0);
        Toast.makeText(this, "已记录：" + name, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void doSnooze() {
        stopAlarm();
        Notify.cancel(this, medId, time);
        Med m = new Med(medId, name, dose, new java.util.ArrayList<String>());
        m.times.add(time);
        AlarmScheduler.scheduleAt(this, m, time, System.currentTimeMillis() + 10 * 60000L);
        Toast.makeText(this, "10 分钟后再响", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ---------- 声音 / 震动 ----------

    private void startAlarm() {
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                if (am.getStreamVolume(AudioManager.STREAM_ALARM) < max * 0.3f) {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, (int) (max * 0.6f), 0);
                }
            }
            Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Throwable e) {
            try {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                fallback = RingtoneManager.getRingtone(this, uri);
                if (fallback != null) fallback.play();
            } catch (Throwable ignored) { }
        }

        try {
            vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null && vib.hasVibrator()) {
                vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 700, 500, 700}, 0));
            }
        } catch (Throwable ignored) { }
    }

    private void stopAlarm() {
        if (stopped) return;
        stopped = true;
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
            }
        } catch (Throwable ignored) { }
        try {
            if (fallback != null && fallback.isPlaying()) fallback.stop();
        } catch (Throwable ignored) { }
        try {
            if (vib != null) vib.cancel();
        } catch (Throwable ignored) { }
    }

    @Override
    protected void onDestroy() {
        stopAlarm();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }
}
