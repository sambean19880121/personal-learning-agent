package com.example.learningagent.agent;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class LearningSessionRepository {
    private final JdbcTemplate jdbc;
    public LearningSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        addColumnIfMissing("answer TEXT");
        addColumnIfMissing("score INTEGER");
        addColumnIfMissing("feedback TEXT");
        addColumnIfMissing("reference_answer TEXT");
        try { jdbc.execute("alter table learning_sessions add column scoring_method TEXT"); }
        catch (Exception ignored) { }
    }

    private void addColumnIfMissing(String definition) {
        try { jdbc.execute("alter table learning_questions add column " + definition); }
        catch (Exception ignored) { }
    }

    public LearningSession findByDate(LocalDate date) {
        List<LearningSession> found = jdbc.query("select session_date, track, title, article, completed, score from learning_sessions where session_date = ?", (rs, n) -> {
            List<LearningSession.Question> questions = jdbc.query("select prompt, expected_points from learning_questions q join learning_sessions s on q.session_id=s.id where s.session_date=? order by question_order", (qr, qn) -> new LearningSession.Question(qr.getString(1), qr.getString(2)), date.toString());
            Integer score = (Integer) rs.getObject("score");
            return new LearningSession(LocalDate.parse(rs.getString(1)), LearningTrack.valueOf(rs.getString(2)), rs.getString(3), rs.getString(4), questions, rs.getBoolean(5), score);
        }, date.toString());
        return found.isEmpty() ? null : found.getFirst();
    }

    public LearningTrack nextTrack() {
        List<String> tracks = jdbc.query("select track from learning_sessions group by track order by count(*) asc, coalesce(avg(score), 0) asc limit 1", (rs, n) -> rs.getString(1));
        return tracks.isEmpty() ? LearningTrack.JAVA : LearningTrack.valueOf(tracks.getFirst());
    }

    public void save(LearningSession session) {
        jdbc.update("insert into learning_sessions(session_date,track,title,article,completed,score,created_at) values(?,?,?,?,?,?,?) on conflict(session_date) do update set track=excluded.track,title=excluded.title,article=excluded.article,completed=excluded.completed,score=excluded.score", session.date().toString(), session.track().name(), session.title(), session.article(), session.completed() ? 1 : 0, session.score(), OffsetDateTime.now().toString());
        Long id = jdbc.queryForObject("select id from learning_sessions where session_date=?", Long.class, session.date().toString());
        jdbc.update("delete from learning_questions where session_id=?", id);
        for (int i = 0; i < session.questions().size(); i++) { var q = session.questions().get(i); jdbc.update("insert into learning_questions(session_id,question_order,prompt,expected_points) values(?,?,?,?)", id, i, q.prompt(), q.expectedPoints()); }
    }

    public void saveAnswers(LearningSession session, List<EvaluationResult.QuestionResult> details, String scoringMethod) {
        Long id = jdbc.queryForObject("select id from learning_sessions where session_date=?", Long.class, session.date().toString());
        jdbc.update("update learning_sessions set scoring_method=? where id=?", scoringMethod, id);
        for (int i = 0; i < details.size(); i++) {
            var detail = details.get(i);
            jdbc.update("update learning_questions set answer=?, score=?, feedback=?, reference_answer=? where session_id=? and question_order=?", detail.answer(), detail.score(), detail.feedback(), detail.referenceAnswer(), id, i);
        }
    }

    public EvaluationResult findEvaluation(LearningSession session) {
        List<EvaluationResult.QuestionResult> details = jdbc.query("select q.answer, q.score, q.feedback, q.reference_answer from learning_questions q join learning_sessions s on q.session_id=s.id where s.session_date=? order by q.question_order", (rs, n) -> new EvaluationResult.QuestionResult(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4)), session.date().toString());
        String method = jdbc.queryForObject("select scoring_method from learning_sessions where session_date=?", String.class, session.date().toString());
        if (method == null) method = "旧成绩按回答长度计算；重新提交可获得逐题评语和参考答案。";
        return new EvaluationResult(session, details, method);
    }
}
