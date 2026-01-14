package com.cfks.fuckzyb.justcrackit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import jiesheng.系统操作;
import org.json.JSONObject;

public class SerialChecker {

    private Activity activity;
    private ProgressDialog progressDialog;
    private String serial = "";

    public SerialChecker(Activity activity) {
        this.activity = activity;
    }

    public void checkSerial() {
        // 显示不可取消的进度对话框
        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("正在验证设备...");
        progressDialog.setCancelable(false);
		progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        // 在后台执行网络请求
        new CheckSerialTask().execute();
    }

    private class CheckSerialTask extends AsyncTask<Void, Void, Boolean> {

        private String errorMessage;

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // 获取序列号并计算MD5
                serial = 系统操作.取设备唯一标识符();

                // 构建请求URL
                String urlString = "https://justcrackit.cfknb.vip/api/checkSerial.php?serial=" + serial;

                // 发送GET请求
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                // 读取响应
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // 解析JSON响应
                JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse.getBoolean("check");

            } catch (Exception e) {
                e.printStackTrace();
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result == null) {
                // 网络请求失败
                Toast.makeText(activity, "网络请求失败: " + errorMessage, Toast.LENGTH_LONG).show();
                return;
            }

			// 关闭进度对话框
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (!result) {
                // check为false，显示不可关闭的对话框
                showBlockingDialog();
            }
            // check为true，什么都不做
        }
    }

    private void showBlockingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("设备未授权");
        builder.setMessage("您的设备不在白名单中，请联系管理员添加您的标识符：" + serial);
        builder.setCancelable(false); // 不可关闭

        // 添加复制按钮
        builder.setPositiveButton("复制标识符", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // 确保返回键也不能关闭
        dialog.setCanceledOnTouchOutside(false);
		dialog.getButton(AlertDialog.BUTTON_POSITIVE)
			.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					copyToClipboard(serial);
					Toast.makeText(activity, "标识符已复制到剪贴板", Toast.LENGTH_SHORT).show();
				}
			});
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("设备标识符", text);
        clipboard.setPrimaryClip(clip);
    }
}
