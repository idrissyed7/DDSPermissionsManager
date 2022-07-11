package io.unityfoundation.dds.permissions.manager.i18n;

import io.micronaut.context.MessageSource;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.i18n.ResourceBundleMessageSource;
import io.micronaut.runtime.context.CompositeMessageSource;
import jakarta.inject.Singleton;

import java.util.Arrays;

@Factory
class MessageSourceFactory {
    @Singleton
    MessageSource createMessageSource() {
        return new CompositeMessageSource(Arrays.asList(new ResourceBundleMessageSource("i18n.messages"),
                new ResourceBundleMessageSource("i18n.groups"),
                new ResourceBundleMessageSource("i18n.footer"),
                new ResourceBundleMessageSource("i18n.home"),
                new ResourceBundleMessageSource("i18n.navigation"),
                new ResourceBundleMessageSource("i18n.users")
        ));
    }
}
