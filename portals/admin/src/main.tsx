import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import { Building2, Flag, HeartPulse, ShieldCheck, Users } from 'lucide-react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AppShell, MetricTile } from '@interview-integrity/components';
import { Box, Chip, CssBaseline, FormControlLabel, Paper, Switch, Typography } from '@interview-integrity/ui';
import './style.css';

const policies = ['No VM', 'No RDP', 'Single monitor', 'Browser focused', 'Camera required', 'Microphone required'];
const queryClient = new QueryClient();

function usePolicyControls() {
  return useQuery({
    queryKey: ['policy-controls'],
    queryFn: async () => policies,
    staleTime: 60_000
  });
}

function PolicyControls() {
  const controls = usePolicyControls();

  return (
    <Paper className="panel">
      <Box className="panel-title">
        <ShieldCheck />
        <Typography variant="h6">Policy Controls</Typography>
      </Box>
      {(controls.data ?? []).map((policy) => (
        <FormControlLabel key={policy} control={<Switch defaultChecked />} label={policy} />
      ))}
    </Paper>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <CssBaseline />
      <BrowserRouter>
        <AppShell
          title="Admin Portal"
          status={<Chip icon={<HeartPulse size={16} />} label="System healthy" color="success" variant="outlined" />}
        >
          <Box className="grid">
            <MetricTile icon={<Building2 />} label="Companies" value={24} />
            <MetricTile icon={<Users />} label="Users" value={318} />
            <MetricTile icon={<Flag />} label="Feature Flags" value={9} />
          </Box>
          <Routes>
            <Route path="/" element={<PolicyControls />} />
          </Routes>
        </AppShell>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

createRoot(document.getElementById('root')!).render(<App />);
