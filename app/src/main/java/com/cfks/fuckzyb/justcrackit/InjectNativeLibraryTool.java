package com.cfks.fuckzyb.justcrackit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import com.tech.pinduoduo_backdoor.AliveModule;
import com.tech.pinduoduo_backdoor.FileProviderV2;
import com.tech.pinduoduo_backdoor.GlobalInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jiesheng.文件操作;

public class InjectNativeLibraryTool {
    private String targetPkg = "com.zuoyebang.iot.pad.appstore";
    private Context ctx;

    public InjectNativeLibraryTool(Context ctx, String targetPkg) {
        this.targetPkg = targetPkg;
        this.ctx = ctx;
    }

    //例子：injectNativeLibrary("liblogan.so", "/path/to/your/new/so/file")
    public void injectNativeLibrary(String libName, String newLibFilePath) throws Exception {
        InputStream fis = new FileInputStream(new File(newLibFilePath));
        Uri uri = Uri.parse(FileProviderV2.instance().obtainNewestIntent(GlobalInfo.SCENE).getTargetUri() + new File(getNativeLibraryDir(), libName).getAbsolutePath());
        OutputStream os = ctx.getContentResolver().openOutputStream(uri);
        copyStream(fis, os);
    }

    //先注入再运行，dexPath为要加载的本地dex文件路径，receptorActivity为受体activity(如com.zuoyebang.iot.pad.appstore.HomeActivity)，launchBySystem为是否用系统权限打开
    public boolean runPluginDex(String dexPath, String receptorActivity, boolean launchBySystem) {
        if (!文件操作.复制文件(dexPath, new File(ctx.getExternalFilesDir("plugin"), "plugin.dex").getAbsolutePath())) {
            return false;
        }
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(new ComponentName(targetPkg, receptorActivity));
        if (launchBySystem) {
            AliveModule.instance().pullSpecialActivity(intent);
        } else {
            ctx.startActivity(intent);
        }
        return true;
    }

    /**
     * 复制流数据
     */
    private void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        output.flush();
    }

    private String getNativeLibraryDir() {
        try {
            PackageManager packageManager = ctx.getPackageManager();
            // 通过包名获取应用信息
            ApplicationInfo applicationInfo =
                packageManager.getApplicationInfo(targetPkg, 0);
            // 获取lib库的路径
            return applicationInfo.nativeLibraryDir;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
