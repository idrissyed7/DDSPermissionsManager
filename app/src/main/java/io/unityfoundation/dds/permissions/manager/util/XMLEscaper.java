package io.unityfoundation.dds.permissions.manager.util;

import jakarta.inject.Singleton;

import java.util.HashMap;

@Singleton
public class XMLEscaper {
    private HashMap<String, String> cache = new HashMap<String, String>();

    public String escape(String input) {
        if (cache.containsKey(input)) {
            return cache.get(input);
        }
        // Five characters in XML text that need escaping: <, >, ', ", &
        String result = input.replace("<", "\\<")
                .replace(">", "\\>")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("&", "\\&");
        cache.put(input, result);
        return result;
    }

    // Names resulting from X500NameBuilder already have characters
    // '<', '>', and '"' escaped. Only to escape ''' and '&' here.
    public String escapeX500Name(String input) {
        if (cache.containsKey(input)) {
            return cache.get(input);
        }
        String result = input.replace("'", "\\'")
                .replace("&", "\\&");
        cache.put(input, result);
        return result;
    }
}
