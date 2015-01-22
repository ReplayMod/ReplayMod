package eu.crushedpixel.replaymod.reflection;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * <p>A helper class for working with obfuscated field names.</p>
 * <p>In the development environment the mappings file will automatically loaded. You can provide the location of a custom mappings file by
 * providing the system property {@code sevencommons.mappingsFile}.</p>
 * @author diesieben07
 * @author CrushedPixel
 */
public final class MCPNames {

	private static final Map<String, String> fields;
	private static final Map<String, String> methods;
	public static final MCPEnvironment env = new MCPEnvironment();

	static {
		if (use()) {
			String mappingsDir = "./../build/unpacked/mappings/";
			ResourceLocation fieldsLocation = new ResourceLocation("assets/replaymod/fields.csv");
			ResourceLocation methodsLocation = new ResourceLocation("assets/replaymod/methods.csv");
			
			InputStream fieldsIs = MCPNames.class.getClassLoader().getResourceAsStream("fields.csv");
			InputStream methodsIs = MCPNames.class.getClassLoader().getResourceAsStream("methods.csv");
			
			fields = readMappings(fieldsIs);
			methods = readMappings(methodsIs);

		} else {
			methods = fields = null;
		}
	}

	/**
	 * <p>Whether the code is running in a development environment or not.</p>
	 * @return true if the code is running in development mode (use MCP instead of SRG names)
	 */
	public static boolean use() {
		return env.isMCPEnvironment();
	}

	/**
	 * <p>Get the correct name for the given SRG field based on the context.</p>
	 * @param srg the SRG name for a field
	 * @return the input if the code is running outside of development mode or the matching MCP name otherwise
	 */
	public static String field(String srg) {
		if (use()) {
			String mcp = fields.get(srg);
			if (mcp == null) {
				// no mapping
				return srg;
			}
			return mcp;
		} else {
			return srg;
		}
	}

	/**
	 * <p>Get the correct name for the given SRG method based on the context.</p>
	 * @param srg the SRG name for a method
	 * @return the input if the code is running outside of development mode or the matching MCP name otherwise
	 */
	public static String method(String srg) {
		if (use()) {
			String mcp = methods.get(srg);
			if (mcp == null) {
				// no mapping
				return srg;
			}
			return mcp;
		} else {
			return srg;
		}
	}

	private static Map<String, String> readMappings(InputStream is) {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			
			MCPFileParser fileParser = new MCPFileParser();
			 
			while(br.ready()) {
				fileParser.processLine(br.readLine());
			}
			
			return fileParser.getResult();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't read SRG->MCP mappings", e);
		}
	}

	private static class MCPFileParser implements LineProcessor<Map<String, String>> {

		private static final Splitter splitter = Splitter.on(',').trimResults();
		private final Map<String, String> map = Maps.newHashMap();
		private boolean foundFirst;

		@Override
		public boolean processLine(String line) throws IOException {
			if (!foundFirst) {
				foundFirst = true;
				return true;
			}

			Iterator<String> splitted = splitter.split(line).iterator();
			try {
				String srg = splitted.next();
				String mcp = splitted.next();
				if (!map.containsKey(srg)) {
					map.put(srg, mcp);
				}
			} catch (NoSuchElementException e) {
				throw new IOException("Invalid Mappings file!", e);
			}

			return true;
		}

		@Override
		public Map<String, String> getResult() {
			return ImmutableMap.copyOf(map);
		}
	}

	public static final String M_SPAWN_BABY = "func_75388_i";

	public static final String F_TARGET_MATE = "field_75391_e";

	public static final String F_THE_ANIMAL = "field_75390_d";

	public static final String M_CLONE_PLAYER = "func_71049_a";

	public static final String M_CONVERT_TO_VILLAGER = "func_82232_p";

	public static final String M_SET_WORLD_AND_RESOLUTION = "func_73872_a";

	public static final String F_BUTTON_LIST = "field_73887_h";

	public static final String F_TAG_LIST = "field_74747_a";

	public static final String F_TAG_MAP = "field_74784_a";

	public static final String F_FOV_MODIFIER_HAND_PREV = "field_78506_S";

	public static final String F_FOV_MODIFIER_HAND = "field_78507_R";

	public static final String F_TRACKED_ENTITY_IDS = "field_72794_c";

	public static final String F_MAP_TEXTURE_OBJECTS = "field_110585_a";

	public static final String F_MY_ENTITY = "field_73132_a";

	public static final String M_TRY_START_WATCHING_THIS = "func_73117_b";

	public static final String M_ON_UPDATE = "func_70071_h_";

	public static final String M_UPDATE_ENTITY = "func_70316_g";

	public static final String M_DETECT_AND_SEND_CHANGES = "func_75142_b";

	public static final String F_IS_REMOTE = "field_72995_K";

	public static final String F_WORLD_OBJ_TILEENTITY = "field_70331_k";

	public static final String F_WORLD_OBJ_ENTITY = "field_70170_p";

	public static final String F_TIMER = "field_71428_T";

	public static final String F_PACKET_CLASS_TO_ID_MAP = "field_73291_a";

	public static final String M_SEND_PACKET_TO_PLAYER = "func_72567_b";

	public static final String M_REMOVE_ENTITY = "func_72900_e";

	public static final String M_WRITE_ENTITY_TO_NBT = "func_70014_b";

	public static final String M_READ_ENTITY_FROM_NBT = "func_70037_a";

	public static final String M_WRITE_TO_NBT_TILEENTITY = "func_70310_b";

	public static final String M_READ_FROM_NBT_TILEENTITY = "func_70307_a";

	public static final String F_ITEM_DAMAGE = "field_77991_e";

	public static final String M_REGISTER_EXT_PROPS = "registerExtendedProperties";

	public static final String M_READ_PACKET_DATA = "func_73267_a";

	public static final String M_WRITE_PACKET_DATA = "func_73273_a";

	public static final String M_GET_PACKET_SIZE = "func_73284_a";

	public static final String F_UNLOCALIZED_NAME_BLOCK = "field_71968_b";

	public static final String M_SET_HAS_SUBTYPES = "func_77627_a";

	public static final String F_ICON_STRING = "field_111218_cA";

	public static final String F_UNLOCALIZED_NAME_ITEM = "field_77774_bZ";

	public static final String F_TEXTURE_NAME_BLOCK = "field_111026_f";

	public static final String M_ACTION_PERFORMED = "func_73875_a";

	public static final String F_Z_LEVEL = "field_73735_i";

	public static final String M_ADD_SLOT_TO_CONTAINER = "func_75146_a";

	public static final String M_MERGE_ITEM_STACK = "func_75135_a";

	public static final String F_CRAFTERS = "field_75149_d";

	public static final String M_GET_ICON_STRING = "func_111208_A";

	public static final String M_GET_TEXTURE_NAME = "func_111023_E";

	public static final String M_NBT_WRITE = "func_74734_a";

	public static final String M_NBT_LOAD = "func_74735_a";

	public static final String F_NBT_STRING_DATA = "field_74751_a";
	public static final String F_NBT_BYTE_DATA = "field_74756_a";
	public static final String F_NBT_SHORT_DATA = "field_74752_a";
	public static final String F_NBT_INT_DATA = "field_74748_a";
	public static final String F_NBT_LONG_DATA = "field_74753_a";
	public static final String F_NBT_FLOAT_DATA = "field_74750_a";
	public static final String F_NBT_DOUBLE_DATA = "field_74755_a";

	public static final String M_SET_TAG = "func_74782_a";

	public static final String M_NBT_GET_ID = "func_74732_a";

	public static final String M_ITEMSTACK_WRITE_NBT = "func_77955_b";
	public static final String M_LOAD_ITEMSTACK_FROM_NBT = "func_77949_a";

	public static final String M_ADD_CRAFTING_TO_CRAFTERS = "func_75132_a";

	public static final String M_CHECK_HOTBAR_KEYS = "func_82319_a";

	public static final String M_HANDLE_MOUSE_CLICK = "func_74191_a";

	public static final String F_GUICONTAINER_THE_SLOT = "field_82320_o";

	private MCPNames() { }

}