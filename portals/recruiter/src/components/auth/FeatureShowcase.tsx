import Box from '@mui/material/Box';
import { ShieldCheck, TriangleAlert } from 'lucide-react';
import { BrandHeader } from './BrandHeader';
import { FeatureCarousel } from './FeatureCarousel';
import { FeatureCard } from './FeatureCard';

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

function FloatingCards() {
  return (
    <>
      <Box sx={{ position: 'absolute', top: '22%', right: '4%', display: { xs: 'none', lg: 'block' } }}>
        <FeatureCard
          icon={<TriangleAlert size={16} />}
          title="Threat blocked"
          subtitle="Tab switch detected"
          accent="#ef4444"
          floating
          delay={0.2}
        />
      </Box>
      <Box sx={{ position: 'absolute', top: '60%', left: '2%', display: { xs: 'none', lg: 'block' } }}>
        <FeatureCard
          icon={<ShieldCheck size={16} />}
          title="Integrity score"
          subtitle="This week · 96"
          stat="96"
          accent="#22c55e"
          floating
          delay={0.6}
        />
      </Box>
    </>
  );
}

/**
 * Left-hand product showcase for the auth screens: brand, animated carousel,
 * trust indicators and floating feature cards.
 */
export function FeatureShowcase() {
  return (
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
      <BrandHeader align="left" size={36} />
      <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', py: 3 }}>
        <FeatureCarousel />
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap', pb: 1 }}>
        <TrustChip label="SOC 2 Type II" />
        <TrustChip label="GDPR ready" />
        <TrustChip label="99.99% uptime" />
        <TrustChip label="End-to-end encryption" />
      </Box>
      <FloatingCards />
    </Box>
  );
}
