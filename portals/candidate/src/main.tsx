import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import { Camera, Download, Mic, MonitorCheck, Wifi } from 'lucide-react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AppShell } from '@interview-integrity/components';
import { Box, Button, Chip, CssBaseline, LinearProgress, Paper, Typography } from '@interview-integrity/ui';
import './style.css';

const checks = [
  { label: 'Desktop client', icon: Download, value: 100 },
  { label: 'Camera', icon: Camera, value: 100 },
  { label: 'Microphone', icon: Mic, value: 100 },
  { label: 'Single monitor', icon: MonitorCheck, value: 100 },
  { label: 'Internet', icon: Wifi, value: 82 }
];

const queryClient = new QueryClient();

function useCompatibilityChecks() {
  return useQuery({
    queryKey: ['compatibility-checks'],
    queryFn: async () => checks,
    staleTime: 30_000
  });
}

function Compatibility() {
  const compatibility = useCompatibilityChecks();

  return (
    <Paper className="panel">
      <Typography variant="h6">System Compatibility</Typography>
      {(compatibility.data ?? []).map(({ label, icon: Icon, value }) => (
        <Box className="check" key={label}>
          <Icon size={20} />
          <span>{label}</span>
          <LinearProgress variant="determinate" value={value} />
        </Box>
      ))}
      <Box className="actions">
        <Button variant="contained" startIcon={<Download size={18} />}>
          Download Client
        </Button>
        <Button variant="outlined">Join Interview</Button>
      </Box>
    </Paper>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <CssBaseline />
      <BrowserRouter>
        <AppShell
          title="Candidate Portal"
          status={<Chip label="Interview at 15:30" color="primary" variant="outlined" />}
        >
          <Routes>
            <Route path="/" element={<Compatibility />} />
          </Routes>
        </AppShell>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

createRoot(document.getElementById('root')!).render(<App />);
