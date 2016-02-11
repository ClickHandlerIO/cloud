package io.clickhandler.files.s3.routing;

import io.clickhandler.files.handler.FileStatusHandler;
import io.clickhandler.files.service.FileService;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.RoutingContext;

/**
 * Abstract Vertx route for capturing upload requests notifications.
 *
 * @see io.vertx.rxjava.ext.web.Route
 * @author Brad Behnke
 */
public class FileUploadRouteHandler implements Handler<RoutingContext> {

    private final FileService fileService;

    public FileUploadRouteHandler(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.request().uploadHandler(httpServerFileUpload -> {
            // todo use this or request?
            // todo need file entity.
            fileService.putAsync(null, httpServerFileUpload, new FileStatusHandler() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            });
        });
    }
}
