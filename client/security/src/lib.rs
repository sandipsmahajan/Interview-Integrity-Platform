use anyhow::{anyhow, Result};
use ring::digest;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CertificatePin {
    pub hostname: String,
    pub sha256_spki: String,
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
