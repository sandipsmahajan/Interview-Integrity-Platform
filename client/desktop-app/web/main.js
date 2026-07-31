const invoke = window.__TAURI__?.core?.invoke;

function setText(id, value) {
  document.getElementById(id).textContent = value;
}

function enabledLabel(value) {
  return value ? 'Disabled' : 'Allowed';
}

async function boot() {
  if (!invoke) {
    setText('devtools', 'Bridge unavailable');
    setText('downloads', 'Bridge unavailable');
    setText('extensions', 'Bridge unavailable');
    setText('printing', 'Bridge unavailable');
    setText('popups', 'Bridge unavailable');
    return;
  }

  const policy = await invoke('browser_policy');
  setText('devtools', enabledLabel(policy.disable_devtools));
  setText('downloads', enabledLabel(policy.disable_downloads));
  setText('extensions', enabledLabel(policy.disable_extensions));
  setText('printing', enabledLabel(policy.disable_printing));
  setText('popups', policy.block_popups ? 'Blocked' : 'Allowed');

  const domains = document.getElementById('domains');
  domains.replaceChildren(
    ...policy.allowed_domains.map((domain) => {
      const item = document.createElement('li');
      item.textContent = domain;
      return item;
    })
  );
}

boot().catch((error) => {
  setText('devtools', `Startup failed: ${error}`);
});
