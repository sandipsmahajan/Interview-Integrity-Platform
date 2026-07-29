import React from 'react';
import { createRoot } from 'react-dom/client';
import { Building2, Flag, HeartPulse, ShieldCheck, Users } from 'lucide-react';
import { Box, Chip, CssBaseline, FormControlLabel, Paper, Stack, Switch, Typography } from '@mui/material';
import './style.css';

const policies = ['No VM', 'No RDP', 'Single monitor', 'Browser focused', 'Camera required', 'Microphone required'];

function App() {
  return <><CssBaseline /><Box className="shell">
    <Stack direction="row" alignItems="center" justifyContent="space-between" className="topbar">
      <Typography variant="h5">Admin Portal</Typography><Chip icon={<HeartPulse size={16}/>} label="System healthy" color="success" variant="outlined" />
    </Stack>
    <Box className="grid">
      <Paper className="tile"><Building2/><span>Companies</span><strong>24</strong></Paper>
      <Paper className="tile"><Users/><span>Users</span><strong>318</strong></Paper>
      <Paper className="tile"><Flag/><span>Feature Flags</span><strong>9</strong></Paper>
    </Box>
    <Paper className="panel"><Stack direction="row" gap={1} alignItems="center"><ShieldCheck/><Typography variant="h6">Policy Controls</Typography></Stack>
      {policies.map(policy => <FormControlLabel key={policy} control={<Switch defaultChecked />} label={policy} />)}
    </Paper>
  </Box></>;
}

createRoot(document.getElementById('root')!).render(<App />);
