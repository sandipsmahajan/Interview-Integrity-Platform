import {
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
  type ColumnDef,
  type SortingState
} from '@tanstack/react-table';
import {
  ArrowDownward,
  ArrowUpward,
  ChevronLeft,
  ChevronRight,
  Search
} from '@mui/icons-material';
import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { useMemo, useState } from 'react';
import { EmptyState } from './EmptyState';
import BoxLoader from './BoxLoader';

interface DataTableProps<T> {
  columns: ColumnDef<T, unknown>[];
  data: T[];
  loading?: boolean;
  searchPlaceholder?: string;
  searchKeys?: (keyof T)[];
  toolbar?: React.ReactNode;
  title?: string;
  emptyTitle?: string;
  emptyDescription?: string;
  pageSize?: number;
}

export function DataTable<T>({
  columns,
  data,
  loading,
  searchPlaceholder = 'Search...',
  searchKeys,
  toolbar,
  title,
  emptyTitle = 'No records',
  emptyDescription,
  pageSize = 10
}: DataTableProps<T>) {
  const [sorting, setSorting] = useState<SortingState>([]);
  const [globalFilter, setGlobalFilter] = useState('');

  const table = useReactTable({
    data,
    columns,
    state: { sorting, globalFilter },
    onSortingChange: setSorting,
    onGlobalFilterChange: setGlobalFilter,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    globalFilterFn: (row, _columnId, filterValue) => {
      const needle = String(filterValue).toLowerCase();
      if (!needle) return true;
      const keys = searchKeys ?? Object.keys(row.original as object);
      return keys.some((key) => String((row.original as Record<string, unknown>)[key as string] ?? '').toLowerCase().includes(needle));
    },
    initialState: { pagination: { pageSize } }
  });

  const rows = useMemo(() => table.getRowModel().rows, [table]);

  if (loading) {
    return <BoxLoader rows={5} />;
  }

  if (rows.length === 0) {
    return (
      <Paper variant="outlined">
        <EmptyState
          title={globalFilter ? 'No matching results' : emptyTitle}
          description={globalFilter ? 'Try adjusting your search query.' : emptyDescription}
        />
      </Paper>
    );
  }

  const pageCount = table.getPageCount();

  return (
    <Paper variant="outlined" sx={{ width: '100%', overflow: 'hidden' }}>
      <Toolbar sx={{ gap: 2, px: 2, minHeight: 56 }}>
        {title ? (
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            {title}
          </Typography>
        ) : null}
        <Box sx={{ flex: 1 }} />
        <TextField
          size="small"
          value={globalFilter}
          onChange={(event) => setGlobalFilter(event.target.value)}
          placeholder={searchPlaceholder}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <Search fontSize="small" />
                </InputAdornment>
              )
            }
          }}
          sx={{ width: 240 }}
          aria-label={searchPlaceholder}
        />
        {toolbar}
      </Toolbar>
      <TableContainer>
        <Table size="small">
          <TableHead>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id}>
                {headerGroup.headers.map((header) => (
                  <TableCell
                    key={header.id}
                    onClick={header.column.getToggleSortingHandler()}
                    sx={{ fontWeight: 700, cursor: 'pointer', userSelect: 'none' }}
                    aria-sort={header.column.getIsSorted() === 'asc' ? 'ascending' : header.column.getIsSorted() === 'desc' ? 'descending' : undefined}
                  >
                    <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
                      <span>{flexRender(header.column.columnDef.header, header.getContext())}</span>
                      {header.column.getIsSorted() === 'asc' ? <ArrowUpward fontSize="inherit" /> : null}
                      {header.column.getIsSorted() === 'desc' ? <ArrowDownward fontSize="inherit" /> : null}
                    </Stack>
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableHead>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={row.id} hover sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                {row.getVisibleCells().map((cell) => (
                  <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {pageCount > 1 ? (
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 1, px: 2, py: 1 }}>
          <IconButton
            size="small"
            onClick={() => table.previousPage()}
            disabled={!table.getCanPreviousPage()}
            aria-label="Previous page"
          >
            <ChevronLeft fontSize="small" />
          </IconButton>
          <Typography variant="body2" color="text.secondary">
            Page {table.getState().pagination.pageIndex + 1} of {pageCount}
          </Typography>
          <IconButton
            size="small"
            onClick={() => table.nextPage()}
            disabled={!table.getCanNextPage()}
            aria-label="Next page"
          >
            <ChevronRight fontSize="small" />
          </IconButton>
        </Box>
      ) : null}
    </Paper>
  );
}

export default DataTable;
