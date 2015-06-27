package eu.crushedpixel.replaymod.utils;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class StringUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static String[] splitStringInMultipleRows(String string, int maxWidth) {
        if(string == null) return new String[0];
        List<String> rows = new ArrayList<String>();
        String remaining = string;
        while(remaining.length() > 0) {
            String[] split = remaining.split(" ");
            String b = "";
            for(String sp : split) {
                b += sp + " ";
                if(mc.fontRendererObj.getStringWidth(b.trim()) > maxWidth) {
                    b = b.substring(0, b.trim().length() - (sp.length()));
                    break;
                }
            }
            String trimmed = b.trim();
            rows.add(trimmed);
            try {
                remaining = remaining.substring(trimmed.length() + 1);
            } catch(Exception e) {
                break;
            }
        }

        return rows.toArray(new String[rows.size()]);
    }

    /**
     * Slightly modified from apache commons exec. Licensed under the Apache License, Version 2.0
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Crack a command line.
     *
     * @param toProcess
     *            the command line to process
     * @return the command line broken into strings. An empty or null toProcess
     *         parameter results in a zero sized array
     */
    public static List<String> translateCommandline(final String toProcess) {
        if (toProcess == null || toProcess.length() == 0) {
            // no command? no string
            return Collections.emptyList();
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        final ArrayList<String> list = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            final String nextTok = tok.nextToken();
            switch (state) {
            case inQuote:
                if ("\'".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            case inDoubleQuote:
                if ("\"".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            default:
                if ("\'".equals(nextTok)) {
                    state = inQuote;
                } else if ("\"".equals(nextTok)) {
                    state = inDoubleQuote;
                } else if (" ".equals(nextTok)) {
                    if (lastTokenHasBeenQuoted || current.length() != 0) {
                        list.add(current.toString());
                        current = new StringBuilder();
                    }
                } else {
                    current.append(nextTok);
                }
                lastTokenHasBeenQuoted = false;
                break;
            }
        }

        if (lastTokenHasBeenQuoted || current.length() != 0) {
            list.add(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new IllegalArgumentException("Unbalanced quotes in "
                    + toProcess);
        }

        return list;
    }
}
