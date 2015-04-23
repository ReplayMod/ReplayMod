package eu.crushedpixel.replaymod.reflection;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.LineProcessor;
import net.minecraft.launchwrapper.Launch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * <p>A helper class for working with obfuscated field names.</p>
 * <p>In the development environment the mappings file will automatically loaded. You can provide the location of a custom mappings file by
 * providing the system property {@code sevencommons.mappingsFile}.</p>
 *
 * @author diesieben07
 * @author CrushedPixel
 */
public final class MCPNames {

    private static final Map<String, String> fields;
    private static final Map<String, String> methods;

    static {
        if(use()) {
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
     *
     * @return true if the code is running in development mode (use MCP instead of SRG names)
     */
    public static boolean use() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }

    /**
     * <p>Get the correct name for the given SRG field based on the context.</p>
     *
     * @param srg the SRG name for a field
     * @return the input if the code is running outside of development mode or the matching MCP name otherwise
     */
    public static String field(String srg) {
        if(use()) {
            String mcp = fields.get(srg);
            if(mcp == null) {
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
     *
     * @param srg the SRG name for a method
     * @return the input if the code is running outside of development mode or the matching MCP name otherwise
     */
    public static String method(String srg) {
        if(use()) {
            String mcp = methods.get(srg);
            if(mcp == null) {
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
        } catch(IOException e) {
            throw new RuntimeException("Could not read SRG->MCP mappings", e);
        }
    }

    private static class MCPFileParser implements LineProcessor<Map<String, String>> {

        private static final Splitter splitter = Splitter.on(',').trimResults();
        private final Map<String, String> map = Maps.newHashMap();
        private boolean foundFirst;

        @Override
        public boolean processLine(String line) throws IOException {
            if(!foundFirst) {
                foundFirst = true;
                return true;
            }

            Iterator<String> splitted = splitter.split(line).iterator();
            try {
                String srg = splitted.next();
                String mcp = splitted.next();
                if(!map.containsKey(srg)) {
                    map.put(srg, mcp);
                }
            } catch(NoSuchElementException e) {
                throw new IOException("Invalid Mappings file!", e);
            }

            return true;
        }

        @Override
        public Map<String, String> getResult() {
            return ImmutableMap.copyOf(map);
        }
    }

}