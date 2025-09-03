package com.runescape;

public final class UserPreferences {
    public static final UserPreferences INSTANCE = new UserPreferences();
    private PreferencesData data = new PreferencesData();

    private UserPreferences() {}

    public static PreferencesData get() { return INSTANCE.data; }

    public void load(Object client) { /* no-op stub */ }

    public void save() { /* no-op stub */ }
}
