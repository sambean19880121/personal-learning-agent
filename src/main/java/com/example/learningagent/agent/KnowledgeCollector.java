package com.example.learningagent.agent;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URI;
import java.net.http.*;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class KnowledgeCollector {
    private final JdbcTemplate jdbc;
    private final HttpClient http = HttpClient.newHttpClient();
    private final List<String[]> feeds = List.of(
            new String[]{"Google AI Blog", "https://blog.google/technology/ai/rss/"},
            new String[]{"GitHub Blog", "https://github.blog/feed/"},
            new String[]{"Inside Java", "https://inside.java/feed.xml"},
            new String[]{"Spring Blog", "https://spring.io/blog.atom"},
            new String[]{"OpenJDK", "https://openjdk.org/jeps/jeps.rss"},
            new String[]{"Baeldung", "https://feeds.feedburner.com/Baeldung"},
            new String[]{"Redis Blog", "https://redis.io/blog/feed/"},
            new String[]{"Confluent Kafka Blog", "https://www.confluent.io/blog/feed/"},
            new String[]{"Apache RocketMQ", "https://rocketmq.apache.org/rss.xml"},
            new String[]{"Apache Kafka", "https://kafka.apache.org/blog"},
            new String[]{"Martin Fowler", "https://martinfowler.com/feed.atom"},
            new String[]{"InfoQ", "https://feed.infoq.com/"},
            new String[]{"Hugging Face", "https://huggingface.co/blog/feed.xml"},
            new String[]{"OpenAI News", "https://openai.com/news/rss.xml"},
            new String[]{"Google AI", "https://blog.research.google/feeds/posts/default/-/AI"},
            new String[]{"DeepSeek", "https://api-docs.deepseek.com/updates/"}
    );
    public KnowledgeCollector(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        try { jdbc.execute("alter table source_articles add column summary TEXT"); }
        catch (Exception ignored) { }
    }

    public int refresh() {
        int saved = 0;
        for (String[] feed : feeds) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(feed[1])).GET().build();
                String xml = http.send(request, HttpResponse.BodyHandlers.ofString()).body();
                var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes()));
                var items = document.getElementsByTagName("item");
                for (int i = 0; i < Math.min(items.getLength(), 10); i++) {
                    Element item = (Element) items.item(i);
                    String title = text(item, "title"), url = text(item, "link"), published = text(item, "pubDate");
                    if (url.isBlank()) continue;
                    String summary = cleanSummary(text(item, "description"));
                    saved += jdbc.update("insert or ignore into source_articles(source,title,url,published_at,summary,fetched_at) values(?,?,?,?,?,?)", feed[0], title, url, published, summary, OffsetDateTime.now().toString());
                    if (!summary.isBlank()) jdbc.update("update source_articles set summary=? where url=? and (summary is null or summary='')", summary, url);
                }
            } catch (Exception ignored) { }
        }
        return saved;
    }

    public List<SourceArticle> latest() {
        return jdbc.query("select source,title,url,published_at,summary from source_articles order by fetched_at desc limit 20", (rs, n) -> article(rs));
    }

    public List<SourceArticle> candidatesFor(LearningTrack track, List<String> recentTitles) {
        List<SourceArticle> articles = track == LearningTrack.REDIS
                ? jdbc.query("select source,title,url,published_at,summary from source_articles where source='Redis Blog' order by fetched_at desc limit 30", (rs, n) -> article(rs))
                : latest();
        List<SourceArticle> unused = articles.stream().filter(article -> !recentTitles.contains(article.title())).toList();
        return unused.isEmpty() ? articles : unused;
    }

    private String text(Element parent, String tag) {
        var nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private SourceArticle article(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SourceArticle(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
    }

    private String cleanSummary(String html) {
        return html.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replace("&amp;", "&")
                .replaceAll("\\s+", " ").trim();
    }
}
