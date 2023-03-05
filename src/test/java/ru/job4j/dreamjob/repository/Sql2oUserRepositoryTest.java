package ru.job4j.dreamjob.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.job4j.dreamjob.configuration.DatasourceConfiguration;
import ru.job4j.dreamjob.model.User;

import java.util.List;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class Sql2oUserRepositoryTest {
    private static Sql2oUserRepository sql2oUserRepository;

    @BeforeAll
    public static void initRepositories() throws Exception {
        var properties = new Properties();
        try (var inputStream = Sql2oUserRepositoryTest.class.getClassLoader()
                .getResourceAsStream("connection.properties")) {
            properties.load(inputStream);
        }
        var url = properties.getProperty("datasource.url");
        var username = properties.getProperty("datasource.username");
        var password = properties.getProperty("datasource.password");

        var configuration = new DatasourceConfiguration();
        var datasource = configuration.connectionPool(url, username, password);
        var sql2o = configuration.databaseClient(datasource);

        sql2oUserRepository = new Sql2oUserRepository(sql2o);
    }

    @AfterEach
    public void clearUsers() {
        var users = sql2oUserRepository.findAll();
        for (var user : users) {
            sql2oUserRepository.deleteByEmail(user.getEmail());
        }
    }

    @Test
    void whenSaveThenGetSame() {
        var user1 = sql2oUserRepository.save(
                new User(0, "user1@mail.ru", "user1", "password1")).get();
        var savedUser = sql2oUserRepository
                .findByEmailAndPassword(user1.getEmail(), user1.getPassword()).get();
        assertThat(user1).usingRecursiveComparison().isEqualTo(savedUser);
    }

    @Test
    void whenSaveSeveralThenGetAll() {
        var user1 = sql2oUserRepository.save(
                new User(0, "user1@mail.ru", "user1", "password1")).get();
        var user2 = sql2oUserRepository.save(
                new User(0, "user2@mail.ru", "user2", "password2")).get();
        var user3 = sql2oUserRepository.save(
                new User(0, "user3@mail.ru", "user3", "password3")).get();
        var result = sql2oUserRepository.findAll();
        assertThat(result).usingRecursiveComparison().isEqualTo(List.of(user1, user2, user3));
    }

    @Test
    public void whenDontSaveThenNothingFound() {
        assertThat(sql2oUserRepository.findAll()).isEqualTo(emptyList());
        assertThat(sql2oUserRepository.findByEmailAndPassword("user1@mail.ru", "user1"))
                .isEqualTo(empty());
    }

    @Test
    public void whenDeleteThenGetEmptyOptional() {
        var user1 = sql2oUserRepository.save(
                new User(0, "user1@mail.ru", "user1", "password1")).get();
        var user2 = sql2oUserRepository.save(
                new User(0, "user2@mail.ru", "user2", "password2")).get();
        var user3 = sql2oUserRepository.save(
                new User(0, "user3@mail.ru", "user3", "password3")).get();

        var isDeleted = sql2oUserRepository.deleteByEmail("user1@mail.ru");
        var savedVacancy = sql2oUserRepository.findByEmailAndPassword("user1@mail.ru", "user1");
        assertThat(isDeleted).isTrue();
        assertThat(savedVacancy).isEqualTo(empty());
    }

    @Test
    public void whenDeleteByInvalidEmailThenGetFalse() {
        assertThat(sql2oUserRepository.deleteByEmail("user111@mail.ru")).isFalse();
    }

    @Test
    public void whenAddSecondUserWithEmailOfFirstOneThenFalse() {
        sql2oUserRepository.save(new User(0, "user1@mail.ru", "user1", "password1"));
        var user = new User(0, "user1@mail.ru", "user2", "password2");
        assertThat(sql2oUserRepository.save(user)).isEqualTo(empty());
    }
}