use anyhow::Result;
use config::ClientSettings;
use rusqlite::{params, Connection};
use security::ConsentRecord;
use telemetry::TelemetryEvent;

pub struct LocalStore {
    conn: Connection,
}

impl LocalStore {
    pub fn open(path: &str) -> Result<Self> {
        let conn = Connection::open(path)?;
        conn.execute_batch(
            "CREATE TABLE IF NOT EXISTS outbound_events (
               id INTEGER PRIMARY KEY AUTOINCREMENT,
               payload TEXT NOT NULL,
               created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
             );
             CREATE TABLE IF NOT EXISTS settings (
               key TEXT PRIMARY KEY,
               value TEXT NOT NULL
             );
             CREATE TABLE IF NOT EXISTS consent (
               session_id TEXT PRIMARY KEY,
               payload TEXT NOT NULL,
               created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
             );
             CREATE TABLE IF NOT EXISTS auth_tokens (
               id INTEGER PRIMARY KEY CHECK (id = 1),
               payload TEXT NOT NULL
             );",
        )?;
        Ok(Self { conn })
    }

    pub fn enqueue(&self, event: &TelemetryEvent) -> Result<()> {
        let payload = serde_json::to_string(event)?;
        self.conn
            .execute("INSERT INTO outbound_events(payload) VALUES (?1)", params![payload])?;
        Ok(())
    }

    pub fn pending_events(&self) -> Result<Vec<TelemetryEvent>> {
        let mut stmt = self.conn.prepare("SELECT payload FROM outbound_events ORDER BY id ASC")?;
        let rows = stmt.query_map([], |row| row.get::<_, String>(0))?;
        rows.map(|row| Ok(serde_json::from_str(&row?)?)).collect()
    }

    pub fn clear_pending(&self) -> Result<()> {
        self.conn.execute("DELETE FROM outbound_events", [])?;
        Ok(())
    }

    pub fn save_settings(&self, settings: &ClientSettings) -> Result<()> {
        let payload = serde_json::to_string(settings)?;
        self.conn.execute(
            "INSERT INTO settings(key, value) VALUES ('client', ?1)
             ON CONFLICT(key) DO UPDATE SET value = excluded.value",
            params![payload],
        )?;
        Ok(())
    }

    pub fn load_settings(&self) -> Result<ClientSettings> {
        let mut stmt = self.conn.prepare("SELECT value FROM settings WHERE key = 'client'")?;
        let mut rows = stmt.query(params![])?;
        if let Some(row) = rows.next()? {
            Ok(serde_json::from_str(&row.get::<_, String>(0)?)?)
        } else {
            Ok(ClientSettings::default())
        }
    }

    pub fn save_consent(&self, record: &ConsentRecord) -> Result<()> {
        let payload = serde_json::to_string(record)?;
        self.conn.execute(
            "INSERT INTO consent(session_id, payload) VALUES (?1, ?2)
             ON CONFLICT(session_id) DO UPDATE SET payload = excluded.payload",
            params![record.session_id.to_string(), payload],
        )?;
        Ok(())
    }

    pub fn save_auth_token(&self, token: &str) -> Result<()> {
        self.conn.execute(
            "INSERT INTO auth_tokens(id, payload) VALUES (1, ?1)
             ON CONFLICT(id) DO UPDATE SET payload = excluded.payload",
            params![token],
        )?;
        Ok(())
    }

    pub fn load_auth_token(&self) -> Result<Option<String>> {
        let mut stmt = self.conn.prepare("SELECT payload FROM auth_tokens WHERE id = 1")?;
        let mut rows = stmt.query(params![])?;
        if let Some(row) = rows.next()? {
            Ok(Some(row.get(0)?))
        } else {
            Ok(None)
        }
    }
}
