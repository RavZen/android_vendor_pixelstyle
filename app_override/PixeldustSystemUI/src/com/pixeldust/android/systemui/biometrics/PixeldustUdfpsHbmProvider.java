package com.pixeldust.android.systemui.biometrics;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.systemui.biometrics.UdfpsHbmProvider;
import com.android.systemui.biometrics.UdfpsHbmTypes;
import com.android.systemui.biometrics.UdfpsHbmTypes.HbmType;
import com.android.systemui.dagger.SysUISingleton;

import com.google.hardware.pixel.display.IDisplay;

import javax.inject.Inject;

@SysUISingleton
public class PixeldustUdfpsHbmProvider implements IBinder.DeathRecipient,UdfpsHbmProvider {

    private boolean halFindFailed = false;
    private Handler mHandler;
    private IDisplay mDisplayHal;

    @Inject
    public PixeldustUdfpsHbmProvider() {
        super();
        mHandler = new Handler(Looper.getMainLooper());
        getPixelDisplayHal();
    }

    private IDisplay getPixelDisplayHal() {
        IDisplay iDisplay = mDisplayHal;
        if (iDisplay != null) {
            return iDisplay;
        }
        if (!halFindFailed) {
            IBinder waitForDeclaredService = ServiceManager.waitForDeclaredService("com.google.hardware.pixel.display.IDisplay/default");
            if (waitForDeclaredService == null) {
                Log.e("PixeldustUdfpsHbmProvider", "getPixelDisplayHal | Failed to find the Display HAL");
                halFindFailed = true;
                return null;
            }
            try {
                waitForDeclaredService.linkToDeath(this, 0);
                mDisplayHal = IDisplay.Stub.asInterface(waitForDeclaredService);
                return mDisplayHal;
            } catch (RemoteException e) {
                Log.e("PixeldustUdfpsHbmProvider", "getPixelDisplayHal | Failed to link to death", e);
                return null;
            }
        }
        return null;
    }

    @Override
    public void binderDied() {
        Log.e("PixeldustUdfpsHbmProvider", "binderDied | Display HAL died");
        this.mDisplayHal = null;
    }

    @Override
    public void enableHbm(@HbmType int hbmType, @Nullable Surface surface,
            @Nullable Runnable onHbmEnabled) {
        if (hbmType == UdfpsHbmTypes.LOCAL_HBM) {
            enableLhbm();
        }
        if (onHbmEnabled != null) {
            mHandler.post(onHbmEnabled);
        }
    }

    @Override
    public void disableHbm(@Nullable Runnable onHbmDisabled) {
        disableLhbm();
        Handler handler = new Handler(Looper.getMainLooper());
        if (onHbmDisabled != null) {
            mHandler.post(onHbmDisabled);
        }
    }

    private void disableLhbm() {
        Log.v("PixeldustUdfpsHbmProvider", "disableLhbm");
        IDisplay displayHal = getPixelDisplayHal();
        if (displayHal == null) {
            Log.e("PixeldustUdfpsHbmProvider", "disableLhbm | displayHal is null");
            return;
        }
        try {
            displayHal.setLhbmState(false);
        } catch (RemoteException e) {
            Log.e("PixeldustUdfpsHbmProvider", "disableLhbm | RemoteException", e);
        }
    }

    private void enableLhbm() {
        Log.v("PixeldustUdfpsHbmProvider", "enableLhbm");
        IDisplay displayHal = getPixelDisplayHal();
        if (displayHal == null) {
            Log.e("PixeldustUdfpsHbmProvider", "enableLhbm | displayHal is null");
            return;
        }
        try {
            displayHal.setLhbmState(true);
        } catch (RemoteException e) {
            Log.e("PixeldustUdfpsHbmProvider", "enableLhbm | RemoteException", e);
        }
    }
}
