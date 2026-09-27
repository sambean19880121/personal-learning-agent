package com.example.learningagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekClient {
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiKey;

    public DeepSeekClient(ObjectMapper mapper, @Value("${deepseek.api-key:}") String apiKey) {
        this.mapper = mapper;
        this.apiKey = apiKey;
    }

    public List<EvaluationResult.QuestionResult> grade(LearningSession session, List<String> answers) {
        if (apiKey == null || apiKey.isBlank()) return null;
        try {
            StringBuilder prompt = new StringBuilder("请根据文章逐题批改。返回 JSON 对象：{\"questions\":[{\"score\":0到100的整数,\"feedback\":\"指出答对的要点、缺失的要点及扣分原因\",\"referenceAnswer\":\"针对该题的完整中文参考答案\"}]}。每题必须有独立结果，空答案为0分。不要返回 Markdown。\n文章：").append(session.article()).append("\n");
            for (int i = 0; i < session.questions().size(); i++) {
                String answer = i < answers.size() ? answers.get(i) : "";
                var question = session.questions().get(i);
                prompt.append("题目").append(i + 1).append("：").append(question.prompt()).append("\n参考要点：").append(question.expectedPoints()).append("\n回答：").append(answer).append("\n");
            }
            Map<String, Object> body = Map.of("model", "deepseek-flash", "messages", List.of(
                    Map.of("role", "system", "content", "你是一位耐心、严格、鼓励坚持的中文老师。"),
                    Map.of("role", "user", "content", prompt.toString())), "temperature", 0.2, "response_format", Map.of("type", "json_object"));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.deepseek.com/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) return null;
            JsonNode root = mapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null) return null;
            JsonNode graded = mapper.readTree(content).path("questions");
            if (!graded.isArray() || graded.size() != session.questions().size()) return null;
            List<EvaluationResult.QuestionResult> results = new java.util.ArrayList<>();
            for (int i = 0; i < graded.size(); i++) {
                JsonNode item = graded.get(i);
                if (!item.path("score").isInt() || item.path("feedback").asText().isBlank() || item.path("referenceAnswer").asText().isBlank()) return null;
                int score = item.path("score").asInt();
                if (score < 0 || score > 100) return null;
                results.add(new EvaluationResult.QuestionResult(i < answers.size() ? answers.get(i) : "", score, item.path("feedback").asText(), item.path("referenceAnswer").asText()));
            }
            return results;
        } catch (Exception ignored) { return null; }
    }

    public LearningSession generate(LearningTrack track, List<SourceArticle> articles) {
        if (apiKey == null || apiKey.isBlank() || articles.isEmpty()) return null;
        try {
            StringBuilder sources = new StringBuilder();
            for (SourceArticle a : articles) {
                sources.append("标题：").append(a.title()).append("\n来源：").append(a.source()).append("\n链接：").append(a.url());
                if (a.summary() != null && !a.summary().isBlank()) sources.append("\n原文摘要：").append(a.summary(), 0, Math.min(a.summary().length(), 1200));
                sources.append("\n\n");
            }
            String prompt = "你是个人学习 Agent。请围绕学习方向 " + track + "，从候选文章中选择最适合今天学习的一篇。title 必须原样复制所选文章标题，内容必须紧扣该文章。返回 JSON：{title, article, questions:[{prompt, expectedPoints}]}。article 用中文写约 800 到 1200 字，分段说明背景、核心机制、一个具体工程例子、适用条件与限制、如何验证效果。以原文摘要为事实依据；摘要没有的版本细节、命令和性能数字不要编造，可明确说需要查证。questions 必须正好 3 道，分别考察理解、应用、思考；expectedPoints 要具体。不要返回 Markdown。\n候选文章：\n" + sources;
            Map<String, Object> body = Map.of("model", "deepseek-flash", "messages", List.of(Map.of("role", "system", "content", "你是一位循序渐进的技术老师。"), Map.of("role", "user", "content", prompt)), "temperature", 0.4, "response_format", Map.of("type", "json_object"));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.deepseek.com/chat/completions")).header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) return null;
            String content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content").asText(null);
            if (content == null) return null;
            JsonNode json = mapper.readTree(content);
            String title = json.path("title").asText();
            if (articles.stream().noneMatch(article -> article.title().equals(title))) return null;
            List<LearningSession.Question> questions = new java.util.ArrayList<>();
            json.path("questions").forEach(q -> questions.add(new LearningSession.Question(q.path("prompt").asText(), q.path("expectedPoints").asText())));
            if (questions.size() != 3) return null;
            return new LearningSession(java.time.LocalDate.now(), track, title, json.path("article").asText(), questions, false, null);
        } catch (Exception ignored) { return null; }
    }
}
