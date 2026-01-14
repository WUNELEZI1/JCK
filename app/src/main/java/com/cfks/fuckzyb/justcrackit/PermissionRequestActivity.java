package com.cfks.fuckzyb.justcrackit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.cfks.fuckzyb.justcrackit.R;
import com.tech.pinduoduo_backdoor.FileProviderV2;
import com.tech.pinduoduo_backdoor.GlobalInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jiesheng.应用操作;

public class PermissionRequestActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private LinearLayout permissionsContainer;
    private Button doneButton;
    private Map<String, PermissionItem> permissionItems = new LinkedHashMap<>();
    private Map<String, View> permissionViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_request);

        initViews();
        setupPermissions();
        checkPermissions();

        if (Integer.parseInt(Build.VERSION.SECURITY_PATCH.replace("-", "")) >= 20230301) {
            应用操作.信息框(this, "提示", "注: 此设备的安全补丁在2023年3月之后，可能无法使用StartAnyWhere提权，您将只能使用无需提权的部分功能");
        }
		new SerialChecker(this).checkSerial();
    }

    private void initViews() {
        permissionsContainer = findViewById(R.id.permissions_container);
        doneButton = findViewById(R.id.done_button);

        // 设置状态栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        doneButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (allPermissionsGranted()) {
						// 所有权限都已获取，进入主界面
						startActivity(new Intent(PermissionRequestActivity.this, MainActivity.class));
						finish();
					} else {
						// 重新检查并申请未授权的权限
						checkPermissions();
					}
				}
			});
    }

    private void setupPermissions() {
        // 使用Android系统自带的图标
        permissionItems.put(Manifest.permission.READ_EXTERNAL_STORAGE, 
                            new PermissionItem("读取外部存储", "用于访问文件", android.R.drawable.ic_menu_save));
        permissionItems.put(Manifest.permission.READ_PHONE_STATE, 
                            new PermissionItem("读取手机状态和身份", "用于获取设备标识符", android.R.drawable.ic_secure));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			//permissionItems.put(Manifest.permission.MANAGE_EXTERNAL_STORAGE, 
			//new PermissionItem("管理存储设备上的所有文件", "用于访问和修改文件", android.R.drawable.ic_menu_save));
		}

        // 添加自定义权限项
        addCustomPermissionItem();

        // 创建权限项视图
        createPermissionViews();
    }

    private void addCustomPermissionItem() {
        View customPermissionView = getLayoutInflater().inflate(
            R.layout.item_custom_permission, permissionsContainer, false);

        TextView customTitle = customPermissionView.findViewById(R.id.custom_permission_title);
        TextView customDesc = customPermissionView.findViewById(R.id.custom_permission_desc);
        Button customActionBtn = customPermissionView.findViewById(R.id.custom_permission_btn);
        ImageView customIcon = customPermissionView.findViewById(R.id.custom_permission_icon);
        TextView customStatus = customPermissionView.findViewById(R.id.custom_permission_status);

        customTitle.setText("StartAnyWhere权限");
        customDesc.setText("提权漏洞");
        customIcon.setImageResource(android.R.drawable.ic_menu_manage);
        customActionBtn.setText("申请StartAnyWhere权限");

        // 设置自定义权限视图的tag，用于后续状态更新
        customPermissionView.setTag("custom_permission");
        permissionViews.put("custom_permission", customPermissionView);

        customActionBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
// 执行自定义权限申请逻辑
					executeCustomPermissionRequest();
				}
			});

        // 更新自定义权限状态
        updateCustomPermissionStatus(customStatus);

        permissionsContainer.addView(customPermissionView);
    }

    private void createPermissionViews() {
        for (Map.Entry<String, PermissionItem> entry : permissionItems.entrySet()) {
            View permissionView = getLayoutInflater().inflate(
                R.layout.item_permission, permissionsContainer, false);

            ImageView icon = permissionView.findViewById(R.id.permission_icon);
            TextView title = permissionView.findViewById(R.id.permission_title);
            TextView description = permissionView.findViewById(R.id.permission_description);
            TextView status = permissionView.findViewById(R.id.permission_status);

            PermissionItem item = entry.getValue();
            icon.setImageResource(item.getIconRes());
            title.setText(item.getTitle());
            description.setText(item.getDescription());

            // 保存视图引用，用于后续更新状态
            permissionViews.put(entry.getKey(), permissionView);

            permissionsContainer.addView(permissionView);
        }
    }

    private void checkPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissionItems.keySet()) {
			if (permission.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
				if (Environment.isExternalStorageManager()) {
					updatePermissionStatusUI(permission, true);
				} else {
					permissionsToRequest.add(permission);
				}
			} else {
				if (ContextCompat.checkSelfPermission(this, permission) 
					!= PackageManager.PERMISSION_GRANTED) {
					permissionsToRequest.add(permission);
				} else {
					// 更新已授权权限的状态
					updatePermissionStatusUI(permission, true);
				}
			}
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
											  permissionsToRequest.toArray(new String[0]),
											  PERMISSION_REQUEST_CODE);
        } else {
            updateUI();
        }
    }

    private void executeCustomPermissionRequest() {
        // 这里实现您的自定义权限申请逻辑
        // 例如：打开系统设置、请求特殊权限等

        // 自定义权限申请
		boolean result =  FileProviderV2.instance().requestGrantPermission(GlobalInfo.SCENE);
		if (result) {
			markCustomPermissionGranted();
			Toast.makeText(this, "StartAnyWhere权限申请成功", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "StartAnyWhere权限申请失败", Toast.LENGTH_SHORT).show();
		}

    }

    private void markCustomPermissionGranted() {
        // 标记自定义权限已获取
        SharedPreferences prefs = getSharedPreferences("permissions", MODE_PRIVATE);
        prefs.edit().putBoolean("custom_permission_granted", true).apply();
        updateCustomPermissionStatus();
        updateUI();
    }

    private void updateCustomPermissionStatus() {
        View customView = permissionViews.get("custom_permission");
        if (customView != null) {
            TextView status = customView.findViewById(R.id.custom_permission_status);
            updateCustomPermissionStatus(status);
        }
    }

    private void updateCustomPermissionStatus(TextView statusView) {
        if (isCustomPermissionGranted()) {
            statusView.setText("已授权");
            statusView.setTextColor(ContextCompat.getColor(this, R.color.green));
        } else {
            statusView.setText("未授权");
            statusView.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }

    private boolean isCustomPermissionGranted() {
        SharedPreferences prefs = getSharedPreferences("permissions", MODE_PRIVATE);
        return prefs.getBoolean("custom_permission_granted", false);
    }

    private boolean allPermissionsGranted() {
        // 检查系统权限
        for (String permission : permissionItems.keySet()) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        // 检查自定义权限
        return isCustomPermissionGranted();
    }

    private void updateUI() {
        if (allPermissionsGranted()) {
            doneButton.setEnabled(true);
            doneButton.setAlpha(1.0f);
            doneButton.setBackgroundResource(R.drawable.btn_enabled);

            // 添加一点动画效果
            doneButton.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .withEndAction(new Runnable(){
					@Override
					public void run() {
						doneButton.animate();
					}
				})
				.scaleX(1.0f)
				.scaleY(1.0f)
				.setDuration(200);
        } else {
            doneButton.setEnabled(false);
            doneButton.setAlpha(0.5f);
            doneButton.setBackgroundResource(R.drawable.btn_disabled);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                updatePermissionStatusUI(permissions[i], granted);

                if (!granted) {
                    // 如果权限被拒绝，显示说明
                    if (shouldShowRequestPermissionRationale(permissions[i])) {
                        showPermissionRationale(permissions[i]);
                    }
                }
            }
            updateUI();
        }
    }

    private void updatePermissionStatusUI(String permission, boolean granted) {
        View permissionView = permissionViews.get(permission);
        if (permissionView != null) {
            TextView status = permissionView.findViewById(R.id.permission_status);
            if (status != null) {
                if (granted) {
                    status.setText("已授权");
                    status.setTextColor(ContextCompat.getColor(this, R.color.green));
                } else {
                    status.setText("未授权");
                    status.setTextColor(ContextCompat.getColor(this, R.color.red));
                }
            }
        }
    }

    private void showPermissionRationale(String permission) {
        String message = "";
        String title = "";

        PermissionItem item = permissionItems.get(permission);
        if (item != null) {
            title = item.getTitle();
            message = "需要" + item.getDescription() + "，请在设置中授予权限";
        }

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dia, int which) {
                    // 打开应用设置页面
					Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					Uri uri = Uri.fromParts("package", getPackageName(), null);
					intent.setData(uri);
					startActivity(intent);
				}
			})
			.setNegativeButton("取消", null)
            .show();
    }

    // 权限项数据类
    private static class PermissionItem {
        private String title;
        private String description;
        private int iconRes;

        public PermissionItem(String title, String description, int iconRes) {
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public int getIconRes() { return iconRes; }
    }
}
