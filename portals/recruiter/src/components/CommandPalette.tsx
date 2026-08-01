import { useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import Divider from '@mui/material/Divider';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useEffect, useMemo, useState } from 'react';
import { NAV_GROUPS, NAV_ITEMS, type NavItem } from '../lib/nav';

export function CommandPalette({ open, onClose }: { open: boolean; onClose: () => void }) {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');

  useEffect(() => {
    if (open) {
      setQuery('');
    }
  }, [open]);

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        onClose();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose]);

  const results = useMemo(() => {
    const needle = query.trim().toLowerCase();
    const base = NAV_ITEMS.filter((item) => {
      if (!needle) return true;
      return (
        item.label.toLowerCase().includes(needle) ||
        item.keywords?.some((keyword) => keyword.includes(needle))
      );
    });
    return base.slice(0, 8);
  }, [query]);

  function go(item: NavItem) {
    onClose();
    navigate(item.path);
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm" slotProps={{ paper: { sx: { borderRadius: 3 } } }}>
      <DialogTitle sx={{ pb: 1 }}>Command Palette</DialogTitle>
      <Box sx={{ px: 3, pb: 2 }}>
        <TextField
          autoFocus
          fullWidth
          size="small"
          placeholder="Search pages... (Ctrl+K)"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          aria-label="Search pages"
        />
      </Box>
      <Divider />
      <Box sx={{ maxHeight: 360, overflowY: 'auto' }}>
        {results.length === 0 ? (
          <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>
            No pages match your search.
          </Typography>
        ) : (
          <List dense>
            {NAV_GROUPS.flatMap((group) => {
              const items = results.filter((item) => item.group === group);
              if (items.length === 0) return [];
              return [
                <Box key={group} sx={{ px: 2, pt: 1 }}>
                  <Typography variant="overline" color="text.secondary" sx={{ fontWeight: 700 }}>
                    {group}
                  </Typography>
                </Box>,
                ...items.map((item) => (
                  <ListItemButton key={item.path} onClick={() => go(item)}>
                    <ListItemIcon sx={{ minWidth: 36 }}>{item.icon}</ListItemIcon>
                    <ListItemText primary={item.label} />
                  </ListItemButton>
                ))
              ];
            })}
          </List>
        )}
      </Box>
    </Dialog>
  );
}
