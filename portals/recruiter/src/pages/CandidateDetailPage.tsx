import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import {
  ArrowBack,
  Delete,
  Edit,
  FilePresent,
  Link as LinkIcon,
  NoteAdd,
  Person,
  PushPin,
  Upload
} from '@mui/icons-material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';
import toast from 'react-hot-toast';
import { api } from '../lib/api';
import { formatDate } from '../lib/format';
import { PageHeader } from '../components/PageHeader';
import { EmptyState } from '../components/EmptyState';
import BoxLoader from '../components/BoxLoader';
import type {
  CandidateDocumentResponse,
  CandidateNoteResponse,
  CandidateProfileResponse
} from '../lib/types';

const profileSchema = z.object({
  headline: z.string().max(200).optional().or(z.literal('')),
  bio: z.string().max(2000).optional().or(z.literal('')),
  location: z.string().max(120).optional().or(z.literal('')),
  timezone: z.string().max(60).optional().or(z.literal('')),
  resumeSummary: z.string().max(4000).optional().or(z.literal('')),
  linkedinUrl: z.string().max(255).optional().or(z.literal('')),
  githubUrl: z.string().max(255).optional().or(z.literal('')),
  skills: z.string().optional().or(z.literal('')),
  experienceYears: z.union([z.number().min(0).max(60), z.null()]).optional()
});

type ProfileFormValues = z.infer<typeof profileSchema>;

const NULL_VALUE = '—';

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function CandidateDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [tab, setTab] = useState(0);

  const candidates = useQuery({ queryKey: ['candidates'], queryFn: () => api.listCandidates() });

  const candidate = useMemo(
    () => candidates.data?.find((c) => c.id === id),
    [candidates.data, id]
  );

  if (candidates.isLoading) return <BoxLoader rows={6} height={40} />;
  if (!candidate) return <Typography sx={{ p: 4 }}>Candidate not found</Typography>;

  return (
    <Box>
      <PageHeader
        title={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <IconButton component={Link} to="/candidates" size="small" aria-label="Back to candidates">
              <ArrowBack />
            </IconButton>
            <Box
              sx={{
                width: 36,
                height: 36,
                borderRadius: '50%',
                bgcolor: 'secondary.main',
                color: 'secondary.contrastText',
                display: 'grid',
                placeItems: 'center',
                fontWeight: 700,
                fontSize: 14
              }}
            >
              {candidate.fullName
                .split(' ')
                .slice(0, 2)
                .map((p) => p[0]?.toUpperCase() ?? '')
                .join('')}
            </Box>
            {candidate.fullName}
          </Box>
        }
        subtitle={candidate.email}
      />
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tab label="Profile" />
        <Tab label="Documents" />
        <Tab label="Notes" />
      </Tabs>
      {tab === 0 && <ProfileTab candidateId={id!} />}
      {tab === 1 && <DocumentsTab candidateId={id!} />}
      {tab === 2 && <NotesTab candidateId={id!} />}
    </Box>
  );
}

function ProfileTab({ candidateId }: { candidateId: string }) {
  const [editing, setEditing] = useState(false);
  const queryClient = useQueryClient();

  const profile = useQuery({
    queryKey: ['candidateProfile', candidateId],
    queryFn: () => api.getCandidateProfile(candidateId)
  });

  const updateMutation = useMutation({
    mutationFn: (payload: Parameters<typeof api.updateCandidateProfile>[1]) =>
      api.updateCandidateProfile(candidateId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['candidateProfile', candidateId] });
      toast.success('Profile updated');
      setEditing(false);
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Update failed')
  });

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    values: profile.data ? profileToForm(profile.data) : undefined
  });

  if (profile.isLoading) return <BoxLoader rows={4} height={30} />;

  const data = profile.data;

  if (!editing) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Profile</Typography>
            <Button startIcon={<Edit />} onClick={() => setEditing(true)}>
              Edit
            </Button>
          </Box>
          <Grid container spacing={3}>
            <Field label="Headline" value={data?.headline} />
            <Field label="Location" value={data?.location} />
            <Field label="Timezone" value={data?.timezone} />
            <Field label="Experience" value={data?.experienceYears != null ? `${data.experienceYears} years` : null} />
            <Grid size={6}>
              <Typography variant="body2" color="text.secondary">LinkedIn</Typography>
              {data?.linkedinUrl ? (
                <Box component="a" href={data.linkedinUrl} target="_blank" rel="noopener noreferrer" sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: 'primary.main', textDecoration: 'none' }}>
                  <LinkIcon fontSize="small" />{data.linkedinUrl}
                </Box>
              ) : <Typography>{NULL_VALUE}</Typography>}
            </Grid>
            <Grid size={6}>
              <Typography variant="body2" color="text.secondary">GitHub</Typography>
              {data?.githubUrl ? (
                <Box component="a" href={data.githubUrl} target="_blank" rel="noopener noreferrer" sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: 'primary.main', textDecoration: 'none' }}>
                  <LinkIcon fontSize="small" />{data.githubUrl}
                </Box>
              ) : <Typography>{NULL_VALUE}</Typography>}
            </Grid>
            <Grid size={12}>
              <Typography variant="body2" color="text.secondary">Skills</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 0.5 }}>
                {data?.skills?.length ? data.skills.map((s) => <Chip key={s} label={s} size="small" />) : NULL_VALUE}
              </Box>
            </Grid>
            <Grid size={12}>
              <Divider />
            </Grid>
            <Field label="Bio" value={data?.bio} />
            <Field label="Resume Summary" value={data?.resumeSummary} />
          </Grid>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">Edit Profile</Typography>
          <Button
            onClick={() => {
              setEditing(false);
              reset();
            }}
          >
            Cancel
          </Button>
        </Box>
        <Box component="form" onSubmit={handleSubmit((values) => updateMutation.mutate(formToProfile(values)))} sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField label="Headline" fullWidth {...register('headline')} error={Boolean(errors.headline)} helperText={errors.headline?.message} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField label="Location" fullWidth {...register('location')} error={Boolean(errors.location)} helperText={errors.location?.message} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField label="Timezone" fullWidth placeholder="America/New_York" {...register('timezone')} error={Boolean(errors.timezone)} helperText={errors.timezone?.message} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField label="Experience Years" type="number" fullWidth slotProps={{ htmlInput: { step: 0.5 } }} {...register('experienceYears')} error={Boolean(errors.experienceYears)} helperText={errors.experienceYears?.message} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField label="LinkedIn URL" fullWidth placeholder="https://linkedin.com/in/..." {...register('linkedinUrl')} error={Boolean(errors.linkedinUrl)} helperText={errors.linkedinUrl?.message} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField label="GitHub URL" fullWidth placeholder="https://github.com/..." {...register('githubUrl')} error={Boolean(errors.githubUrl)} helperText={errors.githubUrl?.message} />
            </Grid>
            <Grid size={12}>
              <TextField label="Skills" fullWidth placeholder="React, TypeScript, Node.js" helperText="Comma-separated list of skills" {...register('skills')} error={Boolean(errors.skills)} />
            </Grid>
            <Grid size={12}>
              <TextField label="Bio" fullWidth multiline rows={3} {...register('bio')} error={Boolean(errors.bio)} helperText={errors.bio?.message} />
            </Grid>
            <Grid size={12}>
              <TextField label="Resume Summary" fullWidth multiline rows={4} {...register('resumeSummary')} error={Boolean(errors.resumeSummary)} helperText={errors.resumeSummary?.message} />
            </Grid>
          </Grid>
          <Button type="submit" variant="contained" disabled={updateMutation.isPending} sx={{ alignSelf: 'flex-start' }}>
            {updateMutation.isPending ? 'Saving...' : 'Save changes'}
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
}

function DocumentsTab({ candidateId }: { candidateId: string }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const documents = useQuery({
    queryKey: ['candidateDocuments', candidateId],
    queryFn: () => api.listCandidateDocuments(candidateId)
  });

  const createMutation = useMutation({
    mutationFn: (payload: Parameters<typeof api.createCandidateDocument>[1]) =>
      api.createCandidateDocument(candidateId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['candidateDocuments', candidateId] });
      toast.success('Document added');
      setOpen(false);
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to add document')
  });

  const deleteMutation = useMutation({
    mutationFn: (documentId: string) => api.deleteCandidateDocument(candidateId, documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['candidateDocuments', candidateId] });
      toast.success('Document removed');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to delete document')
  });

  if (documents.isLoading) return <BoxLoader rows={3} height={30} />;

  const docs = documents.data ?? [];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <Button variant="contained" startIcon={<Upload />} onClick={() => setOpen(true)}>
          Add document
        </Button>
      </Box>
      {docs.length === 0 ? (
        <EmptyState icon={<FilePresent />} title="No documents" description="Upload a resume or other attachments for this candidate." />
      ) : (
        <Stack spacing={1.5}>
          {docs.map((doc) => (
            <DocumentRow key={doc.id} doc={doc} onDelete={() => deleteMutation.mutate(doc.id)} deleting={deleteMutation.isPending} />
          ))}
        </Stack>
      )}
      <AddDocumentDialog
        open={open}
        onClose={() => setOpen(false)}
        loading={createMutation.isPending}
        onSubmit={(payload) => createMutation.mutate(payload)}
      />
    </Box>
  );
}

function DocumentRow({ doc, onDelete, deleting }: { doc: CandidateDocumentResponse; onDelete: () => void; deleting: boolean }) {
  return (
    <Card variant="outlined">
      <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <FilePresent color="action" />
        <Box sx={{ flex: 1 }}>
          <Typography variant="body2" sx={{ fontWeight: 600 }}>{doc.name}</Typography>
          <Typography variant="caption" color="text.secondary">
            {doc.contentType || 'Unknown type'} / {formatBytes(doc.sizeBytes)} / {formatDate(doc.uploadedAt)}
          </Typography>
        </Box>
        <Tooltip title="Delete">
          <IconButton size="small" onClick={onDelete} disabled={deleting}>
            <Delete fontSize="small" />
          </IconButton>
        </Tooltip>
      </CardContent>
    </Card>
  );
}

function NotesTab({ candidateId }: { candidateId: string }) {
  const [editingNote, setEditingNote] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const notes = useQuery({
    queryKey: ['candidateNotes', candidateId],
    queryFn: () => api.listCandidateNotes(candidateId)
  });

  const createMutation = useMutation({
    mutationFn: (payload: { body: string }) => api.createCandidateNote(candidateId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['candidateNotes', candidateId] });
      toast.success('Note added');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to add note')
  });

  const updateMutation = useMutation({
    mutationFn: ({ noteId, body }: { noteId: string; body: string }) =>
      api.updateCandidateNote(candidateId, noteId, { body }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['candidateNotes', candidateId] });
      toast.success('Note updated');
      setEditingNote(null);
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to update note')
  });

  const deleteMutation = useMutation({
    mutationFn: (noteId: string) => api.deleteCandidateNote(candidateId, noteId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['candidateNotes', candidateId] });
      toast.success('Note deleted');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to delete note')
  });

  if (notes.isLoading) return <BoxLoader rows={4} height={30} />;

  const noteList = notes.data ?? [];

  return (
    <Box>
      <NewNoteForm onSubmit={(body) => createMutation.mutate({ body })} loading={createMutation.isPending} />
      <Box sx={{ mt: 2 }}>
        {noteList.length === 0 ? (
          <EmptyState icon={<NoteAdd />} title="No notes yet" description="Add notes about this candidate for your team." />
        ) : (
          <Stack spacing={1.5}>
            {noteList.map((note) => (
              <NoteCard
                key={note.id}
                note={note}
                editing={editingNote === note.id}
                onEdit={() => setEditingNote(note.id)}
                onCancelEdit={() => setEditingNote(null)}
                onSave={(body) => updateMutation.mutate({ noteId: note.id, body })}
                onDelete={() => deleteMutation.mutate(note.id)}
                saving={updateMutation.isPending}
                deleting={deleteMutation.isPending}
              />
            ))}
          </Stack>
        )}
      </Box>
    </Box>
  );
}

function NewNoteForm({ onSubmit, loading }: { onSubmit: (body: string) => void; loading: boolean }) {
  const [body, setBody] = useState('');

  const handleSubmit = () => {
    if (!body.trim()) return;
    onSubmit(body.trim());
    setBody('');
  };

  return (
    <Card variant="outlined">
      <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, py: 2, '&:last-child': { pb: 2 } }}>
        <TextField
          label="Add a note"
          multiline
          rows={2}
          fullWidth
          value={body}
          onChange={(e) => setBody(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) handleSubmit();
          }}
        />
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="caption" color="text.secondary">Cmd+Enter to submit</Typography>
          <Button variant="contained" size="small" startIcon={<NoteAdd />} onClick={handleSubmit} disabled={loading || !body.trim()}>
            {loading ? 'Adding...' : 'Add note'}
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
}

function NoteCard({
  note,
  editing,
  onEdit,
  onCancelEdit,
  onSave,
  onDelete,
  saving,
  deleting
}: {
  note: CandidateNoteResponse;
  editing: boolean;
  onEdit: () => void;
  onCancelEdit: () => void;
  onSave: (body: string) => void;
  onDelete: () => void;
  saving: boolean;
  deleting: boolean;
}) {
  const [body, setBody] = useState(note.body);

  return (
    <Card variant="outlined">
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5 }}>
          <Person color="action" sx={{ mt: 0.3 }} />
          <Box sx={{ flex: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
              {note.pinned && <PushPin fontSize="small" color="warning" />}
              <Typography variant="caption" color="text.secondary">{formatDate(note.createdAt)}</Typography>
              {note.updatedAt !== note.createdAt && (
                <Typography variant="caption" color="text.secondary">(edited)</Typography>
              )}
            </Box>
            {editing ? (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                <TextField multiline rows={2} fullWidth size="small" value={body} onChange={(e) => setBody(e.target.value)} />
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button size="small" variant="contained" onClick={() => onSave(body.trim())} disabled={saving || !body.trim()}>
                    Save
                  </Button>
                  <Button size="small" onClick={onCancelEdit}>Cancel</Button>
                </Box>
              </Box>
            ) : (
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{note.body}</Typography>
            )}
          </Box>
          {!editing && (
            <Box sx={{ display: 'flex', gap: 0.5 }}>
              <Tooltip title="Edit">
                <IconButton size="small" onClick={onEdit}><Edit fontSize="small" /></IconButton>
              </Tooltip>
              <Tooltip title="Delete">
                <IconButton size="small" onClick={onDelete} disabled={deleting}><Delete fontSize="small" /></IconButton>
              </Tooltip>
            </Box>
          )}
        </Box>
      </CardContent>
    </Card>
  );
}

function AddDocumentDialog({ open, onClose, loading, onSubmit }: { open: boolean; onClose: () => void; loading: boolean; onSubmit: (payload: Parameters<typeof api.createCandidateDocument>[1]) => void }) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    setValue
  } = useForm<DocFormValues>({
    resolver: zodResolver(docSchema),
    defaultValues: { storageObjectId: '', name: '', contentType: '', sizeBytes: 0 }
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setValue('name', file.name);
    setValue('contentType', file.type || '');
    setValue('sizeBytes', file.size);
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Add document</DialogTitle>
      <Box
        component="form"
        onSubmit={handleSubmit((values) => {
          onSubmit({ storageObjectId: values.storageObjectId, name: values.name, contentType: values.contentType || null, sizeBytes: values.sizeBytes });
          reset();
        })}
        onReset={() => { reset(); onClose(); }}
      >
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography variant="body2" color="text.secondary">
            Select a file to populate the metadata fields, then provide the storage object ID from the upload service.
          </Typography>
          <Button variant="outlined" component="label" startIcon={<Upload />}>
            Select file (metadata only)
            <input type="file" hidden onChange={handleFileChange} />
          </Button>
          <TextField
            label="Storage Object ID"
            fullWidth
            {...register('storageObjectId')}
            error={Boolean(errors.storageObjectId)}
            helperText={errors.storageObjectId?.message}
          />
          <TextField
            label="File Name"
            fullWidth
            {...register('name')}
            error={Boolean(errors.name)}
            helperText={errors.name?.message}
          />
          <TextField
            label="Content Type"
            fullWidth
            placeholder="application/pdf"
            {...register('contentType')}
            error={Boolean(errors.contentType)}
            helperText={errors.contentType?.message}
          />
          <TextField
            label="Size (bytes)"
            type="number"
            fullWidth
            {...register('sizeBytes')}
            error={Boolean(errors.sizeBytes)}
            helperText={errors.sizeBytes?.message}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button type="reset">Cancel</Button>
          <Button type="submit" variant="contained" startIcon={<Upload />} disabled={loading}>
            {loading ? 'Adding...' : 'Add document'}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

function Field({ label, value }: { label: string; value?: string | null }) {
  return (
    <Grid size={{ xs: 12, sm: 6 }}>
      <Typography variant="body2" color="text.secondary">{label}</Typography>
      <Typography>{value || NULL_VALUE}</Typography>
    </Grid>
  );
}

function profileToForm(p: CandidateProfileResponse): ProfileFormValues {
  return {
    headline: p.headline || '',
    bio: p.bio || '',
    location: p.location || '',
    timezone: p.timezone || '',
    resumeSummary: p.resumeSummary || '',
    linkedinUrl: p.linkedinUrl || '',
    githubUrl: p.githubUrl || '',
    skills: p.skills?.join(', ') || '',
    experienceYears: p.experienceYears ?? null
  };
}

function formToProfile(v: ProfileFormValues) {
  return {
    headline: v.headline || null,
    bio: v.bio || null,
    location: v.location || null,
    timezone: v.timezone || null,
    resumeSummary: v.resumeSummary || null,
    linkedinUrl: v.linkedinUrl || null,
    githubUrl: v.githubUrl || null,
    skills: v.skills ? v.skills.split(',').map((s) => s.trim()).filter(Boolean) : null,
    experienceYears: v.experienceYears ?? null,
    attributes: null
  };
}
