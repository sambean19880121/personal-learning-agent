CREATE TABLE IF NOT EXISTS learning_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_date TEXT NOT NULL UNIQUE,
    track TEXT NOT NULL,
    title TEXT NOT NULL,
    article TEXT NOT NULL,
    completed INTEGER NOT NULL DEFAULT 0,
    score INTEGER,
    source TEXT,
    source_url TEXT,
    created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS learning_questions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    question_order INTEGER NOT NULL,
    prompt TEXT NOT NULL,
    expected_points TEXT NOT NULL,
    answer TEXT,
    score INTEGER,
    feedback TEXT,
    FOREIGN KEY(session_id) REFERENCES learning_sessions(id)
);
CREATE TABLE IF NOT EXISTS source_articles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source TEXT NOT NULL,
    title TEXT NOT NULL,
    url TEXT NOT NULL UNIQUE,
    published_at TEXT,
    summary TEXT,
    fetched_at TEXT NOT NULL
);
