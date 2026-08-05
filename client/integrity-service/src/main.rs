use anyhow::Result;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;

mod server;

fn main() -> Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "integrity_service=info".into()),
        )
        .init();

    logger::init();

    tracing::info!("Integrity Service starting...");

    let rt = tokio::runtime::Builder::new_multi_thread()
        .worker_threads(4)
        .enable_all()
        .build()?;

    rt.block_on(async {
        let running = Arc::new(AtomicBool::new(true));
        let r = running.clone();

        tokio::spawn(async move {
            tokio::signal::ctrl_c().await.ok();
            r.store(false, Ordering::SeqCst);
            tracing::info!("Shutdown signal received");
        });

        if let Err(e) = server::run(running).await {
            tracing::error!(?e, "Service terminated with error");
        }

        tracing::info!("Integrity Service stopped");
        Ok(())
    })
}
