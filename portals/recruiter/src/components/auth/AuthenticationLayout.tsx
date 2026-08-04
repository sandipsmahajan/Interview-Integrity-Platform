import type { ReactNode } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import { ThemeProvider } from '@mui/material/styles';
import Typography from '@mui/material/Typography';
import { authTheme } from '../../theme';
import { AnimatedBackground } from './AnimatedBackground';
import { BrandHeader } from './BrandHeader';
import { FeatureShowcase } from './FeatureShowcase';

interface AuthenticationLayoutProps {
  children: ReactNode;
}

/**
 * Full-screen enterprise auth shell: an animated 60/40 split with a product
 * showcase on the left and a fixed glass login panel on the right. Always dark.
 */
export function AuthenticationLayout({ children }: AuthenticationLayoutProps) {
  return (
    <ThemeProvider theme={authTheme}>
      <Box
        sx={{
          position: 'relative',
          minHeight: '100dvh',
          display: 'grid',
          gridTemplateColumns: { md: '1fr 1fr', lg: '3fr 2fr' },
          bgcolor: '#0b1220',
          overflow: 'hidden'
        }}
      >
        <AnimatedBackground />
        <Box
          component="aside"
          sx={{
            position: 'relative',
            zIndex: 1,
            display: { xs: 'none', md: 'block' }
          }}
        >
          <FeatureShowcase />
        </Box>
        <Box
          component="main"
          sx={{
            position: 'relative',
            zIndex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '100dvh',
            p: { xs: 2, md: 4 }
          }}
        >
          <Card
            sx={{
              width: '100%',
              maxWidth: 440,
              borderRadius: 3,
              bgcolor: 'rgba(17, 24, 39, 0.72)',
              backdropFilter: 'blur(24px)',
              WebkitBackdropFilter: 'blur(24px)',
              border: '1px solid rgba(148, 163, 184, 0.16)',
              boxShadow: '0 32px 80px -20px rgba(0, 0, 0, 0.65)'
            }}
          >
            <Box sx={{ p: { xs: 3, sm: 4 } }}>
              <Box sx={{ mb: 3, display: 'flex', justifyContent: 'center' }}>
                <BrandHeader />
              </Box>
              {children}
            </Box>
          </Card>
        </Box>
      </Box>
    </ThemeProvider>
  );
}

interface AuthHeadingProps {
  title: string;
  subtitle?: string;
}

/**
 * Consistent heading used by the non-login auth screens inside the layout.
 */
export function AuthHeading({ title, subtitle }: AuthHeadingProps) {
  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 800, mb: 0.5 }}>
        {title}
      </Typography>
      {subtitle ? (
        <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
          {subtitle}
        </Typography>
      ) : (
        <Box sx={{ mb: 3 }} />
      )}
    </>
  );
}
