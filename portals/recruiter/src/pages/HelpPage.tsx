import { ContactSupport, KeyboardAlt, MenuBook, RocketLaunch } from '@mui/icons-material';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { PageHeader } from '../components/PageHeader';

const TOPICS = [
  {
    icon: <RocketLaunch />,
    title: 'Getting started',
    description: 'Set up your organization, invite recruiters and run your first integrity-protected interview.'
  },
  {
    icon: <MenuBook />,
    title: 'Using the desktop client',
    description: 'Install, update and troubleshoot the desktop client on Windows, macOS and Linux.'
  },
  {
    icon: <KeyboardAlt />,
    title: 'Keyboard shortcuts',
    description: 'Press Ctrl+K anywhere to open the command palette and navigate the portal quickly.'
  },
  {
    icon: <ContactSupport />,
    title: 'Contact support',
    description: 'Reach out to your account team for help with billing, SSO or enterprise configuration.'
  }
];

export function HelpPage() {
  return (
    <Box>
      <PageHeader title="Help Center" subtitle="Guides and resources to get the most out of Integrity Pro." />
      <Grid container spacing={2}>
        {TOPICS.map((topic) => (
          <Grid key={topic.title} size={{ xs: 12, md: 6 }}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardContent>
                <Stack direction="row" spacing={1.5} sx={{ alignItems: 'flex-start' }}>
                  <Box sx={{ color: 'primary.main' }}>{topic.icon}</Box>
                  <Box>
                    <Typography variant="h6" sx={{ fontWeight: 700, mb: 0.5 }}>
                      {topic.title}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {topic.description}
                    </Typography>
                  </Box>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
