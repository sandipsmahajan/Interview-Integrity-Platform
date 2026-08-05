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
    mut_shutdown: mpsc::UnboundedReceiver<()>,
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
        mut_shutdown: mpsc::UnboundedReceiver<()>,
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
            mut_shutdown,
            streaming_enabled,
        }
    }

    pub async fn run(&mut self) -> Result<()> {
        let mut tick = interval(Duration::from_secs(5));
        let mut tick_count: u64 = 0;
        loop {
            tokio::select! {
                _ = self.mut_shutdown.recv() => {
                    tracing::info!(session_id = %self.session_id, "monitoring agent received shutdown signal");
                    self.flush_queued_events().await;
                    break;
                }
                _ = tick.tick() => {
                    tick_count += 1;
                    self.process_tick(tick_count).await;
                }
            }
        }
        Ok(())
    }

    async fn process_tick(&self, tick_count: u64) {
        for batch in self.collectors.collect_all(self.session_id).await {
            for event in batch.unwrap_or_default() {
                if !self.collector_config.is_enabled(self.collector_name(&event)) {
                    continue;
                }
                for violation in self.policy.evaluate(&event) {
                    tracing::warn!(rule = %violation.rule_code, "local policy violation");
                }
                let panel_event = event.to_panel_event();
                let _ = self.panel_tx.send(panel_event);

                if self.streaming_enabled && tick_count % 3 == 0 {
                    if self.api.send_telemetry(&self.token, &event).await.is_err() {
                        if let Err(e) = self.store.enqueue(&event) {
                            tracing::error!(error = %e, "failed to enqueue telemetry event");
                        }
                    }
                }
            }
        }
    }

    async fn flush_queued_events(&self) {
        let events = match self.store.pending_events() {
            Ok(events) => events,
            Err(e) => {
                tracing::error!(error = %e, "failed to read queued events for flush");
                return;
            }
        };
        if events.is_empty() {
            return;
        }
        let mut failed = 0u32;
        for event in &events {
            if self.api.send_telemetry(&self.token, event).await.is_err() {
                failed += 1;
            }
        }
        if failed == 0 {
            if let Err(e) = self.store.clear_pending() {
                tracing::error!(error = %e, "failed to clear pending events after flush");
            }
        }
        tracing::info!(total = events.len(), failed, "flushed queued telemetry events");
    }

    fn collector_name(&self, event: &TelemetryEvent) -> &str {
        match event.kind {
            telemetry::TelemetryKind::Heartbeat => "heartbeat",
            telemetry::TelemetryKind::Device => "system_summary",
            telemetry::TelemetryKind::Display => "display",
            telemetry::TelemetryKind::WindowFocus => "window_focus",
            telemetry::TelemetryKind::Process => "process_collector",
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
    if enabled.is_enabled("process_collector") {
        registry.register(Box::new(system::ProcessCollector));
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
