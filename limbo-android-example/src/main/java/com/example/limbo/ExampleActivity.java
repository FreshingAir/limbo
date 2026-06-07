package com.example.limbo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.max2idea.android.limbo.VmUtils;

import org.jetbrains.annotations.Contract;
import org.libsdl.app.SDLActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExampleActivity extends AppCompatActivity {
    EditText extraArgs;
    Button startButton;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        extraArgs = findViewById(R.id.args);
        startButton = findViewById(R.id.start);
        startButton.setOnClickListener(view -> {
            VmUtils.init();
            VmUtils.start(ExampleActivity.this, parseParams(String.valueOf(extraArgs.getText())));
            Intent intent = new Intent(this, SDLActivity.class);
            startActivity(intent);
        });
    }

    @NonNull
    @Contract("null -> new")
    public static String[] parseParams(String commandText) {
        if (commandText == null || commandText.trim().isEmpty()) {
            return new String[0];
        }

        // 正则：匹配 --key=value 或 --key 或 value（带空格也能识别）
        String regex = "\"(\\\\\"|[^\"])*\"|'(\\\\'|[^'])*'|\\S+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(commandText);

        List<String> paramsList = new ArrayList<>();
        while (matcher.find()) {
            String param = matcher.group();

            // 去掉首尾引号（如果有）
            if (param.startsWith("\"") && param.endsWith("\"")) {
                param = param.substring(1, param.length() - 1);
            } else if (param.startsWith("'") && param.endsWith("'")) {
                param = param.substring(1, param.length() - 1);
            }

            paramsList.add(param);
        }

        // 转成数组返回
        return paramsList.toArray(new String[0]);
    }
}
