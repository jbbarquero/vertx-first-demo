package com.malsolo.vertx.first.demo;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.platform.Verticle;

/**
 * Created by jbeneito on 22/07/2014.
 */
public class Server extends Verticle {

    public final static int PORT = 8888;

    @Override
    public void start() {

        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest httpServerRequest) {
                container.logger().debug("A request has arrived on the server!");
                httpServerRequest.response().end("Hello Vert.x first demo server ");
            }
        }).listen(PORT, new AsyncResultHandler<HttpServer>() {
            @Override
            public void handle(AsyncResult<HttpServer> asyncResult) {
                container.logger().info("Listen succeeded? " + asyncResult.succeeded());
            }
        });

        container.logger().info("Webserver started, listening on port: " + PORT);

    }
}
