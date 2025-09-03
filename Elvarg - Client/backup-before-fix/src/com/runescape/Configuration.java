package com.runescape;

import java.io.File;

/**
 * The main configuration for the Client
 * 
 * @author Seven
 */
public final class Configuration {
	
	private Configuration() {
		
	}

	/**
	 * Sends client-related debug messages to the client output stream
	 */
	public static boolean client_debug = true;

	/**
	 * The address of the server that the client will be connecting to
	 */
	public static String server_address = "127.0.0.1"; // TODO: set to your server IP or domain

	public static final String CACHE_DIRECTORY = System.getProperty("user.home") + File.separator + "Cache/";

	/**
	 * The port of the server that the client will be connecting to
	 */
	public static int server_port = 43595;

	/**
	 * The JAGGRAB port
	 */
	public static final int JAGGRAB_PORT = 43596;

	/**
	 * The on-demand/file-server port
	 */
	public static final int FILE_SERVER_PORT = 43597;

	/**
	 * Toggles a security feature called RSA to prevent packet sniffers
	 */
	public static final boolean ENABLE_RSA = true;

	/**
	 * The url that the users will get redirected to after clicking "New User"
	 */
	public static final String REGISTER_ACCOUNT = "www.google.com";

	/**
	 * The url that the users will get redirected to after clicking "Forgotten your password?"
	 */
	public static final String FORGOT_PASSWORD = "www.google.com";

	/**
	 * New Cursors
	 */
	public static boolean hdCursors = true;

	/**
	 * Toggles "World Switch" UI; when disabled, the login screen hard-sets localhost.
	 * Keep this TRUE so the client uses the configured server_address/server_port.
	 */
	public static boolean worldSwitch = true;

	/**
	 * Toggles the ability for a player to see roofs in-game
	 */
	public static boolean enableRoofs = true;

	/**
	 * Enables extra frames in-between animations to give the animation a smooth look
	 */
	public static boolean smoothAnimations = true;

	/**
	 * Toggles jagcached for cache file streaming
	 */
	public static boolean JAGCACHED_ENABLED = true;

	/**
	 * Toggles tweening for animations
	 */
	public static boolean enableTweening = true;

	/**
	 * Enables the Music Player
	 */
	public static boolean enableMusic = false;

	/**
	 * Enables the ability for the game to use the "497" gameframe
	 */
	public static boolean gameframe474 = false;

	/**
	 * Enables the ability for the game to use the "562" gameframe
	 */
	public static boolean gameframe562 = false;

	/**
	 * Enables the ability for the game to use the "OSRS" gameframe
	 */
	public static boolean gameframeOSRS = true;

	/**
	 * Enables the old hitbar
	 */
	public static boolean oldHitbar = false;

	/**
	 * Enables the 10x hitpoints
	 */
	public static boolean hpBar10X = true;

	/**
	 * Enables the 554 hitbar
	 */
	public static boolean hpBar554 = false;

	/**
	 * Enables the HUD to display 10 X the amount of hitpoints
	 */
	public static boolean tenXHp = false;
	
	/**
	 * Should it be snow in the game? White floor.
	 */
	public static boolean snow = false;

}
