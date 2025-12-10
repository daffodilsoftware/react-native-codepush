package com.skcodepush;

import android.content.Intent;
import android.os.Build;
import java.io.File;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import android.util.Log;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.content.SharedPreferences;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

public class CodePushModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public CodePushModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "CodePush";
    }

    public static String getBundlePathIfExistsSync(Context context) {
        String version = "";  
        try {
            version = context
                .getPackageManager()
                .getPackageInfo(context.getPackageName(), 0)
                .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        String relativePath = "CodePush/" + version + "/unzipped/ota/index.android.bundle";
        File bundle = new File(
            context.getFilesDir(),
            relativePath
        );
        Log.d("CodePush relative path:",  relativePath);
        Log.d("CodePush relative path:",  bundle.getAbsolutePath());
        Log.d("CodePush relative path:",  bundle.exists()+"");

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

    @ReactMethod
    public void getVersion(Promise promise) {
        try {
            PackageInfo pInfo = reactContext
                    .getPackageManager()
                    .getPackageInfo(reactContext.getPackageName(), 0);

            String version = pInfo.versionName;  // same as DeviceInfo.getVersion()
            promise.resolve(version);

        } catch (PackageManager.NameNotFoundException e) {
            promise.reject("ERR_VERSION", "Failed to get version", e);
        }
    }

    @ReactMethod
    public void getBuildNumber(Promise promise) {
        try {
            PackageInfo pInfo = reactContext
                    .getPackageManager()
                    .getPackageInfo(reactContext.getPackageName(), 0);

            long build = pInfo.getLongVersionCode(); // same as DeviceInfo.getBuildNumber()

            promise.resolve(String.valueOf(build));

        } catch (PackageManager.NameNotFoundException e) {
            Log.e("NameNotFoundException","build number", e);
            promise.reject("ERR_BUILD", "Failed to get build number", e);
        }
    }

    @ReactMethod
    public void unzip(String zipPath, String destPath, Promise promise) {
        try {
            File destDir = new File(destPath);
            if (!destDir.exists()) destDir.mkdirs();

            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
            ZipEntry entry;

            byte[] buffer = new byte[1024];

            while ((entry = zis.getNextEntry()) != null) {

                File newFile = new File(destPath, entry.getName());

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                    continue;
                } else {
                    newFile.getParentFile().mkdirs();
                }

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                zis.closeEntry();
            }

            zis.close();

            promise.resolve(true);

        } catch (Exception e) {
            Log.e("Exception", "unzip", e);
            promise.reject("ERR_UNZIP", "Failed to unzip", e);
        }
    }

    @ReactMethod
    public void getDocumentsPath(Promise promise) {
        try {
        String path = reactContext.getFilesDir().getAbsolutePath();
        promise.resolve(path);
        } catch (Exception e) {
        Log.e("getDocumentsPath", "error en get document path", e);
        promise.reject("E_PATH_ERROR", "Failed to get documents path", e);
        }
    }

    @ReactMethod
    public void exists(String path, Promise promise) {
        try{
        File file = new File(path);
        promise.resolve(file.exists());
        } catch (Exception e) {
            Log.e("exists", "check file exist", e);
            promise.reject("E_PATH_ERROR", "Failed to check documents path", e);
        }
    }

    @ReactMethod
    public void mkdir(String path, Promise promise) {
        try{
            File file = new File(path);
            boolean ok = file.mkdirs();
            promise.resolve(ok);
        } catch (Exception e) {
            Log.e("mkdir", "create directory", e);
            promise.reject("E_PATH_ERROR", "Failed to create dir", e);
        }
    }

    @ReactMethod
    public void unlink(String path, Promise promise) {
        try{
            File file = new File(path);
            if (!file.exists()) {
                promise.resolve(true);
                return;
            }
            boolean deleted = file.delete();
            promise.resolve(deleted);
        } catch (Exception e) {
            Log.e("unlink/delete", "delete directory", e);
            promise.reject("E_PATH_ERROR", "Failed to unlink/delete dir", e);
        }
    }

    @ReactMethod
    public void downloadFile(String url, String destPath, Promise promise) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                promise.reject("ERR_DOWNLOAD", "Failed to download", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    promise.reject("ERR_DOWNLOAD", "HTTP error " + response.code());
                    return;
                }

                InputStream in = response.body().byteStream();
                File outFile = new File(destPath);
                outFile.getParentFile().mkdirs();
                FileOutputStream out = new FileOutputStream(outFile);

                byte[] buffer = new byte[4096];
                int len;

                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }

                out.close();
                in.close();

                WritableMap map = Arguments.createMap();
                map.putInt("statusCode", 200);

                promise.resolve(map);
            }
        });
    }
    @ReactMethod
    public void setItem(String key, String value) {
        SharedPreferences prefs = 
            getReactApplicationContext().getSharedPreferences("CodePushStore", Context.MODE_PRIVATE);

        prefs.edit().putString(key, value).apply();
    }
    @ReactMethod
    public void getItem(String key, Promise promise) {
        try {
            SharedPreferences prefs = 
                getReactApplicationContext().getSharedPreferences("CodePushStore", Context.MODE_PRIVATE);

            String value = prefs.getString(key, "");
            promise.resolve(value);
        } catch (Exception e) {
            promise.reject("ERR_READ", "Failed to read value", e);
        }
    }
}
