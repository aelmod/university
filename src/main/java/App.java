import org.h2.jdbcx.JdbcDataSource;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

import static spark.Spark.*;

public class App {
    public static void main(String[] args) throws SQLException {
        port(80);
        JdbcDataSource jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setUrl("jdbc:h2:~/university/ziiks");
        Connection connection = jdbcDataSource.getConnection();
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS topic(id INTEGER AUTO_INCREMENT PRIMARY KEY, title LONGTEXT, text LONGTEXT)");
        //connection.createStatement().execute("INSERT INTO topic VALUES (NULL, 'fraza', 'imya')");
        staticFiles.location("/static");


        get("/", (req, res) -> {
            String search = req.queryParams("search");
            String sql = "SELECT * FROM topic";
            if (search != null) {
                sql += " WHERE lower(title) LIKE lower(?)";
            }
            sql += " ORDER BY cast(SUBSTRING(TRIM(title), 1, LOCATE('.', TRIM(title)) - 1) as INTEGER)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            if (search != null) {
                preparedStatement.setString(1, "%" + search + "%");
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            LinkedList<Topic> topics = new LinkedList<>();
            while (resultSet.next()) {
                topics.add(new Topic(resultSet.getInt("id"), resultSet.getString("title"), resultSet.getString("text")));
                System.out.println(resultSet.getRow());
            }
            HashMap<String, Object> model = new HashMap<>();
            model.put("topics", topics);
            if (search != null) {
                model.put("searchData", search);
            }
            return new ModelAndView(model, "list.vm");
        }, new VelocityTemplateEngine());

        get("/topic/:id", (req, res) -> {
            String id = req.params(":id");
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM topic where id = ?");
            preparedStatement.setString(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            HashMap<String, Object> model = new HashMap<>();
            model.put("topic", new Topic(resultSet.getInt("id"), resultSet.getString("title"), resultSet.getString("text")));

            String viewName = "topic.vm";
            if (req.headers("user-agent").toLowerCase().contains("windows")) {
                viewName = "topic-with-edit.vm";
            }
            return new ModelAndView(model, viewName);
        }, new VelocityTemplateEngine());

        post("/topic/:id", (request, response) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(("UPDATE topic SET text=? WHERE id = ?"));
            preparedStatement.setString(1, request.queryParams("text"));
            preparedStatement.setString(2, request.params(":id"));
            preparedStatement.executeUpdate();
            response.redirect(request.params(":id"));
            return "Successfully edited";
        });

        get("/add", (req, res) -> new ModelAndView(new HashMap<>(), "add.vm"), new VelocityTemplateEngine());

        get("/topics/delete", (req, res) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(("DELETE FROM topic WHERE id in (" + req.queryParams("ids") + ")"));
            preparedStatement.execute();
            return "azaz";
        });

        post("/add", (request, response) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(("INSERT INTO topic VALUES (NULL, ?, ?)"));
            preparedStatement.setString(1, request.queryParams("title"));
            preparedStatement.setString(2, request.queryParams("text"));
            preparedStatement.executeUpdate();
            response.redirect("/");
            return "Successfully added";
        });

        exception(Exception.class, (e, request, response) -> {
            e.printStackTrace();
        });
    }
}
