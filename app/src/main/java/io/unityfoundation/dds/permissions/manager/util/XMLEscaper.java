package io.unityfoundation.dds.permissions.manager.util;

import jakarta.inject.Singleton;

@Singleton
public class XMLEscaper {
    public String escape(String input) {
        // Five characters in XML text that need escaping: <, >, ', ", &
        return input.replace("<", "\\<")
                .replace(">", "\\>")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("&", "\\&");
    }

    // Names resulting from X500NameBuilder already have characters
    // '<', '>', and '"' escaped. Only to escape ''' and '&' here.
    public String escapeX500Name(String input) {
        return input.replace("'", "\\'")
                .replace("&", "\\&");
    }
}
