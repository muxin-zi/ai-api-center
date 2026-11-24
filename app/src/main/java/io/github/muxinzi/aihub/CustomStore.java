package io.github.muxinzi.aihub;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户自己添加的平台。
 *
 * 只存名称、两个网址、接口地址和图标路径，不含任何密钥 —— 密钥还是走 KeyVault。
 * 这些平台 id 以 c 开头，和内置的区分开。
 */
public class CustomStore {

    private static final String PREF = "aihub_custom";
    private static final String KEY = "list";

    public static List<Provider> load(Context ctx) {
        List<Provider> out = new ArrayList<>();
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String s = sp.getString(KEY, "");
        if (s == null || s.isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String id = o.optString("id", "");
                if (id.isEmpty()) continue;

                Provider p = new Provider(
                        id,
                        o.optString("name", "未命名"),
                        o.optString("short", "?"),
                        o.optInt("color", 0xFF6C8CFF),
                        o.optString("baseUrl", ""),
                        o.optString("apiKey", ""),
                        o.optString("billing", ""),
                        o.optString("console", ""),
                        o.optString("docs", ""),
                        false,
                        o.optString("note", "自定义"));
                out.add(p);

                String icon = o.optString("icon", "");
                if (!icon.isEmpty()) Provider.putIcon(p.id, icon);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void save(Context ctx, List<Provider> customs) {
        JSONArray arr = new JSONArray();
        for (Provider p : customs) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("name", p.name);
                o.put("short", p.shortName);
                o.put("color", p.color);
                o.put("baseUrl", nullToEmpty(p.baseUrl));
                o.put("apiKey", nullToEmpty(p.apiKey));
                o.put("billing", nullToEmpty(p.billing));
                o.put("console", nullToEmpty(p.console));
                o.put("docs", nullToEmpty(p.docs));
                o.put("note", nullToEmpty(p.note));
                String icon = Provider.iconPath(p.id);
                o.put("icon", icon == null ? "" : icon);
                arr.put(o);
            } catch (Exception ignored) {
            }
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(KEY, arr.toString()).apply();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
