package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import hexlet.code.controllers.RootController;
import hexlet.code.controllers.UrlsController;
import hexlet.code.repository.BaseRepository;
import hexlet.code.util.NamedRoutes;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;

import java.io.IOException;
import java.sql.SQLException;

public class App {

    public static int getPort() {
       String port = System.getenv().getOrDefault("PORT", "7070");
        return Integer.parseInt(port);
    }

    public static String getUrl() {
        String url = System.getenv().getOrDefault("JDBC_DATABASE_URL", "jdbc:h2:mem:project;DB_CLOSE_DELAY=-1");
        return url;
    }

    //объект TemplateEngine, который используется для рендеринга HTML-шаблонов.
    //ClassLoader используется для загрузки классов и ресурсов.
    // ResourceCodeResolver отвечает за нахождение и загрузку шаблонов из указанного местоположения
    //TemplateEngine, который будет использовать codeResolver для нахождения шаблонов и генерировать HTML-контент
    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        return templateEngine;
    }

    public static Javalin getApp() throws SQLException, IOException {
        String url = getUrl();
        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);

        var dataSource = new HikariDataSource(hikariConfig);
        BaseRepository.dataSource = dataSource;


        var app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(createTemplateEngine()));
        });

        app.get(NamedRoutes.rootPath(), RootController::index);
        app.post(NamedRoutes.urlsPath(),UrlsController::create);
        app.get(NamedRoutes.urlsPath(), UrlsController::showAllUrls);
        app.get(NamedRoutes.urlsPathWithChecks(), UrlsController::showAllUrlsWithChecks);
        app.get(NamedRoutes.urlPath("{id}"), UrlsController::showUrl);
        app.get(NamedRoutes.urlCheckPath("{id}"), UrlsController::checkUrl);
        return app;
    }

    public static void main(String[] args) throws SQLException, IOException {
            var app = getApp();
            app.start(getPort());
    }
}
