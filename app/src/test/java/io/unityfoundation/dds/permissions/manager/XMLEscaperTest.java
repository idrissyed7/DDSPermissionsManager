package io.unityfoundation.dds.permissions.manager;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.util.XMLEscaper;
import jakarta.inject.Inject;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class XMLEscaperTest {
    @Inject
    XMLEscaper xmlEscaper;

    @Test
    void testEscapedTopicName() {
        String topicName = "<aa> 'Some\" topic&name </aa>";
        String expected = "&lt;aa&gt; &apos;Some&quot; topic&amp;name &lt;/aa&gt;";
        String actual = xmlEscaper.escape(topicName);
        assertTrue(actual.equals(expected));

        String topicName2 = "Nothing to escape";
        assertTrue(xmlEscaper.escape(topicName2).equals(topicName2));
    }
}
