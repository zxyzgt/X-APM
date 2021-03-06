package github.tornaco.xposedmoduletest.provider;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.Observable;

import github.tornaco.xposedmoduletest.xposed.app.XAshmanManager;
import github.tornaco.xposedmoduletest.xposed.service.BuildFingerprintBuildHostInfo;

/**
 * Created by guohao4 on 2017/10/19.
 * Email: Tornaco@163.com
 */

public class AppSettings extends Observable {

    private static final String PREF_NAME = "app_settings";

    private static AppSettings sMe = new AppSettings();

    private AppSettings() {
    }

    public static AppSettings get() {
        return sMe;
    }

    public static boolean isDrawVibrateEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(AppKey.DRAW_VIBRATE, false);
    }

    public static boolean isFirstRun(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(AppKey.FIRST_RUN, true);
    }

    public static boolean setFirstRun(Context context) {
        boolean first = isFirstRun(context);
        if (first) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(AppKey.FIRST_RUN, false)
                    .apply();
        }
        return first;
    }

    public static void setFirstSee(Context context, String tag) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(tag, false)
                .apply();
    }

    public static boolean isFirstSee(Context context, String tag) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(tag, true);
    }

    public static boolean isNewBuild(Context context) {
//        try {
//            String buildDateOld = PreferenceManager.getDefaultSharedPreferences(context)
//                    .getString(AppKey.BUILD_DATE, null);
//            return buildDateOld != null
//                    && !buildDateOld.equals(XAppBuildHostInfo.BUILD_DATE);
//        } finally {
//            PreferenceManager.getDefaultSharedPreferences(context)
//                    .edit()
//                    .putString(AppKey.BUILD_DATE, XAppBuildHostInfo.BUILD_DATE)
//                    .apply();
//        }
        String serverSerial = XAshmanManager.get().isServiceAvailable() ? XAshmanManager.get().getBuildSerial() : null;
        if (serverSerial == null) return false;
        String appBuildSerial = BuildFingerprintBuildHostInfo.BUILD_FINGER_PRINT;
        return !TextUtils.isEmpty(appBuildSerial) && !appBuildSerial.equals(serverSerial);
    }

    public static void setShowInfo(Context context, String who, boolean show) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(AppKey.SHOW_INFO_PREFIX + who, show)
                .apply();
    }

    public static boolean isShowInfoEnabled(Context context, String who) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(AppKey.SHOW_INFO_PREFIX + who, true);
    }

    public static boolean isShowInfoEnabled(Context context, String who, boolean def) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(AppKey.SHOW_INFO_PREFIX + who, def);
    }

    public static boolean show2ColumnsIn(Context context, String where) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(AppKey.MAIN_DASH_COLUMN_COUNT + where, true);
    }

    public static void setShow2ColumnsIn(Context context, String who, boolean show) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(AppKey.MAIN_DASH_COLUMN_COUNT + who, show)
                .apply();
    }

}
