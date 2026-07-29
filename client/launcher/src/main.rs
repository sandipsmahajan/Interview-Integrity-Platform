use anyhow::Result;
use browser::BrowserPolicy;
use policy::{PolicyEngine, PolicyRule, PolicySet, Severity};
use telemetry::CollectorRegistry;
use uuid::Uuid;

#[tokio::main]
async fn main() -> Result<()> {
    let browser_policy = BrowserPolicy::enterprise_default();
    assert!(browser_policy.disable_devtools);

    let _policy = PolicyEngine::new(PolicySet {
        rules: vec![
            PolicyRule { code: "BROWSER_FOCUS_LOST".into(), enabled: true, severity: Severity::Medium },
            PolicyRule { code: "VM_DETECTED".into(), enabled: true, severity: Severity::Critical },
        ],
    });
    let _collectors = CollectorRegistry::new(vec![Box::new(system::SystemSummaryCollector)]);
    let _session_id = Uuid::new_v4();
    Ok(())
}
