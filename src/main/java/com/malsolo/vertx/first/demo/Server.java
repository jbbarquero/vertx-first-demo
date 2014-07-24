package com.malsolo.vertx.first.demo;

import org.vertx.java.core.*;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
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

        container.logger().info("Mongo CONFIG: " + (mongoConfig !=  null ? mongoConfig.encodePrettily() : "NONE!!!")); //Mainly for maven tests

        // deploy the mongo-persistor module, which we'll use for persistence
        container.deployModule("io.vertx~mod-mongo-persistor~2.1.1", mongoConfig);

        // setup Routematcher
        RouteMatcher routeMatcher = new RouteMatcher();

        // the matcher for the complete list and the search
        routeMatcher.get("/zips", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {

                container.logger().info("Requested ZIPS to the server!");

                JsonObject json = new JsonObject();
                MultiMap params = req.params();

                if (params.size() > 0 && params.contains("state") || params.contains("city")) {
                    // create the matcher configuration
                    JsonObject matcher = new JsonObject();
                    if (params.contains("state")) matcher.putString("state", params.get("state"));
                    if (params.contains("city")) matcher.putString("city", params.get("city"));

                    // create the message for the mongo-persistor verticle
                    json = new JsonObject().putString("collection", "zips")
                            .putString("action", "find")
                            .putObject("matcher", matcher);

                } else {
                    // create the query
                    json = new JsonObject().putString("collection", "zips")
                            .putString("action", "find")
                            .putObject("matcher", new JsonObject());
                }

                JsonObject data = new JsonObject();
                data.putArray("results", new JsonArray());
                // and call the event we want to use
                vertx.eventBus().send("mongodb-persistor", json, new ReplyHandler(req, data));
            }
        });

        // the matcher for a specific id
        routeMatcher.get("/zips/:id", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
                String idToRetrieve = req.params().get("id");

                // create the query
                JsonObject matcher = new JsonObject().putString("_id", idToRetrieve);
                JsonObject json = new JsonObject().putString("collection", "zips")
                        .putString("action", "find")
                        .putObject("matcher", matcher);

                // and call the event we want to use
                vertx.eventBus().send("mongodb-persistor", json, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event) {
                        req.response().putHeader("Content-Type", "application/json");
                        if (event.body().getArray("results").size() > 0) {
                            JsonObject result = event.body().getArray("results").get(0);
                            req.response().end(result.encodePrettily());
                        }
                    }
                });
            }
        });

        // the matcher for the update
        routeMatcher.post("/zips/:id", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {

                // process the body
                req.bodyHandler(new Handler<Buffer>() {

                    @Override
                    public void handle(Buffer event) {
                        // normally we'd validate the input, for now just assume it is correct.
                        final String body = event.getString(0,event.length());

                        // create the query
                        JsonObject newObject = new JsonObject(body);
                        JsonObject matcher = new JsonObject().putString("_id", req.params().get("id"));
                        JsonObject json = new JsonObject().putString("collection", "zips")
                                .putString("action", "update")
                                .putObject("criteria", matcher)
                                .putBoolean("upsert", false)
                                .putBoolean("multi",false)
                                .putObject("objNew",newObject);

                        // and call the event we want to use
                        vertx.eventBus().send("mongodb-persistor", json, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> event) {
                                // we could handle the errors here, but for now
                                // assume everything went ok, and return the original
                                // and updated json
                                req.response().end(body);
                            }
                        });
                    }
                });
            }
        });

        vertx.createHttpServer().requestHandler(routeMatcher).listen(PORT, (asyncResult) -> {
                container.logger().info("Listen succeeded? " + asyncResult.succeeded());
        });

        container.logger().info("Webserver started, listening for MONGO on port: " + PORT);

    }

    /**
     * Simple handler that can be used to handle the reply from mongodb-persistor
     * and handles the 'more-exist' field.
     */
    private class ReplyHandler implements Handler<Message<JsonObject>> {

        private final HttpServerRequest request;
        private JsonObject data;

        private ReplyHandler(final HttpServerRequest request, JsonObject data) {
            this.request = request;
            this.data = data;
        }

        @Override
        public void handle(Message<JsonObject> event) {
            // if the response contains more message, we need to get the rest
            if (event.body().getString("status").equals("more-exist")) {
                JsonArray results = event.body().getArray("results");

                for (Object el : results) {
                    data.getArray("results").add(el);
                }

                event.reply(new JsonObject(), new ReplyHandler(request, data));
            } else {

                JsonArray results = event.body().getArray("results");
                for (Object el : results) {
                    data.getArray("results").add(el);
                }

                request.response().putHeader("Content-Type", "application/json");
                request.response().end(data.encodePrettily());
            }
        }
    }
}
