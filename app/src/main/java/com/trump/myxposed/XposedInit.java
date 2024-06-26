package com.trump.myxposed;

import android.content.SharedPreferences;

import com.socks.library.KLog;
import com.trump.myxposed.hook.WeicoHook;
import com.trump.myxposed.util.Utils;
import com.trump.myxposed.util.XSpUtil;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Author: TRUMP
 * Date:   2022/2/10 0010 14:56
 * Desc:
 */
public class XposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        Utils.log("LoadPackage:" + lpparam.packageName);
        switch (lpparam.packageName) {
            case Constant.PackageIds.weico:
                new WeicoHook().handleLoadPackage(lpparam);
                break;
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

    }

}
