package com.example.learningagent.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningSessionRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void preservesSourceWhenSavingAndGrading() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:sqlite:" + tempDir.resolve("sessions.db")));
        jdbc.execute("create table source_articles(source text, title text, url text, fetched_at text)");
        jdbc.execute("create table learning_sessions(id integer primary key, session_date text unique, track text, title text, article text, completed integer, score integer, created_at text)");
        jdbc.execute("create table learning_questions(session_id integer, question_order integer, prompt text, expected_points text)");
        var repository = new LearningSessionRepository(jdbc);
        var date = LocalDate.of(2026, 9, 28);
        var session = new LearningSession(date, LearningTrack.REDIS, "Redis update", "Article", List.of(new LearningSession.Question("Why?", "Reason")), false, null, "Redis Blog", "https://redis.io/blog/example/");

        repository.save(session);
        assertEquals("Redis Blog", repository.findByDate(date).source());
        assertEquals(session.sourceUrl(), repository.findByDate(date).sourceUrl());

        repository.save(new LearningSession(date, session.track(), session.title(), session.article(), session.questions(), true, 90, session.source(), session.sourceUrl()));
        assertEquals(session.sourceUrl(), repository.findByDate(date).sourceUrl());
    }
}
