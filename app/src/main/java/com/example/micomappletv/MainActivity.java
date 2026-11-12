
package com.example.micomappletv;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(new HomeView());
    }

    class AppItem {
        String label, pkg;
        Drawable icon;

        AppItem(String l, String p, Drawable i) {
            label = l;
            pkg = p;
            icon = i;
        }
    }

    class HomeView extends View {

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        ArrayList<AppItem> apps = new ArrayList<>();
        HashSet<String> hidden = new HashSet<>();

        int selected = 0;
        int cols = 4;

        float density;
        float scrollY = 0;
        float gridTop;
        float cardW, cardH, gapX, gapY;
        float contentHeight;
        float maxScroll = 0;

        boolean editMode = false;

        final int BG = 0xff080d1b;
        final int WHITE = 0xfff4f7ff;
        final int MUTED = 0xffaab8d2;
        final int BLUE = 0xff80b8ff;

        HomeView() {
            super(MainActivity.this);

            density = getResources().getDisplayMetrics().density;

            setFocusable(true);
            setFocusableInTouchMode(true);

            hidden.addAll(
                getSharedPreferences("launcher", MODE_PRIVATE)
                    .getStringSet("hidden", new HashSet<String>())
            );

            loadApps();

            post(() -> requestFocus());
        }

        float dp(float v) {
            return v * density;
        }

        void loadApps() {
            PackageManager pm = getPackageManager();

            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);

            List<ResolveInfo> list = pm.queryIntentActivities(q, 0);

            if (list == null || list.isEmpty()) {
                q.removeCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                q.addCategory(Intent.CATEGORY_LAUNCHER);
                list = pm.queryIntentActivities(q, 0);
            }

            HashSet<String> seen = new HashSet<>();

            for (ResolveInfo r : list) {
                String pkg = r.activityInfo.packageName;

                if (pkg.equals(getPackageName()) || seen.contains(pkg))
                    continue;

                seen.add(pkg);

                apps.add(new AppItem(
                    r.loadLabel(pm).toString(),
                    pkg,
                    r.loadIcon(pm)
                ));
            }

            Collections.sort(apps,
                (a, b) -> a.label.compareToIgnoreCase(b.label));
        }

        ArrayList<AppItem> getDisplayApps() {
            ArrayList<AppItem> result = new ArrayList<>();

            for (AppItem a : apps) {
                if (editMode || !hidden.contains(a.pkg))
                    result.add(a);
            }

            return result;
        }

        void saveHidden() {
            getSharedPreferences("launcher", MODE_PRIVATE)
                .edit()
                .putStringSet("hidden", new HashSet<>(hidden))
                .apply();
        }

        void text(Canvas c, String s, float x, float y,
                  float size, int color, boolean bold) {

            p.setShader(null);
            p.clearShadowLayer();
            p.setColor(color);
            p.setTextSize(dp(size));
            p.setTypeface(Typeface.create(
                "sans-serif",
                bold ? Typeface.BOLD : Typeface.NORMAL
            ));
            p.setTextAlign(Paint.Align.LEFT);

            c.drawText(s, x, y, p);
        }

        void rounded(Canvas c, float l, float t, float r, float b,
                     float radius, int color) {

            p.setShader(null);
            p.setColor(color);
            p.setStyle(Paint.Style.FILL);

            c.drawRoundRect(l, t, r, b,
                dp(radius), dp(radius), p);
        }

        void drawGlassCard(Canvas c, float x, float y,
                           float w, float h, boolean selectedCard) {

            p.setStyle(Paint.Style.FILL);

            int top = selectedCard ? 0xff293f62 : 0xdd1a2941;
            int bottom = selectedCard ? 0xff192d4d : 0xbb101b2e;

            p.setShader(new LinearGradient(
                x, y, x, y + h,
                top, bottom,
                Shader.TileMode.CLAMP
            ));

            if (selectedCard) {
                p.setShadowLayer(
                    dp(18), 0, dp(5), 0x995c9fff
                );
            }

            c.drawRoundRect(
                x, y, x + w, y + h,
                dp(19), dp(19), p
            );

            p.clearShadowLayer();
            p.setShader(null);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(selectedCard ? 2 : 1));

            p.setColor(selectedCard ? 0xff9bc8ff : 0x557e9bc2);

            c.drawRoundRect(
                x, y, x + w, y + h,
                dp(19), dp(19), p
            );

            p.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);

            int w = getWidth();
            int h = getHeight();

            if (w == 0 || h == 0) return;

            p.setShader(new LinearGradient(
                0, 0, w, h,
                new int[]{
                    0xff080d1b,
                    0xff14243f,
                    0xff070b13
                },
                null,
                Shader.TileMode.CLAMP
            ));

            c.drawRect(0, 0, w, h, p);
            p.setShader(null);

            p.setShader(new RadialGradient(
                w * 0.78f,
                h * 0.18f,
                dp(360),
                new int[]{
                    0x554b82d0,
                    0x003d72bd
                },
                null,
                Shader.TileMode.CLAMP
            ));

            c.drawRect(0, 0, w, h, p);
            p.setShader(null);

            // Header
            text(c, "MICOM", dp(42), dp(48),
                15, MUTED, true);

            String date = new SimpleDateFormat(
                "EEE, d MMM", Locale.getDefault()
            ).format(new Date());

            String time = new SimpleDateFormat(
                "h:mm a", Locale.getDefault()
            ).format(new Date());

            p.setTextAlign(Paint.Align.RIGHT);
            text(c, time + "   " + date,
                w - dp(42), dp(48), 15, WHITE, true);
            p.setTextAlign(Paint.Align.LEFT);

            text(c, "Your screen. Your apps.",
                dp(42), dp(105), 27, WHITE, true);

            text(c,
                editMode ? "Select an app to hide or show it"
                         : "Choose something to watch",
                dp(44), dp(132), 14, MUTED, false
            );

            // Grid layout
            cols = Math.max(3, Math.min(6,
                (int)(w / dp(190))));

            cardW = Math.min(
                dp(164),
                (w - dp(100)) / cols - dp(14)
            );

            cardH = dp(120);
            gapX = dp(18);
            gapY = dp(22);
            gridTop = dp(165);

            float totalW = cols * cardW + (cols - 1) * gapX;
            float startX = (w - totalW) / 2f;

            ArrayList<AppItem> display = getDisplayApps();

            int totalItems = display.size() + 1;

            int rows = (int)Math.ceil(
                totalItems / (float)cols
            );

            contentHeight = rows * (cardH + gapY);

            float viewportHeight = h - gridTop - dp(62);

            maxScroll = Math.max(
                0,
                contentHeight - viewportHeight
            );

            scrollY = Math.max(0, Math.min(scrollY, maxScroll));

            c.save();

            c.clipRect(
                0, gridTop - dp(5),
                w, h - dp(58)
            );

            for (int i = 0; i < totalItems; i++) {

                int row = i / cols;
                int col = i % cols;

                float x = startX + col * (cardW + gapX);
                float y = gridTop + row * (cardH + gapY) - scrollY;

                boolean selectedCard = i == selected;

                if (y + cardH < gridTop - dp(10) ||
                    y > h - dp(50)) {
                    continue;
                }

                drawGlassCard(
                    c, x, y, cardW, cardH, selectedCard
                );

                if (i == 0) {
                    // Edit Apps tile
                    text(c, "✦", x + cardW / 2 - dp(12),
                        y + dp(52), 28, BLUE, true);

                    p.setTextAlign(Paint.Align.CENTER);
                    text(c, "Edit Apps",
                        x + cardW / 2, y + dp(91),
                        14, WHITE, true);
                    p.setTextAlign(Paint.Align.LEFT);

                    if (editMode) {
                        text(c, "DONE",
                            x + cardW / 2 - dp(15),
                            y + dp(111), 9, BLUE, true);
                    }

                } else {

                    AppItem a = display.get(i - 1);

                    boolean isHidden = hidden.contains(a.pkg);

                    if (a.icon != null) {
                        int iconSize = (int)dp(48);

                        a.icon.setBounds(
                            (int)(x + (cardW - iconSize) / 2),
                            (int)(y + dp(13)),
                            (int)(x + (cardW + iconSize) / 2),
                            (int)(y + dp(13) + iconSize)
                        );

                        a.icon.setAlpha(isHidden ? 90 : 255);
                        a.icon.draw(c);
                        a.icon.setAlpha(255);
                    }

                    String label = a.label;

                    p.setTextSize(dp(12));

                    if (p.measureText(label) > cardW - dp(12)) {
                        while (label.length() > 3 &&
                               p.measureText(label + "…") > cardW - dp(12)) {
                            label = label.substring(0, label.length() - 1);
                        }
                        label += "…";
                    }

                    p.setTextAlign(Paint.Align.CENTER);

                    text(c, label,
                        x + cardW / 2,
                        y + dp(86),
                        12,
                        isHidden ? 0xff8995a9 : WHITE,
                        true
                    );

                    p.setTextAlign(Paint.Align.LEFT);

                    if (editMode) {
                        p.setTextAlign(Paint.Align.CENTER);

                        text(c,
                            isHidden ? "HIDDEN" : "VISIBLE",
                            x + cardW / 2,
                            y + dp(108),
                            9,
                            isHidden ? 0xfff0a7a7 : BLUE,
                            true
                        );

                        p.setTextAlign(Paint.Align.LEFT);
                    }
                }
            }

            c.restore();

            // Scroll indicator
            if (maxScroll > 0) {
                float trackX = w - dp(10);
                float trackTop = gridTop;
                float trackBottom = h - dp(65);
                float trackHeight = trackBottom - trackTop;

                rounded(c, trackX, trackTop,
                    trackX + dp(3), trackBottom,
                    3, 0x335b7190);

                float thumbH = Math.max(
                    dp(25),
                    trackHeight * trackHeight /
                    (contentHeight + trackHeight)
                );

                float thumbY = trackTop +
                    (trackHeight - thumbH) *
                    (scrollY / maxScroll);

                rounded(c, trackX, thumbY,
                    trackX + dp(3), thumbY + thumbH,
                    3, 0xff8dbaff);
            }

            // Footer
            text(c,
                editMode
                    ? "OK: Toggle visibility   •   BACK: Finish editing"
                    : "Arrows: Navigate   •   OK: Open   •   Edit Apps: Customize",
                dp(42), h - dp(24), 11, MUTED, false
            );
        }

        void updateScroll() {
            int row = selected / cols;

            float itemY = gridTop +
                row * (cardH + gapY);

            float viewportBottom = getHeight() - dp(58);

            if (itemY - scrollY + cardH > viewportBottom) {
                scrollY = itemY + cardH - viewportBottom;
            }

            if (itemY - scrollY < gridTop) {
                scrollY = itemY - gridTop;
            }

            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
        }

        @Override
        public boolean onKeyDown(int key, KeyEvent e) {

            ArrayList<AppItem> display = getDisplayApps();

            int total = display.size() + 1;

            if (key == KeyEvent.KEYCODE_BACK && editMode) {
                editMode = false;
                selected = 0;
                scrollY = 0;
                invalidate();
                return true;
            }

            if (key == KeyEvent.KEYCODE_MENU) {
                editMode = !editMode;
                selected = 0;
                scrollY = 0;
                invalidate();
                return true;
            }

            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                selected = Math.min(total - 1, selected + 1);
            }

            else if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                selected = Math.max(0, selected - 1);
            }

            else if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                selected = Math.min(total - 1, selected + cols);
            }

            else if (key == KeyEvent.KEYCODE_DPAD_UP) {
                selected = Math.max(0, selected - cols);
            }

            else if (key == KeyEvent.KEYCODE_DPAD_CENTER ||
                     key == KeyEvent.KEYCODE_ENTER) {

                if (selected == 0) {
                    editMode = !editMode;
                    selected = 0;
                    scrollY = 0;
                }

                else if (selected <= display.size()) {

                    AppItem a = display.get(selected - 1);

                    if (editMode) {

                        if (hidden.contains(a.pkg)) {
                            hidden.remove(a.pkg);
                        } else {
                            hidden.add(a.pkg);
                        }

                        saveHidden();

                    } else {
                        try {
                            Intent launch =
                                getPackageManager()
                                    .getLaunchIntentForPackage(a.pkg);

                            if (launch != null) {
                                startActivity(launch);
                            }

                        } catch (Exception ignored) {}
                    }
                }

                invalidate();
                return true;
            }

            else {
                return super.onKeyDown(key, e);
            }

            updateScroll();
            invalidate();

            return true;
        }
    }
}
