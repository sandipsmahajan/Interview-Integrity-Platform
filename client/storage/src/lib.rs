use anyhow::Result;
use rusqlite::{params, Connection};
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
             );",
        )?;
        Ok(Self { conn })
    }

    pub fn enqueue(&self, event: &TelemetryEvent) -> Result<()> {
        let payload = serde_json::to_string(event)?;
        self.conn.execute("INSERT INTO outbound_events(payload) VALUES (?1)", params![payload])?;
        Ok(())
    }
}
