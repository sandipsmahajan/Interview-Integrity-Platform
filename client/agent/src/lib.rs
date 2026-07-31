use anyhow::Result;
use config::CollectorConfig;
use network::ApiClient;
use policy::PolicyEngine;
use storage::LocalStore;
use telemetry::{CollectorRegistry, TelemetryEvent, TelemetryPanelEvent};
use tokio::sync::mpsc;
use tokio::time::{interval, Duration};
use uuid::Uuid;

pub struct MonitoringAgent {
    session_id: Uuid,
    token: String,
    collectors: CollectorRegistry,
    policy: PolicyEngine,
    api: ApiClient,
    store: LocalStore,
    collector_config: CollectorConfig,
    panel_tx: mpsc::UnboundedSender<TelemetryPanelEvent>,
    streaming_enabled: bool,
}

impl MonitoringAgent {
    #[allow(clippy::too_many_arguments)]
    pub fn new(
        session_id: Uuid,
        token: String,
        collectors: CollectorRegistry,
        policy: PolicyEngine,
        api: ApiClient,
        store: LocalStore,
        collector_config: CollectorConfig,
        panel_tx: mpsc::UnboundedSender<TelemetryPanelEvent>,
        streaming_enabled: bool,
    ) -> Self {
        Self {
            session_id,
            token,
            collectors,
            policy,
            api,
            store,
            collector_config,
            panel_tx,
            streaming_enabled,
        }
    }

    pub async fn run(&self) -> Result<()> {
        let mut tick = interval(Duration::from_secs(5));
        loop {
            tick.tick().await;
            for batch in self.collectors.collect_all(self.session_id).await {
                for event in batch? {
                    if !self.collector_config.is_enabled(self.collector_name(&event)) {
                        continue;
                    }
                    for violation in self.policy.evaluate(&event) {
                        tracing::warn!(rule = %violation.rule_code, "local policy violation");
                    }
                    let panel_event = event.to_panel_event();
                    let _ = self.panel_tx.send(panel_event);
                    if self.streaming_enabled {
                        if self.api.send_telemetry(&self.token, &event).await.is_err() {
                            self.store.enqueue(&event)?;
                        }
                    }
                }
            }
        }
    }

    fn collector_name(&self, event: &TelemetryEvent) -> &str {
        match event.kind {
            telemetry::TelemetryKind::Heartbeat => "heartbeat",
            telemetry::TelemetryKind::Device => "system_summary",
            telemetry::TelemetryKind::Display => "display",
            telemetry::TelemetryKind::WindowFocus => "window_focus",
            telemetry::TelemetryKind::Network => "network",
            telemetry::TelemetryKind::Audio => "microphone",
            telemetry::TelemetryKind::Video => "camera",
            telemetry::TelemetryKind::SystemHealth => "system_health",
            _ => "lifecycle",
        }
    }
}

pub fn build_collector_registry(enabled: &CollectorConfig) -> CollectorRegistry {
    let mut registry = CollectorRegistry::empty();
    if enabled.is_enabled("heartbeat") {
        registry.register(Box::new(system::HeartbeatCollector));
    }
    if enabled.is_enabled("system_summary") {
        registry.register(Box::new(system::SystemSummaryCollector));
    }
    if enabled.is_enabled("system_health") {
        registry.register(Box::new(system::SystemHealthCollector));
    }
    if enabled.is_enabled("network") {
        registry.register(Box::new(system::NetworkCollector));
    }
    if enabled.is_enabled("display") {
        registry.register(Box::new(system::DisplayCollector));
    }
    if enabled.is_enabled("window_focus") {
        registry.register(Box::new(system::WindowFocusCollector));
    }
    if enabled.is_enabled("camera") {
        registry.register(Box::new(camera::CameraConsentCollector));
    }
    if enabled.is_enabled("microphone") {
        registry.register(Box::new(microphone::MicrophoneConsentCollector));
    }
    registry.register(Box::new(system::LifecycleCollector));
    registry
}
