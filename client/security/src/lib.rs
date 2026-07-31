use anyhow::{anyhow, Result};
use chrono::{DateTime, Utc};
use ring::digest;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CertificatePin {
    pub hostname: String,
    pub sha256_spki: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthSession {
    pub access_token: String,
    pub refresh_token: String,
    pub expires_at: DateTime<Utc>,
    pub device_id: String,
}

impl AuthSession {
    pub fn is_expired(&self) -> bool {
        Utc::now() >= self.expires_at
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConsentRecord {
    pub session_id: Uuid,
    pub granted_at: DateTime<Utc>,
    pub organization_name: String,
    pub categories: Vec<String>,
}

pub fn sha256_hex(bytes: &[u8]) -> String {
    let digest = digest::digest(&digest::SHA256, bytes);
    digest.as_ref().iter().map(|b| format!("{b:02x}")).collect::<Vec<_>>().join("")
}

pub fn verify_pin(hostname: &str, spki_der: &[u8], pins: &[CertificatePin]) -> Result<()> {
    let actual = sha256_hex(spki_der);
    pins.iter()
        .any(|pin| pin.hostname == hostname && pin.sha256_spki.eq_ignore_ascii_case(&actual))
        .then_some(())
        .ok_or_else(|| anyhow!("certificate pin verification failed for {hostname}"))
}

pub fn generate_device_id() -> String {
    Uuid::new_v4().to_string()
}

/// Simple XOR obfuscation for local token storage. Production builds should use OS keychain integration.
pub fn obfuscate(value: &str, key: &str) -> String {
    value
        .bytes()
        .zip(key.bytes().cycle())
        .map(|(a, b)| a ^ b)
        .map(|b| format!("{b:02x}"))
        .collect::<Vec<_>>()
        .join("")
}

pub fn deobfuscate(hex: &str, key: &str) -> Result<String> {
    let bytes: Result<Vec<u8>, _> = (0..hex.len())
        .step_by(2)
        .map(|i| u8::from_str_radix(&hex[i..i + 2], 16))
        .collect();
    let bytes = bytes?;
    Ok(String::from_utf8(
        bytes
            .into_iter()
            .zip(key.bytes().cycle())
            .map(|(a, b)| a ^ b)
            .collect(),
    )?)
}
