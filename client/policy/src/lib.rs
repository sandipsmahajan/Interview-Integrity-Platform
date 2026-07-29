use serde::{Deserialize, Serialize};
use telemetry::{TelemetryEvent, TelemetryKind};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub enum Severity {
    Info,
    Low,
    Medium,
    High,
    Critical,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PolicyRule {
    pub code: String,
    pub enabled: bool,
    pub severity: Severity,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PolicySet {
    pub rules: Vec<PolicyRule>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LocalViolation {
    pub rule_code: String,
    pub severity: Severity,
    pub message: String,
}

pub struct PolicyEngine {
    policy_set: PolicySet,
}

impl PolicyEngine {
    pub fn new(policy_set: PolicySet) -> Self {
        Self { policy_set }
    }

    pub fn evaluate(&self, event: &TelemetryEvent) -> Vec<LocalViolation> {
        let mut violations = Vec::new();
        if event.kind == TelemetryKind::Browser
            && event.payload.get("outOfFocus").and_then(|v| v.as_bool()) == Some(true)
        {
            self.push_if_enabled(&mut violations, "BROWSER_FOCUS_LOST", "Interview browser lost focus");
        }
        if event.payload.get("virtualMachineDetected").and_then(|v| v.as_bool()) == Some(true) {
            self.push_if_enabled(&mut violations, "VM_DETECTED", "Virtual machine indicators were detected");
        }
        violations
    }

    fn push_if_enabled(&self, target: &mut Vec<LocalViolation>, code: &str, message: &str) {
        if let Some(rule) = self.policy_set.rules.iter().find(|rule| rule.code == code && rule.enabled) {
            target.push(LocalViolation {
                rule_code: code.to_string(),
                severity: rule.severity.clone(),
                message: message.to_string(),
            });
        }
    }
}
