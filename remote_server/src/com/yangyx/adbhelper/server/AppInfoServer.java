package com.yangyx.adbhelper.server;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.util.DisplayMetrics;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 远端轻量级应用信息与图标提取服务（通过 app_process 无安装直接运行）
 * 用法:
 *   CLASSPATH=/data/local/tmp/adbhelper_app_server.jar app_process / com.yangyx.adbhelper.server.AppInfoServer --version
 *   CLASSPATH=/data/local/tmp/adbhelper_app_server.jar app_process / com.yangyx.adbhelper.server.AppInfoServer <icons_dir> [--refresh] [--no-icons]
 */
public final class AppInfoServer {

    private static final int SERVER_VERSION = 1;
    private static final int ICON_MAX_PX = 128;
    private static final String DEFAULT_ICONS_DIR = "/data/local/tmp/adbhelper_icons";

    private AppInfoServer() {}

    public static void main(String[] args) {
        if (args != null && args.length > 0 && "--version".equals(args[0])) {
            System.out.println("ADBHELPER_APP_SERVER_VERSION=" + SERVER_VERSION);
            System.out.flush();
            System.exit(0);
            return;
        }

        String iconsDirPath = DEFAULT_ICONS_DIR;
        boolean forceRefresh = false;
        boolean extractIcons = true;

        if (args != null) {
            for (String arg : args) {
                if (arg == null || arg.trim().isEmpty()) continue;
                if ("--refresh".equals(arg)) {
                    forceRefresh = true;
                } else if ("--no-icons".equals(arg)) {
                    extractIcons = false;
                } else if (!arg.startsWith("--")) {
                    iconsDirPath = arg.trim();
                }
            }
        }

        try {
            prepareLooper();
            exemptHiddenApis();

            Context sysContext = obtainSystemContext();
            if (sysContext == null) {
                System.err.println("APP_ERROR:Failed to obtain system Context in app_process");
                System.exit(2);
                return;
            }

            PackageManager pm = sysContext.getPackageManager();
            if (pm == null) {
                System.err.println("APP_ERROR:PackageManager is null");
                System.exit(3);
                return;
            }

            File iconsDir = new File(iconsDirPath);
            if (extractIcons && !iconsDir.exists()) {
                iconsDir.mkdirs();
            }

            List<PackageInfo> packages = pm.getInstalledPackages(0);
            if (packages == null) {
                System.out.println("APP_DONE:0");
                System.out.flush();
                System.exit(0);
                return;
            }

            int count = 0;
            for (PackageInfo pkgInfo : packages) {
                if (pkgInfo == null || pkgInfo.packageName == null || pkgInfo.packageName.isEmpty()) {
                    continue;
                }
                ApplicationInfo appInfo = pkgInfo.applicationInfo;
                if (appInfo == null) {
                    continue;
                }

                String pkg = pkgInfo.packageName;
                boolean isSystem = (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;

                boolean isDisabled = !appInfo.enabled;
                if (!isDisabled) {
                    try {
                        int state = pm.getApplicationEnabledSetting(pkg);
                        if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
                            isDisabled = true;
                        }
                    } catch (Throwable ignored) {
                    }
                }

                String apkPath = appInfo.sourceDir != null ? appInfo.sourceDir : "";
                String verName = pkgInfo.versionName != null ? pkgInfo.versionName : "";
                long verCode;
                if (Build.VERSION.SDK_INT >= 28) {
                    verCode = pkgInfo.getLongVersionCode();
                } else {
                    @SuppressWarnings("deprecation")
                    long legacyCode = pkgInfo.versionCode;
                    verCode = legacyCode;
                }

                String label = resolveLabel(pm, sysContext, appInfo, apkPath, pkg);

                boolean hasIcon = false;
                String iconRemotePath = "";
                if (extractIcons) {
                    File outFile = new File(iconsDir, safeFileName(pkg) + ".png");
                    long updateTime = pkgInfo.lastUpdateTime;
                    if (!forceRefresh && outFile.exists() && outFile.length() > 64
                            && (updateTime <= 0 || outFile.lastModified() >= updateTime)) {
                        hasIcon = true;
                        iconRemotePath = outFile.getAbsolutePath();
                    } else {
                        Drawable iconDrawable = resolveIconDrawable(pm, sysContext, appInfo, apkPath, pkg);
                        if (iconDrawable != null && writeDrawableAsPng(iconDrawable, outFile, updateTime)) {
                            hasIcon = true;
                            iconRemotePath = outFile.getAbsolutePath();
                        }
                    }
                }

                JSONObject json = new JSONObject();
                json.put("pkg", pkg);
                json.put("label", label);
                json.put("verName", verName);
                json.put("verCode", verCode);
                json.put("sys", isSystem);
                json.put("disabled", isDisabled);
                json.put("apk", apkPath);
                json.put("icon", hasIcon);
                if (hasIcon) {
                    json.put("iconPath", iconRemotePath);
                }

                System.out.println("APP_JSON:" + json.toString());
                count++;
                if (count % 20 == 0) {
                    System.out.flush();
                }
            }

            System.out.println("APP_DONE:" + count);
            System.out.flush();
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("APP_ERROR:" + t.getClass().getSimpleName() + ":" + t.getMessage());
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void prepareLooper() {
        try {
            if (Looper.getMainLooper() == null) {
                Looper.prepareMainLooper();
            }
        } catch (Throwable ignored) {
        }
    }

    private static void exemptHiddenApis() {
        if (Build.VERSION.SDK_INT < 28) return;
        try {
            Method forName = Class.class.getDeclaredMethod("forName", String.class);
            Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
            Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
            Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null);
            Method setExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});
            Object vmRuntime = getRuntime.invoke(null);
            if (vmRuntime != null) {
                setExemptions.invoke(vmRuntime, new Object[]{new String[]{"L"}});
            }
        } catch (Throwable ignored) {
        }
    }

    private static Context obtainSystemContext() {
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Object at = null;
            try {
                Method currentAt = atClass.getMethod("currentActivityThread");
                at = currentAt.invoke(null);
            } catch (Throwable ignored) {
            }
            if (at == null) {
                try {
                    Method systemMain = atClass.getMethod("systemMain");
                    at = systemMain.invoke(null);
                } catch (Throwable ignored) {
                }
            }
            if (at != null) {
                Method getSysCtx = atClass.getMethod("getSystemContext");
                Object ctx = getSysCtx.invoke(at);
                if (ctx instanceof Context) {
                    return (Context) ctx;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String resolveLabel(
            PackageManager pm,
            Context sysContext,
            ApplicationInfo appInfo,
            String apkPath,
            String pkg
    ) {
        try {
            CharSequence cs = appInfo.loadLabel(pm);
            if (cs != null) {
                String s = cs.toString().trim();
                if (!s.isEmpty() && !s.equals(pkg) && !s.startsWith("@")) {
                    return s;
                }
            }
        } catch (Throwable ignored) {
        }

        if (appInfo.labelRes != 0 && apkPath != null && !apkPath.isEmpty()) {
            Resources apkRes = createApkResources(sysContext, apkPath);
            if (apkRes != null) {
                try {
                    CharSequence text = apkRes.getText(appInfo.labelRes);
                    if (text != null) {
                        String s = text.toString().trim();
                        if (!s.isEmpty() && !s.startsWith("@")) {
                            return s;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        if (appInfo.nonLocalizedLabel != null) {
            String s = appInfo.nonLocalizedLabel.toString().trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return pkg;
    }

    private static Drawable resolveIconDrawable(
            PackageManager pm,
            Context sysContext,
            ApplicationInfo appInfo,
            String apkPath,
            String pkg
    ) {
        if (appInfo.icon != 0) {
            try {
                Drawable d = appInfo.loadIcon(pm);
                if (d != null) return d;
            } catch (Throwable ignored) {
            }

            try {
                Drawable d = pm.getApplicationIcon(appInfo);
                if (d != null) return d;
            } catch (Throwable ignored) {
            }

            if (apkPath != null && !apkPath.isEmpty()) {
                Resources apkRes = createApkResources(sysContext, apkPath);
                if (apkRes != null) {
                    try {
                        Drawable d = apkRes.getDrawableForDensity(appInfo.icon, DisplayMetrics.DENSITY_XHIGH, null);
                        if (d != null) return d;
                    } catch (Throwable ignored) {
                    }
                    try {
                        @SuppressWarnings("deprecation")
                        Drawable d = apkRes.getDrawable(appInfo.icon);
                        if (d != null) return d;
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        // 若 Application 未设置 icon，再尝试检查启动 Activity 是否自带独立图标
        try {
            Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
            if (launchIntent != null) {
                ResolveInfo ri = pm.resolveActivity(launchIntent, 0);
                if (ri != null && ri.activityInfo != null && ri.activityInfo.icon != 0) {
                    Drawable d = ri.activityInfo.loadIcon(pm);
                    if (d != null) return d;
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Resources createApkResources(Context sysContext, String apkPath) {
        try {
            AssetManager am = AssetManager.class.getDeclaredConstructor().newInstance();
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            Integer cookie = (Integer) addAssetPath.invoke(am, apkPath);
            if (cookie == null || cookie == 0) {
                return null;
            }
            DisplayMetrics dm = sysContext.getResources().getDisplayMetrics();
            Configuration cfg = sysContext.getResources().getConfiguration();
            @SuppressWarnings("deprecation")
            Resources res = new Resources(am, dm, cfg);
            return res;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean writeDrawableAsPng(Drawable drawable, File outFile, long updateTime) {
        Bitmap toCompress = null;
        boolean shouldRecycle = false;
        try {
            if (drawable instanceof BitmapDrawable) {
                Bitmap src = ((BitmapDrawable) drawable).getBitmap();
                boolean isHardware = Build.VERSION.SDK_INT >= 26 && src != null && src.getConfig() == Bitmap.Config.HARDWARE;
                if (src != null && !src.isRecycled() && !isHardware && src.getWidth() > 0 && src.getHeight() > 0) {
                    if (src.getWidth() > ICON_MAX_PX || src.getHeight() > ICON_MAX_PX) {
                        float scale = Math.min((float) ICON_MAX_PX / src.getWidth(), (float) ICON_MAX_PX / src.getHeight());
                        int w = Math.max(1, Math.round(src.getWidth() * scale));
                        int h = Math.max(1, Math.round(src.getHeight() * scale));
                        toCompress = Bitmap.createScaledBitmap(src, w, h, true);
                        shouldRecycle = (toCompress != src);
                    } else {
                        toCompress = src;
                    }
                }
            }

            if (toCompress == null) {
                int w = drawable.getIntrinsicWidth();
                int h = drawable.getIntrinsicHeight();
                if (w <= 0 || h <= 0) {
                    w = ICON_MAX_PX;
                    h = ICON_MAX_PX;
                } else if (w > ICON_MAX_PX || h > ICON_MAX_PX) {
                    float scale = Math.min((float) ICON_MAX_PX / w, (float) ICON_MAX_PX / h);
                    w = Math.max(1, Math.round(w * scale));
                    h = Math.max(1, Math.round(h * scale));
                }
                Bitmap canvasBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(canvasBmp);
                drawable.setBounds(0, 0, w, h);
                drawable.draw(canvas);
                toCompress = canvasBmp;
                shouldRecycle = true;
            }

            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            File tmpFile = new File(parent, outFile.getName() + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                if (!toCompress.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                    tmpFile.delete();
                    return false;
                }
                fos.flush();
            }

            if (outFile.exists()) {
                outFile.delete();
            }
            if (!tmpFile.renameTo(outFile)) {
                tmpFile.delete();
                return false;
            }
            if (updateTime > 0) {
                outFile.setLastModified(updateTime);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (shouldRecycle && toCompress != null && !toCompress.isRecycled()) {
                try {
                    toCompress.recycle();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static String safeFileName(String pkg) {
        return pkg.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
