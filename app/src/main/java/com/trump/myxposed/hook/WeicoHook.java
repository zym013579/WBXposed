package com.trump.myxposed.hook;

import android.app.Application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** 微博轻享版（国际版）去广告，启用模块后自动生效。 */
public final class WeicoHook {

    private static final String SETTING = "com.weico.international.activity.v4.Setting";
    private static final String FUNCTION1 = "kotlin.jvm.functions.Function1";
    private static final String AD_CALLBACK = "queryUveAdRequest$lambda$";

    public void hook(ClassLoader classLoader, String versionName) {
        XposedBridge.log("WBXposed: 安装微博去广告 Hook，版本 " + versionName);
        removeSplashAd(classLoader);
        removeTimelineAd(classLoader, versionName);
    }

    private void removeSplashAd(ClassLoader classLoader) {
        hookMethod(classLoader, "com.weico.international.activity.LogoActivity", "doWhatNext",
                XC_MethodReplacement.returnConstant("main"));
        hookMethod(classLoader, "com.weico.international.activity.LogoActivity", "triggerPermission",
                boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.args[0] = true;
                    }
                });
        // 保留原有的热启动广告拦截，阻止回到前台时再次弹出广告。
        hookMethod(classLoader, "com.weico.international.manager.ProcessMonitor", "attach",
                Application.class, XC_MethodReplacement.returnConstant(null));
    }

    private void removeTimelineAd(ClassLoader classLoader, String versionName) {
        int firstCallback = getFirstAdCallback(versionName);
        if (firstCallback == 12 || firstCallback == 21) {
            hookMethod(classLoader, "com.weico.international.manager.uvead.UveAdHelper",
                    AD_CALLBACK + firstCallback, FUNCTION1, Object.class, emptyAdList());
        } else {
            String rxApi = "com.weico.international.api.RxApiKt";
            hookMethod(classLoader, rxApi, AD_CALLBACK + firstCallback, Map.class,
                    XC_MethodReplacement.returnConstant(""));
            hookMethod(classLoader, rxApi, AD_CALLBACK + (firstCallback + 1), FUNCTION1, Object.class,
                    XC_MethodReplacement.returnConstant(""));
            hookMethod(classLoader, rxApi, AD_CALLBACK + (firstCallback + 2), FUNCTION1, Object.class,
                    emptyAdList());
        }

        hookMethod(classLoader, "com.weico.international.utility.KotlinExtendKt", "isWeiboUVEAd",
                "com.weico.international.model.sina.Status", XC_MethodReplacement.returnConstant(false));
        hookMethod(classLoader, "com.weico.international.utility.KotlinUtilKt", "findUVEAd",
                "com.weico.international.model.sina.PageInfo", XC_MethodReplacement.returnConstant(null));

        hookMethod(classLoader, SETTING, "loadBoolean", String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String key = (String) param.args[0];
                if ("BOOL_UVE_FEED_AD".equals(key)) {
                    param.setResult(false);
                } else if (key != null && key.startsWith("BOOL_AD_ACTIVITY_BLOCK_")) {
                    param.setResult(true);
                }
            }
        });

        XC_MethodHook adIntSetting = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String key = (String) param.args[0];
                if ("ad_interval".equals(key)) {
                    param.setResult(Integer.MAX_VALUE);
                } else if ("display_ad".equals(key)) {
                    param.setResult(0);
                }
            }
        };
        hookMethod(classLoader, SETTING, "loadInt", String.class, adIntSetting);
        hookMethod(classLoader, SETTING, "loadInt", String.class, int.class, adIntSetting);

        hookMethod(classLoader, SETTING, "loadStringSet", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if ("CYT_DAYS".equals(param.args[0])) {
                    param.setResult(new HashSet<String>());
                }
            }
        });

        XC_MethodHook videoAdSetting = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if ("video_ad".equals(param.args[0])) {
                    param.setResult("");
                }
            }
        };
        hookMethod(classLoader, SETTING, "loadString", String.class, videoAdSetting);
        hookMethod(classLoader, SETTING, "loadString", String.class, String.class, videoAdSetting);
    }

    private static XC_MethodHook emptyAdList() {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return new ArrayList<>();
            }
        };
    }

    private static int getFirstAdCallback(String versionName) {
        // 沿用已有版本映射，包括本地修正的 6.9.8 起使用 lambda$21。
        if (isVersionBefore(versionName, "6.2.6")) return 156;
        if (isVersionBefore(versionName, "6.3.8")) return 151;
        if (isVersionBefore(versionName, "6.4.4")) return 159;
        if (isVersionBefore(versionName, "6.4.5")) return 163;
        if (isVersionBefore(versionName, "6.4.8")) return 165;
        if (isVersionBefore(versionName, "6.5.0")) return 167;
        if (isVersionBefore(versionName, "6.5.8")) return 164;
        if (isVersionBefore(versionName, "6.9.8")) return 12;
        return 21;
    }

    private static boolean isVersionBefore(String versionName, String target) {
        // 按数字分段比较，避免 6.10.x 被字符串比较误判为早于 6.2.6。
        String[] actual = versionName == null ? new String[0] : versionName.split("\\D+");
        String[] expected = target.split("\\.");
        for (int i = 0; i < expected.length; i++) {
            int part = i < actual.length && !actual[i].isEmpty() ? Integer.parseInt(actual[i]) : 0;
            int targetPart = Integer.parseInt(expected[i]);
            if (part != targetPart) {
                return part < targetPart;
            }
        }
        return false;
    }

    private static void hookMethod(ClassLoader classLoader, String className, String methodName,
                                   Object... parameterTypesAndCallback) {
        try {
            // 类型名也交给 Xposed 在目标应用的 ClassLoader 中解析。
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError | RuntimeException e) {
            // 单个版本相关的类或方法缺失时，继续安装其余广告 Hook。
            XposedBridge.log("WBXposed: 跳过 " + className + "#" + methodName + ": " + e);
        }
    }
}
