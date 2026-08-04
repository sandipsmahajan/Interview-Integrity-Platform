import type { ReactNode } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { Fingerprint } from 'lucide-react';

export interface FeatureSlideData {
  id: string;
  kicker: string;
  accent: string;
  title: string;
  description: string;
  visual: ReactNode;
  bubbles?: ReactNode[];
}

interface FeatureSlideProps {
  slide: FeatureSlideData;
}

/**
 * Renders a single carousel slide. The illustration lives in a padded relative
 * wrapper so floating bubbles stay inside the reserved gutters and can never
 * overlap the title or description above them.
 */
export function FeatureSlide({ slide }: FeatureSlideProps) {
  return (
    <Box sx={{ maxWidth: { md: 560, lg: 720 } }}>
      <Box
        sx={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 0.5,
          px: 1.25,
          py: 0.45,
          borderRadius: 999,
          fontSize: 11,
          fontWeight: 700,
          letterSpacing: '0.06em',
          textTransform: 'uppercase',
          color: slide.accent,
          bgcolor: `${slide.accent}1f`,
          border: `1px solid ${slide.accent}3d`
        }}
      >
        <Fingerprint size={12} />
        {slide.kicker}
      </Box>
      <Typography
        variant="h3"
        sx={{
          fontWeight: 800,
          color: '#ffffff',
          mt: 1.5,
          mb: 1,
          fontSize: { md: 28, lg: 34 },
          lineHeight: 1.12,
          letterSpacing: '-0.02em'
        }}
      >
        {slide.title}
      </Typography>
      <Typography sx={{ color: '#94a3b8', maxWidth: 520, fontSize: 15, lineHeight: 1.55 }}>{slide.description}</Typography>
      <Box sx={{ position: 'relative', mt: 2.75, pt: 3, pb: 2.25, px: { md: 1.5 } }}>
        {slide.visual}
        {slide.bubbles}
      </Box>
    </Box>
  );
}
