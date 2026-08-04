import Box from '@mui/material/Box';
import { BrandBanner } from '../BrandLogo';

interface BrandHeaderProps {
  align?: 'left' | 'center';
  maxWidth?: number | string;
}

/**
 * Header branding for the auth screens: the official horizontal Integrity Pro
 * logo. No duplicate logo, product name, or subtitle appears inside the form.
 */
export function BrandHeader({ align = 'left', maxWidth = 190 }: BrandHeaderProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: align === 'center' ? 'center' : 'flex-start'
      }}
    >
      <BrandBanner maxWidth={maxWidth} />
    </Box>
  );
}
