package com.ikun.pill;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private boolean dark;
    private Dialog healthDialog;
    private boolean guideMode;
    private int C_BG, C_CARD, C_CARD2, C_TEXT, C_SUB, C_LINE, C_ACCENT, C_WARN, C_DANGER;

    private List<Med> meds = new ArrayList<>();
    private LinearLayout listBox;
    private Ring ring;
    private TextView statBig, statLabel, nextTxt, dateTxt;

    // ---------- 生命周期 ----------

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dark = Store.dark(this);
        theme();
        buildUi();
        askPerms();
        if (Store.guard(this)) GuardService.start(this);
        if (!Store.onboarded(this)) {
            listBox.post(new Runnable() {
                public void run() { showHealth(true); }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        dark = Store.dark(this);
        theme();
        render();
        if (healthDialog != null && healthDialog.isShowing()) {
            boolean g = guideMode;
            healthDialog.dismiss();
            showHealth(g);
        }
    }

    private void theme() {
        if (dark) {
            C_BG = 0xFF0B1017; C_CARD = 0xFF151C26; C_CARD2 = 0xFF1C2431;
            C_TEXT = 0xFFE8EDF4; C_SUB = 0xFF8B98A8; C_LINE = 0xFF28313F;
            C_ACCENT = 0xFF4ADE80; C_WARN = 0xFFFBBF24; C_DANGER = 0xFFF87171;
        } else {
            C_BG = 0xFFF4F6F8; C_CARD = 0xFFFFFFFF; C_CARD2 = 0xFFEDF1F5;
            C_TEXT = 0xFF101828; C_SUB = 0xFF667085; C_LINE = 0xFFE4E7EC;
            C_ACCENT = 0xFF16A34A; C_WARN = 0xFFD97706; C_DANGER = 0xFFDC2626;
        }
    }

    // ---------- 界面骨架 ----------

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(C_BG);

        // 顶栏
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(16), dp(14), dp(6));

        TextView title = tv("按时吃药", 22, C_TEXT, true);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView health = tv("体检", 13, C_SUB, false);
        health.setPadding(dp(10), dp(6), dp(8), dp(6));
        health.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showHealth(false); }
        });
        bar.addView(health);

        TextView themeBtn = tv(dark ? "☀" : "☾", 18, C_SUB, false);
        themeBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        themeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Store.setDark(MainActivity.this, !dark);
                recreate();
            }
        });
        bar.addView(themeBtn);
        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        // 概览卡
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(round(C_CARD, 20, 0, 0));
        card.setPadding(dp(18), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(dp(16), dp(8), dp(16), dp(12));
        root.addView(card, clp);

        ring = new Ring(this, dark);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(dp(92), dp(92));
        rlp.setMargins(0, 0, dp(18), 0);
        card.addView(ring, rlp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        dateTxt = tv("", 12, C_SUB, false);
        info.addView(dateTxt);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);
        info.addView(row);

        statBig = tv("0 / 0", 26, C_TEXT, true);
        row.addView(statBig);
        statLabel = tv("  已完成", 12, C_SUB, false);
        statLabel.setPadding(0, 0, 0, dp(4));
        row.addView(statLabel);

        nextTxt = tv("", 12, C_ACCENT, false);
        nextTxt.setPadding(0, dp(6), 0, 0);
        info.addView(nextTxt);

        // 列表
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listBox.setPadding(dp(16), dp(2), dp(16), dp(20));
        sv.addView(listBox, new LinearLayout.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        // 底部按钮
        LinearLayout bottom = new LinearLayout(this);
        bottom.setPadding(dp(16), dp(8), dp(16), dp(16));
        TextView add = tv("＋  添加药品", 16, dark ? 0xFF06210F : 0xFFFFFFFF, true);
        add.setGravity(Gravity.CENTER);
        add.setBackground(round(C_ACCENT, 16, 0, 0));
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAddDialog(); }
        });
        bottom.addView(add, new LinearLayout.LayoutParams(-1, dp(52)));
        root.addView(bottom);

        setContentView(root);
        render();
    }

    // ---------- 渲染 ----------

    private void render() {
        meds = Store.load(this);
        String today = Store.today();

        listBox.removeAllViews();

        List<Row2> rows = new ArrayList<>();
        for (Med m : meds) {
            if (!m.enabled) continue;
            for (String t : m.times) rows.add(new Row2(m, t));
        }
        Collections.sort(rows, new java.util.Comparator<Row2>() {
            public int compare(Row2 a, Row2 b) { return a.time.compareTo(b.time); }
        });

        int taken = 0;
        for (Row2 r : rows) {
            if (Store.isTaken(this, r.med.id, r.time, today)) taken++;
        }

        int total = rows.size();
        ring.set(total == 0 ? 0f : (float) taken / total);
        statBig.setText(taken + " / " + total);
        dateTxt.setText(new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date()));

        String next = "";
        String nowHm = new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date());
        for (Row2 r : rows) {
            if (!Store.isTaken(this, r.med.id, r.time, today) && r.time.compareTo(nowHm) >= 0) {
                next = "下一颗 " + r.time + " · " + r.med.name;
                break;
            }
        }
        if (total == 0) {
            nextTxt.setText("今天还没有安排");
            nextTxt.setTextColor(C_SUB);
        } else if (taken == total) {
            nextTxt.setText("今天的药都吃完啦 ✓");
            nextTxt.setTextColor(C_ACCENT);
        } else if (next.isEmpty()) {
            nextTxt.setText("今天的剂量已过时间，记得补一下");
            nextTxt.setTextColor(C_WARN);
        } else {
            nextTxt.setText(next);
            nextTxt.setTextColor(C_ACCENT);
        }

        if (rows.isEmpty()) {
            listBox.addView(emptyView());
            return;
        }

        String lastSlot = "";
        for (Row2 r : rows) {
            String slot = slotOf(r.time);
            if (!slot.equals(lastSlot)) {
                lastSlot = slot;
                listBox.addView(slotHeader(slot, r.time));
            }
            listBox.addView(medCard(r, Store.isTaken(this, r.med.id, r.time, today)));
        }
    }

    private View emptyView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(60), dp(24), dp(40));

        TextView icon = tv("💊", 44, C_SUB, false);
        icon.setGravity(Gravity.CENTER);
        box.addView(icon);

        TextView t1 = tv("还没有添加药品", 16, C_TEXT, true);
        t1.setGravity(Gravity.CENTER);
        t1.setPadding(0, dp(14), 0, dp(6));
        box.addView(t1);

        TextView t2 = tv("点下面的按钮，从内置的 120 多种常见药里搜一个", 12, C_SUB, false);
        t2.setGravity(Gravity.CENTER);
        box.addView(t2);
        return box;
    }

    private View slotHeader(String slot, String firstTime) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(4), dp(16), dp(4), dp(8));
        h.setLayoutParams(lp);

        TextView t = tv(slot, 13, C_SUB, true);
        h.addView(t);
        return h;
    }

    private View medCard(final Row2 r, boolean taken) {
        final Med m = r.med;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(12));
        card.setBackground(round(taken ? C_CARD2 : C_CARD, 16, 1, C_LINE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(lp);

        // 左侧分类色条
        View bar = new View(this);
        bar.setBackground(round(Drugs.colorOf(m.cat), 2, 0, 0));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(4), dp(40));
        blp.setMargins(0, 0, dp(12), 0);
        card.addView(bar, blp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        card.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout line1 = new LinearLayout(this);
        line1.setOrientation(LinearLayout.HORIZONTAL);
        line1.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(line1);

        TextView time = tv(r.time, 17, taken ? C_SUB : C_TEXT, true);
        line1.addView(time);

        TextView name = tv("  " + m.name, 15, taken ? C_SUB : C_TEXT, false);
        if (taken) name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        line1.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));

        StringBuilder meta = new StringBuilder();
        if (!m.dose.isEmpty()) meta.append(m.dose);
        if (!m.note.isEmpty()) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(m.note);
        }
        boolean lowStock = false;
        if (m.trackStock()) {
            int left = m.left < 0 ? m.stock : m.left;
            if (meta.length() > 0) meta.append(" · ");
            meta.append("剩 ").append(left).append(" 份");
            lowStock = left <= 5;
        }
        if (lowStock) meta.append("  快没了");
        if (meta.length() > 0) {
            TextView mt = tv(meta.toString(), 12, lowStock ? C_WARN : C_SUB, false);
            mt.setPadding(0, dp(4), 0, 0);
            col.addView(mt);
        }

        TextView chip = tv(taken ? "✓ 已服" : "打卡", 13, taken ? C_ACCENT : C_SUB, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(12), dp(7), dp(12), dp(7));
        chip.setBackground(round(taken ? C_CARD : C_CARD2, 12, 1, taken ? C_ACCENT : C_LINE));
        card.addView(chip);

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String today = Store.today();
                boolean now = !Store.isTaken(MainActivity.this, m.id, r.time, today);
                Store.setTaken(MainActivity.this, m.id, r.time, today, now);
                if (m.trackStock()) Store.adjustStock(MainActivity.this, m.id, now ? -1 : 1);
                if (now) {
                    Notify.cancel(MainActivity.this, m.id, r.time);
                    Toast.makeText(MainActivity.this, "已记录：" + m.name, Toast.LENGTH_SHORT).show();
                }
                render();
            }
        });

        card.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                showMedMenu(m);
                return true;
            }
        });
        return card;
    }

    private void showMedMenu(final Med m) {
        String toggle = m.enabled ? "暂停这个药的提醒" : "恢复这个药的提醒";
        String[] items = {toggle, "删除这个药"};
        new AlertDialog.Builder(this)
                .setTitle(m.name)
                .setItems(items, new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int which) {
                        List<Med> all = Store.load(MainActivity.this);
                        if (which == 0) {
                            for (Med x : all) if (x.id == m.id) x.enabled = !x.enabled;
                        } else {
                            for (int i = 0; i < all.size(); i++) {
                                if (all.get(i).id == m.id) { all.remove(i); break; }
                            }
                        }
                        Store.save(MainActivity.this, all);
                        for (String tt : m.times) Notify.cancel(MainActivity.this, m.id, tt);
                        AlarmScheduler.scheduleAll(MainActivity.this, Store.load(MainActivity.this));
                        render();
                    }
                })
                .show();
    }

    // ---------- 提醒体检 ----------

    private boolean overlayOk() {
        return Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(this);
    }

    private boolean fullScreenOk() {
        if (Build.VERSION.SDK_INT < 34) return true;
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            return nm != null && nm.canUseFullScreenIntent();
        } catch (Throwable e) {
            return true;
        }
    }

    private boolean exactOk() {
        if (Build.VERSION.SDK_INT < 31) return true;
        try {
            android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            return am != null && am.canScheduleExactAlarms();
        } catch (Throwable e) {
            return true;
        }
    }

    private boolean batteryOk() {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        } catch (Throwable e) {
            return true;
        }
    }

    private boolean notifOk() {
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                return checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        == PackageManager.PERMISSION_GRANTED;
            }
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            return nm == null || nm.areNotificationsEnabled();
        } catch (Throwable e) {
            return true;
        }
    }

    private void showHealth(final boolean guide) {
        guideMode = guide;
        final Dialog[] holder = new Dialog[1];
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(C_BG);
        box.setPadding(dp(20), dp(18), dp(20), dp(14));

        TextView h = tv(guide ? "先花一分钟设置" : "提醒体检", 19, C_TEXT, true);
        box.addView(h);

        TextView tip = tv(guide
                ? "这些配置好，手机才会在到点时真的响。点一行去开启，回来会自动打勾。"
                : "这些全绿，退出应用、锁屏、重启手机之后到点都还会响。", 12, C_SUB, false);
        tip.setPadding(0, dp(6), 0, dp(4));
        box.addView(tip);

        int total = 7;
        int okCount = 0;
        if (exactOk()) okCount++;
        if (batteryOk()) okCount++;
        if (Store.autoDone(this)) okCount++;
        if (notifOk()) okCount++;
        if (overlayOk()) okCount++;
        if (fullScreenOk()) okCount++;
        if (Store.guard(this)) okCount++;

        TextView prog = tv("已配置 " + okCount + " / " + total + (okCount == total ? "  ✓ 可以用了" : ""),
                13, okCount == total ? 0xFF4ADE80 : C_WARN, true);
        prog.setPadding(0, dp(8), 0, dp(12));
        box.addView(prog);

        addRow(box, "通知权限", notifOk(), "没有它连通知都发不出来",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            Intent i = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                            i.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
                            startActivity(i);
                        } catch (Throwable e) {
                            Toast.makeText(MainActivity.this, "打不开通知设置", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        addRow(box, "精确闹钟", exactOk(), "系统允许精确到分钟地叫醒你",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            Intent i = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    android.net.Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        } catch (Throwable e) {
                            Toast.makeText(MainActivity.this, "打不开，去设置里搜「闹钟和提醒」", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        addRow(box, "电池优化白名单", batteryOk(), "不然系统会在后台把它掐掉",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            Intent i = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    android.net.Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        } catch (Throwable e) {
                            Toast.makeText(MainActivity.this, "打不开，去设置里搜「电池优化」", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        addRow(box, "自启动 / 后台运行", Store.autoDone(this), "国产手机这步最关键，要手动开",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        openAutostart();
                        Store.setAutoDone(MainActivity.this, true);
                        if (holder[0] != null) holder[0].dismiss();
                        showHealth(guide);
                    }
                });

        addRow(box, "显示在其他应用上层", overlayOk(), "不开这个，闹钟可能弹不出来",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        } catch (Throwable e) {
                            Toast.makeText(MainActivity.this, "打不开悬浮窗设置，去设置里搜「悬浮窗」", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        addRow(box, "全屏通知权限", fullScreenOk(), "Android 14 起要单独授权，否则只弹小横幅",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            if (Build.VERSION.SDK_INT >= 34) {
                                Intent i = new Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT",
                                        android.net.Uri.parse("package:" + getPackageName()));
                                startActivity(i);
                            } else {
                                Toast.makeText(MainActivity.this, "这个系统版本不用单独开", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Throwable e) {
                            Toast.makeText(MainActivity.this, "打不开，去设置里搜「全屏通知」", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        final boolean guardOn = Store.guard(this);
        addRow(box, "后台守护", guardOn, guardOn ? "常驻一条通知，系统不会随便杀它" : "建议打开，划掉应用后也能响",
                new View.OnClickListener() {
                    public void onClick(View v) {
                        boolean on = !Store.guard(MainActivity.this);
                        Store.setGuard(MainActivity.this, on);
                        if (on) GuardService.start(MainActivity.this);
                        else GuardService.stop(MainActivity.this);
                        Toast.makeText(MainActivity.this, on ? "已开启后台守护" : "已关闭后台守护", Toast.LENGTH_SHORT).show();
                        if (holder[0] != null) holder[0].dismiss();
                        showHealth(guide);
                    }
                });

        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (guide) {
            TextView go = tv("开始使用", 16, dark ? 0xFF06210F : 0xFFFFFFFF, true);
            go.setGravity(Gravity.CENTER);
            go.setBackground(round(C_ACCENT, 16, 0, 0));
            go.setPadding(0, dp(14), 0, dp(14));
            LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, -2);
            glp.setMargins(0, dp(16), 0, 0);
            go.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Store.setOnboarded(MainActivity.this, true);
                    if (!Store.guard(MainActivity.this)) {
                        Store.setGuard(MainActivity.this, true);
                        GuardService.start(MainActivity.this);
                    }
                    d.dismiss();
                }
            });
            box.addView(go, glp);

            TextView skip = tv("先跳过，我知道可能不响", 12, C_SUB, false);
            skip.setGravity(Gravity.CENTER);
            skip.setPadding(0, dp(12), 0, 0);
            skip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Store.setOnboarded(MainActivity.this, true);
                    d.dismiss();
                }
            });
            box.addView(skip);
        } else {
            TextView close = tv("关掉", 13, C_SUB, false);
            close.setGravity(Gravity.CENTER);
            close.setPadding(0, dp(14), 0, 0);
            close.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { d.dismiss(); }
            });
            box.addView(close);
        }

        ScrollView sc = new ScrollView(this);
        sc.addView(box);
        d.setContentView(sc);
        d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            public void onDismiss(android.content.DialogInterface di) { healthDialog = null; }
        });
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(round(C_BG, 20, 0, 0));
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.86f));
        }
        healthDialog = d;
        holder[0] = d;
        d.show();
    }
    private void addRow(LinearLayout box, String title, boolean ok, String desc, View.OnClickListener action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(round(C_CARD, 14, 1, C_LINE));
        row.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(8));
        box.addView(row, lp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        row.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout t1 = new LinearLayout(this);
        t1.setOrientation(LinearLayout.HORIZONTAL);
        t1.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(t1);

        TextView dot = tv(ok ? "●" : "●", 11, ok ? C_ACCENT : C_WARN, false);
        dot.setPadding(0, 0, dp(6), 0);
        t1.addView(dot);
        t1.addView(tv(title, 14, C_TEXT, true));

        TextView sub = tv(desc, 11, C_SUB, false);
        sub.setPadding(dp(17), dp(3), 0, 0);
        col.addView(sub);

        TextView go = tv(ok ? "已开" : "去开", 12, ok ? C_SUB : C_ACCENT, true);
        go.setPadding(dp(10), dp(6), dp(10), dp(6));
        row.addView(go);
        if (!ok) {
            row.setOnClickListener(action);
            go.setOnClickListener(action);
        }
    }

    private void testAlarm(int seconds) {
        try {
            Intent i = new Intent(this, ReminderReceiver.class);
            i.putExtra("medId", 999999L);
            i.putExtra("name", "测试提醒");
            i.putExtra("dose", "50mg · 1片");
            i.putExtra("time", new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date()));
            i.putExtra("cat", "精神情绪");
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(this, 999999, i, flags);
            android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (am == null) return;
            long at = System.currentTimeMillis() + seconds * 1000L;
            if (exactOk()) {
                am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, at, pi);
            }
            Toast.makeText(this, seconds + " 秒后响，现在可以把应用划掉试试", Toast.LENGTH_LONG).show();
        } catch (Throwable e) {
            Toast.makeText(this, "排闹钟失败：" + e, Toast.LENGTH_LONG).show();
        }
    }

    private void openAutostart() {
        String[][] known = {
                {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
                {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
                {"com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"},
                {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"},
                {"com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"},
                {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"},
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
                {"com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"},
                {"com.bbk.launcher2", "com.bbk.launcher2.whitelist.WhiteListActivity"},
                {"com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"},
                {"com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"}
        };
        for (String[] k : known) {
            try {
                Intent i = new Intent();
                i.setComponent(new android.content.ComponentName(k[0], k[1]));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return;
            } catch (Throwable ignored) { }
        }
        try {
            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + getPackageName()));
            startActivity(i);
            Toast.makeText(this, "没找到自启动页，请在应用详情里找「自启动 / 后台运行」", Toast.LENGTH_LONG).show();
        } catch (Throwable e) {
            Toast.makeText(this, "打不开设置", Toast.LENGTH_SHORT).show();
        }
    }
    // ---------- 添加对话框 ----------

    private void showAddDialog() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(C_BG);
        col.setPadding(dp(20), dp(18), dp(20), dp(16));

        col.addView(tv("添加药品", 18, C_TEXT, true));

        // 搜索
        final EditText search = input("搜药名或分类，如 氟伏沙明 / 布洛芬 / 感冒");
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.setMargins(0, dp(12), 0, dp(8));
        col.addView(search, slp);

        // 分类 chips
        final String[] cat = {""};
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        final LinearLayout cats = new LinearLayout(this);
        cats.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(cats, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
        hlp.setMargins(0, 0, 0, dp(8));
        col.addView(hsv, hlp);

        // 结果列表
        final ListView list = new ListView(this);
        list.setDividerHeight(0);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                TextView t = (TextView) super.getView(pos, convertView, parent);
                t.setTextColor(C_TEXT);
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                t.setPadding(dp(6), dp(11), dp(6), dp(11));
                t.setBackgroundColor(Color.TRANSPARENT);
                return t;
            }
        };
        list.setAdapter(adapter);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-1, dp(200));
        col.addView(list, llp);

        final EditText name = input("药名");
        final EditText dose = input("剂量，如 50mg · 1片");
        LinearLayout rowA = new LinearLayout(this);
        rowA.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2);
        alp.setMargins(0, dp(10), 0, dp(2));
        col.addView(rowA, alp);
        LinearLayout.LayoutParams w1 = new LinearLayout.LayoutParams(0, -2, 1f);
        w1.setMargins(0, 0, dp(8), 0);
        rowA.addView(name, w1);
        rowA.addView(dose, new LinearLayout.LayoutParams(0, -2, 1f));

        // 时段
        col.addView(label("什么时候吃"));
        final String[] QUICK = {"08:00", "12:30", "19:00", "22:00"};
        final String[] QUICK_TXT = {"早", "中", "晚", "睡前"};
        final boolean[] picked = new boolean[4];
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        col.addView(timeRow);
        final TextView[] timeChips = new TextView[4];
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final TextView chip = tv(QUICK_TXT[i] + "\n" + QUICK[i], 12, C_TEXT, false);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(10), dp(8), dp(10), dp(8));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
            cp.setMargins(i == 0 ? 0 : dp(4), 0, i == 3 ? 0 : dp(4), 0);
            timeRow.addView(chip, cp);
            timeChips[i] = chip;
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    picked[idx] = !picked[idx];
                    paintChip(chip, picked[idx]);
                }
            });
        }

        final EditText custom = input("其他时间，如 09:30,21:00（可不填）");
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-1, -2);
        elp.setMargins(0, dp(8), 0, 0);
        col.addView(custom, elp);

        // 服用方式
        col.addView(label("怎么吃"));
        final String[] WAYS = {"饭前", "饭后", "随餐", "睡前", "空腹"};
        final String[] way = {""};
        HorizontalScrollView hw = new HorizontalScrollView(this);
        hw.setHorizontalScrollBarEnabled(false);
        final LinearLayout wayRow = new LinearLayout(this);
        wayRow.setOrientation(LinearLayout.HORIZONTAL);
        hw.addView(wayRow);
        col.addView(hw);
        final TextView[] wayChips = new TextView[WAYS.length];
        for (int i = 0; i < WAYS.length; i++) {
            final int idx = i;
            final TextView chip = tv(WAYS[i], 13, C_SUB, false);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(7), dp(14), dp(7));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
            cp.setMargins(0, dp(6), dp(8), 0);
            wayRow.addView(chip, cp);
            wayChips[i] = chip;
            paintChip(chip, false);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    way[0] = WAYS[idx].equals(way[0]) ? "" : WAYS[idx];
                    for (int k = 0; k < WAYS.length; k++) {
                        paintChip(wayChips[k], WAYS[k].equals(way[0]));
                    }
                }
            });
        }

        // 库存
        col.addView(label("一共有多少份（选填，用来提醒补药）"));
        final EditText stock = input("例如 30");
        stock.setInputType(InputType.TYPE_CLASS_NUMBER);
        col.addView(stock);

        // 按钮
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.setMargins(0, dp(16), 0, 0);
        col.addView(btns, blp);

        TextView cancel = tv("取消", 15, C_SUB, false);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(13), 0, dp(13));
        cancel.setBackground(round(C_CARD2, 14, 0, 0));
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(0, -2, 1f);
        olp.setMargins(0, 0, dp(8), 0);
        btns.addView(cancel, olp);

        TextView ok = tv("保存", 15, dark ? 0xFF06210F : 0xFFFFFFFF, true);
        ok.setGravity(Gravity.CENTER);
        ok.setPadding(0, dp(13), 0, dp(13));
        ok.setBackground(round(C_ACCENT, 14, 0, 0));
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String nm = name.getText().toString().trim();
                if (nm.isEmpty()) {
                    Toast.makeText(MainActivity.this, "先写个药名", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<String> times = new ArrayList<>();
                for (int i = 0; i < 4; i++) if (picked[i]) times.add(QUICK[i]);
                String extra = custom.getText().toString().trim();
                if (!extra.isEmpty()) {
                    for (String p : extra.split("[,，\\s]+")) {
                        if (p.matches("\\d{1,2}:\\d{2}")) {
                            String[] hm = p.split(":");
                            times.add(String.format(Locale.CHINA, "%02d:%s", Integer.parseInt(hm[0]), hm[1]));
                        }
                    }
                }
                if (times.isEmpty()) times.add("08:00");
                Collections.sort(times);

                Med m = new Med(Store.nextId(MainActivity.this), nm, dose.getText().toString().trim(), times);
                m.note = way[0];
                m.cat = cat[0];
                String st = stock.getText().toString().trim();
                if (!st.isEmpty()) {
                    try {
                        int n = Integer.parseInt(st);
                        if (n > 0) { m.stock = n; m.left = n; }
                    } catch (Exception ignored) { }
                }
                List<Med> all = Store.load(MainActivity.this);
                all.add(m);
                Store.save(MainActivity.this, all);
                AlarmScheduler.scheduleAll(MainActivity.this, Store.load(MainActivity.this));
                d.dismiss();
                render();
                Toast.makeText(MainActivity.this, "已添加：" + nm, Toast.LENGTH_SHORT).show();
            }
        });
        btns.addView(ok, new LinearLayout.LayoutParams(0, -2, 1f));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        d.setContentView(scroll);

        // ---- 过滤逻辑 ----
        final Runnable refilter = new Runnable() {
            public void run() {
                String q = search.getText().toString();
                List<Integer> idx = Drugs.search(q, cat[0]);
                adapter.clear();
                boolean plain = q.trim().isEmpty() && cat[0].isEmpty();
                if (plain) {
                    List<Integer> rec = Store.recent(MainActivity.this);
                    if (!rec.isEmpty()) {
                        for (int i : rec) if (i >= 0 && i < Drugs.DATA.length) adapter.add("★  " + Drugs.label(i));
                    }
                }
                for (int i : idx) adapter.add(Drugs.label(i));
                adapter.notifyDataSetChanged();
            }
        };

        TextWatcher tw = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable s) { refilter.run(); }
        };
        search.addTextChangedListener(tw);

        // 分类 chips 构建
        String[] all = new String[Drugs.CATS.length + 1];
        all[0] = "全部";
        System.arraycopy(Drugs.CATS, 0, all, 1, Drugs.CATS.length);
        final TextView[] catChips = new TextView[all.length];
        for (int i = 0; i < all.length; i++) {
            final String cname = i == 0 ? "" : all[i];
            final int idx = i;
            TextView chip = tv(all[i], 12, C_SUB, false);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
            cp.setMargins(0, 0, dp(8), 0);
            cats.addView(chip, cp);
            catChips[i] = chip;
            paintChip(chip, i == 0);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    cat[0] = cname;
                    for (int k = 0; k < catChips.length; k++) paintChip(catChips[k], k == idx);
                    refilter.run();
                }
            });
        }

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                String txt = (String) p.getItemAtPosition(pos);
                String nm = txt.replace("★  ", "");
                String[] parts = nm.split("\u3000·\u3000");
                String drugName = parts[0];
                String drugDose = parts.length > 1 ? parts[1] : "";
                name.setText(drugName);
                dose.setText(drugDose);
                int di = Drugs.indexOfName(drugName);
                if (di >= 0) {
                    cat[0] = Drugs.DATA[di][2];
                    Store.pushRecent(MainActivity.this, di);
                    for (int k = 0; k < catChips.length; k++) {
                        boolean on = (k == 0 && cat[0].isEmpty()) || (k > 0 && Drugs.CATS[k - 1].equals(cat[0]));
                        paintChip(catChips[k], on);
                    }
                }
            }
        });

        refilter.run();

        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(round(C_BG, 20, 0, 0));
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.94f),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.90f));
        }
        d.show();
    }

    private void paintChip(TextView chip, boolean on) {
        chip.setTextColor(on ? (dark ? 0xFF06210F : 0xFFFFFFFF) : C_SUB);
        chip.setBackground(round(on ? C_ACCENT : C_CARD2, 12, on ? 0 : 1, on ? C_ACCENT : C_LINE));
    }

    private TextView label(String s) {
        TextView t = tv(s, 12, C_SUB, false);
        t.setPadding(0, dp(12), 0, dp(4));
        return t;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(C_SUB);
        e.setTextColor(C_TEXT);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        e.setSingleLine(true);
        e.setBackground(round(C_CARD, 12, 1, C_LINE));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        return e;
    }

    // ---------- 小工具 ----------

    private TextView tv(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int color, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), strokeColor);
        return g;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String slotOf(String time) {
        int h = 8;
        try { h = Integer.parseInt(time.split(":")[0]); } catch (Exception ignored) { }
        if (h < 5) return "深夜";
        if (h < 11) return "早上";
        if (h < 14) return "中午";
        if (h < 18) return "下午";
        if (h < 22) return "晚上";
        return "睡前";
    }

    private void askPerms() {
        try {
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 7);
            }
        } catch (Exception ignored) { }
    }

    private static class Row2 {
        final Med med;
        final String time;
        Row2(Med m, String t) { med = m; time = t; }
    }

    // ---------- 进度环 ----------

    static class Ring extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float pct = 0f;
        private final boolean dark;
        private final int accent;
        private final int sub;

        Ring(Context c, boolean dark) {
            super(c);
            this.dark = dark;
            this.accent = dark ? 0xFF4ADE80 : 0xFF16A34A;
            this.sub = dark ? 0xFF28313F : 0xFFE4E7EC;
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeCap(Paint.Cap.ROUND);
            arc.setStyle(Paint.Style.STROKE);
            arc.setStrokeCap(Paint.Cap.ROUND);
            text.setTextAlign(Paint.Align.CENTER);
            text.setFakeBoldText(true);
        }

        void set(float p) { pct = Math.max(0f, Math.min(1f, p)); invalidate(); }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            float sw = Math.max(6f, w * 0.09f);
            track.setStrokeWidth(sw);
            arc.setStrokeWidth(sw);
            float r = Math.min(w, h) / 2f - sw / 2f - 1f;
            track.setColor(sub);
            cv.drawCircle(w / 2f, h / 2f, r, track);
            arc.setColor(accent);
            if (pct > 0.001f) {
                RectF rf = new RectF(w / 2f - r, h / 2f - r, w / 2f + r, h / 2f + r);
                cv.drawArc(rf, -90f, 360f * pct, false, arc);
            }
            text.setColor(dark ? 0xFFE8EDF4 : 0xFF101828);
            text.setTextSize(w * 0.26f);
            String s = Math.round(pct * 100) + "%";
            cv.drawText(s, w / 2f, h / 2f + text.getTextSize() * 0.36f, text);
        }
    }
}
