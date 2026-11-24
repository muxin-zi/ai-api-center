package io.github.muxinzi.aihub;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * API Key 本地存储（每个平台可存多条备忘）。
 *
 * 主路径：Android KeyStore 里生成一把不可导出的 AES-256 密钥，用 AES/GCM 加密后存进
 * SharedPreferences，密文格式 v1:Base64(iv):Base64(ciphertext)。
 * 明文是 JSON 数组：[{id,name,purpose,key}, ...]
 *
 * 兜底：少数机器 KeyStore 不可用（或密钥被系统清掉）时，退化为异或混淆 + Base64。
 * 这条路径只是混淆，不是加密，仅在无法使用 KeyStore 时启用。
 */
public class KeyVault {

    private static final String PREF = "aihub_keys";
    private static final String ALIAS = "aihub_master_v1";

    private final Context ctx;
    private final SharedPreferences sp;

    public KeyVault(Context c) {
        ctx = c.getApplicationContext();
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /* ---------------- 读 ---------------- */

    public List<KeyEntry> list(String pid) {
        List<KeyEntry> out = new ArrayList<>();
        String raw = read(pid);
        if (raw == null || raw.isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                KeyEntry e = new KeyEntry();
                e.id = o.optString("id", "");
                e.name = o.optString("name", "");
                e.purpose = o.optString("purpose", "");
                e.key = o.optString("key", "");
                if (e.id == null || e.id.isEmpty()) e.id = UUID.randomUUID().toString();
                out.add(e);
            }
        } catch (Exception ignore) {
            // 老版本是直接存一个 key 字符串，迁移成一条备忘
            KeyEntry e = new KeyEntry(UUID.randomUUID().toString(), "默认", "", raw);
            out.add(e);
            write(pid, out);
        }
        return out;
    }

    public int countOf(String pid) {
        return list(pid).size();
    }

    public int countAll() {
        int n = 0;
        for (String k : sp.getAll().keySet()) n += list(k).size();
        return n;
    }

    public int countWithKey(String pid) {
        int n = 0;
        for (KeyEntry e : list(pid)) {
            if (e.key != null && !e.key.trim().isEmpty()) n++;
        }
        return n;
    }

    /* ---------------- 写 ---------------- */

    /** 新增或按 id 覆盖 */
    public void put(String pid, KeyEntry entry) {
        if (entry == null) return;
        if (entry.id == null || entry.id.isEmpty()) entry.id = UUID.randomUUID().toString();
        List<KeyEntry> l = list(pid);
        boolean replaced = false;
        for (int i = 0; i < l.size(); i++) {
            if (entry.id.equals(l.get(i).id)) {
                l.set(i, entry);
                replaced = true;
                break;
            }
        }
        if (!replaced) l.add(entry);
        write(pid, l);
    }

    public void remove(String pid, String entryId) {
        List<KeyEntry> l = list(pid);
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id.equals(entryId)) {
                l.remove(i);
                break;
            }
        }
        write(pid, l);
    }

    private void write(String pid, List<KeyEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            sp.edit().remove(pid).apply();
            return;
        }
        JSONArray arr = new JSONArray();
        for (KeyEntry e : entries) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", e.id == null ? "" : e.id);
                o.put("name", e.name == null ? "" : e.name);
                o.put("purpose", e.purpose == null ? "" : e.purpose);
                o.put("key", e.key == null ? "" : e.key);
                arr.put(o);
            } catch (Exception ignored) {
            }
        }
        encryptInto(pid, arr.toString());
    }

    /* ---------------- 加解密 ---------------- */

    private void encryptInto(String pid, String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKey());
            byte[] iv = cipher.getIV();
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            String s = "v1:" + Base64.encodeToString(iv, Base64.NO_WRAP)
                    + ":" + Base64.encodeToString(ct, Base64.NO_WRAP);
            sp.edit().putString(pid, s).apply();
        } catch (Throwable t) {
            sp.edit().putString(pid,
                    "f1:" + Base64.encodeToString(xor(plain.getBytes(StandardCharsets.UTF_8)),
                            Base64.NO_WRAP)).apply();
        }
    }

    private String read(String pid) {
        String s = sp.getString(pid, null);
        if (s == null || s.isEmpty()) return null;
        try {
            if (s.startsWith("v1:")) {
                String[] p = s.substring(3).split(":");
                if (p.length != 2) return null;
                byte[] iv = Base64.decode(p[0], Base64.NO_WRAP);
                byte[] ct = Base64.decode(p[1], Base64.NO_WRAP);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, masterKey(), new GCMParameterSpec(128, iv));
                return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
            }
            if (s.startsWith("f1:")) {
                byte[] raw = Base64.decode(s.substring(3), Base64.NO_WRAP);
                return new String(xor(raw), StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }

    /* ---------------- 内部 ---------------- */

    private SecretKey masterKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(ALIAS)) {
            java.security.Key k = ks.getKey(ALIAS, null);
            if (k instanceof SecretKey) return (SecretKey) k;
        }
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return kg.generateKey();
    }

    private static final byte[] SALT = new byte[]{
            0x3C, 0x51, 0x7A, 0x19, (byte) 0xA4, 0x62, (byte) 0xD8, 0x0F,
            0x75, (byte) 0xBE, 0x2C, 0x43, (byte) 0x91, 0x08, 0x6D, (byte) 0xE7
    };

    private static byte[] xor(byte[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (byte) (in[i] ^ SALT[i % SALT.length] ^ (i * 31));
        }
        return out;
    }
}
