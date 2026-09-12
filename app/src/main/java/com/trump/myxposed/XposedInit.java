package com.trump.myxposed;

import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;

import com.trump.myxposed.hook.WeicoHook;

import io.github.libxposed.api.XposedModule;

public final class XposedInit extends XposedModule {

    private static final String TARGET_PACKAGE = "com.weico.international";
    private static final String TAG = "WBXposed";
    private boolean initialized;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        String processName = param.getProcessName();
        if (param.isSystemServer() || !(TARGET_PACKAGE.equals(processName)
                || processName.startsWith(TARGET_PACKAGE + ":"))) {
            detach();
        }
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        try {
            // 保持原有初始化时机，等 Application 的上下文和实际 ClassLoader 就绪。
            hook(Application.class.getDeclaredMethod("onCreate"))
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Application application = (Application) chain.getThisObject();
                        if (!initialized && TARGET_PACKAGE.equals(application.getPackageName())) {
                            initialized = true;
                            String versionName = "";
                            try {
                                versionName = application.getPackageManager()
                                        .getPackageInfo(TARGET_PACKAGE, 0).versionName;
                            } catch (PackageManager.NameNotFoundException e) {
                                log(Log.WARN, TAG, "无法读取微博版本", e);
                            }
                            new WeicoHook(this).hook(application.getClassLoader(), versionName);
                        }
                        return chain.proceed();
                    });
        } catch (NoSuchMethodException | RuntimeException e) {
            log(Log.ERROR, TAG, "无法安装微博初始化 Hook", e);
        }
    }
}
