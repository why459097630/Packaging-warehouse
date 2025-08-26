package com.example.app;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 最小实现：去掉 Gson 依赖，先保证项目可编译。
 * 如果后续确实需要解析 JSON，可以改为使用 org.json 或在 app/build.gradle 中添加 gson 依赖。
 */
public final class SpecLoader {

    private static final String TAG = "SpecLoader";

    private SpecLoader() { }

    /** 从 assets 读取并返回 Spec；当前不解析，返回 null 以保证编译通过。 */
    public static Spec load(Context ctx) {
        try {
            String text = readAsset(ctx, "generated/spec.json");
            if (text == null) return null;

            // TODO: 根据你的 Spec.java 结构实现解析逻辑（可用 org.json）
            // 例如：
            // JSONObject obj = new JSONObject(text);
            // Spec s = new Spec(); // 按需填充字段
            // return s;

            return null;
        } catch (Exception e) {
            Log.e(TAG, "load spec error", e);
            return null;
        }
    }

    private static String readAsset(Context ctx, String path) {
        try (InputStream in = ctx.getAssets().open(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
