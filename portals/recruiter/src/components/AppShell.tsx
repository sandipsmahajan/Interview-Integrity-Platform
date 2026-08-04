import { useState, type ReactNode } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import {
  DarkModeOutlined,
  HelpOutlined,
  KeyboardCommandKey,
  LightModeOutlined,
  Logout,
  Menu as MenuIcon,
  NotificationsNone,
  Search
} from '@mui/icons-material';
import AppBar from '@mui/material/AppBar';
import Avatar from '@mui/material/Avatar';
import Badge from '@mui/material/Badge';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import IconButton from '@mui/material/IconButton';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import ListSubheader from '@mui/material/ListSubheader';
import MenuItem from '@mui/material/MenuItem';
import Menu from '@mui/material/Menu';
import Toolbar from '@mui/material/Toolbar';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { initials, formatRelative } from '../lib/format';
import { NAV_GROUPS, NAV_ITEMS, navItemForPath } from '../lib/nav';
import { useAuth, useCurrentUserId } from '../hooks/useAuth';
import { CommandPalette } from './CommandPalette';
import { BrandBanner } from './BrandLogo';

const DRAWER_WIDTH = 260;

interface AppShellProps {
  mode: 'light' | 'dark';
  onToggleMode: () => void;
  children: ReactNode;
}

export function AppShell({ mode, onToggleMode, children }: AppShellProps) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [userMenuAnchor, setUserMenuAnchor] = useState<HTMLElement | null>(null);
  const { user, logout } = useAuth();
  const userId = useCurrentUserId();
  const navigate = useNavigate();
  const location = useLocation();

  const { data: unread = 0 } = useQuery({
    queryKey: ['notifications', userId],
    queryFn: async () => {
      if (!userId) return 0;
      const items = await api.listNotifications(userId);
      return items.filter((item) => !item.readAt && item.status !== 'FAILED').length;
    },
    enabled: Boolean(userId),
    refetchInterval: 30_000
  });

  const current = navItemForPath(location.pathname);

  const drawer = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar>
        <BrandBanner maxWidth={170} />
      </Toolbar>
      <Box sx={{ px: 2, pb: 1 }}>
        <Tooltip title="Search pages (Ctrl+K)">
          <Box
            role="button"
            tabIndex={0}
            aria-label="Open command palette"
            onClick={() => setPaletteOpen(true)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') setPaletteOpen(true);
            }}
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1,
              px: 1.5,
              py: 1,
              borderRadius: 2,
              border: 1,
              borderColor: 'divider',
              color: 'text.secondary',
              cursor: 'pointer',
              '&:hover': { borderColor: 'primary.main' }
            }}
          >
            <Search fontSize="small" />
            <Typography variant="body2" sx={{ flex: 1 }}>
              Search
            </Typography>
            <Box component="span" sx={{ border: 1, borderColor: 'divider', borderRadius: 0.75, px: 0.5, fontSize: 11 }}>
              Ctrl K
            </Box>
          </Box>
        </Tooltip>
      </Box>
      <Box sx={{ flex: 1, overflowY: 'auto' }}>
        {NAV_GROUPS.map((group) => {
          const items = NAV_ITEMS.filter((item) => item.group === group);
          if (items.length === 0) return null;
          return (
            <List
              key={group}
              subheader={
                <ListSubheader sx={{ bgcolor: 'transparent', lineHeight: '32px', fontWeight: 700 }}>{group}</ListSubheader>
              }
            >
              {items.map((item) => (
                <NavLink key={item.path} to={item.path} style={{ color: 'inherit', textDecoration: 'none' }}>
                  {({ isActive }) => (
                    <ListItemButton
                      onClick={() => setMobileOpen(false)}
                      sx={{
                        mx: 1,
                        borderRadius: 1.5,
                        ...(isActive && {
                          bgcolor: 'primary.main',
                          color: 'primary.contrastText',
                          '&:hover': { bgcolor: 'primary.dark' }
                        })
                      }}
                    >
                      <ListItemIcon sx={{ minWidth: 36, color: isActive ? 'inherit' : 'text.secondary' }}>
                        {item.icon}
                      </ListItemIcon>
                      <ListItemText primary={item.label} slotProps={{ primary: { sx: { fontWeight: 600, fontSize: 14 } } }} />
                    </ListItemButton>
                  )}
                </NavLink>
              ))}
            </List>
          );
        })}
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper' }}
      >
        <Toolbar>
          <IconButton edge="start" onClick={() => setMobileOpen(true)} sx={{ mr: 1, display: { md: 'none' } }} aria-label="Open navigation">
            <MenuIcon />
          </IconButton>
          <Typography variant="subtitle1" sx={{ fontWeight: 700, display: { xs: 'none', sm: 'block' } }}>
            {current?.label ?? 'Integrity Pro'}
          </Typography>
          <Box sx={{ flex: 1 }} />
          <IconButton onClick={() => setPaletteOpen(true)} aria-label="Command palette (Ctrl+K)">
            <KeyboardCommandKey />
          </IconButton>
          <IconButton onClick={onToggleMode} aria-label={mode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
            {mode === 'dark' ? <LightModeOutlined /> : <DarkModeOutlined />}
          </IconButton>
          <Tooltip title="Notifications">
            <IconButton onClick={() => setNotificationsOpen(true)} aria-label="Open notifications">
              <Badge badgeContent={unread} color="error" max={99}>
                <NotificationsNone />
              </Badge>
            </IconButton>
          </Tooltip>
          <IconButton onClick={(event) => setUserMenuAnchor(event.currentTarget)} aria-label="Account menu" sx={{ ml: 0.5 }}>
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main', fontSize: 14 }}>
              {initials(user?.displayName)}
            </Avatar>
          </IconButton>
          <Menu
            anchorEl={userMenuAnchor}
            open={Boolean(userMenuAnchor)}
            onClose={() => setUserMenuAnchor(null)}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            transformOrigin={{ vertical: 'top', horizontal: 'right' }}
          >
            <Box sx={{ px: 2, py: 1 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                {user?.displayName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {user?.email}
              </Typography>
            </Box>
            <MenuItem
              onClick={() => {
                setUserMenuAnchor(null);
                navigate('/settings?tab=security');
              }}
            >
              <ListItemIcon sx={{ minWidth: 32 }}>
                <NotificationsNone fontSize="small" />
              </ListItemIcon>
              Security & Sessions
            </MenuItem>
            <MenuItem
              onClick={() => {
                setUserMenuAnchor(null);
                navigate('/help');
              }}
            >
              <ListItemIcon sx={{ minWidth: 32 }}>
                <HelpOutlined fontSize="small" />
              </ListItemIcon>
              Help Center
            </MenuItem>
            <MenuItem
              onClick={async () => {
                setUserMenuAnchor(null);
                await logout();
                navigate('/login');
              }}
            >
              <ListItemIcon sx={{ minWidth: 32 }}>
                <Logout fontSize="small" />
              </ListItemIcon>
              Sign out
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 }, display: { xs: 'none', md: 'block' } }}>
        <Drawer
          variant="permanent"
          open
          sx={{
            width: DRAWER_WIDTH,
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box', borderRight: 1, borderColor: 'divider' }
          }}
        >
          {drawer}
        </Drawer>
      </Box>
      <Box component="nav" sx={{ width: { xs: DRAWER_WIDTH }, display: { xs: 'block', md: 'none' } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' }
          }}
        >
          {drawer}
        </Drawer>
      </Box>

      <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, md: 3 }, pt: { xs: 10, md: 12 }, minWidth: 0 }}>
        {children}
      </Box>

      <NotificationsDrawer open={notificationsOpen} onClose={() => setNotificationsOpen(false)} />
      <CommandPalette open={paletteOpen} onClose={() => setPaletteOpen(false)} />
    </Box>
  );
}

function NotificationsDrawer({ open, onClose }: { open: boolean; onClose: () => void }) {
  const userId = useCurrentUserId();
  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ['notifications-drawer', userId],
    queryFn: () => {
      if (!userId) return Promise.resolve([]);
      return api.listNotifications(userId);
    },
    enabled: Boolean(userId),
    refetchInterval: 30_000
  });

  const recent = notifications.slice(0, 12);

  return (
    <Drawer anchor="right" open={open} onClose={onClose}>
      <Box sx={{ width: 360, p: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: 800, mb: 2 }}>
          Notifications
        </Typography>
        {isLoading ? (
          <Typography variant="body2" color="text.secondary">
            Loading...
          </Typography>
        ) : recent.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            You are all caught up.
          </Typography>
        ) : (
          <List>
            {recent.map((item) => (
              <Box key={item.id} sx={{ py: 1, borderBottom: 1, borderColor: 'divider', mb: 1 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                  {item.subject}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ fontSize: 13 }}>
                  {item.body}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {formatRelative(item.createdAt)}
                </Typography>
              </Box>
            ))}
          </List>
        )}
      </Box>
    </Drawer>
  );
}
