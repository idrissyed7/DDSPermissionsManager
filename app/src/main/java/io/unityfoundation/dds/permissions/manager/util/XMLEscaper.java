// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.util;

import jakarta.inject.Singleton;

@Singleton
public class XMLEscaper {
    public String escape(String input) {
        // Five characters in XML text that need escaping: <, >, ', ", &
        // Replace & first because it appears in other subsitutions.
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("'", "&apos;")
            .replace("\"", "&quot;") ;
    }
}
