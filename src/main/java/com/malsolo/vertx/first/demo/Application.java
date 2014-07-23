package com.malsolo.vertx.first.demo;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.platform.Verticle;

/**
 * Created by jbeneito on 22/07/2014.
 */
public class Application extends Verticle {

    @Override
    public void start() {
        super.start();
        container.logger().debug("Application: Deploying PingVerticle...");
        container.deployVerticle("com.malsolo.vertx.first.demo.PingVerticle");
        container.logger().info("Application: PingVerticle deployed");
        container.logger().debug("Application: Deploying Server...");
        //container.deployVerticle("com.malsolo.vertx.first.demo.Server");
        container.deployVerticle("com.malsolo.vertx.first.demo.Server", new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> deployResult) {
                if (deployResult.succeeded()) {
                    System.out.println("The verticle has been deployed, deployment ID is " + deployResult.result());
                } else {
                    deployResult.cause().printStackTrace();
                }
            }
        });
        container.logger().info("Application: Server deployed");
    }
}
