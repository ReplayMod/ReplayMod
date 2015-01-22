package eu.crushedpixel.replaymod.authentication;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import eu.crushedpixel.replaymod.reflection.MCPNames;

public class AuthenticationHandler {

	private static Minecraft mc = Minecraft.getMinecraft();

	private static boolean auth = false;

	public static boolean isAuthenticated() {
		return auth;
	}

	public static void authenticate() {
		auth = isPremiumUsername(Minecraft.getMinecraft().getSession().getUsername());
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
