package com.example.learningagent.agent;

import java.time.LocalDate;
import java.util.List;

public record LearningSession(
        LocalDate date,
        LearningTrack track,
        String title,
        String article,
        List<Question> questions,
        boolean completed,
        Integer score,
        String source,
        String sourceUrl) {
    public record Question(String prompt, String expectedPoints) {}
}
