use anyhow::Result;
use network::ApiClient;
use policy::PolicyEngine;
use storage::LocalStore;
use telemetry::CollectorRegistry;
use tokio::time::{interval, Duration};
use uuid::Uuid;

pub struct MonitoringAgent {
    session_id: Uuid,
    token: String,
    collectors: CollectorRegistry,
    policy: PolicyEngine,
    api: ApiClient,
    store: LocalStore,
}

impl MonitoringAgent {
    pub fn new(
        session_id: Uuid,
        token: String,
        collectors: CollectorRegistry,
        policy: PolicyEngine,
        api: ApiClient,
        store: LocalStore,
    ) -> Self {
        Self { session_id, token, collectors, policy, api, store }
    }

    pub async fn run(&self) -> Result<()> {
        let mut tick = interval(Duration::from_secs(5));
        loop {
            tick.tick().await;
            for batch in self.collectors.collect_all(self.session_id).await {
                for event in batch? {
                    for violation in self.policy.evaluate(&event) {
                        tracing::warn!(rule = %violation.rule_code, "local policy violation");
                    }
                    if self.api.send_telemetry(&self.token, &event).await.is_err() {
                        self.store.enqueue(&event)?;
                    }
                }
            }
        }
    }
}
