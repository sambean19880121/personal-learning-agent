package com.example.learningagent;

import com.example.learningagent.agent.DailyLearningAgent;
import com.example.learningagent.agent.LearningSession;
import com.example.learningagent.agent.EvaluationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.example.learningagent.agent.LearningTrack;

@RestController
@RequestMapping("/api/learning")
public class LearningController {
    private final DailyLearningAgent agent;

    public LearningController(DailyLearningAgent agent) { this.agent = agent; }

    @GetMapping("/today")
    public LearningSession today() { return agent.current(); }

    @PostMapping("/start")
    public LearningSession startNow(@RequestParam(required = false) LearningTrack track) { return track == null ? agent.planToday() : agent.planToday(track); }

    @GetMapping("/start")
    public LearningSession startNowFromBrowser(@RequestParam(required = false) LearningTrack track) { return track == null ? agent.planToday() : agent.planToday(track); }

    @PostMapping("/evaluate")
    public EvaluationResult evaluate(@Valid @RequestBody Answers answers) { return agent.evaluate(answers.answers()); }

    @GetMapping("/evaluation")
    public EvaluationResult evaluation() { return agent.evaluation(); }

    public record Answers(@NotNull List<String> answers) {}
}
