package io.unityfoundation.dds.permissions.manager.model.topic;

public enum TopicKind {
//    A, // Unrestricted writers, unrestricted readers
    B, // Restricted writers, unrestricted readers
    C; // Restricted writers, restricted readers
//    D; // Unrestricted writers, restricted readers
}
