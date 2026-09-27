package com.example.learningagent.agent;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DailyLearningAgent {
    private final AtomicReference<LearningSession> current = new AtomicReference<>();
    private final LearningSessionRepository repository;
    private final DeepSeekClient deepSeek;
    private final KnowledgeCollector collector;
    private int completedSessions;

    public DailyLearningAgent(LearningSessionRepository repository, DeepSeekClient deepSeek, KnowledgeCollector collector) { this.repository = repository; this.deepSeek = deepSeek; this.collector = collector; }

    public LearningSession planToday() {
        return planToday(null);
    }

    public LearningSession planToday(LearningTrack requestedTrack) {
        LearningSession saved = requestedTrack == null ? repository.findByDate(LocalDate.now()) : null;
        if (saved != null) { current.set(saved); return saved; }
        LearningTrack track = requestedTrack == null ? repository.nextTrack() : requestedTrack;
        collector.refresh();
        LearningSession generated = deepSeek.generate(track, collector.latest());
        if (generated != null) { current.set(generated); repository.save(generated); return generated; }
        LearningSession session = switch (track) {
            case JAVA -> new LearningSession(LocalDate.now(), track, "Java 基础：集合与不可变性",
                    "今天理解 List、Set、Map 的基本选择，以及为什么不可变对象更容易推理和并发使用。请结合一个实际例子思考。",
                    questions("List、Set、Map 分别适合什么场景？", "解释不可变对象的一个好处。", "设计一个方法，如何避免把内部可变集合直接暴露出去？"), false, null);
            case COMPUTER_SCIENCE -> new LearningSession(LocalDate.now(), track, "计算机基础：进程、线程与共享状态",
                    "今天学习进程和线程的区别，以及多个线程访问共享数据时为什么需要同步。重点关注状态、竞态和可见性。",
                    questions("进程和线程的主要区别是什么？", "什么是竞态条件？", "给出一个避免共享可变状态的办法。"), false, null);
            case AI -> new LearningSession(LocalDate.now(), track, "AI 基础：Agent 的感知、决策与行动",
                    "Agent 不是一次性回答，而是围绕目标持续执行感知、判断、行动和记忆。今天用一个学习助手的例子拆解这条闭环。",
                    questions("Agent 和普通聊天问答有什么区别？", "Agent 为什么需要记忆？", "为学习助手设计一个工具调用场景。"), false, null);
            case JAVA_COLLECTIONS -> fallback(track, "Java 集合与泛型：选择正确的数据结构");
            case JAVA_CONCURRENCY -> fallback(track, "Java 并发：线程安全与共享状态");
            case JVM -> fallback(track, "JVM：内存区域与垃圾回收");
            case SPRING -> fallback(track, "Spring：依赖注入与应用边界");
            case DATA_STRUCTURES -> fallback(track, "数据结构：复杂度与基本结构");
            case OPERATING_SYSTEMS -> fallback(track, "操作系统：进程、线程与内存");
            case COMPUTER_NETWORKS -> fallback(track, "计算机网络：HTTP 与 TCP");
            case ARCHITECTURE -> fallback(track, "系统架构：从需求到可演进设计");
            case DATABASE -> fallback(track, "数据库：索引、事务与一致性");
            case DISTRIBUTED_SYSTEMS -> fallback(track, "分布式系统：一致性与故障");
            case REDIS -> fallback(track, "Redis：数据结构与缓存策略");
            case KAFKA -> fallback(track, "Kafka：分区、消费组与消息语义");
            case ROCKETMQ -> fallback(track, "RocketMQ：可靠消息与消费重试");
            case MACHINE_LEARNING -> fallback(track, "机器学习：训练、验证与泛化");
            case LLM -> fallback(track, "大语言模型：上下文、Token 与生成");
            case RAG -> fallback(track, "RAG：检索、上下文与答案 grounding");
            case AI_AGENT -> fallback(track, "AI Agent：工具、状态与行动闭环");
            case AI_TOOLS -> fallback(track, "AI 工具：模型、工具调用与工作流");
            case MODEL_UPDATES -> fallback(track, "最新模型：能力、限制与评测");
        };
        current.set(session);
        repository.save(session);
        return session;
    }

    private LearningSession fallback(LearningTrack track, String title) {
        return new LearningSession(LocalDate.now(), track, title,
                "今天围绕「" + title + "」学习一个核心概念，理解它解决的问题、基本原理和实际使用方式，并思考它在真实项目中的边界。",
                questions("这个主题解决什么问题？", "它的核心原理是什么？", "请结合一个工程场景说明如何使用。"), false, null);
    }

    public LearningSession current() {
        LearningSession session = current.get();
        return session == null ? planToday() : session;
    }

    public EvaluationResult evaluate(List<String> answers) {
        LearningSession session = current();
        List<EvaluationResult.QuestionResult> details = deepSeek.grade(session, answers);
        String scoringMethod = "AI 按题意和参考要点批改；总分为各题得分的平均值。";
        if (details == null) {
            scoringMethod = "AI 批改暂不可用；以下为按回答长度计算的临时分数，不代表内容正确性。";
            details = new ArrayList<>();
            for (int i = 0; i < session.questions().size(); i++) {
                String answer = i < answers.size() && answers.get(i) != null ? answers.get(i) : "";
                int questionScore = answer.trim().length() >= 8 ? 100 : 0;
                String feedback = questionScore == 0 ? "回答少于 8 个字符，临时计 0 分。请补充观点、理由和例子。" : "回答达到 8 个字符，临时计 100 分；内容尚未经过 AI 评估。";
                String expected = session.questions().get(i).expectedPoints();
                String reference = expected == null || expected.isBlank() ? "参考文章：" + session.article() : expected;
                details.add(new EvaluationResult.QuestionResult(answer, questionScore, feedback, reference));
            }
        }
        int score = (int) Math.round(details.stream().mapToInt(EvaluationResult.QuestionResult::score).average().orElse(0));
        LearningSession result = new LearningSession(session.date(), session.track(), session.title(), session.article(), session.questions(), true, score);
        current.set(result);
        repository.save(result);
        repository.saveAnswers(result, details, scoringMethod);
        completedSessions++;
        return new EvaluationResult(result, details, scoringMethod);
    }

    public EvaluationResult evaluation() {
        LearningSession session = current();
        return session.completed() ? repository.findEvaluation(session) : null;
    }

    private List<LearningSession.Question> questions(String... prompts) {
        List<LearningSession.Question> result = new ArrayList<>();
        for (String prompt : prompts) result.add(new LearningSession.Question(prompt, "回答应包含定义、理由或实际例子。"));
        return result;
    }
}
