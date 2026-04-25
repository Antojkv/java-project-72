package hexlet.code;

import io.javalin.Javalin;

public class App {

    public static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
        });

        app.get("/", ctx -> ctx.result("Hello World"));
        return app;
    }

    private static String getDatabaseUrl() {
        return System.getenv().getOrDefault("DATABASE_URL", "jdbc:h2:mem:project");
    }

    public static void main(String[] args) {
        String portStr = System.getenv().getOrDefault("PORT", "7070");
        int port = Integer.valueOf(portStr);

        Javalin app = getApp();
        app.start(port);
    }
}
