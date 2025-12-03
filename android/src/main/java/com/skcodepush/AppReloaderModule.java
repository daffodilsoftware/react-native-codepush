package com.skcodepush;

import android.content.Intent;
import android.os.Build;
import java.io.File;
import android.content.Context;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class AppReloaderModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public AppReloaderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "AppReloader";
    }

public static String getBundlePathIfExistsSync(Context context) {
        File bundle = new File(
            context.getFilesDir(),
            "CodePush/unzipped/ota/index.android.bundle"
        );

        return bundle.exists() ? bundle.getAbsolutePath() : null;
    }

    @ReactMethod
    public void restartApp() {
        if (getCurrentActivity() == null) return;

        final android.app.Activity activity = getCurrentActivity();
        if (activity == null) return;

        Intent intent = activity.getIntent();
        intent.putExtra("APP_RESTARTED_BY_OTA", true);

        android.content.pm.PackageManager pm = activity.getPackageManager();
        Intent mainIntent = pm.getLaunchIntentForPackage(activity.getPackageName());

        if (mainIntent != null) {
            android.content.ComponentName componentName = mainIntent.getComponent();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                Intent restartIntent = Intent.makeRestartActivityTask(componentName);
                activity.startActivity(restartIntent);
            }
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }

        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
