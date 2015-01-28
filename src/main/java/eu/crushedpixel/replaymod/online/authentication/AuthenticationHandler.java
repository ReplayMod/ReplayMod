package eu.crushedpixel.replaymod.online.authentication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.client.ApiException;
import eu.crushedpixel.replaymod.reflection.MCPNames;

public class AuthenticationHandler {

	public static final int SUCCESS = 1;
	public static final int INVALID = 2;
	public static final int NO_CONNECTION = 3;
	
	private static Minecraft mc = Minecraft.getMinecraft();

	private static String authkey = null;

	public static boolean isAuthenticated() {
		return authkey != null;
	}

	public static int authenticate(String username, String password) {
		try {
			authkey = ReplayMod.apiClient.getLogin(username, password).getAuthkey();
			return SUCCESS;
		} catch(ApiException e) {
			return INVALID;
		} catch(Exception e) {
			return NO_CONNECTION;
		}
	}

	private static final List<String> PREMIUM_USERS = new ArrayList<String>() {
		{
			add("Ender_Workbench");
			add("oleoleMC");
			add("Johni0702");
			add("Rafessor");
			add("bluffamachuck");
			add("Panguino");
			add("SixteenBy16");
		}
	};
	
	private static boolean isPremiumUsername(String username) {
		//TODO: API check with the website
		return (PREMIUM_USERS.contains(username) || MCPNames.env.isMCPEnvironment());
	}

	private static boolean isPremiumUUID(String uuid) {
		//TODO: API check with the website
		return false;
	}
}
