import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import toast from 'react-hot-toast';
import { AuthLayout } from '../../components/AuthLayout';
import { useAuth } from '../../hooks/useAuth';

const schema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required').min(8, 'Password must be at least 8 characters'),
  organizationId: z.string().uuid('Enter a valid organization id').optional().or(z.literal('')),
  rememberMe: z.boolean()
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '', organizationId: '', rememberMe: true }
  });

  async function onSubmit(values: FormValues) {
    try {
      await login(
        {
          email: values.email,
          password: values.password,
          organizationId: values.organizationId ? values.organizationId : null
        },
        values.rememberMe
      );
      toast.success('Signed in successfully');
      navigate('/');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Sign in failed';
      toast.error(message);
    }
  }

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to your organization workspace.">
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="Work email"
          type="email"
          fullWidth
          margin="normal"
          autoComplete="email"
          autoFocus
          {...register('email')}
          error={Boolean(errors.email)}
          helperText={errors.email?.message}
        />
        <TextField
          label="Password"
          type="password"
          fullWidth
          margin="normal"
          autoComplete="current-password"
          {...register('password')}
          error={Boolean(errors.password)}
          helperText={errors.password?.message}
        />
        <TextField
          label="Organization ID (optional)"
          placeholder="00000000-0000-0000-0000-000000000000"
          fullWidth
          margin="normal"
          {...register('organizationId')}
          error={Boolean(errors.organizationId)}
          helperText={errors.organizationId?.message}
        />
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 1 }}>
          <FormControlLabel
            control={<Checkbox {...register('rememberMe')} />}
            label={<Typography variant="body2">Remember me</Typography>}
          />
          <Link to="/forgot-password" style={{ color: 'primary.main' }}>
            <Typography variant="body2" color="primary">
              Forgot password?
            </Typography>
          </Link>
        </Box>
        <Button type="submit" variant="contained" fullWidth size="large" disabled={isSubmitting} sx={{ mt: 2 }}>
          {isSubmitting ? 'Signing in...' : 'Sign in'}
        </Button>
      </form>
      <Box sx={{ mt: 3, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          New to Integrity Pro?{' '}
          <Link to="/register" style={{ color: 'primary.main' }}>
            Create an organization
          </Link>
        </Typography>
      </Box>
    </AuthLayout>
  );
}
