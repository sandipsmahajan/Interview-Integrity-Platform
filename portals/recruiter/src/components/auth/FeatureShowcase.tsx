import Box from '@mui/material/Box';
import { ShieldCheck } from 'lucide-react';
import { AnimationController } from './AnimationController';
import { BrandHeader } from './BrandHeader';
import { FeatureCarousel } from './FeatureCarousel';

function TrustChip({ label }: { label: string }) {
  return (
    <Box
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.75,
        px: 1.25,
        py: 0.6,
        borderRadius: 8,
        fontSize: 11.5,
        color: '#94a3b8',
        border: '1px solid rgba(148, 163, 184, 0.16)',
        bgcolor: 'rgba(255, 255, 255, 0.02)'
      }}
    >
      <ShieldCheck size={12} color="#22c55e" />
      {label}
    </Box>
  );
}

/**
 * Left-hand product showcase for the auth screens: horizontal brand header,
 * animated capability carousel and trust indicators. Floating bubbles live
 * inside each slide's illustration wrapper so they never overlap the text.
 */
export function FeatureShowcase() {
  return (
    <AnimationController>
      <Box
        sx={{
          position: 'relative',
          height: '100dvh',
          display: 'flex',
          flexDirection: 'column',
          px: { md: 4, lg: 7 },
          py: { md: 5, lg: 6 },
          overflow: 'hidden'
        }}
      >
        <BrandHeader align="left" />
        <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', py: 3 }}>
          <FeatureCarousel />
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap', pb: 1 }}>
          <TrustChip label="SOC 2 Type II" />
          <TrustChip label="GDPR ready" />
          <TrustChip label="99.99% uptime" />
          <TrustChip label="End-to-end encryption" />
        </Box>
      </Box>
    </AnimationController>
  );
}
