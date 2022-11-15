package io.unityfoundation.dds.permissions.manager.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Produces
@Singleton
@Requires(classes = {DPMException.class, ExceptionHandler.class})
public class DPMExceptionHandler implements ExceptionHandler<DPMException, HttpResponse> {

    private static final Logger LOG = LoggerFactory.getLogger(DPMExceptionHandler.class);

    @Override
    public HttpResponse<?> handle(HttpRequest request, DPMException exception) {
        HttpStatus httpStatus = exception.getHttpStatus();
        MutableHttpResponse<?> response;
        if ( httpStatus != null ) {
            response = HttpResponse.status(httpStatus);
        } else {
            response = HttpResponse.badRequest();
        }

        String code = exception.getResponseStatusCode();
        LOG.error("Code: " + code + ". Violation: " + exception.getMessage());

        return response.body(List.of(new DPMErrorResponse(code)));
    }
}
