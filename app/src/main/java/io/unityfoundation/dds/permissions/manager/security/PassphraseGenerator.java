// Copyright 2023 DDS Permissions Manager Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;

import java.security.SecureRandom;

@Singleton
@ConfigurationProperties("permissions-manager")
public class PassphraseGenerator {

    @Property(name = "permissions-manager.application.passphrase.length")
    protected Integer passphraseLength;

    public String generatePassphrase(){
        int length = 16;
        if (passphraseLength != null) {
            length = passphraseLength;
        }

        // see https://www.baeldung.com/java-random-string#java8-alphanumeric
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        SecureRandom random = new SecureRandom();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
