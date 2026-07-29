import React from 'react';
import { createRoot } from 'react-dom/client';
import { Activity, AlertTriangle, ClipboardList, Radio } from 'lucide-react';
import { CssBaseline, Box, Typography, Stack, Paper, Chip, Table, TableBody, TableCell, TableHead, TableRow, LinearProgress } from '@mui/material';
import './style.css';

const interviews = [
  { name: 'Aarav Sharma', role: 'Senior Rust Engineer', status: 'Live', risk: 18, network: 'Stable', alerts: 1 },
  { name: 'Maya Iyer', role: 'Platform Architect', status: 'Waiting', risk: 0, network: 'Pending', alerts: 0 },
  { name: 'Noah Lee', role: 'Backend Lead', status: 'Review', risk: 42, network: 'Recovered', alerts: 3 }
];

function App() {
  return (
    <>
      <CssBaseline />
      <Box className="app-shell">
        <Stack direction="row" alignItems="center" justifyContent="space-between" className="topbar">
          <Typography variant="h5">Recruiter Console</Typography>
          <Chip icon={<Radio size={16} />} label="Live telemetry connected" color="success" variant="outlined" />
        </Stack>
        <Box className="grid">
          <Paper className="metric"><Activity /><Typography>Live Interviews</Typography><strong>7</strong></Paper>
          <Paper className="metric"><AlertTriangle /><Typography>Open Alerts</Typography><strong>4</strong></Paper>
          <Paper className="metric"><ClipboardList /><Typography>Reports Ready</Typography><strong>12</strong></Paper>
        </Box>
        <Paper className="panel">
          <Typography variant="h6">Interview Queue</Typography>
          <Table size="small">
            <TableHead><TableRow><TableCell>Candidate</TableCell><TableCell>Role</TableCell><TableCell>Status</TableCell><TableCell>Risk</TableCell><TableCell>Network</TableCell><TableCell>Alerts</TableCell></TableRow></TableHead>
            <TableBody>
              {interviews.map((row) => (
                <TableRow key={row.name}>
                  <TableCell>{row.name}</TableCell><TableCell>{row.role}</TableCell><TableCell>{row.status}</TableCell>
                  <TableCell><LinearProgress variant="determinate" value={row.risk} /></TableCell>
                  <TableCell>{row.network}</TableCell><TableCell>{row.alerts}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      </Box>
    </>
  );
}

createRoot(document.getElementById('root')!).render(<App />);
