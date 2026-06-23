package org.example.net;

import com.sun.net.httpserver.HttpServer;
import org.example.auth.JwtAuthenticator;
import org.example.auth.JwtService;
import org.example.auth.UserService;
import org.example.domain.ProductService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class StoreServerHTTP {
    private final HttpServer server;

    public StoreServerHTTP(int port, ProductService productService, UserService userService, JwtService jwtService) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/login", new LoginHandler(userService, jwtService));

        JwtAuthenticator authenticator = new JwtAuthenticator(jwtService);
        var productCtx = server.createContext("/products", new ProductHandler(productService));
        productCtx.setAuthenticator(authenticator);

        server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
        System.out.println("[HTTP-Server] Listening on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        StoreServerHTTP server = new StoreServerHTTP(port, new ProductService(), new UserService(), new JwtService());
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        Thread.currentThread().join();
    }
}
