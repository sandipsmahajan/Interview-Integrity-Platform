import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { BrandMark } from '../BrandLogo';

interface BrandHeaderProps {
  align?: 'left' | 'center';
  size?: number;
}

export function BrandHeader({ align = 'center', size = 40 }: BrandHeaderProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: align === 'center' ? 'center' : 'flex-start',
        gap: 1.5
      }}
    >
      <BrandMark size={size} />
      <Box>
        <Typography sx={{ fontWeight: 800, lineHeight: 1.15, fontSize: 20 }}>Integrity Pro</Typography>
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          Recruiter Portal
        </Typography>
      </Box>
    </Box>
  );
}
