package com.cfks.fuckzyb.justcrackit;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class FileDownloader {
    private static final String TAG = "FileDownloader";
    private static final int TIME_OUT = 30 * 1000; // 超时时间
    private static final int BUFFER_SIZE = 1024 * 8; // 缓冲区大小

    // 下载任务缓存
    private static final Map<String, DownloadTask> taskCache = new HashMap<>();

    private Context context;
    private ProgressDialog progressDialog;
    private Handler handler = new Handler();

    public FileDownloader(Context context) {
        this.context = context;
    }

    /**
     * 下载文件
     * @param url 文件URL
     * @param savePath 保存路径（包含文件名）
     * @param showProgress 是否显示进度对话框
     */
    public void download(String url, String savePath, boolean showProgress) {
        download(url, savePath, showProgress, null);
    }

    /**
     * 下载文件
     * @param url 文件URL
     * @param savePath 保存路径（包含文件名）
     * @param showProgress 是否显示进度对话框
     * @param callback 下载回调
     */
    public void download(String url, String savePath, boolean showProgress, DownloadCallback callback) {
        // 如果任务已存在，则不再创建
        if (taskCache.containsKey(url)) {
            return;
        }

        DownloadTask task = new DownloadTask(url, savePath, showProgress, callback);
        taskCache.put(url, task);
        task.execute();
    }

    /**
     * 取消下载
     * @param url 文件URL
     */
    public void cancel(String url) {
        DownloadTask task = taskCache.get(url);
        if (task != null) {
            task.cancel(true);
            taskCache.remove(url);
        }
    }

    /**
     * 下载回调接口
     */
    public interface DownloadCallback {
        void onStart();
        void onProgress(long currentSize, long totalSize, long speed, int percent, long remainingTime);
        void onFinish(File file);
        void onError(Exception e);
    }

    /**
     * 下载任务
     */
    private class DownloadTask extends AsyncTask<Void, Long, File> {
        private String urlStr;
        private String savePath;
        private boolean showProgress;
        private DownloadCallback callback;

        private long totalSize;
        private long downloadedSize;
        private long lastDownloadedSize;
        private long lastTimeStamp;
        private long speed;

        public DownloadTask(String urlStr, String savePath, boolean showProgress, DownloadCallback callback) {
            this.urlStr = urlStr;
            this.savePath = savePath;
            this.showProgress = showProgress;
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            if (callback != null) {
                callback.onStart();
            }

            if (showProgress) {
                progressDialog = new ProgressDialog(context);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage("正在下载...");
                progressDialog.setCancelable(false);
				progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setMax(100);
                progressDialog.show();
            }

            lastTimeStamp = System.currentTimeMillis();
        }

        @Override
        protected File doInBackground(Void... params) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            File file = new File(savePath);

            try {
                // 检查本地是否已存在部分下载的文件
                long existingSize = file.exists() ? file.length() : 0;

                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(TIME_OUT);
                connection.setReadTimeout(TIME_OUT);

                // 支持断点续传
                if (existingSize > 0) {
                    connection.setRequestProperty("Range", "bytes=" + existingSize + "-");
                }

                connection.connect();

                // 获取文件总大小
                if (existingSize == 0) {
                    totalSize = connection.getContentLength();
                    if (totalSize <= 0) {
                        throw new RuntimeException("无法获取文件大小");
                    }
                } else {
                    totalSize = connection.getContentLength() + existingSize;
                }

                // 检查服务器是否支持断点续传
                String ranges = connection.getHeaderField("Accept-Ranges");
                boolean supportResume = "bytes".equals(ranges);

                if (!supportResume && existingSize > 0) {
                    // 如果不支持断点续传且本地有部分文件，则删除重新下载
                    file.delete();
                    existingSize = 0;
                    connection.disconnect();

                    // 重新连接
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(TIME_OUT);
                    connection.setReadTimeout(TIME_OUT);
                    connection.connect();
                    totalSize = connection.getContentLength();
                }

                // 开始下载
                input = connection.getInputStream();

                if (existingSize > 0) {
                    // 断点续传模式
                    output = new FileOutputStream(file, true);
                    downloadedSize = existingSize;
                } else {
                    // 全新下载模式
                    output = new FileOutputStream(file);
                    downloadedSize = 0;
                }

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    if (isCancelled()) {
                        return null;
                    }

                    output.write(buffer, 0, bytesRead);
                    downloadedSize += bytesRead;

                    // 计算下载速度
                    long now = System.currentTimeMillis();
                    long elapsed = now - lastTimeStamp;
                    if (elapsed > 500) { // 每500ms更新一次速度
                        speed = (long) ((downloadedSize - lastDownloadedSize) * 1000.0 / elapsed);
                        lastDownloadedSize = downloadedSize;
                        lastTimeStamp = now;
                    }

                    // 更新进度
                    publishProgress(downloadedSize, totalSize, speed);
                }

                return file;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (output != null) {
                        output.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            long currentSize = values[0];
            long totalSize = values[1];
            long speed = values[2];

            // 计算百分比
            int percent = (int) (currentSize * 100 / totalSize);

            // 计算剩余时间（秒）
            long remainingTime = 0;
            if (speed > 0) {
                remainingTime = (totalSize - currentSize) / speed;
            }

            // 格式化显示信息
            String currentSizeStr = formatFileSize(currentSize);
            String totalSizeStr = formatFileSize(totalSize);
            String speedStr = formatFileSize(speed) + "/s";
            String timeStr = formatTime(remainingTime);

            String message = String.format("%s/%s %s %d%% 剩余 %s", 
										   currentSizeStr, totalSizeStr, speedStr, percent, timeStr);

            if (showProgress && progressDialog != null) {
                progressDialog.setProgress(percent);
                progressDialog.setMessage(message);
            }

            if (callback != null) {
                callback.onProgress(currentSize, totalSize, speed, percent, remainingTime);
            }
        }

        @Override
        protected void onPostExecute(File file) {
            taskCache.remove(urlStr);

            if (showProgress && progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (file != null && file.exists()) {
                if (callback != null) {
                    callback.onFinish(file);
                }
            } else {
                if (callback != null) {
                    callback.onError(new RuntimeException("下载失败"));
                }
            }
        }

        @Override
        protected void onCancelled() {
            taskCache.remove(urlStr);

            if (showProgress && progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (callback != null) {
                callback.onError(new RuntimeException("下载已取消"));
            }
        }
    }

    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化后的字符串
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return new DecimalFormat("#.##").format(size / 1024.0) + "KB";
        } else if (size < 1024 * 1024 * 1024) {
            return new DecimalFormat("#.##").format(size / (1024.0 * 1024)) + "MB";
        } else {
            return new DecimalFormat("#.##").format(size / (1024.0 * 1024 * 1024)) + "GB";
        }
    }

    /**
     * 格式化时间
     * @param seconds 秒数
     * @return 格式化后的字符串
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分" + (seconds % 60) + "秒";
        } else {
            return (seconds / 3600) + "时" + ((seconds % 3600) / 60) + "分" + (seconds % 60) + "秒";
        }
    }
}
