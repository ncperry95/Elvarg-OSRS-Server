package com.runescape.loginscreen;

public enum LoginBackground {
    NORMAL(0),
    ANIMATED_GAME_WORLD(1),
    FADING_BACKGROUNDS(2);

    private final int spriteID;
    LoginBackground(int spriteID) { this.spriteID = spriteID; }
    public int getSpriteID() { return spriteID; }
}
