package com.example.learningagent.agent;

import java.util.List;

public record EvaluationResult(LearningSession session, List<QuestionResult> questions, String scoringMethod) {
    public record QuestionResult(String answer, int score, String feedback, String referenceAnswer) {}
}
