package io.unityfoundation.dds.permissions.manager.i18n;

import io.micronaut.context.MessageSource;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.i18n.ResourceBundleMessageSource;
import io.micronaut.runtime.context.CompositeMessageSource;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.stream.Collectors;

@Factory
class MessageSourceFactory {

    @Singleton
    MessageSource createMessageSource(@Property(name = "i18n.bundles") List<String> ids) {
        List<MessageSource> resourceBundleMessageSources = ids.stream().map(id -> new ResourceBundleMessageSource("i18n." + id)).collect(Collectors.toList());
        return new CompositeMessageSource(resourceBundleMessageSources);
    }
}
