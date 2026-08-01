import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Inbox, MarkEmailRead } from '@mui/icons-material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import toast from 'react-hot-toast';
import { api } from '../lib/api';
import { formatRelative, titleCase } from '../lib/format';
import { PageHeader } from '../components/PageHeader';
import { EmptyState } from '../components/EmptyState';
import BoxLoader from '../components/BoxLoader';
import { useCurrentUserId } from '../hooks/useAuth';

type Filter = 'ALL' | 'UNREAD' | 'READ';

export function NotificationsPage() {
  const userId = useCurrentUserId();
  const [filter, setFilter] = useState<Filter>('ALL');
  const queryClient = useQueryClient();

  const notifications = useQuery({
    queryKey: ['notifications-page', userId],
    queryFn: () => (userId ? api.listNotifications(userId) : Promise.resolve([])),
    enabled: Boolean(userId)
  });

  const readMutation = useMutation({
    mutationFn: api.markNotificationRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications-page', userId] });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('Marked as read');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Update failed')
  });

  const filtered = useMemo(() => {
    const list = notifications.data ?? [];
    if (filter === 'UNREAD') return list.filter((item) => !item.readAt);
    if (filter === 'READ') return list.filter((item) => Boolean(item.readAt));
    return list;
  }, [notifications.data, filter]);

  const filters: { key: Filter; label: string }[] = [
    { key: 'ALL', label: 'All' },
    { key: 'UNREAD', label: 'Unread' },
    { key: 'READ', label: 'Read' }
  ];

  return (
    <Box>
      <PageHeader title="Notifications" subtitle="Interviews, system alerts and critical updates." />
      <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
        {filters.map((item) => (
          <Chip
            key={item.key}
            label={item.label}
            clickable
            color={filter === item.key ? 'primary' : 'default'}
            onClick={() => setFilter(item.key)}
          />
        ))}
      </Stack>
      {notifications.isLoading ? (
        <BoxLoader rows={5} />
      ) : filtered.length === 0 ? (
        <Card variant="outlined">
          <EmptyState
            icon={<Inbox />}
            title="No notifications"
            description={filter === 'ALL' ? 'Notifications about interviews and system events will appear here.' : 'Nothing in this filter.'}
          />
        </Card>
      ) : (
        <Card variant="outlined">
          <CardContent>
            <Stack divider={<Divider />} spacing={2}>
              {filtered.slice(0, 50).map((item) => (
                <Box
                  key={item.id}
                  sx={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: 2,
                    p: 1,
                    borderRadius: 1,
                    ...(!item.readAt && { bgcolor: 'action.hover' })
                  }}
                >
                  <Box sx={{ flex: 1 }}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 0.5 }}>
                      <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                        {item.subject}
                      </Typography>
                      <Chip
                        size="small"
                        label={titleCase(item.priority)}
                        color={item.priority === 'URGENT' || item.priority === 'HIGH' ? 'error' : item.priority === 'MEDIUM' ? 'warning' : 'default'}
                        variant="outlined"
                      />
                    </Stack>
                    <Typography variant="body2" color="text.secondary">
                      {item.body}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {formatRelative(item.createdAt)}
                    </Typography>
                  </Box>
                  {!item.readAt ? (
                    <Button size="small" startIcon={<MarkEmailRead />} onClick={() => readMutation.mutate(item.id)}>
                      Mark read
                    </Button>
                  ) : null}
                </Box>
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
