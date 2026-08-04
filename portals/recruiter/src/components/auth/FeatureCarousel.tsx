import { useEffect, useState, type ReactNode } from 'react';
import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import {
  CheckCircle2,
  Database,
  FileText,
  Fingerprint,
  Globe,
  KeyRound,
  LockKeyhole,
  Server,
  ShieldCheck,
  TriangleAlert,
  Webcam
} from 'lucide-react';
import { DashboardPreview, IntegrityGauge, MiniBars, Pill } from './DashboardPreview';

interface Slide {
  id: string;
  kicker: string;
  accent: string;
  title: string;
  description: string;
  visual: ReactNode;
}

const SERVER_ICONS: Record<string, ReactNode> = {
  gateway: <Server size={14} />,
  identity: <KeyRound size={14} />,
  discovery: <Globe size={14} />,
  interviews: <Webcam size={14} />,
  reports: <FileText size={14} />,
  database: <Database size={14} />
};

function SecurePlatformVisual() {
  const sessions = [
    { title: 'Senior Frontend · Round 2', status: 'Live', color: '#22c55e' },
    { title: 'Backend System Design', status: 'Watching', color: '#3b82f6' },
    { title: 'Behavioral · Round 1', status: 'Queued', color: '#f59e0b' }
  ];
  return (
    <DashboardPreview>
      <Box sx={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 1.5, alignItems: 'center' }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
          {sessions.map((session, index) => (
            <motion.div
              key={session.title}
              initial={{ opacity: 0, x: -12 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.35 + index * 0.12, duration: 0.4 }}
            >
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1,
                  px: 1.5,
                  py: 1,
                  borderRadius: 1.5,
                  bgcolor: 'rgba(255, 255, 255, 0.04)'
                }}
              >
                <Box
                  sx={{
                    width: 8,
                    height: 8,
                    borderRadius: '50%',
                    bgcolor: session.color,
                    boxShadow: `0 0 8px ${session.color}`
                  }}
                />
                <Typography sx={{ fontSize: 12, color: 'text.primary', flex: 1 }}>{session.title}</Typography>
                <Typography sx={{ fontSize: 10, color: 'text.secondary' }}>{session.status}</Typography>
              </Box>
            </motion.div>
          ))}
        </Box>
        <IntegrityGauge value={92} />
      </Box>
    </DashboardPreview>
  );
}

function AiDetectionVisual() {
  const events = [
    { text: 'AI assistant overlay detected', color: '#ef4444', delay: 0.6 },
    { text: 'screen.grab intercepted', color: '#f59e0b', delay: 1.05 },
    { text: 'whisper feed monitored', color: '#22d3ee', delay: 1.5 }
  ];
  return (
    <DashboardPreview>
      <Box sx={{ display: 'flex', gap: 2.25, alignItems: 'center' }}>
        <Box
          sx={{
            position: 'relative',
            width: 108,
            height: 108,
            borderRadius: '50%',
            border: '1px solid rgba(148, 163, 184, 0.2)',
            overflow: 'hidden',
            flexShrink: 0
          }}
        >
          {[0.72, 0.46, 0.2].map((inset) => (
            <Box
              key={inset}
              sx={{
                position: 'absolute',
                inset: `${((1 - inset) / 2) * 100}%`,
                borderRadius: '50%',
                border: '1px solid rgba(34, 211, 238, 0.28)'
              }}
            />
          ))}
          <motion.div
            style={{
              position: 'absolute',
              inset: 0,
              background: 'conic-gradient(from 0deg, rgba(34,211,238,0.5), transparent 70deg)'
            }}
            animate={{ rotate: 360 }}
            transition={{ duration: 3.6, repeat: Infinity, ease: 'linear' }}
          />
          <Box
            sx={{
              position: 'absolute',
              inset: '42%',
              borderRadius: '50%',
              bgcolor: 'rgba(34, 211, 238, 0.55)',
              boxShadow: '0 0 18px 5px rgba(34, 211, 238, 0.5)'
            }}
          />
        </Box>
        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 0.75 }}>
          {events.map((event) => (
            <motion.div
              key={event.text}
              initial={{ opacity: 0, x: 12 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: event.delay, duration: 0.4 }}
            >
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 0.75,
                  px: 1.5,
                  py: 0.9,
                  borderRadius: 1.5,
                  bgcolor: 'rgba(255, 255, 255, 0.04)',
                  border: `1px solid ${event.color}33`
                }}
              >
                <TriangleAlert size={12} color={event.color} />
                <Typography sx={{ fontSize: 11, color: 'text.primary', flex: 1 }}>{event.text}</Typography>
                <CheckCircle2 size={12} color="#22c55e" />
              </Box>
            </motion.div>
          ))}
        </Box>
      </Box>
    </DashboardPreview>
  );
}

function TimelineVisual() {
  const events = [
    { title: 'Camera on', status: 'Identity verified', color: '#22c55e', delay: 0.3 },
    { title: 'Tab switched', status: 'Violation flagged', color: '#ef4444', delay: 0.55 },
    { title: 'Screen locked', status: 'Resolved', color: '#f59e0b', delay: 0.8 },
    { title: 'Session ended', status: 'Evidence captured', color: '#3b82f6', delay: 1.05 }
  ];
  return (
    <DashboardPreview>
      <Box sx={{ position: 'relative', ml: 1 }}>
        <Box
          sx={{
            position: 'absolute',
            left: 3,
            top: 8,
            bottom: 8,
            width: 2,
            bgcolor: 'rgba(148, 163, 184, 0.2)'
          }}
        />
        {events.map((event) => (
          <motion.div
            key={event.title}
            initial={{ opacity: 0, x: -14 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: event.delay, duration: 0.45 }}
          >
            <Box
              sx={{
                position: 'relative',
                pl: 3.25,
                py: 0.85,
                display: 'flex',
                alignItems: 'center',
                gap: 1
              }}
            >
              <Box
                sx={{
                  position: 'absolute',
                  left: 0,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  bgcolor: event.color,
                  boxShadow: `0 0 10px ${event.color}`
                }}
              />
              <Typography sx={{ fontSize: 12, fontWeight: 700, color: 'text.primary', flex: 1 }}>
                {event.title}
              </Typography>
              <Typography sx={{ fontSize: 10.5, color: event.color }}>{event.status}</Typography>
            </Box>
          </motion.div>
        ))}
      </Box>
    </DashboardPreview>
  );
}

function PoliciesVisual() {
  const policies = [
    { title: 'No external AI tools', severity: 'Critical', color: '#ef4444', delay: 0.3 },
    { title: 'Camera always on', severity: 'High', color: '#fb923c', delay: 0.55 },
    { title: 'Single screen only', severity: 'Medium', color: '#f59e0b', delay: 0.8 }
  ];
  return (
    <DashboardPreview>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
        <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>Policies enforced</Typography>
        <Pill color="#22c55e" bg="rgba(34, 197, 94, 0.14)">
          12 active
        </Pill>
      </Box>
      {policies.map((policy) => (
        <motion.div
          key={policy.title}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: policy.delay, duration: 0.4 }}
        >
          <Box sx={{ px: 1.5, py: 1.15, borderRadius: 1.5, bgcolor: 'rgba(255, 255, 255, 0.04)' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.75 }}>
              <Typography sx={{ fontSize: 12, fontWeight: 600 }}>{policy.title}</Typography>
              <Box
                sx={{
                  fontSize: 10,
                  fontWeight: 700,
                  color: policy.color,
                  px: 1,
                  py: 0.25,
                  borderRadius: 6,
                  bgcolor: `${policy.color}22`
                }}
              >
                {policy.severity}
              </Box>
            </Box>
            <Box sx={{ height: 4, borderRadius: 4, bgcolor: 'rgba(148, 163, 184, 0.12)' }}>
              <motion.div
                initial={{ width: '8%' }}
                animate={{ width: '86%' }}
                transition={{ delay: 0.6 + policy.delay, duration: 0.8, ease: 'easeOut' }}
                style={{ height: '100%', borderRadius: 4, background: policy.color }}
              />
            </Box>
          </Box>
        </motion.div>
      ))}
    </DashboardPreview>
  );
}

function AnalyticsVisual() {
  const stats = [
    { value: '128', label: 'Interviews' },
    { value: '92%', label: 'Avg score' },
    { value: '+14%', label: 'Δ Month' }
  ];
  return (
    <DashboardPreview>
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 1 }}>
        {stats.map((stat, index) => (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 + index * 0.1, duration: 0.4 }}
          >
            <Box sx={{ px: 1, py: 1, borderRadius: 1.5, bgcolor: 'rgba(255, 255, 255, 0.04)', textAlign: 'center' }}>
              <Typography sx={{ fontSize: 15, fontWeight: 800, color: '#60a5fa' }}>{stat.value}</Typography>
              <Typography sx={{ fontSize: 10, color: 'text.secondary' }}>{stat.label}</Typography>
            </Box>
          </motion.div>
        ))}
      </Box>
      <MiniBars />
      <Typography sx={{ fontSize: 10, color: 'text.secondary' }}>Violations per week</Typography>
    </DashboardPreview>
  );
}

function ReportsVisual() {
  const reports = [
    { name: 'q4_engineering_audit.pdf', progress: '92%', width: '92%', delay: 0.3 },
    { name: 'q4_finance_screen.pdf', progress: 'Complete', width: '100%', delay: 0.55 }
  ];
  return (
    <DashboardPreview>
      {reports.map((report) => (
        <motion.div
          key={report.name}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: report.delay, duration: 0.4 }}
        >
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1.5,
              px: 1.5,
              py: 1.15,
              borderRadius: 1.5,
              bgcolor: 'rgba(255, 255, 255, 0.04)'
            }}
          >
            <Box
              sx={{
                width: 26,
                height: 32,
                borderRadius: 1,
                bgcolor: '#ef4444',
                display: 'grid',
                placeItems: 'center',
                flexShrink: 0
              }}
            >
              <FileText size={14} color="#fff" />
            </Box>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography
                sx={{
                  fontSize: 12,
                  fontWeight: 600,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis'
                }}
              >
                {report.name}
              </Typography>
              <Box sx={{ mt: 0.5, height: 4, borderRadius: 4, bgcolor: 'rgba(148, 163, 184, 0.12)' }}>
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: report.width }}
                  transition={{ delay: 0.8 + report.delay, duration: 0.9, ease: 'easeOut' }}
                  style={{ height: '100%', borderRadius: 4, background: '#3b82f6' }}
                />
              </Box>
            </Box>
            <Typography sx={{ fontSize: 10, color: 'text.secondary', flexShrink: 0 }}>{report.progress}</Typography>
          </Box>
        </motion.div>
      ))}
      <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap' }}>
        <Pill color="#3b82f6" bg="rgba(59, 130, 246, 0.14)">
          Audit trail · 214 events
        </Pill>
        <Pill>PDF export ready</Pill>
      </Box>
    </DashboardPreview>
  );
}

function SecurityVisual() {
  const logs = [
    { time: '10:42:11', action: 'MFA verified · 203.0.113.5', delay: 0.3 },
    { time: '10:42:09', action: 'Session created · 203.0.113.5', delay: 0.5 },
    { time: '10:40:00', action: 'Login attempt · 198.51.100.2', delay: 0.7 }
  ];
  return (
    <DashboardPreview>
      <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center', mb: 0.5 }}>
        <Box
          sx={{
            width: 52,
            height: 52,
            borderRadius: '50%',
            display: 'grid',
            placeItems: 'center',
            bgcolor: 'rgba(34, 197, 94, 0.12)',
            color: '#22c55e',
            border: '1px solid rgba(34, 197, 94, 0.3)',
            flexShrink: 0
          }}
        >
          <ShieldCheck size={26} />
        </Box>
        <Box>
          <Typography sx={{ fontSize: 14, fontWeight: 800 }}>2FA enabled</Typography>
          <Typography sx={{ fontSize: 11, color: 'text.secondary' }}>Authenticator + recovery codes</Typography>
        </Box>
      </Box>
      {logs.map((log) => (
        <motion.div
          key={log.time}
          initial={{ opacity: 0, x: 12 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: log.delay, duration: 0.4 }}
        >
          <Box
            sx={{
              display: 'flex',
              gap: 1,
              alignItems: 'center',
              px: 1.5,
              py: 0.9,
              borderRadius: 1.5,
              bgcolor: 'rgba(255, 255, 255, 0.04)'
            }}
          >
            <LockKeyhole size={12} color="#94a3b8" />
            <Typography sx={{ fontSize: 10.5, color: 'text.secondary', flex: 1, fontFamily: 'monospace' }}>
              {log.action}
            </Typography>
            <Typography sx={{ fontSize: 10.5, color: 'text.secondary', fontFamily: 'monospace' }}>{log.time}</Typography>
          </Box>
        </motion.div>
      ))}
    </DashboardPreview>
  );
}

function CloudVisual() {
  const nodes = [
    { label: 'API Gateway', icon: 'gateway', color: '#3b82f6', delay: 0.2 },
    { label: 'Identity', icon: 'identity', color: '#8b5cf6', delay: 0.3 },
    { label: 'Discovery', icon: 'discovery', color: '#22d3ee', delay: 0.4 },
    { label: 'Interviews', icon: 'interviews', color: '#22c55e', delay: 0.5 },
    { label: 'Reports', icon: 'reports', color: '#f59e0b', delay: 0.6 },
    { label: 'Postgres', icon: 'database', color: '#ef4444', delay: 0.7 }
  ];
  return (
    <DashboardPreview>
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 1 }}>
        {nodes.map((node) => (
          <motion.div
            key={node.label}
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: node.delay, duration: 0.4 }}
          >
            <Box
              sx={{
                px: 1,
                py: 1,
                borderRadius: 1.5,
                bgcolor: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid rgba(148, 163, 184, 0.12)',
                textAlign: 'center'
              }}
            >
              <Box sx={{ display: 'grid', placeItems: 'center', mb: 0.5, color: node.color }}>{SERVER_ICONS[node.icon]}</Box>
              <Typography sx={{ fontSize: 10, fontWeight: 700 }}>{node.label}</Typography>
              <Box sx={{ fontSize: 9, color: '#22c55e', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.4 }}>
                <Box sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: '#22c55e' }} />
                healthy
              </Box>
            </Box>
          </motion.div>
        ))}
      </Box>
      <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap' }}>
        <Pill>us-east-1</Pill>
        <Pill>eu-west-1</Pill>
        <Pill>3 availability zones</Pill>
      </Box>
    </DashboardPreview>
  );
}

const SLIDES: Slide[] = [
  {
    id: 'secure-platform',
    kicker: 'Platform',
    accent: '#3b82f6',
    title: 'Secure Interview Platform',
    description: 'Protect remote interviews from integrity violations with live, monitored sessions.',
    visual: <SecurePlatformVisual />
  },
  {
    id: 'ai-detection',
    kicker: 'Detection',
    accent: '#22d3ee',
    title: 'AI Copilot Detection',
    description: 'Detect unauthorized AI interview assistants. Invisible overlay detection. Background monitoring.',
    visual: <AiDetectionVisual />
  },
  {
    id: 'timeline',
    kicker: 'Timeline',
    accent: '#22c55e',
    title: 'Real-Time Integrity Timeline',
    description: 'Display every integrity event as it happens — from identity verification to session evidence.',
    visual: <TimelineVisual />
  },
  {
    id: 'policies',
    kicker: 'Policies',
    accent: '#f59e0b',
    title: 'Enterprise Policy Engine',
    description: 'Organization-specific interview policies. Risk scoring. Violation detection.',
    visual: <PoliciesVisual />
  },
  {
    id: 'analytics',
    kicker: 'Analytics',
    accent: '#60a5fa',
    title: 'Recruiter Analytics',
    description: 'Interview trends. Integrity scores. Organization insights.',
    visual: <AnalyticsVisual />
  },
  {
    id: 'reports',
    kicker: 'Reports',
    accent: '#fb7185',
    title: 'Comprehensive Reports',
    description: 'Download PDF reports. Audit trails. Interview evidence.',
    visual: <ReportsVisual />
  },
  {
    id: 'security',
    kicker: 'Security',
    accent: '#a78bfa',
    title: 'Enterprise Security',
    description: 'Multi-factor authentication. Encrypted communication. Secure audit logs.',
    visual: <SecurityVisual />
  },
  {
    id: 'cloud',
    kicker: 'Cloud',
    accent: '#22d3ee',
    title: 'Cloud Native Platform',
    description: 'AWS deployment. High availability. Scalable microservices.',
    visual: <CloudVisual />
  }
];

const SLIDE_INTERVAL = 6000;

function SlideView({ slide }: { slide: Slide }) {
  return (
    <Box sx={{ maxWidth: 560 }}>
      <Box
        sx={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 0.5,
          px: 1.25,
          py: 0.45,
          borderRadius: 999,
          fontSize: 11,
          fontWeight: 700,
          letterSpacing: '0.06em',
          textTransform: 'uppercase',
          color: slide.accent,
          bgcolor: `${slide.accent}1f`,
          border: `1px solid ${slide.accent}3d`
        }}
      >
        <Fingerprint size={12} />
        {slide.kicker}
      </Box>
      <Typography
        variant="h3"
        sx={{
          fontWeight: 800,
          color: '#ffffff',
          mt: 1.5,
          mb: 1,
          fontSize: { md: 28, lg: 34 },
          lineHeight: 1.12,
          letterSpacing: '-0.02em'
        }}
      >
        {slide.title}
      </Typography>
      <Typography sx={{ color: '#94a3b8', mb: 2.5, maxWidth: 500, fontSize: 15, lineHeight: 1.55 }}>
        {slide.description}
      </Typography>
      {slide.visual}
    </Box>
  );
}

/**
 * Auto-rotating product showcase carousel for the auth screens.
 */
export function FeatureCarousel() {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const reduceMotion = useReducedMotion();

  useEffect(() => {
    if (reduceMotion || paused) return;
    const id = window.setInterval(() => setIndex((current) => (current + 1) % SLIDES.length), SLIDE_INTERVAL);
    return () => window.clearInterval(id);
  }, [paused, reduceMotion]);

  const slide = SLIDES[index];
  const goTo = (next: number) => setIndex((next + SLIDES.length) % SLIDES.length);

  return (
    <Box
      role="region"
      aria-label="Integrity Pro capabilities"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={() => setPaused(false)}
    >
      <Box sx={{ position: 'relative', minHeight: 430 }}>
        <AnimatePresence mode="wait">
          <motion.div
            key={slide.id}
            initial={{ opacity: 0, y: 28 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.55, ease: 'easeOut' }}
          >
            <SlideView slide={slide} />
          </motion.div>
        </AnimatePresence>
      </Box>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mt: 2.5 }}>
        <Box sx={{ display: 'flex', gap: 1 }}>
          {SLIDES.map((item, dotIndex) => (
            <button
              key={item.id}
              type="button"
              aria-label={`Show slide ${dotIndex + 1}: ${item.title}`}
              aria-current={dotIndex === index ? 'true' : undefined}
              onClick={() => goTo(dotIndex)}
              style={{
                width: dotIndex === index ? 22 : 7,
                height: 7,
                borderRadius: 999,
                border: 'none',
                cursor: 'pointer',
                padding: 0,
                background: dotIndex === index ? item.accent : 'rgba(148, 163, 184, 0.35)',
                transition: 'all 0.3s ease'
              }}
            />
          ))}
        </Box>
        <Box sx={{ flex: 1 }} />
        <Box sx={{ display: 'flex', gap: 0.75 }}>
          <IconButton
            aria-label="Previous slide"
            size="small"
            onClick={() => goTo(index - 1)}
            sx={{
              color: 'text.secondary',
              border: '1px solid rgba(148, 163, 184, 0.22)',
              '&:hover': { borderColor: '#3b82f6', color: '#3b82f6' }
            }}
          >
            <Box component="span" sx={{ fontSize: 16, lineHeight: 1, mt: -0.5 }}>
              ‹
            </Box>
          </IconButton>
          <IconButton
            aria-label="Next slide"
            size="small"
            onClick={() => goTo(index + 1)}
            sx={{
              color: 'text.secondary',
              border: '1px solid rgba(148, 163, 184, 0.22)',
              '&:hover': { borderColor: '#3b82f6', color: '#3b82f6' }
            }}
          >
            <Box component="span" sx={{ fontSize: 16, lineHeight: 1, mt: -0.5 }}>
              ›
            </Box>
          </IconButton>
        </Box>
      </Box>
    </Box>
  );
}
