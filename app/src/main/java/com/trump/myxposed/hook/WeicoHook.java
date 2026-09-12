package com.trump.myxposed.hook;

import android.app.Application;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.Hooker;

/** 微博轻享版（国际版）去广告，启用模块后自动生效。 */
public final class WeicoHook {

    private static final String TAG = "WBXposed";
    private static final String SETTING = "com.weico.international.activity.v4.Setting";
    private static final String FUNCTION1 = "kotlin.jvm.functions.Function1";
    private static final String AD_CALLBACK = "queryUveAdRequest$lambda$";
    private final XposedInterface framework;

    public WeicoHook(XposedInterface framework) {
        this.framework = framework;
    }

    public void hook(ClassLoader classLoader, String versionName) {
        framework.log(Log.INFO, TAG, "安装微博去广告 Hook，版本 " + versionName);
        removeSplashAd(classLoader);
        removeTimelineAd(classLoader, versionName);
    }

    private void removeSplashAd(ClassLoader classLoader) {
        hookMethod(classLoader, "com.weico.international.activity.LogoActivity", "doWhatNext",
                chain -> "main");
        hookMethod(classLoader, "com.weico.international.activity.LogoActivity", "triggerPermission",
                chain -> chain.proceed(new Object[]{true}), boolean.class);
        // 保留原有的热启动广告拦截，阻止回到前台时再次弹出广告。
        hookMethod(classLoader, "com.weico.international.manager.ProcessMonitor", "attach",
                chain -> null, Application.class);
    }

    private void removeTimelineAd(ClassLoader classLoader, String versionName) {
        int firstCallback = getFirstAdCallback(versionName);
        if (firstCallback == 12 || firstCallback == 21) {
            hookMethod(classLoader, "com.weico.international.manager.uvead.UveAdHelper",
                    AD_CALLBACK + firstCallback, chain -> new ArrayList<>(), FUNCTION1, Object.class);
        } else {
            String rxApi = "com.weico.international.api.RxApiKt";
            hookMethod(classLoader, rxApi, AD_CALLBACK + firstCallback, chain -> "", Map.class);
            hookMethod(classLoader, rxApi, AD_CALLBACK + (firstCallback + 1),
                    chain -> "", FUNCTION1, Object.class);
            hookMethod(classLoader, rxApi, AD_CALLBACK + (firstCallback + 2),
                    chain -> new ArrayList<>(), FUNCTION1, Object.class);
        }

        hookMethod(classLoader, "com.weico.international.utility.KotlinExtendKt", "isWeiboUVEAd",
                chain -> false, "com.weico.international.model.sina.Status");
        hookMethod(classLoader, "com.weico.international.utility.KotlinUtilKt", "findUVEAd",
                chain -> null, "com.weico.international.model.sina.PageInfo");

        hookMethod(classLoader, SETTING, "loadBoolean", chain -> {
            String key = (String) chain.getArg(0);
            if ("BOOL_UVE_FEED_AD".equals(key)) return false;
            if (key != null && key.startsWith("BOOL_AD_ACTIVITY_BLOCK_")) return true;
            return chain.proceed();
        }, String.class, boolean.class);

        Hooker adIntSetting = chain -> {
            String key = (String) chain.getArg(0);
            if ("ad_interval".equals(key)) return Integer.MAX_VALUE;
            if ("display_ad".equals(key)) return 0;
            return chain.proceed();
        };
        hookMethod(classLoader, SETTING, "loadInt", adIntSetting, String.class);
        hookMethod(classLoader, SETTING, "loadInt", adIntSetting, String.class, int.class);

        hookMethod(classLoader, SETTING, "loadStringSet", chain -> {
            if ("CYT_DAYS".equals(chain.getArg(0))) return new HashSet<String>();
            return chain.proceed();
        }, String.class);

        Hooker videoAdSetting = chain -> {
            if ("video_ad".equals(chain.getArg(0))) return "";
            return chain.proceed();
        };
        hookMethod(classLoader, SETTING, "loadString", videoAdSetting, String.class);
        hookMethod(classLoader, SETTING, "loadString", videoAdSetting, String.class, String.class);
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

    private void hookMethod(ClassLoader classLoader, String className, String methodName,
                            Hooker hooker, Object... parameterTypes) {
        try {
            Class<?> owner = Class.forName(className, false, classLoader);
            Class<?>[] signature = new Class<?>[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Object type = parameterTypes[i];
                signature[i] = type instanceof Class<?> ? (Class<?>) type
                        : Class.forName((String) type, false, classLoader);
            }
            Method method = owner.getDeclaredMethod(methodName, signature);
            method.setAccessible(true);
            framework.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(hooker);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            // 应用版本相关的类或方法缺失时继续安装其余 Hook；不吞掉框架自身的致命错误。
            framework.log(Log.WARN, TAG, "跳过 " + className + "#" + methodName, e);
        }
    }
}
