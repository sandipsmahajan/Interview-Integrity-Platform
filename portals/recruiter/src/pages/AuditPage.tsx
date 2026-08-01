import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { History } from '@mui/icons-material';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Typography from '@mui/material/Typography';
import { api } from '../lib/api';
import { formatDateTime, titleCase } from '../lib/format';
import { DataTable } from '../components/DataTable';
import { PageHeader } from '../components/PageHeader';
import type { ColumnDef } from '@tanstack/react-table';
import type { AuditEventResponse } from '../lib/types';

export function AuditPage() {
  const audit = useQuery({
    queryKey: ['audit-events'],
    queryFn: () => api.listAuditEvents({ page: 0, size: 200 })
  });

  const columns = useMemo<ColumnDef<AuditEventResponse, unknown>[]>(
    () => [
      {
        accessorKey: 'action',
        header: 'Action',
        cell: (info) => (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <History fontSize="small" color="action" />
            <Typography sx={{ fontWeight: 600 }}>{titleCase(info.getValue<string>())}</Typography>
          </Box>
        )
      },
      {
        accessorKey: 'resourceType',
        header: 'Resource',
        cell: (info) => <Chip size="small" label={titleCase(info.getValue<string>())} variant="outlined" />
      },
      {
        accessorKey: 'outcome',
        header: 'Outcome',
        cell: (info) => (
          <Chip
            size="small"
            label={titleCase(info.getValue<string>())}
            color={String(info.getValue()).toUpperCase() === 'SUCCESS' ? 'success' : 'error'}
            variant="outlined"
          />
        )
      },
      { accessorKey: 'actorType', header: 'Actor type', cell: (info) => titleCase(info.getValue<string>()) },
      { accessorKey: 'ipAddress', header: 'IP address', cell: (info) => info.getValue<string>() || '—' },
      { accessorKey: 'occurredAt', header: 'Occurred', cell: (info) => formatDateTime(info.getValue<string>()) }
    ],
    []
  );

  return (
    <Box>
      <PageHeader
        title="Audit Logs"
        subtitle="A compliance trail of actions across your organization."
      />
      <DataTable
        columns={columns}
        data={audit.data?.items ?? []}
        loading={audit.isLoading}
        searchPlaceholder="Search audit events..."
        searchKeys={['action', 'resourceType', 'actorType']}
        emptyTitle="No audit events"
        emptyDescription="Audit events will appear here as your team performs actions."
        pageSize={20}
      />
    </Box>
  );
}
