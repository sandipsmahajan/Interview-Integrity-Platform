import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App } from './App';
import { CssBaseline } from '@mui/material';
import { Toaster } from 'react-hot-toast';
import './style.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000
    }
  }
});

createRoot(document.getElementById('root')!).render(
  <QueryClientProvider client={queryClient}>
    <CssBaseline />
    <App />
    <Toaster
      position="top-right"
      toastOptions={{
        style: { borderRadius: '10px', fontSize: '14px' }
      }}
    />
  </QueryClientProvider>
);
