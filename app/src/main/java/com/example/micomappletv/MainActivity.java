package com.example.micomappletv;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(new HomeView());
    }

    class AppItem {
        String label, pkg;
        Drawable icon;
        AppItem(String l, String p, Drawable i) { label=l; pkg=p; icon=i; }
    }

    class HomeView extends View {
        Paint p = new Paint(3);
        ArrayList<AppItem> apps = new ArrayList<>();
        int selected = 0;
        float density;
        HomeView() {
            super(MainActivity.this);
            density = getResources().getDisplayMetrics().density;
            setFocusable(true); setFocusableInTouchMode(true);
            loadApps();
            post(() -> requestFocus());
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
                if (pkg.equals(getPackageName()) || seen.contains(pkg)) continue;
                seen.add(pkg);
                apps.add(new AppItem(r.loadLabel(pm).toString(), pkg, r.loadIcon(pm)));
            }
            Collections.sort(apps, (a,b) -> a.label.compareToIgnoreCase(b.label));
        }
        float dp(float x) { return x*density; }
        void text(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
            p.setColor(color); p.setTextSize(dp(size)); p.setTypeface(bold ? Typeface.create("sans-serif",Typeface.BOLD) : Typeface.create("sans-serif",Typeface.NORMAL));
            c.drawText(s,x,y,p);
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            int w=getWidth(), h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h, new int[]{0xff080d1b,0xff111d36,0xff05070d},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            // soft ambient glow
            p.setShader(new RadialGradient(w*0.78f,h*0.18f,dp(330),new int[]{0x443d72bd,0x003d72bd},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            text(c,"MICOM",dp(42),dp(54),15,0xffaab8d2,true);
            String date = new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(new Date());
            String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
            p.setTextAlign(Paint.Align.RIGHT);
            text(c,time+"   "+date,w-dp(42),dp(54),16,0xfff4f6fb,true);
            p.setTextAlign(Paint.Align.LEFT);
            text(c,"Your screen. Your apps.",dp(44),dp(112),28,0xffffffff,true);
            text(c,"Choose something to watch",dp(46),dp(143),14,0xffaab6cb,false);

            int cols = Math.max(3, Math.min(6, (int)(w/dp(190))));
            float tileW = Math.min(dp(164),(w-dp(100))/cols-dp(16));
            float tileH = dp(112), gap=dp(20);
            float total = cols*tileW+(cols-1)*gap;
            float startX=(w-total)/2f, startY=dp(190);
            for(int i=0;i<apps.size();i++){
                int row=i/cols, col=i%cols;
                float x=startX+col*(tileW+gap), y=startY+row*(tileH+dp(42));
                boolean sel=i==selected;
                p.setColor(sel?0xff354b70:0xff1d2a40);
                p.setShadowLayer(sel?dp(18):dp(5),0,dp(5),sel?0x885d9bff:0x33000000);
                c.drawRoundRect(x,y,x+tileW,y+tileH,dp(18),dp(18),p); p.clearShadowLayer();
                if(apps.get(i).icon!=null){
                    int iconSize=(int)dp(48);
                    apps.get(i).icon.setBounds((int)(x+(tileW-iconSize)/2),(int)(y+dp(15)),(int)(x+(tileW+iconSize)/2),(int)(y+dp(15)+iconSize));
                    apps.get(i).icon.draw(c);
                }
                p.setColor(0xfff5f7fb); p.setTextSize(dp(13)); p.setTypeface(Typeface.create("sans-serif-medium",0)); p.setTextAlign(Paint.Align.CENTER);
                String label=apps.get(i).label;
                if(p.measureText(label)>tileW-dp(12)) label=label.substring(0,Math.min(label.length(),14))+"…";
                c.drawText(label,x+tileW/2,y+dp(94),p); p.setTextAlign(Paint.Align.LEFT);
                if(sel){ p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(0xff9bc5ff); c.drawRoundRect(x- dp(2),y-dp(2),x+tileW+dp(2),y+tileH+dp(2),dp(20),dp(20),p); p.setStyle(Paint.Style.FILL); }
            }
            if(apps.isEmpty()) text(c,"No TV apps found. Check that apps are installed.",dp(46),dp(220),16,0xffffffff,false);
            text(c,"Use the remote arrows to move • OK to open • Home to return",dp(44),h-dp(28),12,0xff8e9bb2,false);
        }
        @Override public boolean onKeyDown(int key, android.view.KeyEvent e) {
            int cols=Math.max(3,Math.min(6,(int)(getWidth()/dp(190))));
            if(apps.isEmpty()) return true;
            if(key==KeyEvent.KEYCODE_DPAD_RIGHT) selected=Math.min(apps.size()-1,selected+1);
            else if(key==KeyEvent.KEYCODE_DPAD_LEFT) selected=Math.max(0,selected-1);
            else if(key==KeyEvent.KEYCODE_DPAD_DOWN) selected=Math.min(apps.size()-1,selected+cols);
            else if(key==KeyEvent.KEYCODE_DPAD_UP) selected=Math.max(0,selected-cols);
            else if(key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER) {
                try { Intent launch=getPackageManager().getLaunchIntentForPackage(apps.get(selected).pkg); if(launch!=null) startActivity(launch); }
                catch(Exception ignored) {}
            } else return super.onKeyDown(key,e);
            invalidate(); return true;
        }
    }
}
