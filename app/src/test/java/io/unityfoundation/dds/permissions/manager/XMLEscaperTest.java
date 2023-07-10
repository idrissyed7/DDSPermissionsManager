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
        String expected = "\\<aa\\> \\'Some\\\" topic\\&name \\</aa\\>";
        assertTrue(xmlEscaper.escape(topicName).equals(expected));

        String topicName2 = "Nothing to escape";
        assertTrue(xmlEscaper.escape(topicName2).equals(topicName2));
    }

    @Test
    void testEscapedSubjectName() {
        HashMap<String, String> oidMap = new HashMap<>();
        oidMap.put("2.5.4.4", "SN");
        oidMap.put("2.5.4.42", "GN");

        String subject = buildSubject("68_nonce", "<appname> 'Some\"DPDApp&lication </appname>", "63");
        String expected = "CN=68_nonce,GIVENNAME=\\<appname\\> \\'Some\\\"DPDApp\\&lication \\</appname\\>,SURNAME=63";
        assertTrue(xmlEscaper.escapeX500Name(subject).equals(expected));

        String subject2 = buildSubject("68_nonce", "Nothing to escape", "63");
        assertTrue(xmlEscaper.escapeX500Name(subject2).equals(subject2));
    }

    private String buildSubject(String cn, String givenname, String surname) {
        X500NameBuilder nameBuilder = new X500NameBuilder();
        nameBuilder.addRDN(BCStyle.CN, cn);
        nameBuilder.addRDN(BCStyle.GIVENNAME, givenname);
        nameBuilder.addRDN(BCStyle.SURNAME, surname);
        return nameBuilder.build().toString();
    }
}
