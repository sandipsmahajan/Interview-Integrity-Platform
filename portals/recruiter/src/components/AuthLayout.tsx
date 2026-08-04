import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { BrandBanner } from './BrandLogo';

interface AuthLayoutProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
}

export function AuthLayout({ title, subtitle, children }: AuthLayoutProps) {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        p: 2,
        background: 'radial-gradient(1200px 600px at 10% 0%, rgba(37,99,235,0.12), transparent), radial-gradient(1000px 500px at 100% 100%, rgba(14,165,233,0.1), transparent)'
      }}
    >
      <Box sx={{ width: '100%', maxWidth: 440, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Box sx={{ mb: 2 }}>
          <BrandBanner maxWidth={180} />
        </Box>
        <Card sx={{ width: '100%', borderRadius: 3, boxShadow: 6 }}>
          <CardContent sx={{ p: 4 }}>
            <Typography variant="h5" sx={{ fontWeight: 800, mb: 0.5 }}>
              {title}
            </Typography>
            {subtitle ? (
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                {subtitle}
              </Typography>
            ) : (
              <Box sx={{ mb: 3 }} />
            )}
            {children}
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
}
