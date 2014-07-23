package com.malsolo.vertx.first.demo;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Created by jbeneito on 22/07/2014.
 */
public class Server extends Verticle {

    public final static int PORT = 8888;

    @Override
    public void start() {

        // load the general config object, loaded by using -config on command line
        JsonObject appConfig = container.config(); //or created programatically and passed to the deployModule method of a PlatformManager

        container.logger().info("Container CONFIG: " + appConfig.encodePrettily());

        JsonObject mongoConfig = appConfig.getObject("mongo-persistor");
        if (mongoConfig == null) {
            container.logger().error("Mongo CONFIG NULL!!! ");
            mongoConfig = new JsonObject();
            mongoConfig.putString("address", "mongodb-persistor");
            mongoConfig.putString("host", "localhost");
            mongoConfig.putNumber("port", 27017);
            mongoConfig.putNumber("pool_size", 10);
            mongoConfig.putString("db_name", "vertx");
        }

        container.logger().info("Mongo CONFIG: " + mongoConfig.encodePrettily());

        // deploy the mongo-persistor module, which we'll use for persistence
        container.deployModule("io.vertx~mod-mongo-persistor~2.1.1", mongoConfig);


        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {

            @Override
            public void handle(final HttpServerRequest httpServerRequest) {
                container.logger().info("A request has arrived on the server!");
                //httpServerRequest.response().end("Hello Vert.x first demo server ");

                // we send the response from the mongo query back to the client.
                // first create the query
                JsonObject matcher = new JsonObject().putString("state", "AL");
                JsonObject json = new JsonObject().putString("collection", "zips")
                        .putString("action", "find")
                        .putObject("matcher", matcher);

                vertx.eventBus().send("mongodb-persistor", json, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        // send the response back, encoded as string
                        httpServerRequest.response().end(message.body().encodePrettily());
                    }
                });

            }
        }).listen(PORT, (asyncResult) -> {
                container.logger().info("Listen succeeded? " + asyncResult.succeeded());
        });

        container.logger().info("Webserver started, listening for MONGO on port: " + PORT);

    }
}
