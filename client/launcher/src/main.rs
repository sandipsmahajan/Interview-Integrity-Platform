use anyhow::Result;
use browser::BrowserPolicy;
use policy::{PolicyEngine, PolicyRule, PolicySet, Severity};
use telemetry::{CollectorRegistry, TelemetryEvent};
use uuid::Uuid;

#[tokio::main]
async fn main() -> Result<()> {
    let browser_policy = BrowserPolicy::enterprise_default();
    assert!(browser_policy.disable_devtools);

    let policy = PolicyEngine::new(PolicySet {
        rules: vec![
            PolicyRule { code: "BROWSER_FOCUS_LOST".into(), enabled: true, severity: Severity::Medium },
            PolicyRule { code: "VM_DETECTED".into(), enabled: true, severity: Severity::Critical },
        ],
    });
    let collectors = CollectorRegistry::new(vec![Box::new(system::SystemSummaryCollector)]);
    let session_id = Uuid::new_v4();

    println!("Interview Integrity Launcher");
    println!("session_id: {session_id}");
    println!("browser hardening:");
    println!("  devtools disabled: {}", browser_policy.disable_devtools);
    println!("  downloads disabled: {}", browser_policy.disable_downloads);
    println!("  extensions disabled: {}", browser_policy.disable_extensions);
    println!("  printing disabled: {}", browser_policy.disable_printing);
    println!("  popups blocked: {}", browser_policy.block_popups);
    println!("allowed domains:");
    for domain in &browser_policy.allowed_domains {
        println!("  - {domain}");
    }

    println!("collecting consented telemetry...");
    let batches = collectors.collect_all(session_id).await;
    for batch in batches {
        for event in batch? {
            print_event(&policy, &event);
        }
    }
    println!("launcher smoke run completed successfully.");
    Ok(())
}

fn print_event(policy: &PolicyEngine, event: &TelemetryEvent) {
    println!("telemetry: {:?} {}", event.kind, event.payload);
    for violation in policy.evaluate(event) {
        println!(
            "policy violation: {} {:?} {}",
            violation.rule_code, violation.severity, violation.message
        );
    }
}
