package com.trump.myxposed;

import android.app.Application;
import android.content.pm.PackageManager;

import com.trump.myxposed.hook.WeicoHook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class XposedInit implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.weico.international";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        // 等应用的 ClassLoader 就绪后，在原有的 Application.onCreate 时机安装广告 Hook。
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            private boolean initialized;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Application application = (Application) param.thisObject;
                if (initialized || !TARGET_PACKAGE.equals(application.getPackageName())) {
                    return;
                }
                initialized = true;

                String versionName = "";
                try {
                    versionName = application.getPackageManager()
                            .getPackageInfo(TARGET_PACKAGE, 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    XposedBridge.log("WBXposed: 无法读取微博版本: " + e);
                }
                new WeicoHook().hook(application.getClassLoader(), versionName);
            }
        });
    }
}
