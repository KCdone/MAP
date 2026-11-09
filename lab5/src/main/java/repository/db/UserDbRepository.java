package repository.db;

import domain.User;
import domain.validators.Validator;
import exceptions.RepositoryException;
import repository.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC repository for users, backed by the {@code users} table of a PostgreSQL database.
 * Every method opens its own connection (try-with-resources), so no connection is kept
 * open between calls. The id is {@code GENERATED ALWAYS AS IDENTITY} in the database:
 * save() never writes the id column, it reads it back from the generated keys and sets
 * it on the entity, so the storage - not the service - decides the id of a new user.
 */
public class UserDbRepository implements Repository<Long, User> {
    private final Validator<User> validator;
    private final String url;
    private final String user;
    private final String password;

    /**
     * @param validator the validation strategy for users
     * @param url       the JDBC url of the database
     * @param user      the database user
     * @param password  the database password
     */
    public UserDbRepository(Validator<User> validator, String url, String user, String password) {
        this.validator = validator;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * {@inheritDoc}
     *
     * @throws RepositoryException if the database cannot be reached
     */
    @Override
    public User findOne(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        String sql = "SELECT id, username, first_name, last_name, email FROM users WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? toUser(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Cannot read the user with id " + id + " from the database: "
                    + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws RepositoryException if the database cannot be reached
     */
    @Override
    public Iterable<User> findAll() {
        String sql = "SELECT id, username, first_name, last_name, email FROM users";
        List<User> result = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(toUser(resultSet));
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Cannot read the users from the database: " + e.getMessage(), e);
        }
    }

    /**
     * Inserts the user; the id column is not written, the database generates it
     * (GENERATED ALWAYS AS IDENTITY) and the generated value is set on the entity.
     * {@inheritDoc}
     *
     * @throws RepositoryException if the database cannot be reached or a constraint is violated
     *                             (for example a duplicate username)
     */
    @Override
    public User save(User entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        validator.validate(entity);
        String sql = "INSERT INTO users (username, first_name, last_name, email) VALUES (?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, entity.getUsername());
            statement.setString(2, entity.getFirstName());
            statement.setString(3, entity.getLastName());
            statement.setString(4, entity.getEmail());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }
            return null;
        } catch (SQLException e) {
            throw new RepositoryException("Cannot save the user: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws RepositoryException if the database cannot be reached
     */
    @Override
    public User delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        String sql = "DELETE FROM users WHERE id = ? RETURNING id, username, first_name, last_name, email";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? toUser(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Cannot delete the user with id " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws RepositoryException if the database cannot be reached or a constraint is violated
     */
    @Override
    public User update(User entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        validator.validate(entity);
        String sql = "UPDATE users SET username = ?, first_name = ?, last_name = ?, email = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entity.getUsername());
            statement.setString(2, entity.getFirstName());
            statement.setString(3, entity.getLastName());
            statement.setString(4, entity.getEmail());
            statement.setLong(5, entity.getId());
            int rowsUpdated = statement.executeUpdate();
            return rowsUpdated == 0 ? entity : null;
        } catch (SQLException e) {
            throw new RepositoryException("Cannot update the user with id " + entity.getId() + ": "
                    + e.getMessage(), e);
        }
    }

    private User toUser(ResultSet resultSet) throws SQLException {
        return new User(resultSet.getLong("id"), resultSet.getString("username"),
                resultSet.getString("first_name"), resultSet.getString("last_name"),
                resultSet.getString("email"));
    }
}
