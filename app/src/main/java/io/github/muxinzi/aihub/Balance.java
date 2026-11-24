package io.github.muxinzi.aihub;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 余额自动读取。
 *
 * 只支持很小一部分平台 —— 大部分厂商的余额挂在云平台账户上，要 AK/SK 签名，普通用户
 * 根本不会去建。所以这里的原则是：能读就读（走 Bearer key），读不了就让用户点「费用页」。
 *
 * 请求在子线程，回调切回主线程。结果按平台缓存进 SharedPreferences，下次打开先显示旧值，
 * 再后台刷新，避免每次进详情页都卡一下。
 */
public class Balance {

    private static final String PREF = "aihub_balance";
    private static final int TIMEOUT_MS = 12000;
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler UI = new Handler(Looper.getMainLooper());

    /** 一次查询的结果 */
    public static class R {
        public boolean ok;
        /** 展示用金额，如 ¥112.95 */
        public String amount = "";
        /** 副标题明细，如 赠金 ¥20.00 · 充值 ¥92.95 */
        public String detail = "";
        public String error = "";
        public long ts;

        public static R fail(String msg) {
            R r = new R();
            r.ok = false;
            r.error = msg;
            r.ts = System.currentTimeMillis();
            return r;
        }
    }

    public interface Cb {
        void onResult(R r);
    }

    /* ---------------- 缓存 ---------------- */

    public static R cached(Context ctx, String pid) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String s = sp.getString(pid, null);
        if (s == null || s.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(s);
            R r = new R();
            r.ok = o.optBoolean("ok", false);
            r.amount = o.optString("amount", "");
            r.detail = o.optString("detail", "");
            r.error = o.optString("error", "");
            r.ts = o.optLong("ts", 0L);
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private static void save(Context ctx, String pid, R r) {
        try {
            JSONObject o = new JSONObject();
            o.put("ok", r.ok);
            o.put("amount", r.amount);
            o.put("detail", r.detail);
            o.put("error", r.error);
            o.put("ts", r.ts);
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit().putString(pid, o.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /* ---------------- 请求 ---------------- */

    public static void fetch(final Context ctx, final Provider p, final String key, final Cb cb) {
        final Provider.BalanceSpec spec = Provider.balanceSpec(p.id);
        if (spec == null) {
            if (cb != null) cb.onResult(R.fail("这家没有公开的余额接口"));
            return;
        }
        if (key == null || key.trim().isEmpty()) {
            if (cb != null) cb.onResult(R.fail("先在「API KEY 备忘录」里存一条密钥"));
            return;
        }

        POOL.execute(new Runnable() {
            @Override
            public void run() {
                final R r = request(spec, key.trim());
                r.ts = System.currentTimeMillis();
                // 失败不落盘，别把上次成功的余额覆盖成错误
                if (r.ok) save(ctx, p.id, r);
                if (cb != null) UI.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onResult(r);
                    }
                });
            }
        });
    }

    private static R request(Provider.BalanceSpec spec, String key) {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(spec.url);
            conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Authorization", "Bearer " + key);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "AiHub/2.0 (Android)");

            int code = conn.getResponseCode();
            if (code == 401 || code == 403) {
                return R.fail(spec.needAdminKey
                        ? "这条 key 权限不够，需要管理级密钥"
                        : "密钥无效、已过期或没有查看余额的权限");
            }
            if (code != 200) {
                return R.fail("接口返回 HTTP " + code);
            }

            String body = read(conn.getInputStream());
            return parse(spec, body);
        } catch (java.net.SocketTimeoutException e) {
            return R.fail("连接超时，网络可能不通");
        } catch (java.net.UnknownHostException e) {
            return R.fail("域名解析失败，检查网络");
        } catch (Exception e) {
            return R.fail("读取失败：" + shortMsg(e));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String read(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    /* ---------------- 解析 ---------------- */

    private static R parse(Provider.BalanceSpec spec, String body) {
        try {
            JSONObject o = new JSONObject(body);
            R r = new R();
            r.ok = true;

            if ("ds".equals(spec.kind)) {
                JSONArray arr = o.optJSONArray("balance_infos");
                if (arr == null || arr.length() == 0) return R.fail("返回内容里没有余额字段");
                JSONObject first = arr.optJSONObject(0);
                if (first == null) return R.fail("返回内容格式异常");
                String cur = first.optString("currency", "CNY");
                String sym = "USD".equalsIgnoreCase(cur) ? "$" : "¥";
                double total = num(first.optString("total_balance", "0"));
                double gift = num(first.optString("granted_balance", "0"));
                double top = num(first.optString("topped_up_balance", "0"));
                r.amount = money(sym, total);
                r.detail = "赠金 " + money(sym, gift) + " · 充值 " + money(sym, top);
                boolean avail = o.optBoolean("is_available", true);
                if (!avail) r.detail = "余额不足，接口已停用 · " + r.detail;

            } else if ("moonshot".equals(spec.kind)) {
                JSONObject d = o.optJSONObject("data");
                if (d == null) return R.fail("返回内容里没有 data 字段");
                double avail = d.optDouble("available_balance", Double.NaN);
                if (Double.isNaN(avail)) return R.fail("返回内容里没有余额字段");
                double voucher = d.optDouble("voucher_balance", 0);
                double cash = d.optDouble("cash_balance", 0);
                r.amount = money(spec.symbol, avail);
                r.detail = "代金券 " + money(spec.symbol, voucher)
                        + " · 现金 " + money(spec.symbol, cash);

            } else if ("siliconflow".equals(spec.kind)) {
                JSONObject d = o.optJSONObject("data");
                if (d == null) return R.fail("返回内容里没有 data 字段");
                String total = d.optString("totalBalance", "");
                if (total.isEmpty()) return R.fail("返回内容里没有余额字段");
                r.amount = money(spec.symbol, num(total));
                r.detail = "赠送 " + money(spec.symbol, num(d.optString("balance", "0")))
                        + " · 充值 " + money(spec.symbol, num(d.optString("chargeBalance", "0")));

            } else if ("openrouter".equals(spec.kind)) {
                JSONObject d = o.optJSONObject("data");
                if (d == null) return R.fail("返回内容里没有 data 字段");
                double credits = d.optDouble("total_credits", 0);
                double used = d.optDouble("total_usage", 0);
                r.amount = money(spec.symbol, credits - used);
                r.detail = "已购 " + money(spec.symbol, credits)
                        + " · 已用 " + money(spec.symbol, used);

            } else if ("zhipu".equals(spec.kind)) {
                JSONObject info = o.optJSONObject("balanceInfo");
                if (info == null) info = o.optJSONObject("data");
                if (info == null) info = o;
                double bal = info.optDouble("balance", Double.NaN);
                if (Double.isNaN(bal)) return R.fail("返回内容里没有余额字段");
                double gift = info.optDouble("giftBalance", 0);
                double voucher = info.optDouble("voucherBalance", 0);
                long usedTokens = info.optLong("usedTokens", 0L);
                r.amount = money(spec.symbol, bal);
                r.detail = "赠金 " + money(spec.symbol, gift)
                        + " · 代金券 " + money(spec.symbol, voucher)
                        + (usedTokens > 0 ? " · 已用 " + usedTokens + " tokens" : "");

            } else {
                return R.fail("没有对应的解析器");
            }

            if (r.amount.isEmpty()) return R.fail("金额解析失败");
            return r;
        } catch (Exception e) {
            return R.fail("返回内容不是合法 JSON");
        }
    }

    /* ---------------- 小工具 ---------------- */

    private static double num(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static final DecimalFormat FMT = new DecimalFormat("#,##0.00");

    private static String money(String sym, double v) {
        return sym + FMT.format(v);
    }

    private static String shortMsg(Exception e) {
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        return m.length() > 60 ? m.substring(0, 60) : m;
    }

    /** 3 分钟前 / 2 小时前 —— 给卡片上的更新时间用 */
    public static String ago(long ts) {
        if (ts <= 0) return "还没查过";
        long d = System.currentTimeMillis() - ts;
        if (d < 60_000L) return "刚刚更新";
        if (d < 3_600_000L) return (d / 60_000L) + " 分钟前更新";
        if (d < 86_400_000L) return (d / 3_600_000L) + " 小时前更新";
        return (d / 86_400_000L) + " 天前更新";
    }
}
