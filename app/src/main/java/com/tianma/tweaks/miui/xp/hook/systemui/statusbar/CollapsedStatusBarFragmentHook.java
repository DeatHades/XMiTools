package com.tianma.tweaks.miui.xp.hook.systemui.statusbar;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.tianma.tweaks.miui.utils.XLog;
import com.tianma.tweaks.miui.utils.XSPUtils;
import com.tianma.tweaks.miui.utils.rom.MiuiVersion;
import com.tianma.tweaks.miui.xp.hook.BaseSubHook;
import com.tianma.tweaks.miui.xp.hook.systemui.helper.ResHelpers;
import com.tianma.tweaks.miui.xp.wrapper.MethodHookWrapper;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

import static com.tianma.tweaks.miui.xp.wrapper.XposedWrapper.findAndHookMethod;

public class CollapsedStatusBarFragmentHook extends BaseSubHook {

    private static final String CLASS_STATUS_BAR_FRAGMENT = "com.android.systemui.statusbar.phone.CollapsedStatusBarFragment";

    private boolean mSignalAlignLeft;
    private boolean mAlwaysShowStatusBarClock;

    public CollapsedStatusBarFragmentHook(ClassLoader classLoader, XSharedPreferences xsp, MiuiVersion miuiVersion) {
        super(classLoader, xsp, miuiVersion);

        mSignalAlignLeft = XSPUtils.isSignalAlignLeft(xsp);
        mAlwaysShowStatusBarClock = XSPUtils.alwaysShowStatusBarClock(xsp);
    }

    @Override
    public void startHook() {
        try {
            XLog.d("Hooking CollapsedStatusBarFragment... ");
            if (mSignalAlignLeft) {
                hookOnViewCreated();
            }

            if (mAlwaysShowStatusBarClock) {
                hookClockVisibleAnimate();
            }
        } catch (Throwable t) {
            XLog.e("Error occurs when hook CollapsedStatusBarFragment", t);
        }
    }

    // CollapsedStatusBarFragment#onViewCreated()
    private void hookOnViewCreated() {
        findAndHookMethod(CLASS_STATUS_BAR_FRAGMENT,
                mClassLoader,
                "onViewCreated",
                View.class,
                Bundle.class,
                new MethodHookWrapper() {
                    @Override
                    protected void after(MethodHookParam param) {
                        ViewGroup phoneStatusBarView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mStatusBar");
                        Resources res = phoneStatusBarView.getResources();

                        View signalClusterViewContainer = phoneStatusBarView
                                .findViewById(ResHelpers.getId(res, "signal_cluster_view"));
                        ((ViewGroup) signalClusterViewContainer.getParent()).removeView(signalClusterViewContainer);

                        if (mMiuiVersion.getTime() >= MiuiVersion.V_19_5_7.getTime()) {
                            LinearLayout contentsContainer = phoneStatusBarView
                                    .findViewById(ResHelpers.getId(res, "phone_status_bar_contents_container"));
                            contentsContainer.setGravity(Gravity.CENTER_VERTICAL);
                            contentsContainer.addView(signalClusterViewContainer, 0);
                        } else {
                            LinearLayout statusBarContents = phoneStatusBarView
                                    .findViewById(ResHelpers.getId(res, "status_bar_contents"));
                            statusBarContents.setGravity(Gravity.CENTER_VERTICAL);
                            statusBarContents.addView(signalClusterViewContainer, 0);
                        }
                    }
                });
    }

    private void hookClockVisibleAnimate() {
        findAndHookMethod(CLASS_STATUS_BAR_FRAGMENT,
                mClassLoader,
                "clockVisibleAnimate",
                boolean.class,
                boolean.class,
                new MethodHookWrapper() {
                    @Override
                    protected void before(MethodHookParam param) {
                        View mStatusClock = (View) XposedHelpers.getObjectField(param.thisObject, "mStatusClock");
                        mStatusClock.setVisibility(View.VISIBLE);
                        param.setResult(null);
                    }
                });
    }

}
