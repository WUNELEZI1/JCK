package com.cfks.fuckzyb.justcrackit;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.cfks.fuckzyb.justcrackit.nanohttpd.FileServer;
import com.tech.pinduoduo_backdoor.AliveModule;
import com.tech.pinduoduo_backdoor.FileProviderV2;
import com.tech.pinduoduo_backdoor.GlobalInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Random;
import jiesheng.加解密操作;
import jiesheng.文件操作;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
	private long firstBackTime;
    private FileServer mFileServer;
    private InjectNativeLibraryTool mInjectNativeLibraryTool;
    private FileDownloader mFileDownloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		GlobalInfo.context = this;
		findViewById(R.id.readBuildProp).setOnClickListener(this);
		findViewById(R.id.openIntent).setOnClickListener(this);
		findViewById(R.id.installApk).setOnClickListener(this);
		findViewById(R.id.openMtkEm).setOnClickListener(this);
        findViewById(R.id.openAppStoreDebugTool).setOnClickListener(this);
        findViewById(R.id.injectNativeLibrary).setOnClickListener(this);
        findViewById(R.id.runSystemShellCommand).setOnClickListener(this);

        //本地文件服务器
        refreshFileServer();
        //初始化其他工具
        mInjectNativeLibraryTool = new InjectNativeLibraryTool(this, "com.zuoyebang.iot.pad.appstore");
        mFileDownloader = new FileDownloader(this);
    }

    private void refreshFileServer() {
        try {
            if (mFileServer != null) {
                mFileServer.stop();
                mFileServer = null;
            }
            mFileServer = new FileServer(文件操作.取子文件列表(getExternalCacheDir().getAbsolutePath()));
            mFileServer.start();
        } catch (Exception e) {
            alert("异常", e.toString());
        }
    }

	private void alert(final String title, final String text) {
		runOnUiThread(new Runnable(){
				@Override
				public void run() {
					AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
						.setTitle(title)
						.setMessage(text)
						.setPositiveButton(android.R.string.ok, null)
						.create();
					dialog.show();
				}
			});
	}

	private void toast(final String content) {
		runOnUiThread(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(getApplication(), content, Toast.LENGTH_SHORT).show();
				}
			});
	}

	@Override
	public void onBackPressed() {
		if (System.currentTimeMillis() - firstBackTime > 2000) {
			toast("再按一次返回键退出程序");
			firstBackTime = System.currentTimeMillis();
			return;
		}

		super.onBackPressed();
	}

	@Override
	public void onClick(View v) {
		try {
			switch (v.getId()) {
				case R.id.readBuildProp:
					Uri uri = Uri.parse(FileProviderV2.instance().obtainNewestIntent(GlobalInfo.SCENE).getTargetUri() + "/system/build.prop");
					String content = readFile(getContentResolver().openInputStream(uri));
					alert("build.prop", content);
					break;

				case R.id.openIntent:
					final EditText editText = new EditText(MainActivity.this);
					editText.setHint("intent:#Intent;[attributes];end");

					AlertDialog inputDialog = new AlertDialog.Builder(MainActivity.this)
						.setTitle("请输入要打开的Intent Uri")
						.setView(editText)
						.setPositiveButton(android.R.string.ok, 
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								try {
									String input = editText.getText().toString();
									AliveModule.instance().pullSpecialActivity(Intent.parseUri(input, Intent.URI_INTENT_SCHEME));
								} catch (Exception e) {
									alert("异常", e.toString());
								}
							}
						})
						.setNegativeButton(android.R.string.cancel, null) // 添加取消按钮
						.create();
					inputDialog.show();
					break;

				case R.id.installApk:
					final EditText editText1 = new EditText(MainActivity.this);
					editText1.setHint("https://example.com/myApp.apk");
					AlertDialog inputDialog1 = new AlertDialog.Builder(MainActivity.this)
						.setTitle("请输入要安装的apk直链或本地文件路径")
						.setView(editText1)
						.setPositiveButton("确定", 
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								try {
									final String input = editText1.getText().toString();
									if (input.startsWith("http://") || input.startsWith("https://")) {
										installApkByZybAppStore(input, true);
									} else if (new File(input).exists()) {
                                        installApkByZybAppStore(input, false);
									} else {
										toast("请输入正确的URL或本地apk文件路径");
									}
								} catch (Exception e) {
									alert("异常", e.toString());
								}
							}
						})
						.setNegativeButton(android.R.string.cancel, null) // 添加取消按钮
						.create();
					inputDialog1.show();
					break;
				case R.id.openMtkEm:
					AliveModule.instance().pullSpecialActivity(Intent.parseUri("intent:#Intent;component=com.mediatek.engineermode/.EngineerMode;end", Intent.URI_INTENT_SCHEME));
					break;
                case R.id.openAppStoreDebugTool:
                    AliveModule.instance().pullSpecialActivity(Intent.parseUri("intent:#Intent;component=com.zuoyebang.iot.pad.appstore/com.zuoyebang.iot.pad.lib.tools.tool.DebugToolsActivity;end", Intent.URI_INTENT_SCHEME));
                    break;
                case R.id.injectNativeLibrary:
                    AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("警告")
                        .setMessage("此操作可能导致软件无法打开/系统运行错误，点击确定即代表您同意自行承担您操作所带来的风险")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dia, int which) {
                                final String newSoPath = new File(getExternalFilesDir("download"), "liblogan.so").getAbsolutePath();
                                mFileDownloader.download("https://justcrackit.cfknb.vip/files/so/appstore/liblogan.so", newSoPath, true, new FileDownloader.DownloadCallback(){
                                        public void onStart() {
                                            toast("下载so中");
                                        };
                                        public void onProgress(long currentSize, long totalSize, long speed, int percent, long remainingTime) {};
                                        public void onFinish(File file) {
                                            try {
                                                toast("so下载完成");
                                                mInjectNativeLibraryTool.injectNativeLibrary("liblogan.so", newSoPath);
                                                toast("注入完成");
                                            } catch (Exception e) {
                                                alert("异常", e.toString());
                                            }
                                        };
                                        public void onError(Exception e) {
                                            alert("异常", e.toString());
                                        };
                                    });
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                    dialog.show();
                    break;
                case R.id.runSystemShellCommand:
                    final EditText editText2 = new EditText(MainActivity.this);
                    editText2.setHint("id");
                    AlertDialog inputDialog2 = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("请输入要执行的shell命令")
                        .setView(editText2)
                        .setPositiveButton(android.R.string.ok, 
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String input = editText2.getText().toString();
                                final String pluginPath = new File(getExternalFilesDir("download"), "RunSystemShell.dex").getAbsolutePath();
                                mFileDownloader.download("https://justcrackit.cfknb.vip/files/plugin/RunSystemShell.dex", pluginPath, true, new FileDownloader.DownloadCallback(){
                                        public void onStart() {
                                            toast("下载插件中");
                                        };
                                        public void onProgress(long currentSize, long totalSize, long speed, int percent, long remainingTime) {};
                                        public void onFinish(File file) {
                                            toast("插件下载完成");
                                            //写入执行的命令
                                            文件操作.写出文本文件(new File(getExternalFilesDir(null), "runSystemShell.txt").getAbsolutePath(), input);
                                            mInjectNativeLibraryTool.runPluginDex(pluginPath, "com.zuoyebang.iot.pad.appstore.HomeActivity", false);
                                            toast("运行完成");
                                        };
                                        public void onError(Exception e) {
                                            alert("异常", e.toString());
                                        };
                                    });
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null) // 添加取消按钮
                        .create();
					inputDialog2.show();
                    break;
			}
		} catch (Exception e) {
			alert("异常", e.toString());
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 添加一个 id 为 0，顺序为 0 的菜单
        menu.add(0, 0, 0, "清空缓存");
        // 添加一个 id 为 1，顺序为 1 的菜单
        menu.add(0, 1, 1, "关于");
        return true;
    }

	private static void clearDirectory(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				File temp = new File(dir, children[i]);
				if (temp.isDirectory()) {
					clearDirectory(temp);
				} else {
					temp.delete();
				}
			}
		} else if (dir != null && dir.isFile()) {
			dir.delete(); // 如果 dir 直接是一个文件，则删除它
		}
	}

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        // 这里的 itemId 就是上面add方法的第二个参数
        switch (item.getItemId()) {
            case 0:
                clearDirectory(getExternalFilesDir(null));
				clearDirectory(getExternalCacheDir());
                refreshFileServer();
				toast("清理缓存完成");
                break;
            case 1:
                alert("关于", "By caofangkuai\n软件禁止任何未经允许的外传以及倒卖");
                break;
        }
        return true;
    }

	private String readFile(InputStream inputStream) {
		try {
			Reader reader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(reader);
			StringBuilder result = new StringBuilder();
			String temp;
			while ((temp = bufferedReader.readLine()) != null) {
				result.append(temp);
			}
			return result.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private void installApkByZybAppStore(final String apk, boolean isUrl) {
		//解析apk
        if (isUrl) {
            final File apkCache = new File(getExternalCacheDir(), "apkcache-" + System.currentTimeMillis() + ".apk");
            mFileDownloader.download(apk, apkCache.getAbsolutePath(), true, new FileDownloader.DownloadCallback(){
                    public void onStart() {
                        toast("下载apk中");
                    };
                    public void onProgress(long currentSize, long totalSize, long speed, int percent, long remainingTime) {};
                    public void onFinish(File file) {
                        try {
                            toast("apk下载完成");
                            sendAppStoreInstallBroadCast(apk, apkCache);
                        } catch (Exception e) {
                            alert("异常", e.toString());
                        }
                    };
                    public void onError(Exception e) {
                        alert("异常", e.toString());
                    };
                });
        } else {
            try {
                sendAppStoreInstallBroadCast("http://127.0.0.1:" + FileServer.DEFAULT_SERVER_PORT + apk, new File(apk));
            } catch (Exception e) {
                alert("异常", e.toString());
            }
        }
	}

    private void sendAppStoreInstallBroadCast(String apkUrl, File apkCache) throws JSONException {
        ApkInfoHelper.ApkInfo mApkInfo = new ApkInfoHelper(getPackageManager()).getApkInfo(apkCache);
        String apkMd5 = mApkInfo.getMd5();
        String apkPackageName = mApkInfo.getPackageName();
        String apkName = mApkInfo.getAppName();
        //删除缓存的apk
        //apkCache.delete();
        refreshFileServer();
        //安装
        final String pkg_name = "com.zuoyebang.iot.pad.appstore";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append(random.nextInt(9) + 1);
        for (int i = 0; i < 17; i++) {
            sb.append(random.nextInt(10));
        }
        final String crackSign = "Crack By caofangkuai";
        JSONObject data = new JSONObject();
        data.put("advertisement", 0)
            .put("advertisementLabel", "无")
            .put("age", 0)
            .put("ageLabel", "")
            .put("apkMd5", apkMd5)
            .put("apkName", apkPackageName)
            .put("apkSize", 32767)
            .put("apkSizeStr", "32767M")
            .put("apkUrl", apkUrl)
            .put("apkVersion", "32767")
            .put("appIdThird", 32767)
            .put("browseWeb", 0)
            .put("browseWebLabel", "无")
            .put("changeLog", crackSign)
            .put("containPayContent", 1)
            .put("developer", crackSign)
            .put("enName", "")
            .put("entertainment", 0)
            .put("entertainmentLabel", "无")
            .put("extraThird", 加解密操作.Base64编码(crackSign))
            .put("from", 1)
            .put("icon", "https://tencentcdna.production.link3.cc/profile_images/1753283565750")
            .put("icpNumber", crackSign)
            .put("id", -(random.nextInt(3000) + 1))
            .put("isCtlWhite", 0)
            .put("isGreenApp", 1)
            .put("isMonitored", false)
            .put("isSensitive", 0)
            .put("name", apkName)
            .put("onShelf", 1)
            .put("payContentLabel", "")
            .put("permissions", JSONObject.NULL)
            .put("previewPics", JSONObject.NULL)
            .put("privacyLink", "https://justcrackit.cfknb.vip")
            .put("remark", crackSign)
            .put("remoteInstallMsg", "安装 " + apkName + " 中...（" + crackSign + "）")
            .put("risk", 0)
            .put("statusInPad", 0)
            .put("summary", crackSign)
            .put("supervise", 1)
            .put("tags", JSONObject.NULL)
            .put("type", 1)
            .put("uploadTime", 32767L)
            .put("versionCodeThird", 32767);
        JSONObject IpcRequest = new JSONObject();
        IpcRequest.put("pkg_name", pkg_name)
            .put("msg_id", sb.toString())
            .put("sub_type", 3)
            .put("type", 2)
            .put("data", data);
        Intent intent = new Intent("com.zuoyebang.iot.push");
        intent.setPackage(pkg_name);
        intent.putExtra("response", IpcRequest.toString());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendBroadcast(intent);
        alert("提示", "已尝试发起安装请求，请返回桌面查看");
    }
}
