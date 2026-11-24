package io.github.muxinzi.aihub;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    /* ---------------- 配色 ---------------- */
    private static final int BG        = 0xFF0A0D14;
    private static final int BG2       = 0xFF0E1520;
    private static final int CARD      = 0xFF121822;
    private static final int CARD2     = 0xFF141926;
    private static final int STROKE    = 0xFF1E2635;
    private static final int FIELD     = 0xFF0E131C;
    private static final int TXT       = 0xFFE9EDF5;
    private static final int TXT2      = 0xFF9AA6BA;
    private static final int TXT3      = 0xFF6F7A8C;
    /* 主题色：默认蓝紫，用户可在标题栏的色块里换。换完走 recreate() 重建界面。 */
    private static final int[] THEME_A = {
            0xFF6C8CFF, 0xFF22D3C5, 0xFFFF9F45, 0xFFF472B6, 0xFF34D399, 0xFFFF6B6B};
    private static final int[] THEME_B = {
            0xFFA855F7, 0xFF3B82F6, 0xFFF2545B, 0xFFA78BFA, 0xFF0EA5E9, 0xFFFFA94D};
    private static final String[] THEME_NAMES = {
            "蓝紫", "青碧", "橙金", "粉紫", "森绿", "朱红"};
    private int acc1 = THEME_A[0];
    private int acc2 = THEME_B[0];
    private int themeIndex = 0;
    private static final int GREEN     = 0xFF3DDC97;

    private static final String UA_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    /* ---------------- 视图 ---------------- */
    private FrameLayout root;
    private ScrollView homeScroll;
    private LinearLayout listBox;
    private LinearLayout statsBox;
    private EditText search;
    private TextView savedChip;

    private ScrollView keysPage;
    private LinearLayout keysList;
    private TextView keysCount;
    private boolean keysOpen = false;

    /* 本地详情页 */
    private LinearLayout detailPage;
    private ScrollView detailScroll;
    private LinearLayout detailBox;
    private TextView detailName;
    private TextView detailSub;
    private boolean detailOpen = false;

    /* 网页页 */
    private FrameLayout webPage;
    private WebView web;
    private TextView webTitle;
    private TextView uaChip;
    private ProgressBar bar;
    private boolean webOpen = false;
    private ValueCallback<Uri[]> filePathCallback;

    /* 选平台页 */
    private LinearLayout pickPage;
    private LinearLayout pickList;
    private ScrollView pickScroll;
    private TextView pickDone;
    private TextView pickSkip;
    private boolean pickOpen = false;
    private boolean firstRun = false;
    private final List<String> pickDraft = new ArrayList<>();

    /* ---------------- 状态 ---------------- */
    private KeyVault vault;
    private List<Provider> all;
    private List<Provider> customs = new ArrayList<>();
    private Provider current;
    private boolean desktopUa = true;
    private long lastBack = 0L;
    private final java.util.Set<String> fetching = new java.util.HashSet<>();
    /** 用户选中的平台 id，按选择顺序 */
    private final List<String> picked = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        applyImmersive();

        vault = new KeyVault(this);
        customs = CustomStore.load(this);
        all = new ArrayList<>(Provider.all());
        all.addAll(customs);

        SharedPreferences pref = getSharedPreferences("aihub_pref", Context.MODE_PRIVATE);
        desktopUa = pref.getBoolean("desktop", true);
        themeIndex = pref.getInt("theme", 0);
        if (themeIndex < 0 || themeIndex >= THEME_A.length) themeIndex = 0;
        acc1 = THEME_A[themeIndex];
        acc2 = THEME_B[themeIndex];
        loadPicked();

        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.addView(buildHome(), new FrameLayout.LayoutParams(MATCH, MATCH));
        root.addView(buildKeysPage(), new FrameLayout.LayoutParams(MATCH, MATCH));
        root.addView(buildDetailPage(), new FrameLayout.LayoutParams(MATCH, MATCH));
        root.addView(buildWebPage(), new FrameLayout.LayoutParams(MATCH, MATCH));
        /* 选平台页最后加，盖在最上层 */
        root.addView(buildPickPage(), new FrameLayout.LayoutParams(MATCH, MATCH));
        keysPage.setVisibility(View.GONE);
        detailPage.setVisibility(View.GONE);
        webPage.setVisibility(View.GONE);
        pickPage.setVisibility(View.GONE);
        setContentView(root);

        render();

        /* 第一次进来先选平台 */
        firstRun = !pref.getBoolean("onboarded", false);
        if (firstRun) openPick();
    }

    /* ==================== 主页 ==================== */

    private View buildHome() {
        homeScroll = new ScrollView(this);
        homeScroll.setFillViewport(true);
        homeScroll.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{BG, BG2}));
        homeScroll.setVerticalScrollBarEnabled(false);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int ph = dp(18);
        col.setPadding(ph, dp(26), ph, dp(30));

        /* --- 标题 --- */
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = text("AI API 中心", 25f, TXT, true);
        title.setLetterSpacing(0.02f);
        titleBox.addView(title);

        TextView sub = text("余额 · 密钥 · 控制台", 11.5f, TXT3, false);
        sub.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(WRAP, WRAP);
        slp.topMargin = dp(5);
        titleBox.addView(sub, slp);

        head.addView(titleBox, new LinearLayout.LayoutParams(0, WRAP, 1f));

        /* 配色切换：现实当前主题的两个色 */
        TextView themeBtn = text("", 10f, Color.WHITE, true);
        themeBtn.setBackground(ripple(gradientRound(acc1, acc2, 14), 0x33FFFFFF));
        themeBtn.setOnClickListener(v -> showThemeDialog());
        LinearLayout.LayoutParams tblp = new LinearLayout.LayoutParams(dp(26), dp(26));
        tblp.rightMargin = dp(9);
        head.addView(themeBtn, tblp);

        savedChip = text("密钥本 · 0", 10.5f, GREEN, true);
        savedChip.setGravity(Gravity.CENTER);
        savedChip.setBackground(ripple(
                roundRect(withAlpha(GREEN, 0x1F), 9, withAlpha(GREEN, 0x40), 1), 0x33FFFFFF));
        savedChip.setPadding(dp(10), dp(6), dp(10), dp(6));
        savedChip.setOnClickListener(v -> openKeys());
        head.addView(savedChip);

        col.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        /* --- 渐变细线 --- */
        View line = new View(this);
        line.setBackground(gradientRound(acc1, acc2, 2));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(dp(46), dp(3));
        llp.topMargin = dp(16);
        col.addView(line, llp);

        /* --- 搜索 --- */
        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setBackground(roundRect(CARD, 14, STROKE, 1));
        searchBox.setPadding(dp(14), 0, dp(12), 0);

        TextView icon = text("⌕", 17f, TXT3, false);
        icon.setPadding(0, 0, dp(6), 0);
        searchBox.addView(icon);

        search = new EditText(this);
        search.setBackground(null);
        search.setHint("搜索平台或接口地址");
        search.setHintTextColor(0xFF5B6473);
        search.setTextColor(TXT);
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        search.setSingleLine(true);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(0, 0, 0, 0);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { render(); }
            public void afterTextChanged(Editable s) { }
        });
        searchBox.addView(search, new LinearLayout.LayoutParams(0, dp(46), 1f));

        LinearLayout.LayoutParams sblp = new LinearLayout.LayoutParams(MATCH, WRAP);
        sblp.topMargin = dp(16);
        col.addView(searchBox, sblp);

        /* --- 总览三格 --- */
        statsBox = new LinearLayout(this);
        statsBox.setOrientation(LinearLayout.HORIZONTAL);
        statsBox.setGravity(Gravity.CENTER_VERTICAL);
        statsBox.setBackground(roundRect(CARD, 16, STROKE, 1));
        statsBox.setPadding(dp(4), dp(13), dp(4), dp(13));
        LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        stlp.topMargin = dp(11);
        col.addView(statsBox, stlp);

        /* --- 列表容器 --- */
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(MATCH, WRAP);
        blp.topMargin = dp(6);
        col.addView(listBox, blp);

        /* --- 底部说明 --- */
        TextView foot = text("标了「余额」的平台能直接读出金额，其余点「用量 · 账单」进网页看\n"
                        + "登录一次后 Cookie 留在本机，下次不用重新登",
                10.5f, 0xFF4E586B, false);
        foot.setGravity(Gravity.CENTER);
        foot.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(MATCH, WRAP);
        flp.topMargin = dp(22);
        col.addView(foot, flp);

        homeScroll.addView(col, new ScrollView.LayoutParams(MATCH, WRAP));
        return homeScroll;
    }

    private void render() {
        if (listBox == null) return;
        String q = search == null ? "" : search.getText().toString().trim().toLowerCase();
        listBox.removeAllViews();

        /* 我在用的：按用户选择的先后顺序 */
        List<Provider> mine = new ArrayList<>();
        for (String id : picked) {
            Provider p = findById(id);
            if (p != null && match(p, q)) mine.add(p);
        }

        List<Provider> dom = new ArrayList<>();
        List<Provider> ovs = new ArrayList<>();
        for (Provider p : all) {
            if (!match(p, q)) continue;
            if (picked.contains(p.id)) continue;
            if (p.overseas) ovs.add(p); else dom.add(p);
        }

        boolean first = true;
        boolean onlyMine = !picked.isEmpty();

        if (!mine.isEmpty()) {
            listBox.addView(sectionHeader("我在用的", mine.size() + " 家", first, pickChip()));
            first = false;
            for (Provider p : mine) listBox.addView(buildCard(p));
        }
        /* 已经选过平台就不再铺全部，没选的只会干扰 */
        if (!onlyMine) {
            if (!dom.isEmpty()) {
                listBox.addView(sectionHeader("国内直连", dom.size() + " 家", first, pickChip()));
                first = false;
                for (Provider p : dom) listBox.addView(buildCard(p));
            }
            if (!ovs.isEmpty()) {
                listBox.addView(sectionHeader("海外服务", ovs.size() + " 家 · 需相应网络", first, null));
                first = false;
                for (Provider p : ovs) listBox.addView(buildCard(p));
            }
        }
        if (mine.isEmpty() && (onlyMine || (dom.isEmpty() && ovs.isEmpty()))) {
            TextView empty = text(onlyMine && (q == null || q.isEmpty())
                            ? "还没选平台，点右上角「选择」挑几个"
                            : "没找到匹配的平台",
                    13f, TXT3, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, dp(40));
            listBox.addView(empty, new LinearLayout.LayoutParams(MATCH, WRAP));
        }

        if (savedChip != null) savedChip.setText("密钥本 · " + vault.countAll());
        renderStats();
    }

    /** 三格总览：在用平台 / 存的密钥 / 可查余额 */
    private void renderStats() {
        if (statsBox == null) return;
        statsBox.removeAllViews();

        int keyCount = vault.countAll();
        int canRead = 0;
        if (picked.isEmpty()) {
            for (Provider p : all) if (Provider.balanceSpec(p.id) != null) canRead++;
        } else {
            for (String id : picked) {
                if (Provider.balanceSpec(id) != null && vault.countWithKey(id) > 0) canRead++;
            }
        }

        statsBox.addView(statCell(String.valueOf(picked.size()), "在用平台", () -> openPick()),
                new LinearLayout.LayoutParams(0, WRAP, 1f));
        statsBox.addView(vDivider());
        statsBox.addView(statCell(String.valueOf(keyCount), "存的密钥", () -> openKeys()),
                new LinearLayout.LayoutParams(0, WRAP, 1f));
        statsBox.addView(vDivider());
        statsBox.addView(statCell(String.valueOf(canRead), "可查余额", null),
                new LinearLayout.LayoutParams(0, WRAP, 1f));
    }

    private View statCell(String num, String label, Runnable click) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);

        TextView n = text(num, 19f, TXT, true);
        n.setGravity(Gravity.CENTER);
        c.addView(n, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView l = text(label, 9.6f, 0xFF6F7A8C, false);
        l.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(3);
        c.addView(l, lp);

        if (click != null) {
            c.setClickable(true);
            c.setFocusable(true);
            c.setOnClickListener(v -> click.run());
        }
        return c;
    }

    private View vDivider() {
        View v = new View(this);
        v.setBackgroundColor(0xFF202938);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(26));
        lp.leftMargin = dp(2);
        lp.rightMargin = dp(2);
        v.setLayoutParams(lp);
        return v;
    }

    private boolean match(Provider p, String q) {
        if (q == null || q.isEmpty()) return true;
        String hay = (p.name + " " + p.shortName + " " + p.baseUrl + " " + p.id).toLowerCase();
        return hay.contains(q);
    }

    /** 分组标题右侧的「选择」小胶囊 */
    private TextView pickChip() {
        TextView t = chip("选择 ＋", 9.8f, 0xFF9FB0CC,
                withAlpha(acc1, 0x16), withAlpha(acc1, 0x33));
        t.setOnClickListener(v -> openPick());
        return t;
    }

    /* ---------- 自定义平台 ---------- */

    private String pendingIconPath;
    private ImageView customPreview;

    private void showCustomDialog(final Provider editing) {
        pendingIconPath = editing == null ? null : Provider.iconPath(editing.id);

        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(16));
        box.setBackground(roundRect(BG2, 20, STROKE, 1));

        box.addView(text(editing == null ? "自定义平台" : "编辑平台", 16f, TXT, true));
        TextView tip = text("名称和网址自己填，之后用法和内置平台一模一样", 10.2f, TXT3, false);
        tip.setPadding(0, dp(5), 0, dp(14));
        box.addView(tip);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);

        final EditText eName = addField(form, "名称（必填）", editing == null ? "" : editing.name);
        final EditText eBase = addField(form, "接口地址 base_url（可空）",
                editing == null ? "" : editing.baseUrl);
        final EditText eApi = addField(form, "建 Key 页网址",
                editing == null ? "" : editing.apiKey);
        final EditText eBill = addField(form, "用量 · 账单页网址",
                editing == null ? "" : editing.billing);

        /* 图标 */
        TextView il = text("图标（可空，不选就用名称首字）", 9.8f, 0xFF7A869B, true);
        il.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams illp = new LinearLayout.LayoutParams(MATCH, WRAP);
        illp.topMargin = dp(13);
        form.addView(il, illp);

        LinearLayout iconRow = new LinearLayout(this);
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams irlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        irlp.topMargin = dp(7);
        form.addView(iconRow, irlp);

        customPreview = new ImageView(this);
        customPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        customPreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(40), dp(40));
        plp.rightMargin = dp(10);
        iconRow.addView(customPreview, plp);

        if (pendingIconPath != null) {
            Bitmap bm = decodeIcon(pendingIconPath, (int) dp(40));
            if (bm != null) {
                customPreview.setImageBitmap(roundBitmap(bm, (int) dp(11)));
                customPreview.setVisibility(View.VISIBLE);
            }
        }

        TextView pickImg = chip("从相册选图", 10.5f, 0xFFC3CFF0,
                withAlpha(acc1, 0x18), withAlpha(acc1, 0x44));
        pickImg.setPadding(dp(12), dp(8), dp(12), dp(8));
        pickImg.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            try {
                startActivityForResult(i, 1002);
            } catch (Exception e) {
                toast("没有可用的图片选择器");
            }
        });
        iconRow.addView(pickImg);

        sv.addView(form, new ScrollView.LayoutParams(MATCH, WRAP));
        box.addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        /* 底部按钮 */
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(MATCH, WRAP);
        blp.topMargin = dp(16);
        box.addView(btns, blp);

        TextView cancel = text("取消", 13.5f, TXT2, true);
        cancel.setGravity(Gravity.CENTER);
        cancel.setBackground(ripple(roundRect(0xFF161C29, 13, STROKE, 1), 0x18FFFFFF));
        cancel.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        clp.rightMargin = dp(9);
        btns.addView(cancel, clp);

        TextView ok = text("保存", 13.5f, Color.WHITE, true);
        ok.setGravity(Gravity.CENTER);
        ok.setBackground(ripple(gradientRound(acc1, acc2, 13), 0x33FFFFFF));
        ok.setOnClickListener(v -> {
            String name = eName.getText().toString().trim();
            if (name.isEmpty()) {
                toast("名称不能为空");
                return;
            }
            String api = fixUrl(eApi.getText().toString().trim());
            String bill = fixUrl(eBill.getText().toString().trim());
            if (api.isEmpty() && bill.isEmpty()) {
                toast("两个网址至少填一个");
                return;
            }
            saveCustom(editing, name, fixUrl(eBase.getText().toString().trim()),
                    api, bill, pendingIconPath);
            d.dismiss();
        });
        btns.addView(ok, new LinearLayout.LayoutParams(0, dp(46), 1f));

        d.setContentView(box);
        d.show();

        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0x00000000));
            int wid = Math.min(getResources().getDisplayMetrics().widthPixels - dp(56), dp(460));
            w.setLayout(wid, Math.min(getResources().getDisplayMetrics().heightPixels - dp(120),
                    dp(560)));
        }
    }

    private EditText addField(LinearLayout parent, String label, String value) {
        TextView l = text(label, 9.8f, 0xFF7A869B, true);
        l.setLetterSpacing(0.05f);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(MATCH, WRAP);
        llp.topMargin = dp(parent.getChildCount() == 0 ? 2 : 13);
        parent.addView(l, llp);

        EditText e = new EditText(this);
        if (value != null) e.setText(value);
        e.setTextSize(13f);
        e.setTextColor(TXT);
        e.setHintTextColor(0xFF3D4A5E);
        e.setSingleLine(true);
        e.setBackground(roundRect(FIELD, 11, STROKE, 1));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(MATCH, WRAP);
        elp.topMargin = dp(6);
        parent.addView(e, elp);
        return e;
    }

    private String fixUrl(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        return "https://" + s;
    }

    private void saveCustom(Provider editing, String name, String base,
                            String api, String bill, String iconPath) {
        String id = editing == null ? ("c" + System.currentTimeMillis()) : editing.id;
        String shortName = name.length() <= 2 ? name : name.substring(0, 2);
        int color = editing == null ? acc1 : editing.color;

        Provider p = new Provider(id, name, shortName, color,
                base, api, bill, "", "", false, "自定义");

        if (iconPath != null && !iconPath.isEmpty()) Provider.putIcon(id, iconPath);

        if (editing == null) {
            customs.add(p);
            all.add(p);
            if (!pickDraft.contains(id)) pickDraft.add(id);
        } else {
            for (int i = 0; i < customs.size(); i++) {
                if (customs.get(i).id.equals(id)) {
                    customs.set(i, p);
                    break;
                }
            }
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id.equals(id)) {
                    all.set(i, p);
                    break;
                }
            }
        }

        CustomStore.save(this, customs);
        renderPick();
        toast(editing == null ? "已添加，记得点「开始使用」" : "已保存");
    }

    /** 从相册选的图复制进应用私有目录，之后不依赖原图还在不在 */
    private String copyCustomIcon(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;

            java.io.File dir = new java.io.File(getFilesDir(), "icons");
            if (!dir.exists()) dir.mkdirs();
            java.io.File out = new java.io.File(dir, "icon_" + System.currentTimeMillis() + ".png");

            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
            fos.close();
            in.close();
            return out.getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }

    /* ---------- 配色 ---------- */

    private void showThemeDialog() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(20), dp(22), dp(20));
        box.setBackground(roundRect(BG2, 20, STROKE, 1));

        box.addView(text("选个配色", 16f, TXT, true));
        TextView tip = text("换完会立即重载界面", 10.5f, TXT3, false);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        tlp.topMargin = dp(5);
        box.addView(tip, tlp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        rlp.topMargin = dp(20);
        box.addView(row, rlp);

        for (int i = 0; i < THEME_A.length; i++) {
            final int idx = i;
            boolean on = idx == themeIndex;

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);

            View dot = new View(this);
            dot.setBackground(gradientRound(THEME_A[i], THEME_B[i], 17));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(34), dp(34));
            cell.addView(dot, dlp);

            TextView nm = text(THEME_NAMES[i], 8.8f, on ? TXT : TXT3, on);
            nm.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(MATCH, WRAP);
            nlp.topMargin = dp(7);
            cell.addView(nm, nlp);

            if (on) {
                cell.setBackground(ripple(roundRect(withAlpha(acc1, 0x18), 12,
                        withAlpha(acc1, 0x55), 1), 0x22FFFFFF));
            } else {
                cell.setBackground(ripple(roundRect(0x00000000, 12, 0xFF1A2230, 1), 0x18FFFFFF));
            }
            cell.setPadding(0, dp(9), 0, dp(9));

            cell.setOnClickListener(v -> {
                getSharedPreferences("aihub_pref", Context.MODE_PRIVATE)
                        .edit().putInt("theme", idx).apply();
                d.dismiss();
                recreate();
            });

            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, WRAP, 1f);
            clp.leftMargin = dp(3);
            clp.rightMargin = dp(3);
            row.addView(cell, clp);
        }

        d.setContentView(box);
        d.show();

        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0x00000000));
            int wid = Math.min(getResources().getDisplayMetrics().widthPixels - dp(56), dp(440));
            w.setLayout(wid, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private View sectionHeader(String name, String desc, boolean first, View action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(this);
        dot.setBackground(gradientRound(acc1, acc2, 2));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(3), dp(11));
        dlp.rightMargin = dp(8);
        row.addView(dot, dlp);

        TextView t = text(name, 12f, 0xFFAEB9CC, true);
        t.setLetterSpacing(0.06f);
        row.addView(t);

        TextView d = text("   " + desc, 10f, 0xFF525E72, false);
        row.addView(d, new LinearLayout.LayoutParams(0, WRAP, 1f));

        if (action != null) row.addView(action);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = first ? dp(22) : dp(26);
        lp.bottomMargin = dp(11);
        row.setLayoutParams(lp);
        return row;
    }

    private View buildCard(final Provider p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(13), dp(11), dp(13));
        card.setBackground(cardBg());
        card.setClickable(true);
        card.setFocusable(true);

        View ic = providerIcon(p, 44, 13, p.shortName.length() >= 3 ? 10.5f : 13.5f);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(44), dp(44));
        ilp.rightMargin = dp(12);
        card.addView(ic, ilp);

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        nameRow.addView(text(p.name, 15f, TXT, true));

        if (Provider.balanceSpec(p.id) != null) {
            TextView bt = text("余额", 8.6f, 0xFF7FE3B4, true);
            bt.setBackground(roundRect(withAlpha(GREEN, 0x18), 5, 0, 0));
            bt.setPadding(dp(4), dp(1), dp(4), dp(1));
            LinearLayout.LayoutParams btlp = new LinearLayout.LayoutParams(WRAP, WRAP);
            btlp.leftMargin = dp(6);
            nameRow.addView(bt, btlp);
        }

        int n = vault.countOf(p.id);
        if (n > 0) {
            TextView tag = text(n + " 条", 9f, GREEN, false);
            tag.setBackground(roundRect(withAlpha(GREEN, 0x1C), 5, 0, 0));
            tag.setPadding(dp(5), dp(1), dp(5), dp(1));
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(WRAP, WRAP);
            tlp.leftMargin = dp(6);
            nameRow.addView(tag, tlp);
        }
        mid.addView(nameRow);

        String hintText = (p.note == null || p.note.trim().isEmpty()) ? p.baseUrl : p.note;
        TextView url = text(hintText, 10.4f, TXT3, false);
        url.setSingleLine(true);
        url.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(MATCH, WRAP);
        ulp.topMargin = dp(4);
        mid.addView(url, ulp);

        card.addView(mid);

        TextView chev = text("›", 19f, 0xFF3C4759, false);
        card.addView(chev);

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(MATCH, WRAP);
        clp.bottomMargin = dp(9);
        card.setLayoutParams(clp);

        card.setOnClickListener(v -> openProvider(p));
        return card;
    }

    /* ==================== 选平台页 ==================== */

    private View buildPickPage() {
        pickPage = new LinearLayout(this);
        pickPage.setOrientation(LinearLayout.VERTICAL);
        pickPage.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{BG, BG2}));
        pickPage.setVisibility(View.GONE);

        final int ph = dp(20);

        /* 标题 */
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(ph, dp(28), ph, dp(6));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("你在用哪几个 AI", 23f, TXT, true);
        title.setLetterSpacing(0.01f);
        titleCol.addView(title);
        TextView sub = text("点一下加入 · 排在前面的先显示", 11f, TXT3, false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(WRAP, WRAP);
        slp.topMargin = dp(6);
        titleCol.addView(sub, slp);
        head.addView(titleCol, new LinearLayout.LayoutParams(0, WRAP, 1f));

        pickSkip = text("跳过", 11.5f, TXT3, true);
        pickSkip.setGravity(Gravity.CENTER);
        pickSkip.setBackground(ripple(roundRect(0xFF161C29, 10, STROKE, 1)));
        pickSkip.setPadding(dp(12), dp(7), dp(12), dp(7));
        pickSkip.setOnClickListener(v -> finishPick());
        head.addView(pickSkip);

        pickPage.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        /* 列表 */
        pickScroll = new ScrollView(this);
        pickScroll.setFillViewport(true);
        pickScroll.setVerticalScrollBarEnabled(false);

        pickList = new LinearLayout(this);
        pickList.setOrientation(LinearLayout.VERTICAL);
        pickList.setPadding(ph, dp(10), ph, dp(14));
        pickScroll.addView(pickList, new ScrollView.LayoutParams(MATCH, WRAP));
        pickPage.addView(pickScroll, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        /* 底部按钮 */
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setBackgroundColor(0xF00A0D14);
        bottom.setPadding(ph, dp(12), ph, dp(24));

        TextView customBtn = text("＋ 自定义平台", 12.5f, 0xFFC3CFF0, true);
        customBtn.setGravity(Gravity.CENTER);
        customBtn.setBackground(ripple(roundRect(withAlpha(acc1, 0x14), 13,
                withAlpha(acc1, 0x44), 1), 0x22FFFFFF));
        customBtn.setOnClickListener(v -> showCustomDialog(null));
        LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(MATCH, dp(44));
        cblp.bottomMargin = dp(9);
        bottom.addView(customBtn, cblp);

        pickDone = text("开始使用", 15f, Color.WHITE, true);
        pickDone.setGravity(Gravity.CENTER);
        pickDone.setBackground(ripple(gradientRound(acc1, acc2, 15), 0x33FFFFFF));
        pickDone.setOnClickListener(v -> finishPick());
        bottom.addView(pickDone, new LinearLayout.LayoutParams(MATCH, dp(50)));

        pickPage.addView(bottom, new LinearLayout.LayoutParams(MATCH, WRAP));
        return pickPage;
    }

    private void openPick() {
        pickDraft.clear();
        pickDraft.addAll(picked);
        pickOpen = true;
        pickPage.setVisibility(View.VISIBLE);
        homeScroll.setVisibility(View.GONE);
        pickPage.bringToFront();
        renderPick();
    }

    private void finishPick() {
        picked.clear();
        picked.addAll(pickDraft);
        savePicked();
        getSharedPreferences("aihub_pref", Context.MODE_PRIVATE)
                .edit().putBoolean("onboarded", true).apply();
        firstRun = false;
        pickPage.setVisibility(View.GONE);
        pickOpen = false;
        homeScroll.setVisibility(View.VISIBLE);
        render();
    }

    private void renderPick() {
        if (pickList == null) return;
        final int y = pickScroll == null ? 0 : pickScroll.getScrollY();
        pickList.removeAllViews();

        List<Provider> sel = new ArrayList<>();
        for (String id : pickDraft) {
            Provider p = findById(id);
            if (p != null) sel.add(p);
        }

        List<Provider> rest = new ArrayList<>();
        for (Provider p : all) {
            if (!pickDraft.contains(p.id)) rest.add(p);
        }

        if (!sel.isEmpty()) {
            pickList.addView(pickGroup("已选 · 主页按这个顺序排", true));
            for (Provider p : sel) pickList.addView(pickRow(p));
        }
        if (!rest.isEmpty()) {
            pickList.addView(pickGroup(sel.isEmpty() ? "全部平台" : "还没选", sel.isEmpty()));
            for (Provider p : rest) pickList.addView(pickRow(p));
        }

        int n = pickDraft.size();
        pickDone.setText(n == 0 ? "先不选，直接进入" : "开始使用 · 已选 " + n + " 家");
        if (pickSkip != null) pickSkip.setText(firstRun && n == 0 ? "跳过" : "关闭");

        if (pickScroll != null) {
            pickScroll.post(() -> pickScroll.scrollTo(0, y));
        }
    }

    private View pickGroup(String name, boolean top) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(this);
        dot.setBackground(gradientRound(acc1, acc2, 2));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(3), dp(11));
        dlp.rightMargin = dp(8);
        row.addView(dot, dlp);

        TextView t = text(name, 11.5f, 0xFFAEB9CC, true);
        t.setLetterSpacing(0.05f);
        row.addView(t);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = top ? dp(10) : dp(22);
        lp.bottomMargin = dp(10);
        row.setLayoutParams(lp);
        return row;
    }

    private View pickRow(final Provider p) {
        final int idx = pickDraft.indexOf(p.id);
        final boolean on = idx >= 0;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(11), dp(10));
        row.setBackground(on
                ? ripple(roundRect(withAlpha(p.color, 0x1C), 15, withAlpha(p.color, 0x66), 1),
                0x22FFFFFF)
                : ripple(roundRect(CARD, 15, STROKE, 1), 0x16FFFFFF));

        View ic = providerIcon(p, 40, 12, p.shortName.length() >= 3 ? 10.5f : 13.5f);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(40), dp(40));
        ilp.rightMargin = dp(12);
        row.addView(ic, ilp);

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));
        mid.addView(text(p.name, 14f, on ? TXT : 0xFFC8D0DE, true));

        String sub = Provider.balanceSpec(p.id) != null
                ? "可自动查余额"
                : (p.overseas ? "海外 · 需相应网络" : "国内直连 · 跳网页看");
        TextView sv = text(sub, 10.2f, on ? TXT2 : 0xFF5F6B7E, false);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        svlp.topMargin = dp(3);
        mid.addView(sv, svlp);
        row.addView(mid);

        TextView badge = text(on ? String.valueOf(idx + 1) : "＋",
                on ? 12f : 15f, on ? Color.WHITE : 0xFF4E586B, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(on
                ? gradientRound(acc1, acc2, 14)
                : roundRect(0x00000000, 14, 0xFF2A3444, 1));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(28), dp(28));
        blp.leftMargin = dp(8);
        row.addView(badge, blp);

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        rlp.bottomMargin = dp(8);
        row.setLayoutParams(rlp);

        row.setOnClickListener(v -> {
            if (pickDraft.contains(p.id)) pickDraft.remove(p.id);
            else pickDraft.add(p.id);
            renderPick();
        });

        /* 自定义平台长按可以编辑或删除 */
        if (p.id.startsWith("c")) {
            row.setOnLongClickListener(v -> {
                showCustomOptions(p);
                return true;
            });
        }
        return row;
    }

    private void showCustomOptions(final Provider p) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(16));
        box.setBackground(roundRect(BG2, 20, STROKE, 1));

        box.addView(text(p.name, 15.5f, TXT, true));
        TextView sub = text("自定义平台", 10f, TXT3, false);
        sub.setPadding(0, dp(5), 0, dp(16));
        box.addView(sub);

        TextView edit = text("编辑名称和网址", 13.5f, TXT, true);
        edit.setGravity(Gravity.CENTER);
        edit.setBackground(ripple(roundRect(0xFF161C29, 13, STROKE, 1), 0x18FFFFFF));
        edit.setOnClickListener(v -> {
            d.dismiss();
            showCustomDialog(p);
        });
        box.addView(edit, new LinearLayout.LayoutParams(MATCH, dp(46)));

        TextView del = text("删除这个平台", 13.5f, 0xFFE08A8A, true);
        del.setGravity(Gravity.CENTER);
        del.setBackground(ripple(roundRect(withAlpha(0xFFE08A8A, 0x14), 13,
                withAlpha(0xFFE08A8A, 0x40), 1), 0x22FFFFFF));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(MATCH, dp(46));
        dlp.topMargin = dp(9);
        del.setOnClickListener(v -> {
            d.dismiss();
            confirmDeleteCustom(p);
        });
        box.addView(del, dlp);

        d.setContentView(box);
        d.show();
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0x00000000));
            int wid = Math.min(getResources().getDisplayMetrics().widthPixels - dp(56), dp(400));
            w.setLayout(wid, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void confirmDeleteCustom(final Provider p) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(16));
        box.setBackground(roundRect(BG2, 20, STROKE, 1));

        box.addView(text("删除「" + p.name + "」？", 15f, TXT, true));
        TextView tip = text("这个平台下存的密钥也会一起删掉，删了找不回来", 10.8f, 0xFFE08A8A, false);
        tip.setLineSpacing(dp(3), 1f);
        tip.setPadding(0, dp(8), 0, dp(18));
        box.addView(tip);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);

        TextView cancel = text("算了", 13.5f, TXT2, true);
        cancel.setGravity(Gravity.CENTER);
        cancel.setBackground(ripple(roundRect(0xFF161C29, 13, STROKE, 1), 0x18FFFFFF));
        cancel.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        clp.rightMargin = dp(9);
        btns.addView(cancel, clp);

        TextView ok = text("删除", 13.5f, Color.WHITE, true);
        ok.setGravity(Gravity.CENTER);
        ok.setBackground(ripple(roundRect(0xFFC2483F, 13, 0x00000000, 0), 0x33FFFFFF));
        ok.setOnClickListener(v -> {
            d.dismiss();
            deleteCustom(p);
        });
        btns.addView(ok, new LinearLayout.LayoutParams(0, dp(46), 1f));

        box.addView(btns);
        d.setContentView(box);
        d.show();

        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0x00000000));
            int wid = Math.min(getResources().getDisplayMetrics().widthPixels - dp(56), dp(400));
            w.setLayout(wid, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void deleteCustom(Provider p) {
        String icon = Provider.iconPath(p.id);

        customs.remove(p);
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).id.equals(p.id)) all.remove(i);
        }
        picked.remove(p.id);
        pickDraft.remove(p.id);
        savePicked();

        /* 连带清掉这个平台的密钥，别留孤儿数据 */
        for (KeyEntry e : vault.list(p.id)) vault.remove(p.id, e.id);

        Provider.removeIcon(p.id);
        if (icon != null) {
            try {
                java.io.File f = new java.io.File(icon);
                if (f.exists()) f.delete();
            } catch (Throwable ignored) {
            }
        }

        CustomStore.save(this, customs);
        renderPick();
        render();
        toast("已删除");
    }

    private Provider findById(String id) {
        for (Provider p : all) if (p.id.equals(id)) return p;
        return null;
    }

    private void loadPicked() {
        picked.clear();
        String s = getSharedPreferences("aihub_pref", Context.MODE_PRIVATE)
                .getString("picked", "");
        if (s == null || s.isEmpty()) return;
        for (String x : s.split(",")) {
            String t = x.trim();
            if (!t.isEmpty() && findById(t) != null && !picked.contains(t)) picked.add(t);
        }
    }

    private void savePicked() {
        StringBuilder sb = new StringBuilder();
        for (String id : picked) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        getSharedPreferences("aihub_pref", Context.MODE_PRIVATE)
                .edit().putString("picked", sb.toString()).apply();
    }

    /* ==================== 本地详情页 ==================== */

    private View buildDetailPage() {
        detailPage = new LinearLayout(this);
        detailPage.setOrientation(LinearLayout.VERTICAL);
        detailPage.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{BG, BG2}));
        detailPage.setVisibility(View.GONE);

        /* 顶栏 */
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(0xFF101520);
        top.setPadding(dp(4), 0, dp(14), 0);

        TextView back = text("‹", 27f, TXT, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(ripple(roundRect(Color.TRANSPARENT, 10, 0, 0)));
        back.setOnClickListener(v -> closeDetail());
        top.addView(back, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout txtBox = new LinearLayout(this);
        txtBox.setOrientation(LinearLayout.VERTICAL);
        detailName = text("", 14.5f, TXT, true);
        detailName.setSingleLine(true);
        detailName.setEllipsize(TextUtils.TruncateAt.END);
        txtBox.addView(detailName);
        detailSub = text("", 9.8f, TXT3, false);
        detailSub.setSingleLine(true);
        detailSub.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        sLp.topMargin = dp(2);
        txtBox.addView(detailSub, sLp);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        tLp.leftMargin = dp(4);
        top.addView(txtBox, tLp);

        detailPage.addView(top, new LinearLayout.LayoutParams(MATCH, dp(52)));

        /* 内容 */
        detailScroll = new ScrollView(this);
        detailScroll.setFillViewport(true);
        detailScroll.setVerticalScrollBarEnabled(false);
        detailScroll.setBackgroundColor(Color.TRANSPARENT);

        detailBox = new LinearLayout(this);
        detailBox.setOrientation(LinearLayout.VERTICAL);
        int ph = dp(18);
        detailBox.setPadding(ph, dp(16), ph, dp(34));

        detailScroll.addView(detailBox, new ScrollView.LayoutParams(MATCH, WRAP));
        detailPage.addView(detailScroll, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        return detailPage;
    }

    private void openProvider(Provider p) {
        current = p;
        detailName.setText(p.name);
        detailSub.setText(p.note + "  ·  " + (p.overseas ? "海外" : "国内直连"));
        renderDetail();
        detailPage.setVisibility(View.VISIBLE);
        homeScroll.setVisibility(View.GONE);
        detailOpen = true;
        if (detailScroll != null) detailScroll.scrollTo(0, 0);
    }

    private void closeDetail() {
        detailPage.setVisibility(View.GONE);
        homeScroll.setVisibility(View.VISIBLE);
        detailOpen = false;
        render();
    }

    /** 重绘本页：接口地址 / 密钥备忘 / 两个网站入口 */
    private void renderDetail() {
        if (detailBox == null || current == null) return;
        final Provider p = current;
        detailBox.removeAllViews();

        /* ---- 0. 账户余额（这是普通人进来最想看的东西，放第一屏） ---- */
        detailBox.addView(buildBalanceCard(p), sectionLp(false));

        /* ---- 1. 接口地址 ---- */
        detailBox.addView(sectionBar("接口地址   BASE URL", null), sectionLp(true));

        LinearLayout urlRow = new LinearLayout(this);
        urlRow.setOrientation(LinearLayout.HORIZONTAL);
        urlRow.setGravity(Gravity.CENTER_VERTICAL);
        urlRow.setBackground(roundRect(FIELD, 13, 0xFF222B3B, 1));
        urlRow.setPadding(dp(13), dp(12), dp(9), dp(12));

        TextView urlTv = text(p.baseUrl, 11f, 0xFFC7D0E0, false);
        urlTv.setTypeface(Typeface.MONOSPACE);
        urlTv.setTextIsSelectable(true);
        urlRow.addView(urlTv, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView copy1 = chip("复制", 11f, 0xFFB9C7FF,
                withAlpha(acc1, 0x24), withAlpha(acc1, 0x50));
        copy1.setOnClickListener(v -> {
            copy("接口地址", p.baseUrl);
            toast("接口地址已复制");
        });
        LinearLayout.LayoutParams c1lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        c1lp.leftMargin = dp(8);
        urlRow.addView(copy1, c1lp);

        detailBox.addView(urlRow, new LinearLayout.LayoutParams(MATCH, WRAP));

        /* ---- 2. 密钥备忘录 ---- */
        List<KeyEntry> entries = vault.list(p.id);

        TextView addBtn = chip("＋ 新建", 10.5f, 0xFFCBD5E8,
                withAlpha(acc1, 0x1C), withAlpha(acc1, 0x3A));
        addBtn.setOnClickListener(v -> new EntryDialog(p, null).show());

        detailBox.addView(sectionBar("API KEY 备忘录 · " + entries.size() + " 条", addBtn),
                sectionLp(true));

        if (entries.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.setBackground(roundRect(FIELD, 14, 0, 0));
            empty.setPadding(dp(16), dp(22), dp(16), dp(20));

            TextView t1 = text("还没有记任何密钥", 12.5f, 0xFF7A869B, true);
            t1.setGravity(Gravity.CENTER);
            empty.addView(t1);

            TextView t2 = text("一个平台可以存多条，比如「主力号 / 测试号」\n名字、用途、key 都可以先留空，以后再补",
                    10.5f, 0xFF4E586B, false);
            t2.setGravity(Gravity.CENTER);
            t2.setLineSpacing(dp(4), 1f);
            LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(WRAP, WRAP);
            l2.topMargin = dp(8);
            empty.addView(t2, l2);

            detailBox.addView(empty, new LinearLayout.LayoutParams(MATCH, WRAP));
        } else {
            for (KeyEntry e : entries) {
                detailBox.addView(buildEntryCard(p, e));
            }
        }

        /* ---- 3 & 4. 两个网站入口（各自指向具体页面，按钮上直接显示目标地址） ---- */
        detailBox.addView(sectionBar("打开网站", null), sectionLp(true));

        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(MATCH, WRAP);
        b1.bottomMargin = dp(9);
        detailBox.addView(entryAction("API 页", "建 Key · 管理密钥", p.apiKey, true), b1);
        detailBox.addView(entryAction("用量 · 账单", "用量 · 消费 · 余额", p.billing, false),
                new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView tip = text("只有上面这两个按钮会打开网站，其余都在本地。\n登录一次后 Cookie 存在本机，下次进来不用重新登。",
                10f, 0xFF4E586B, false);
        tip.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        tlp.topMargin = dp(12);
        detailBox.addView(tip, tlp);
    }

    /* ---------- 余额卡 ---------- */

    /**
     * 能自动读余额的平台才长这个样子；读不了的给一条说明，引导去「费用页」。
     * 先画缓存值再后台刷新，进页面不会白等一下。
     */
    private View buildBalanceCard(final Provider p) {
        final Provider.BalanceSpec spec = Provider.balanceSpec(p.id);

        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(13), dp(15), dp(15));
        card.setBackground(roundRect(CARD2, 18, STROKE, 1));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout labBox = new LinearLayout(this);
        labBox.setOrientation(LinearLayout.VERTICAL);
        TextView label = text("账户余额", 9.8f, 0xFF6B7689, true);
        label.setLetterSpacing(0.1f);
        labBox.addView(label);
        head.addView(labBox, new LinearLayout.LayoutParams(0, WRAP, 1f));

        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        bodyLp.topMargin = dp(9);

        card.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));
        card.addView(body, bodyLp);

        /* 这家没有余额接口 */
        if (spec == null) {
            body.addView(text("这家没有公开的余额接口", 14f, 0xFF8E9BB0, true));
            TextView t = text("点下面「用量 · 账单」进网页看，登录一次之后就记住了",
                    10.5f, 0xFF4E586B, false);
            t.setLineSpacing(dp(3), 1f);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(MATCH, WRAP);
            tlp.topMargin = dp(6);
            body.addView(t, tlp);
            return card;
        }

        final TextView refreshBtn = chip("刷新", 10f, 0xFFB9C7FF,
                withAlpha(acc1, 0x22), withAlpha(acc1, 0x40));
        head.addView(refreshBtn);

        /* 先贴缓存 */
        paintBalance(body, Balance.cached(this, p.id));

        refreshBtn.setOnClickListener(v -> queryBalance(p, body, refreshBtn, true));

        /* 有 key 且没有缓存（或缓存超过 10 分钟）就自动查一次 */
        Balance.R cache = Balance.cached(this, p.id);
        boolean stale = cache == null || System.currentTimeMillis() - cache.ts > 10 * 60 * 1000L;
        if (stale && vault.countWithKey(p.id) > 0) {
            queryBalance(p, body, refreshBtn, false);
        }
        return card;
    }

    private void queryBalance(final Provider p, final LinearLayout body,
                              final TextView btn, final boolean manual) {
        if (fetching.contains(p.id)) return;
        final String key = firstKey(p.id);
        if (key.isEmpty()) {
            if (manual) toast("先在下面「API KEY 备忘录」里存一条密钥");
            return;
        }
        fetching.add(p.id);
        btn.setText("查询中");
        btn.setAlpha(0.6f);

        Balance.fetch(this, p, key, new Balance.Cb() {
            @Override
            public void onResult(Balance.R r) {
                fetching.remove(p.id);
                btn.setText("刷新");
                btn.setAlpha(1f);
                if (r.ok) {
                    paintBalance(body, r);
                } else if (manual) {
                    paintBalance(body, r);
                    toast(r.error);
                }
            }
        });
    }

    private void paintBalance(LinearLayout body, Balance.R r) {
        if (body == null) return;
        body.removeAllViews();

        if (r == null) {
            body.addView(text("—", 30f, 0xFF3C4759, true));
            TextView t = text("点「刷新」读取余额", 10.5f, 0xFF4E586B, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.topMargin = dp(4);
            body.addView(t, lp);
            return;
        }

        if (!r.ok) {
            body.addView(text("—", 30f, 0xFF3C4759, true));
            TextView t = text(r.error, 11f, 0xFFE08A8A, false);
            t.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.topMargin = dp(5);
            body.addView(t, lp);
            return;
        }

        TextView amount = text(r.amount, 30f, TXT, true);
        amount.setLetterSpacing(-0.015f);
        body.addView(amount);

        if (r.detail != null && !r.detail.isEmpty()) {
            TextView d = text(r.detail, 10.8f, TXT2, false);
            d.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.topMargin = dp(6);
            body.addView(d, lp);
        }

        TextView when = text(Balance.ago(r.ts), 9.8f, 0xFF4E586B, false);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(MATCH, WRAP);
        wp.topMargin = dp(5);
        body.addView(when, wp);
    }

    /** 拿这个平台第一条真的填了 key 的备忘 */
    private String firstKey(String pid) {
        for (KeyEntry e : vault.list(pid)) {
            String k = e.key == null ? "" : e.key.trim();
            if (!k.isEmpty()) return k;
        }
        return "";
    }

    private LinearLayout sectionBar(String title, View action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView t = text(title, 9.8f, 0xFF6B7689, true);
        t.setLetterSpacing(0.08f);
        row.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));

        if (action != null) row.addView(action);
        return row;
    }

    private LinearLayout.LayoutParams sectionLp(boolean extraTop) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(extraTop ? 22 : 16);
        lp.bottomMargin = dp(9);
        return lp;
    }

    private View buildEntryCard(final Provider p, final KeyEntry e) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(11), dp(11), dp(11));
        card.setBackground(roundRect(CARD, 14, STROKE, 1));

        /* 名字 + 操作 */
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = text(e.displayName(), 13.5f,
                e.name == null || e.name.trim().isEmpty() ? 0xFF6F7A8C : TXT, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        r1.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView copyBtn = chip("复制", 10.5f, 0xFFB9C7FF,
                withAlpha(acc1, 0x22), 0);
        copyBtn.setOnClickListener(v -> {
            String k = e.key == null ? "" : e.key.trim();
            if (k.isEmpty()) {
                toast("这条还没填 key");
                return;
            }
            copy("密钥", k);
            toast("已复制 " + e.displayName());
        });
        r1.addView(copyBtn);

        TextView editBtn = chip("编辑", 10.5f, TXT2, 0xFF1B2231, 0);
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        eLp.leftMargin = dp(6);
        editBtn.setOnClickListener(v -> new EntryDialog(p, e).show());
        r1.addView(editBtn, eLp);

        card.addView(r1, new LinearLayout.LayoutParams(MATCH, WRAP));

        /* 用途 */
        String purpose = e.purpose == null ? "" : e.purpose.trim();
        if (!purpose.isEmpty()) {
            TextView pv = text(purpose, 10.6f, TXT2, false);
            pv.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(MATCH, WRAP);
            pLp.topMargin = dp(6);
            card.addView(pv, pLp);
        }

        /* 掩码 key + 删除 */
        LinearLayout r3 = new LinearLayout(this);
        r3.setOrientation(LinearLayout.HORIZONTAL);
        r3.setGravity(Gravity.CENTER_VERTICAL);

        String key = e.key == null ? "" : e.key.trim();
        TextView kv = text(key.isEmpty() ? "（未填 key）" : mask(key), 10.5f,
                key.isEmpty() ? 0xFF4E586B : 0xFF8E9BB0, false);
        kv.setTypeface(Typeface.MONOSPACE);
        kv.setSingleLine(true);
        kv.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        r3.addView(kv, new LinearLayout.LayoutParams(0, WRAP, 1f));

        final TextView del = text("删除", 10.5f, 0xFF5B6473, false);
        del.setGravity(Gravity.CENTER);
        del.setPadding(dp(9), dp(4), dp(4), dp(4));
        del.setOnClickListener(new View.OnClickListener() {
            private long armed = 0L;
            private final Runnable reset = () -> {
                del.setText("删除");
                del.setTextColor(0xFF5B6473);
            };

            @Override
            public void onClick(View v) {
                long now = System.currentTimeMillis();
                if (now - armed < 3000) {
                    vault.remove(p.id, e.id);
                    toast("已删除「" + e.displayName() + "」");
                    renderDetail();
                    render();
                } else {
                    armed = now;
                    del.setText("确认删除");
                    del.setTextColor(0xFFFF6B6B);
                    del.postDelayed(reset, 3000);
                }
            }
        });
        r3.addView(del);

        LinearLayout.LayoutParams r3Lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        r3Lp.topMargin = dp(6);
        card.addView(r3, r3Lp);

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(MATCH, WRAP);
        clp.bottomMargin = dp(9);
        card.setLayoutParams(clp);
        return card;
    }

    /** 入口按钮：标题 + 用途 + 真实目标地址（点之前就能看出会去哪一页） */
    private View entryAction(String title, String hint, final String url, boolean primary) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15), dp(13), dp(14), dp(13));
        box.setBackground(primary
                ? ripple(gradientRound(acc1, acc2, 16), 0x33FFFFFF)
                : ripple(roundRect(CARD, 16, STROKE, 1), 0x22FFFFFF));
        box.setClickable(true);
        box.setFocusable(true);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        titleRow.addView(text(title, 15f, primary ? Color.WHITE : TXT, true),
                new LinearLayout.LayoutParams(WRAP, WRAP));

        TextView hintTv = text(hint, 10f, primary ? 0xCCFFFFFF : 0xFF6F7A8C, false);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        hLp.leftMargin = dp(8);
        titleRow.addView(hintTv, hLp);

        TextView arrow = text("↗", 14f, primary ? 0xFFFFE7FF : 0xFF8E9BB0, true);
        titleRow.addView(arrow);
        box.addView(titleRow, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView urlTv = text(shortUrl(url), 9.8f, primary ? 0x99FFFFFF : 0xFF5B6473, false);
        urlTv.setTypeface(Typeface.MONOSPACE);
        urlTv.setSingleLine(true);
        urlTv.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        uLp.topMargin = dp(5);
        box.addView(urlTv, uLp);

        box.setOnClickListener(v -> openWeb(url, title));
        return box;
    }

    /** 去掉协议头，只留 host + path，给按钮做副标题 */
    private String shortUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        String s = url;
        int i = s.indexOf("://");
        if (i > 0) s = s.substring(i + 3);
        if (s.startsWith("www.")) s = s.substring(4);
        int q = s.indexOf('?');
        if (q > 0) s = s.substring(0, q);
        int h = s.indexOf('#');
        if (h > 0) s = s.substring(0, h);
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /* ==================== 网页页 ==================== */

    private View buildWebPage() {
        webPage = new FrameLayout(this);
        webPage.setBackgroundColor(BG);
        webPage.setVisibility(View.GONE);

        web = new WebView(this);
        setupWeb();
        FrameLayout.LayoutParams wlp = new FrameLayout.LayoutParams(MATCH, MATCH);
        wlp.topMargin = dp(50);
        webPage.addView(web, wlp);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressDrawable(barDrawable());
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(MATCH, dp(2), Gravity.TOP);
        blp.topMargin = dp(50);
        webPage.addView(bar, blp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(0xFF101520);
        top.setPadding(dp(4), 0, dp(10), 0);

        TextView back = text("‹", 27f, TXT, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(ripple(roundRect(Color.TRANSPARENT, 10, 0, 0)));
        back.setOnClickListener(v -> closeWeb());
        top.addView(back, new LinearLayout.LayoutParams(dp(38), dp(38)));

        webTitle = text("", 13.5f, TXT, true);
        webTitle.setSingleLine(true);
        webTitle.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        tlp.leftMargin = dp(2);
        top.addView(webTitle, tlp);

        uaChip = text(desktopUa ? "PC" : "MOBILE", 10.5f, 0xFF9FB0CC, true);
        uaChip.setGravity(Gravity.CENTER);
        uaChip.setBackground(ripple(roundRect(0xFF1A2130, 8, STROKE, 1)));
        uaChip.setPadding(dp(9), dp(5), dp(9), dp(5));
        uaChip.setOnClickListener(v -> toggleUa());
        top.addView(uaChip, new LinearLayout.LayoutParams(WRAP, WRAP));

        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(MATCH, dp(50), Gravity.TOP);
        webPage.addView(top, topLp);

        return webPage;
    }

    private void openWeb(String url, String title) {
        if (current == null || url == null || url.isEmpty()) return;
        webTitle.setText(current.name + " · " + title);
        webPage.setVisibility(View.VISIBLE);
        detailPage.setVisibility(View.GONE);
        homeScroll.setVisibility(View.GONE);
        webOpen = true;
        if (web != null) {
            web.getSettings().setUserAgentString(ua());
            /* 不 clearHistory 的话，换个平台打开会叠在上一家的历史栈上面，
               用户按返回会退回上一个平台的页面 —— 就是「进 ds 之后再进别家会先见到 ds」 */
            web.stopLoading();
            web.clearHistory();
            web.loadUrl(url);
        }
    }

    private void closeWeb() {
        if (web != null) web.stopLoading();
        webPage.setVisibility(View.GONE);
        detailPage.setVisibility(View.VISIBLE);
        webOpen = false;
        renderDetail();
    }

    private void toggleUa() {
        desktopUa = !desktopUa;
        getSharedPreferences("aihub_pref", Context.MODE_PRIVATE)
                .edit().putBoolean("desktop", desktopUa).apply();
        if (web != null) {
            web.getSettings().setUserAgentString(ua());
            web.reload();
        }
        uaChip.setText(desktopUa ? "PC" : "MOBILE");
        toast(desktopUa ? "已切换为电脑版页面" : "已切换为手机版页面");
    }

    private String ua() {
        return desktopUa ? UA_DESKTOP : WebSettings.getDefaultUserAgent(this);
    }

    /* ==================== 密钥本（总览） ==================== */

    private View buildKeysPage() {
        keysPage = new ScrollView(this);
        keysPage.setFillViewport(true);
        keysPage.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{BG, BG2}));
        keysPage.setVerticalScrollBarEnabled(false);
        keysPage.setVisibility(View.GONE);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int ph = dp(18);
        col.setPadding(ph, dp(26), ph, dp(30));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = text("‹", 27f, TXT, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(ripple(roundRect(0xFF161C29, 10, 0, 0)));
        back.setOnClickListener(v -> closeKeys());
        head.addView(back, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(text("密钥本", 21f, TXT, true));
        TextView sub = text("存在本机 · 点一下即可复制", 10.5f, TXT3, false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(WRAP, WRAP);
        slp.topMargin = dp(4);
        titleBox.addView(sub, slp);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        tlp.leftMargin = dp(10);
        head.addView(titleBox, tlp);

        keysCount = text("0 条", 10.5f, GREEN, true);
        keysCount.setBackground(roundRect(withAlpha(GREEN, 0x1F), 9, withAlpha(GREEN, 0x40), 1));
        keysCount.setPadding(dp(10), dp(5), dp(10), dp(5));
        head.addView(keysCount);

        col.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        keysList = new LinearLayout(this);
        keysList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(MATCH, WRAP);
        blp.topMargin = dp(22);
        col.addView(keysList, blp);

        keysPage.addView(col, new ScrollView.LayoutParams(MATCH, WRAP));
        return keysPage;
    }

    private void openKeys() {
        refreshKeys();
        keysPage.setVisibility(View.VISIBLE);
        homeScroll.setVisibility(View.GONE);
        keysOpen = true;
    }

    private void closeKeys() {
        keysPage.setVisibility(View.GONE);
        homeScroll.setVisibility(View.VISIBLE);
        keysOpen = false;
        render();
    }

    private void refreshKeys() {
        if (keysList == null) return;
        keysList.removeAllViews();

        int total = 0;
        for (Provider p : all) total += vault.countOf(p.id);
        if (keysCount != null) keysCount.setText(total + " 条");

        if (total == 0) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.setPadding(0, dp(70), 0, dp(40));

            TextView t1 = text("还没有保存任何密钥", 14f, 0xFF7A869B, true);
            t1.setGravity(Gravity.CENTER);
            empty.addView(t1);

            TextView t2 = text("回到主页点开任一平台，\n在「API KEY 备忘录」里点 ＋ 新建",
                    11.5f, 0xFF505C70, false);
            t2.setGravity(Gravity.CENTER);
            t2.setLineSpacing(dp(4), 1f);
            LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(WRAP, WRAP);
            l2.topMargin = dp(10);
            empty.addView(t2, l2);

            keysList.addView(empty, new LinearLayout.LayoutParams(MATCH, WRAP));
            return;
        }

        for (Provider p : all) {
            List<KeyEntry> es = vault.list(p.id);
            if (es.isEmpty()) continue;

            /* 平台分组标题 */
            LinearLayout gh = new LinearLayout(this);
            gh.setOrientation(LinearLayout.HORIZONTAL);
            gh.setGravity(Gravity.CENTER_VERTICAL);
            TextView gname = text(p.name, 11.5f, 0xFFAEB9CC, true);
            gh.addView(gname, new LinearLayout.LayoutParams(0, WRAP, 1f));
            gh.addView(text(es.size() + " 条", 9.5f, 0xFF525E72, false));
            LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(MATCH, WRAP);
            glp.topMargin = dp(16);
            glp.bottomMargin = dp(8);
            keysList.addView(gh, glp);

            for (KeyEntry e : es) keysList.addView(buildKeyRow(p, e));
        }

        TextView tip = text("密钥用系统密钥库加密后存在本机，不会上传到任何服务器",
                10f, 0xFF4E586B, false);
        tip.setGravity(Gravity.CENTER);
        tip.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams tlpp = new LinearLayout.LayoutParams(MATCH, WRAP);
        tlpp.topMargin = dp(22);
        keysList.addView(tip, tlpp);
    }

    private View buildKeyRow(final Provider p, final KeyEntry e) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(13), dp(11), dp(9), dp(11));
        row.setBackground(cardBg());

        View ic = providerIcon(p, 36, 11, p.shortName.length() >= 3 ? 9f : 12f);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(36), dp(36));
        ilp.rightMargin = dp(11);
        row.addView(ic, ilp);

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));

        String nm = e.displayName();
        String purpose = e.purpose == null ? "" : e.purpose.trim();
        mid.addView(text(purpose.isEmpty() ? nm : nm + " · " + purpose, 13.2f, TXT, true));

        String key = e.key == null ? "" : e.key.trim();
        TextView mk = text(key.isEmpty() ? "（未填 key）" : mask(key), 10.5f,
                key.isEmpty() ? 0xFF4E586B : TXT2, false);
        mk.setTypeface(Typeface.MONOSPACE);
        mk.setSingleLine(true);
        mk.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        mlp.topMargin = dp(4);
        mid.addView(mk, mlp);

        row.addView(mid);

        TextView copyBtn = chip("复制", 11f, 0xFFB9C7FF,
                withAlpha(acc1, 0x24), withAlpha(acc1, 0x50));
        copyBtn.setOnClickListener(v -> {
            String k = e.key == null ? "" : e.key.trim();
            if (k.isEmpty()) {
                toast("这条还没填 key");
                return;
            }
            copy("密钥", k);
            toast("已复制 " + nm);
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(WRAP, WRAP);
        clp.rightMargin = dp(3);
        row.addView(copyBtn, clp);

        final TextView del = text("✕", 13f, 0xFF5B6473, false);
        del.setGravity(Gravity.CENTER);
        del.setPadding(dp(10), dp(8), dp(10), dp(8));
        del.setOnClickListener(new View.OnClickListener() {
            private long armed = 0L;
            private final Runnable reset = () -> {
                del.setText("✕");
                del.setTextColor(0xFF5B6473);
            };

            @Override
            public void onClick(View v) {
                long now = System.currentTimeMillis();
                if (now - armed < 3000) {
                    vault.remove(p.id, e.id);
                    toast("已删除「" + e.displayName() + "」");
                    refreshKeys();
                    render();
                } else {
                    armed = now;
                    del.setText("确认");
                    del.setTextColor(0xFFFF6B6B);
                    del.postDelayed(reset, 3000);
                }
            }
        });
        row.addView(del, new LinearLayout.LayoutParams(WRAP, WRAP));

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        rlp.bottomMargin = dp(8);
        row.setLayoutParams(rlp);
        return row;
    }

    /* ==================== 新建 / 编辑密钥 ==================== */

    private class EntryDialog extends Dialog {

        EntryDialog(final Provider p, final KeyEntry src) {
            super(MainActivity.this, android.R.style.Theme_Translucent_NoTitleBar);
            requestWindowFeature(Window.FEATURE_NO_TITLE);

            ScrollView sv = new ScrollView(MainActivity.this);
            GradientDrawable sheet = new GradientDrawable();
            sheet.setColor(CARD2);
            sheet.setCornerRadii(new float[]{
                    dp(22), dp(22), dp(22), dp(22), 0, 0, 0, 0});
            sv.setBackground(sheet);

            LinearLayout col = new LinearLayout(MainActivity.this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(dp(20), 0, dp(20), dp(20));

            View accent = new View(MainActivity.this);
            accent.setBackground(gradientRound(acc1, acc2, 0));
            col.addView(accent, new LinearLayout.LayoutParams(MATCH, dp(3)));

            View grab = new View(MainActivity.this);
            grab.setBackground(roundRect(0xFF2A3444, 3, 0, 0));
            LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(dp(38), dp(4));
            glp.gravity = Gravity.CENTER_HORIZONTAL;
            glp.topMargin = dp(10);
            glp.bottomMargin = dp(16);
            col.addView(grab, glp);

            /* 标题 */
            LinearLayout head = new LinearLayout(MainActivity.this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout htext = new LinearLayout(MainActivity.this);
            htext.setOrientation(LinearLayout.VERTICAL);
            htext.addView(text(src == null ? "新建密钥" : "编辑密钥", 16f, TXT, true));
            TextView hsub = text(p.name + " · 只存本机，系统密钥库加密",
                    10.5f, TXT3, false);
            LinearLayout.LayoutParams hslp = new LinearLayout.LayoutParams(WRAP, WRAP);
            hslp.topMargin = dp(3);
            htext.addView(hsub, hslp);
            head.addView(htext, new LinearLayout.LayoutParams(0, WRAP, 1f));

            TextView close = text("✕", 13f, TXT3, false);
            close.setGravity(Gravity.CENTER);
            close.setBackground(ripple(roundRect(0xFF1B2231, 12, 0, 0)));
            close.setOnClickListener(v -> dismiss());
            head.addView(close, new LinearLayout.LayoutParams(dp(28), dp(28)));

            col.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

            /* 名字（可空） */
            col.addView(smallLabel("名字  ·  可留空"), labelLp());
            final EditText etName = field("比如：主力号 / 公司账号", false, 1);
            if (src != null && src.name != null) etName.setText(src.name);
            col.addView(etName, fieldLp(1));

            /* 用途（可空） */
            col.addView(smallLabel("用途  ·  可留空"), labelLp());
            final EditText etUse = field("比如：写代码 / 日常问答 / 备用", false, 1);
            if (src != null && src.purpose != null) etUse.setText(src.purpose);
            col.addView(etUse, fieldLp(1));

            /* key（可空） */
            col.addView(smallLabel("API KEY  ·  可留空，以后再补"), labelLp());

            LinearLayout keyRow = new LinearLayout(MainActivity.this);
            keyRow.setOrientation(LinearLayout.HORIZONTAL);
            keyRow.setGravity(Gravity.CENTER_VERTICAL);
            keyRow.setBackground(roundRect(FIELD, 12, 0xFF222B3B, 1));
            keyRow.setPadding(dp(12), dp(4), dp(8), dp(4));

            final EditText etKey = new EditText(MainActivity.this);
            etKey.setBackground(null);
            etKey.setHint("粘贴 sk- 开头的密钥");
            etKey.setHintTextColor(0xFF4E586B);
            etKey.setTextColor(TXT);
            etKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            etKey.setTypeface(Typeface.MONOSPACE);
            etKey.setSingleLine(true);
            etKey.setPadding(0, dp(8), 0, dp(8));
            etKey.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            if (src != null && src.key != null) etKey.setText(src.key);
            keyRow.addView(etKey, new LinearLayout.LayoutParams(0, WRAP, 1f));

            final TextView eye = text("显示", 11f, TXT3, true);
            eye.setGravity(Gravity.CENTER);
            eye.setPadding(dp(10), dp(5), dp(10), dp(5));
            final boolean[] shown = new boolean[]{false};
            eye.setOnClickListener(v -> {
                shown[0] = !shown[0];
                etKey.setInputType(InputType.TYPE_CLASS_TEXT
                        | (shown[0] ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));
                etKey.setTypeface(Typeface.MONOSPACE);
                etKey.setSelection(etKey.getText().length());
                eye.setText(shown[0] ? "隐藏" : "显示");
            });
            keyRow.addView(eye, new LinearLayout.LayoutParams(WRAP, WRAP));

            col.addView(keyRow, fieldLp(1));

            /* 保存 */
            TextView save = text(src == null ? "保存到本机" : "保存修改", 14.5f, Color.WHITE, true);
            save.setGravity(Gravity.CENTER);
            save.setBackground(ripple(gradientRound(acc1, acc2, 13)));
            save.setOnClickListener(v -> {
                KeyEntry e = src == null ? new KeyEntry() : src;
                if (e.id == null || e.id.isEmpty()) e.id = java.util.UUID.randomUUID().toString();
                e.name = etName.getText().toString().trim();
                e.purpose = etUse.getText().toString().trim();
                e.key = etKey.getText().toString().trim();
                vault.put(p.id, e);
                toast(src == null ? "已新建「" + e.displayName() + "」" : "已保存修改");
                dismiss();
                renderDetail();
                render();
                if (keysOpen) refreshKeys();
            });
            LinearLayout.LayoutParams savlp = new LinearLayout.LayoutParams(MATCH, dp(48));
            savlp.topMargin = dp(20);
            col.addView(save, savlp);

            TextView note = text("三项都可以留空先占个位，随时回来点「编辑」补上",
                    10f, 0xFF4E586B, false);
            note.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(MATCH, WRAP);
            nlp.topMargin = dp(10);
            col.addView(note, nlp);

            sv.addView(col, new ScrollView.LayoutParams(MATCH, WRAP));
            setContentView(sv);
            setCanceledOnTouchOutside(true);
        }

        private TextView smallLabel(String s) {
            TextView t = text(s, 9.8f, 0xFF6B7689, true);
            t.setLetterSpacing(0.06f);
            return t;
        }

        private EditText field(String hint, boolean mono, int lines) {
            EditText et = new EditText(MainActivity.this);
            et.setBackground(roundRect(FIELD, 12, 0xFF222B3B, 1));
            et.setHint(hint);
            et.setHintTextColor(0xFF4E586B);
            et.setTextColor(TXT);
            et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            if (mono) et.setTypeface(Typeface.MONOSPACE);
            et.setSingleLine(lines <= 1);
            et.setMaxLines(lines);
            et.setGravity(Gravity.CENTER_VERTICAL);
            et.setPadding(dp(12), dp(10), dp(12), dp(10));
            return et;
        }

        private LinearLayout.LayoutParams labelLp() {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.topMargin = dp(15);
            lp.bottomMargin = dp(7);
            return lp;
        }

        private LinearLayout.LayoutParams fieldLp(int lines) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            return lp;
        }

        @Override
        protected void onStart() {
            super.onStart();
            Window w = getWindow();
            if (w != null) {
                w.setLayout(MATCH, WRAP);
                w.setGravity(Gravity.BOTTOM);
                w.setDimAmount(0.68f);
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
    }

    /* ==================== WebView ==================== */

    private void setupWeb() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(ua());
        s.setMediaPlaybackRequiresUserGesture(false);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return handleScheme(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                return handleScheme(req.getUrl().toString());
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                if (bar == null) return;
                bar.setProgress(p);
                bar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb,
                                             FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;
                try {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(Intent.createChooser(i, "选择文件"), 1001);
                    return true;
                } catch (Exception e) {
                    filePathCallback = null;
                    toast("打不开文件选择器");
                    return false;
                }
            }
        });

        web.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String ua, String cd, String mime, long len) {
                try {
                    DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                    r.setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    String name = URLUtil.guessFileName(url, cd, mime);
                    r.setDestinationInExternalFilesDir(MainActivity.this,
                            Environment.DIRECTORY_DOWNLOADS, name);
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(r);
                        toast("开始下载 " + name);
                    }
                } catch (Exception e) {
                    toast("下载失败");
                }
            }
        });
    }

    private boolean handleScheme(String url) {
        if (url == null) return false;
        if (url.startsWith("http://") || url.startsWith("https://")
                || url.startsWith("about:") || url.startsWith("data:")
                || url.startsWith("javascript:")) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1002) {
            if (res == RESULT_OK && data != null && data.getData() != null) {
                String path = copyCustomIcon(data.getData());
                if (path != null) {
                    pendingIconPath = path;
                    if (customPreview != null) {
                        Bitmap bm = decodeIcon(path, (int) dp(40));
                        if (bm != null) {
                            customPreview.setImageBitmap(roundBitmap(bm, (int) dp(11)));
                            customPreview.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    toast("读取图片失败");
                }
            }
            return;
        }
        if (req == 1001) {
            if (filePathCallback == null) return;
            Uri[] out = null;
            if (res == RESULT_OK && data != null && data.getData() != null) {
                out = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(out);
            filePathCallback = null;
        }
    }

    /* ==================== 工具 ==================== */

    private TextView chip(String s, float sp, int color, int fill, int stroke) {
        TextView t = text(s, sp, color, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(ripple(roundRect(fill, 9, stroke, 1)));
        t.setPadding(dp(11), dp(6), dp(11), dp(6));
        return t;
    }

    private String mask(String k) {
        if (k == null || k.isEmpty()) return "—";
        if (k.length() <= 12) return "••••••••";
        return k.substring(0, 6) + "••••••" + k.substring(k.length() - 4);
    }

    private String hostOf(String url) {
        if (url == null) return "";
        try {
            String h = Uri.parse(url).getHost();
            return h == null ? url : h;
        } catch (Exception e) {
            return url;
        }
    }

    private void copy(String label, String value) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(label, value));
        if (Build.VERSION.SDK_INT < 33) toast("已复制" + label);
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Drawable roundRect(int fill, float radius, int stroke, int strokeW) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if (strokeW > 0) g.setStroke(dp(strokeW), stroke);
        return g;
    }

    private Drawable gradientRound(int c1, int c2, float radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{c1, c2});
        g.setCornerRadius(dp(radius));
        return g;
    }

    /** 图标块：从左上到右下的淡渐变 + 细描边，比纯色块更有层次 */
    private Drawable iconBg(int color) {
        return iconBg(color, 13);
    }

    /**
     * 平台图标：有自定义图片就显示图片，没有就用简称字标。
     * 尺寸和圆角都按 dp 传，三处列表共用同一套规则。
     */
    /**
     * 平台图标，三级回退：
     * 1) 用户自己指定的图片（自定义平台、或以后替换过的）
     * 2) assets/logos/&lt;pid&gt;.png 内置 logo（已按品牌色处理好，自带圆角）
     * 3) 品牌色圆角块 + 首字母
     */
    private View providerIcon(Provider p, int sizeDp, float radiusDp, float textSize) {
        int px = (int) dp(sizeDp);
        Bitmap bm = null;
        boolean needRound = false;

        String path = Provider.iconPath(p.id);
        if (path != null && !path.isEmpty()) {
            bm = decodeIcon(path, px);
            needRound = bm != null;
        }
        if (bm == null) {
            bm = decodeAsset("logos/" + p.id + ".png", px);
        }
        if (bm != null) {
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(needRound ? roundBitmap(bm, (int) dp(radiusDp)) : bm);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return iv;
        }

        TextView tv = text(p.shortName, textSize, iconTextColor(p.color), true);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(iconBg(p.color, radiusDp));
        return tv;
    }

    /** 从 assets 读图，按目标像素做降采样 */
    private Bitmap decodeAsset(String name, int targetPx) {
        try {
            BitmapFactory.Options probe = new BitmapFactory.Options();
            probe.inJustDecodeBounds = true;
            try (InputStream is = getAssets().open(name)) {
                BitmapFactory.decodeStream(is, null, probe);
            }
            if (probe.outWidth <= 0) return null;

            int sample = 1;
            int limit = Math.max(targetPx * 2, 96);
            while (probe.outWidth / sample > limit) sample *= 2;

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = sample;
            try (InputStream is = getAssets().open(name)) {
                return BitmapFactory.decodeStream(is, null, opt);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /** 按目标像素解码，避免大图直接进内存 */
    private Bitmap decodeIcon(String path, int targetPx) {
        try {
            BitmapFactory.Options probe = new BitmapFactory.Options();
            probe.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, probe);
            int sample = 1;
            int limit = Math.max(targetPx * 2, 96);
            while (probe.outWidth / sample > limit) sample *= 2;

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = sample;
            return BitmapFactory.decodeFile(path, opt);
        } catch (Throwable t) {
            return null;
        }
    }

    private Bitmap roundBitmap(Bitmap src, int radiusPx) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF rf = new RectF(0, 0, w, h);
        c.drawRoundRect(rf, radiusPx, radiusPx, pt);
        pt.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        c.drawBitmap(src, 0, 0, pt);
        return out;
    }

    private Drawable iconBg(int color, float radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{withAlpha(color, 0x74), withAlpha(color, 0x22)});
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), withAlpha(color, 0x55));
        return g;
    }

    private Drawable cardBg() {
        GradientDrawable content = new GradientDrawable();
        content.setColor(CARD);
        content.setCornerRadius(dp(16));
        content.setStroke(dp(1), STROKE);

        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(16));

        return new RippleDrawable(ColorStateList.valueOf(0x22FFFFFF), content, mask);
    }

    private Drawable ripple(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x28FFFFFF), content, null);
    }

    private Drawable ripple(Drawable content, int color) {
        return new RippleDrawable(ColorStateList.valueOf(color), content, null);
    }

    private Drawable barDrawable() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.TRANSPARENT);
        GradientDrawable fg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{acc1, acc2});
        fg.setCornerRadius(dp(1));
        ClipDrawable clip = new ClipDrawable(fg, Gravity.LEFT, ClipDrawable.HORIZONTAL);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{bg, clip});
        ld.setId(0, android.R.id.background);
        ld.setId(1, android.R.id.progress);
        return ld;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static int blend(int c1, int c2, float t) {
        int r = (int) (Color.red(c1) * (1 - t) + Color.red(c2) * t);
        int g = (int) (Color.green(c1) * (1 - t) + Color.green(c2) * t);
        int b = (int) (Color.blue(c1) * (1 - t) + Color.blue(c2) * t);
        return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    private static int iconTextColor(int c) {
        float lum = (0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)) / 255f;
        return blend(c, 0xFFFFFFFF, lum < 0.42f ? 0.55f : 0.22f);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    /* ==================== 生命周期 ==================== */

    private void applyImmersive() {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    @Override
    public void onWindowFocusChanged(boolean has) {
        super.onWindowFocusChanged(has);
        if (has) applyImmersive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (pickOpen) {
            finishPick();
            return;
        }
        if (webOpen) {
            if (web != null && web.canGoBack()) {
                web.goBack();
                return;
            }
            closeWeb();
            return;
        }
        if (detailOpen) {
            closeDetail();
            return;
        }
        if (keysOpen) {
            closeKeys();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBack < 1800) {
            super.onBackPressed();
        } else {
            lastBack = now;
            toast("再按一次退出");
        }
    }
}
