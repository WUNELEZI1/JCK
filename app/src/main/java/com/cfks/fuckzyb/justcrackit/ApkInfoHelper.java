package com.cfks.fuckzyb.justcrackit;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class ApkInfoHelper {
    private PackageManager packageManager;

    public ApkInfoHelper(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    /**
     * APK信息实体类
     */
    public static class ApkInfo {
        private String packageName;
        private String appName;
        private String versionName;
        private int versionCode;
        private String md5;
        private Drawable icon;
        private long fileSize;
        private String filePath;

        // getter 和 setter 方法
        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }

        public String getAppName() { return appName; }
        public void setAppName(String appName) { this.appName = appName; }

        public String getVersionName() { return versionName; }
        public void setVersionName(String versionName) { this.versionName = versionName; }

        public int getVersionCode() { return versionCode; }
        public void setVersionCode(int versionCode) { this.versionCode = versionCode; }

        public String getMd5() { return md5; }
        public void setMd5(String md5) { this.md5 = md5; }

        public Drawable getIcon() { return icon; }
        public void setIcon(Drawable icon) { this.icon = icon; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        @Override
        public String toString() {
            return "ApkInfo{" +
				"packageName='" + packageName + '\'' +
				", appName='" + appName + '\'' +
				", versionName='" + versionName + '\'' +
				", versionCode=" + versionCode +
				", md5='" + md5 + '\'' +
				", fileSize=" + fileSize +
				", filePath='" + filePath + '\'' +
				'}';
        }
    }

    /**
     * 获取APK文件的基本信息
     */
    public ApkInfo getApkInfo(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            return null;
        }

        try {
            // 获取PackageInfo
            PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
            if (packageInfo == null) {
                return null;
            }

            ApkInfo apkInfo = new ApkInfo();

            // 设置包名和版本信息
            apkInfo.setPackageName(packageInfo.packageName);
            apkInfo.setVersionName(packageInfo.versionName);
            apkInfo.setVersionCode(packageInfo.versionCode);

            // 获取应用名称和图标
            packageInfo.applicationInfo.sourceDir = apkFile.getAbsolutePath();
            packageInfo.applicationInfo.publicSourceDir = apkFile.getAbsolutePath();

            String appName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
            Drawable icon = packageManager.getApplicationIcon(packageInfo.applicationInfo);

            apkInfo.setAppName(appName);
            apkInfo.setIcon(icon);

            // 计算MD5
            String md5 = calculateFileMD5(apkFile);
            apkInfo.setMd5(md5);

            // 设置文件信息
            apkInfo.setFileSize(apkFile.length());
            apkInfo.setFilePath(apkFile.getAbsolutePath());

            return apkInfo;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 计算文件的MD5值
     */
    private String calculateFileMD5(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            InputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read;

            while ((read = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }

            inputStream.close();

            byte[] md5Bytes = digest.digest();
            StringBuilder hexString = new StringBuilder();

            for (byte md5Byte : md5Bytes) {
                String hex = Integer.toHexString(0xff & md5Byte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 仅获取APK包名
     */
    public String getApkPackageName(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            return null;
        }

        try {
            PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            return packageInfo != null ? packageInfo.packageName : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 仅获取APK文件MD5
     */
    public String getApkMD5(File apkFile) {
        return calculateFileMD5(apkFile);
    }

    /**
     * 仅获取APK应用名称
     */
    public String getApkAppName(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            return null;
        }

        try {
            PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
            if (packageInfo != null) {
                packageInfo.applicationInfo.sourceDir = apkFile.getAbsolutePath();
                packageInfo.applicationInfo.publicSourceDir = apkFile.getAbsolutePath();
                return packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 批量获取APK信息
     */
    public List<ApkInfo> getBatchApkInfo(List<File> apkFiles) {
        List<ApkInfo> apkInfoList = new ArrayList<>();
        if (apkFiles == null || apkFiles.isEmpty()) {
            return apkInfoList;
        }

        for (File apkFile : apkFiles) {
            ApkInfo apkInfo = getApkInfo(apkFile);
            if (apkInfo != null) {
                apkInfoList.add(apkInfo);
            }
        }

        return apkInfoList;
    }

    /**
     * 验证APK文件是否有效
     */
    public boolean isApkFileValid(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        // 检查文件扩展名
        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".apk")) {
            return false;
        }

        // 尝试解析APK信息
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(file.getAbsolutePath(), 0);
        return packageInfo != null && packageInfo.packageName != null;
    }
}
