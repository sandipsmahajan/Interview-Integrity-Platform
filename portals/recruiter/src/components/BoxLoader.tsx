import Box from '@mui/material/Box';
import LinearProgress from '@mui/material/LinearProgress';
import Paper from '@mui/material/Paper';

export function BoxLoader({ rows = 4, height = 28 }: { rows?: number; height?: number }) {
  return (
    <Paper variant="outlined" sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }} role="status" aria-label="Loading">
      {Array.from({ length: rows }).map((_, index) => (
        <Box key={index} sx={{ height, borderRadius: 1, overflow: 'hidden' }}>
          <LinearProgress />
        </Box>
      ))}
    </Paper>
  );
}

export default BoxLoader;
