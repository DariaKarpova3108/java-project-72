package hexlet.code.repository;

import hexlet.code.model.Checks;
import hexlet.code.model.Url;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class CheckRepository extends BaseRepository {
    public static void saveCheckedUrl(Checks urlCheck) throws SQLException, IOException {
        String sql = "INSERT INTO checks (status_code, title, h1, description, url_id, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (var conn = dataSource.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, urlCheck.getStatus());
            pst.setString(2, urlCheck.getTitle());
            pst.setString(3, urlCheck.getH1());
            pst.setString(4, urlCheck.getDescription());
            pst.setLong(5, urlCheck.getUrl_id());
            pst.setTimestamp(6, urlCheck.getCreated_at());
            pst.executeUpdate();
            ResultSet generatedKeys = pst.getGeneratedKeys();
            if (generatedKeys.next()) {
                urlCheck.setId(generatedKeys.getLong(1));
            } else {
                throw new SQLException("DB have not returned an id after saving an entity");
            }
        }
    }

    //добавить метод показать проверки
    public static Optional<Checks> getLastCheck(long id) throws SQLException {
        String sql = "SELECT * FROM checks WHERE id = ? " +
                "ORDER BY created_at DESC LIMIT 1";
        try (var conn = dataSource.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setLong(1, id);
            ResultSet resultSet = pst.executeQuery();
            if (resultSet.next()) {
                long urlId = resultSet.getLong("url_id");
                var status = resultSet.getInt("status_code");
                var title = resultSet.getString("title");
                var h1 = resultSet.getString("h1");
                var description = resultSet.getString("description");
                var createdAt = resultSet.getTimestamp("created_at");
                var urlCheck = new Checks(status, title, h1, description, createdAt);
                urlCheck.setUrl_id(urlId);
                return Optional.of(urlCheck);
            }
        }
        return Optional.empty();
    }

    public static Map<Url, Checks> getListLastCheck() throws SQLException {
        String sql = "SELECT * FROM checks " +
                "INNER JOIN urls ON checks.url_id = urls.id" +
                " ORDER BY checks.created_at DESC LIMIT 1";
        Map<Url, Checks> listChecks = new LinkedHashMap<>();
        try (var conn = dataSource.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            ResultSet resultSet = pst.executeQuery();
            while (resultSet.next()) {
                var id = resultSet.getLong("urls.id");
                var name = resultSet.getString("urls.name");
                var date = resultSet.getTimestamp("urls.created_at");
                var urlModel = new Url(name, date);
                urlModel.setId(id);

                var idCheck = resultSet.getLong("checks.id");
                var urlId = resultSet.getLong("checks.url_id");
                var status = resultSet.getInt("checks.status_code");
                var title = resultSet.getString("checks.title");
                var h1 = resultSet.getString("checks.h1");
                var description = resultSet.getString("checks.description");
                var createdAt = resultSet.getTimestamp("checks.created_at");
                var urlCheck = new Checks(status, title, h1, description, createdAt);
                urlCheck.setUrl_id(urlId);
                urlCheck.setId(idCheck);
                listChecks.put(urlModel, urlCheck);
                return listChecks;
            }
        }
        return listChecks;
    }

    public static Checks parsingURL(String urlModel) throws IOException {
        Document document = Jsoup.connect(urlModel).get();
        Elements titleElement = document.select("head > title");
        Elements h1Element = document.select("h1");
        Elements descriptionMeta = document.select("meta[name=description]");

        URI uri = URI.create(urlModel);
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int statusCode = conn.getResponseCode();
        conn.disconnect();
        Timestamp date = new Timestamp(System.currentTimeMillis());

        String title = titleElement.text();
        String h1 = h1Element.text();
        String description = descriptionMeta.text(); //разобраться почему не работает вставка description
        return new Checks(statusCode, title, h1, description, date);
    }
}
