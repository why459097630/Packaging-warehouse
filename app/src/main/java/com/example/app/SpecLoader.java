package com.example.app;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class SpecLoader {
    private SpecLoader() {}

    public static Spec load(Context ctx) {
        String text = readAsset(ctx, "generated/spec.json");
        if (text == null) return null;
        try {
            return new Gson().fromJson(text, Spec.class);
        } catch (JsonSyntaxException e) {
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
