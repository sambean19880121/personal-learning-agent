package com.example.learningagent;

import com.example.learningagent.agent.DailyLearningAgent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {
    private final DailyLearningAgent agent;
    private final com.example.learningagent.agent.KnowledgeCollector collector;
    public ReminderScheduler(DailyLearningAgent agent, com.example.learningagent.agent.KnowledgeCollector collector) { this.agent = agent; this.collector = collector; }

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Shanghai")
    public void remind() {
        collector.refresh();
        agent.planToday();
        try {
            new ProcessBuilder("osascript", "-e", "display notification \"今晚学习 20 分钟，坚持学习，不要放弃。生活如此美好。\" with title \"个人学习 Agent\"").start();
        } catch (Exception ignored) { }
    }
}
