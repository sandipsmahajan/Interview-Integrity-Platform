import { AuthenticationLayout } from '../../components/auth/AuthenticationLayout';
import { LoginPanel } from '../../components/auth/LoginPanel';

export function LoginPage() {
  return (
    <AuthenticationLayout>
      <LoginPanel />
    </AuthenticationLayout>
  );
}
