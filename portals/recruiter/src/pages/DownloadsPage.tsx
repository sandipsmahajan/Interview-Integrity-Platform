import { Apple, DesktopWindows, Download, Terminal, VerifiedUser } from '@mui/icons-material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import toast from 'react-hot-toast';
import { PageHeader } from '../components/PageHeader';

interface DownloadOption {
  platform: string;
  icon: React.ReactNode;
  version: string;
  note: string;
}

const DOWNLOADS: DownloadOption[] = [
  { platform: 'Windows', icon: <DesktopWindows />, version: '2.4.1', note: 'WebView2 runtime required' },
  { platform: 'macOS', icon: <Apple />, version: '2.4.1', note: 'Universal binary, Apple Silicon and Intel' },
  { platform: 'Linux', icon: <Terminal />, version: '2.4.1', note: 'AppImage and deb packages' }
];

const RELEASE_NOTES = [
  '2.4.1 - Improved network resilience and reconnect handling.',
  '2.4.0 - Added camera and microphone health diagnostics.',
  '2.3.0 - Policy violation evidence now includes screen context.'
];

export function DownloadsPage() {
  function download(option: DownloadOption) {
    toast.success(`${option.platform} client ${option.version} download started`);
  }

  return (
    <Box>
      <PageHeader
        title="Downloads"
        subtitle="Install the Integrity Pro desktop client on your interview environments."
      />
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {DOWNLOADS.map((option) => (
          <Grid key={option.platform} size={{ xs: 12, md: 4 }}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, height: '100%' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                  {option.icon}
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    {option.platform}
                  </Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  v{option.version}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {option.note}
                </Typography>
                <Box sx={{ flex: 1 }} />
                <Button variant="contained" startIcon={<Download />} onClick={() => download(option)} fullWidth>
                  Download {option.platform} client
                </Button>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Release notes
              </Typography>
              <List dense>
                {RELEASE_NOTES.map((note) => (
                  <ListItem key={note} disableGutters>
                    <Typography variant="body2">{note}</Typography>
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Verification
              </Typography>
              <List dense>
                <ListItem disableGutters>
                  <ListItemIcon sx={{ minWidth: 32 }}>
                    <VerifiedUser color="success" fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="SHA-256 checksums" secondary="Published on the release page for every installer." />
                </ListItem>
                <ListItem disableGutters>
                  <ListItemIcon sx={{ minWidth: 32 }}>
                    <VerifiedUser color="success" fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="Code signing" secondary="Windows and macOS installers are signed and notarized." />
                </ListItem>
              </List>
              <Divider sx={{ my: 2 }} />
              <Typography variant="body2" color="text.secondary">
                Installation guide: run the installer, sign in with your recruiter account, and allow the
                browser policy and telemetry permissions requested during the first interview.
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
