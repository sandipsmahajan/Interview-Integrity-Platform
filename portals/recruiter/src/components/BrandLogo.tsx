import Box from '@mui/material/Box';

interface BrandMarkProps {
  size?: number;
}

export function BrandMark({ size = 34 }: BrandMarkProps) {
  return (
    <Box
      component="img"
      src="/logos/vertical_logo.jpeg"
      alt="Integrity Pro"
      loading="lazy"
      sx={{
        width: size,
        height: size,
        borderRadius: 2,
        objectFit: 'cover',
        bgcolor: '#fff',
        flexShrink: 0
      }}
    />
  );
}

interface BrandBannerProps {
  maxWidth?: number | string;
}

export function BrandBanner({ maxWidth = 200 }: BrandBannerProps) {
  return (
    <Box
      component="img"
      src="/logos/horizontal_logo.jpeg"
      alt="Integrity Pro"
      loading="lazy"
      sx={{
        width: '100%',
        maxWidth,
        height: 'auto',
        borderRadius: 2,
        objectFit: 'contain',
        bgcolor: '#fff'
      }}
    />
  );
}
