package com.example.limbo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.max2idea.android.limbo.VmUtils;
import com.max2idea.android.limbo.main.LimboApplication;

import org.libsdl.app.SDLActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExampleActivity extends AppCompatActivity {
    private EditText extraArgs;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        extraArgs = findViewById(R.id.args);
        extraArgs.setText(R.string.deafult_args);
        startButton = findViewById(R.id.start);
        startButton.setOnClickListener(v -> startVM());

        XXPermissions.with(this)
                // 申请多个权限
                .permission(PermissionLists.getManageExternalStoragePermission())
                // 设置不触发错误检测机制（局部设置）
                //.unchecked()
                .request((grantedList, deniedList) -> {
                    boolean allGranted = deniedList.isEmpty();
                    if (!allGranted) {
                        // 判断请求失败的权限是否被用户勾选了不再询问的选项
                        boolean doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(this, deniedList);
                        // 在这里处理权限请求失败的逻辑
                        // ......
                        finish();
                    }
                    // 在这里处理权限请求成功的逻辑
                    // ......
                });
    }

    private void startVM() {
        try {
            LimboApplication.initialize();
            String args = extraArgs.getText().toString().trim();

            // ✅ 正确解析 QEMU 参数
            String[] params = parseCorrectQemuArgs(args);

            VmUtils.init();

            Intent intent = new Intent(this, SDLActivity.class);
            startActivity(intent);

            new Thread(() -> {
                try {
                    Thread.sleep(300);
                    VmUtils.start(ExampleActivity.this, params);
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();

        } catch (Exception e) {
            Toast.makeText(this, "错误: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ==============================================
    // ✅ 这是 QEMU 唯一正确的参数解析方法
    // ==============================================
    @NonNull
    public static String[] parseCorrectQemuArgs(String command) {
        List<String> tokens = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");
        Matcher matcher = pattern.matcher(command);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                tokens.add(matcher.group(2));
            } else if (matcher.group(3) != null) {
                tokens.add(matcher.group(3));
            }
        }

        return tokens.toArray(new String[0]);
    }
}