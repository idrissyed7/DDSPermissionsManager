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
package io.unityfoundation.dds.permissions.manager.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.unityfoundation.dds.permissions.manager.security.PassphraseGenerator;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Produces
@Singleton
@Requires(classes = {DPMException.class, ExceptionHandler.class})
public class DPMExceptionHandler implements ExceptionHandler<DPMException, HttpResponse> {

    private static final Logger LOG = LoggerFactory.getLogger(DPMExceptionHandler.class);

    private final PassphraseGenerator passphraseGenerator;

    public DPMExceptionHandler(PassphraseGenerator passphraseGenerator) {
        this.passphraseGenerator = passphraseGenerator;
    }

    @Override
    public HttpResponse<?> handle(HttpRequest request, DPMException exception) {
        HttpStatus httpStatus = exception.getHttpStatus();
        MutableHttpResponse<?> response;
        if ( httpStatus != null ) {
            response = HttpResponse.status(httpStatus);
        } else {
            response = HttpResponse.badRequest();
        }

        String errorId = passphraseGenerator.generatePassphrase();
        String code = exception.getResponseStatusCode();
        response.body(List.of(new DPMErrorResponse(errorId, code)));

        LOG.error("Id: {} Code: {}. See exception below", errorId, code, exception);
        return response;
    }
}
