import React from 'react';
import { createRoot } from 'react-dom/client';
import { Camera, Download, Mic, MonitorCheck, Wifi } from 'lucide-react';
import { Box, Button, Chip, CssBaseline, LinearProgress, Paper, Stack, Typography } from '@mui/material';
import './style.css';

const checks = [
  { label: 'Desktop client', icon: Download, value: 100 },
  { label: 'Camera', icon: Camera, value: 100 },
  { label: 'Microphone', icon: Mic, value: 100 },
  { label: 'Single monitor', icon: MonitorCheck, value: 100 },
  { label: 'Internet', icon: Wifi, value: 82 }
];

function App() {
  return <><CssBaseline /><Box className="shell">
    <Stack direction="row" alignItems="center" justifyContent="space-between" className="topbar">
      <Typography variant="h5">Candidate Portal</Typography><Chip label="Interview at 15:30" color="primary" variant="outlined" />
    </Stack>
    <Paper className="panel">
      <Typography variant="h6">System Compatibility</Typography>
      {checks.map(({ label, icon: Icon, value }) => <Box className="check" key={label}><Icon size={20}/><span>{label}</span><LinearProgress variant="determinate" value={value}/></Box>)}
      <Stack direction="row" gap={1} flexWrap="wrap"><Button variant="contained" startIcon={<Download size={18}/>}>Download Client</Button><Button variant="outlined">Join Interview</Button></Stack>
    </Paper>
  </Box></>;
}

createRoot(document.getElementById('root')!).render(<App />);
