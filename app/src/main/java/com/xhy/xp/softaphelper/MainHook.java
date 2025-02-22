package com.xhy.xp.softaphelper;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.net.LinkAddress;
import android.os.Build;

import java.lang.reflect.Constructor;
import java.util.HashSet;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final boolean SHOULD_FILTER_PKG_NAME = true;

    private static final String className_P = "com.android.server.connectivity.tethering.TetherInterfaceStateMachine";
    private static final String className_Q = "android.net.ip.IpServer";

    private static final String methodName_P_Q = "getRandomWifiIPv4Address";
    private static final String methodName_R = "requestIpv4Address";

    private static final String callerMethodName_Q = "configureIPv4";

    private static final String WIFI_HOST_IFACE_ADDR = "192.168.43.1";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String packageName = lpparam.packageName;
        ClassLoader classLoader = lpparam.classLoader;
//        XposedBridge.log("[handleLoadPackage] packageName: " + packageName);

        final String className = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ? className_P :
                className_Q;
        final String methodName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? methodName_R :
                methodName_P_Q;
        final String WIFI_HOST_IFACE_ADDRESS = WIFI_HOST_IFACE_ADDR + "/24";

        HashSet<String> pkgNameSet = new HashSet<>();
        pkgNameSet.add("com.android.networkstack.tethering.inprocess");
        pkgNameSet.add("com.android.networkstack.tethering");
        pkgNameSet.add("com.google.android.networkstack.tethering.inprocess");
        pkgNameSet.add("com.google.android.networkstack.tethering");
        pkgNameSet.add("android");
        if (SHOULD_FILTER_PKG_NAME && !pkgNameSet.contains(packageName))
            return;

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // 安卓框架
            findAndHookMethod(className, classLoader, methodName,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            return WIFI_HOST_IFACE_ADDR;
                        }
                    });
        }
        else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            Constructor<?> ctor = LinkAddress.class.getDeclaredConstructor(String.class);
            final Object mLinkAddress = ctor.newInstance(WIFI_HOST_IFACE_ADDRESS);

            findAndHookMethod(className, classLoader, methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
//                            XposedBridge.log(StackUtils.getStackTraceString());
                            if (StackUtils.isCallingFrom(className, callerMethodName_Q)) {
                                param.setResult(mLinkAddress);
                            }
                        }
                    });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Constructor<?> ctor = LinkAddress.class.getDeclaredConstructor(String.class);
            final Object mLinkAddress = ctor.newInstance(WIFI_HOST_IFACE_ADDRESS);

            findAndHookMethod(className, classLoader, methodName, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
//                            XposedBridge.log(StackUtils.getStackTraceString());
                            if (StackUtils.isCallingFrom(className, callerMethodName_Q)) {
                                param.setResult(mLinkAddress);
                            }
                        }
                    });
        }
    }


}