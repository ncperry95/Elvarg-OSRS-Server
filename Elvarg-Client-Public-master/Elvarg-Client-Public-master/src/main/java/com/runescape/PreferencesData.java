package com.runescape;

import com.runescape.loginscreen.LoginBackground;

// Make it assignable to the Client's RS return type
public class PreferencesData implements net.runelite.rs.api.RSClientPreferences {
    private boolean combatOverlayBox = true;
    private boolean mergeExpDrops = false;
    private boolean changeChatArea = false;
    private boolean stackSideStones = false;
    private boolean enableShiftClickDrop = true;
    private boolean enableMusic = false;
    private boolean hpAboveHeads = false;
    private boolean enableOrbs = true;
    private boolean enableSpecOrb = true;
    private boolean eulaAccepted = true;
    private boolean rememberUsername = false;
    private boolean escapeCloseInterface = false;

    // Newly required toggles
    private boolean enableRoofs = true;
    private boolean enableTooltipHovers = true;
    private boolean enableBuffOverlay = false;
    private boolean transparentTabArea = false;
    private boolean enableSkillOrbs = false;
    private boolean enableGroundItemNames = false;

    private String  savedUsername = "";
    private int npcAttackOptionPriority = 0;
    private int playerAttackOptionPriority = 0;
    private double brightnessState = 0.8;
    private LoginBackground loginBackground = LoginBackground.NORMAL;

    // ---- RuneLite Preferences bridge state ----
    private boolean hideUsername = false;
    private int soundEffectVolume = 127;      // 0..127
    private int areaSoundEffectVolume = 127;  // 0..127
    // -------------------------------------------

    public boolean getCombatOverlayBox() { return combatOverlayBox; }
    public void setCombatOverlayBox(boolean v) { combatOverlayBox = v; }

    public boolean getMergeExpDrops() { return mergeExpDrops; }
    public void setMergeExpDrops(boolean v) { mergeExpDrops = v; }

    public boolean getChangeChatArea() { return changeChatArea; }
    public void setChangeChatArea(boolean v) { changeChatArea = v; }

    public boolean getStackSideStones() { return stackSideStones; }
    public void setStackSideStones(boolean v) { stackSideStones = v; }

    public boolean getEnableShiftClickDrop() { return enableShiftClickDrop; }
    public void setEnableShiftClickDrop(boolean v) { enableShiftClickDrop = v; }

    public boolean getEnableMusic() { return enableMusic; }
    public void setEnableMusic(boolean v) { enableMusic = v; }

    public boolean getHpAboveHeads() { return hpAboveHeads; }
    public void setHpAboveHeads(boolean v) { hpAboveHeads = v; }

    public boolean getEnableOrbs() { return enableOrbs; }
    public void setEnableOrbs(boolean v) { enableOrbs = v; }

    public boolean getEnableSpecOrb() { return enableSpecOrb; }
    public void setEnableSpecOrb(boolean v) { enableSpecOrb = v; }

    public boolean getEulaAccepted() { return eulaAccepted; }
    public void setEulaAccepted(boolean v) { eulaAccepted = v; }

    public boolean getRememberUsername() { return rememberUsername; }
    public void setRememberUsername(boolean v) { rememberUsername = v; }

    public String getSavedUsername() { return savedUsername == null ? "" : savedUsername; }
    public void setSavedUsername(String v) { savedUsername = v == null ? "" : v; }

    public int getNpcAttackOptionPriority() { return npcAttackOptionPriority; }
    public void setNpcAttackOptionPriority(int v) { npcAttackOptionPriority = v; }

    public int getPlayerAttackOptionPriority() { return playerAttackOptionPriority; }
    public void setPlayerAttackOptionPriority(int v) { playerAttackOptionPriority = v; }

    public double getBrightnessState() { return brightnessState; }
    public void setBrightnessState(double v) { brightnessState = v; }

    public LoginBackground getLoginBackground() { return loginBackground; }
    public void setLoginBackground(LoginBackground v) { loginBackground = v == null ? LoginBackground.NORMAL : v; }

    public boolean getEscapeCloseInterface() { return escapeCloseInterface; }
    public void setEscapeCloseInterface(boolean v) { escapeCloseInterface = v; }

    // Newly required getters/setters
    public boolean getEnableRoofs() { return enableRoofs; }
    public void setEnableRoofs(boolean v) { enableRoofs = v; }

    public boolean getEnableTooltipHovers() { return enableTooltipHovers; }
    public void setEnableTooltipHovers(boolean v) { enableTooltipHovers = v; }

    public boolean getEnableBuffOverlay() { return enableBuffOverlay; }
    public void setEnableBuffOverlay(boolean v) { enableBuffOverlay = v; }

    public boolean getTransparentTabArea() { return transparentTabArea; }
    public void setTransparentTabArea(boolean v) { transparentTabArea = v; }

    public boolean getEnableSkillOrbs() { return enableSkillOrbs; }
    public void setEnableSkillOrbs(boolean v) { enableSkillOrbs = v; }

    public boolean getEnableGroundItemNames() { return enableGroundItemNames; }
    public void setEnableGroundItemNames(boolean v) { enableGroundItemNames = v; }

    // --- RuneLite Preferences interface impl ---
    @Override public int getSoundEffectVolume() { return soundEffectVolume; }
    @Override public void setSoundEffectVolume(int volume) {
        soundEffectVolume = Math.max(0, Math.min(127, volume));
    }

    @Override public int getAreaSoundEffectVolume() { return areaSoundEffectVolume; }
    @Override public void setAreaSoundEffectVolume(int volume) {
        areaSoundEffectVolume = Math.max(0, Math.min(127, volume));
    }

    @Override public boolean getHideUsername() { return hideUsername; }
    public void setHideUsername(boolean hide) { hideUsername = hide; } // convenience

    @Override public String getRememberedUsername() { return getSavedUsername(); }
    @Override public void setRememberedUsername(String username) { setSavedUsername(username); }
    // --- end bridge ---
}
