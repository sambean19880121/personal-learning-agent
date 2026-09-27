package com.example.learningagent.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeCollectorTest {
    @TempDir Path tempDir;

    @Test
    void redisCandidatesExcludeRecentlyUsedTitleAndOtherTopics() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:sqlite:" + tempDir.resolve("articles.db")));
        jdbc.execute("create table source_articles(source text, title text, url text, published_at text, fetched_at text)");
        jdbc.update("insert into source_articles values(?,?,?,?,?)", "Redis Blog", "The official FastAPI Redis SDK is now available", "redis-1", "", "2026-09-27T09:00:00");
        jdbc.update("insert into source_articles values(?,?,?,?,?)", "Redis Blog", "Announcing Redis 8.10", "redis-2", "", "2026-09-27T08:00:00");
        jdbc.update("insert into source_articles values(?,?,?,?,?)", "GitHub Blog", "Unrelated article", "other", "", "2026-09-27T10:00:00");

        var candidates = new KnowledgeCollector(jdbc).candidatesFor(LearningTrack.REDIS, List.of("The official FastAPI Redis SDK is now available"));

        assertEquals(List.of("Announcing Redis 8.10"), candidates.stream().map(SourceArticle::title).toList());
    }
}
