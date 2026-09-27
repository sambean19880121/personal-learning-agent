package com.example.learningagent;

import com.example.learningagent.agent.KnowledgeCollector;
import com.example.learningagent.agent.SourceArticle;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeCollector collector;
    public KnowledgeController(KnowledgeCollector collector) { this.collector = collector; }
    @PostMapping("/refresh")
    public String refresh() { return "抓取完成，新增 " + collector.refresh() + " 篇文章"; }
    @GetMapping("/latest")
    public List<SourceArticle> latest() { return collector.latest(); }
}
