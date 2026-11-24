package io.github.muxinzi.aihub;

/**
 * 一条 API Key 备忘。
 * name / purpose / key 三者都可以为空 —— 允许先记个名字、key 以后再补。
 */
public class KeyEntry {

    public String id;
    public String name;
    public String purpose;
    public String key;

    public KeyEntry() {
    }

    public KeyEntry(String id, String name, String purpose, String key) {
        this.id = id;
        this.name = name;
        this.purpose = purpose;
        this.key = key;
    }

    public boolean isBlank() {
        return trimToEmpty(name).isEmpty()
                && trimToEmpty(purpose).isEmpty()
                && trimToEmpty(key).isEmpty();
    }

    public String displayName() {
        String n = trimToEmpty(name);
        return n.isEmpty() ? "未命名" : n;
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
