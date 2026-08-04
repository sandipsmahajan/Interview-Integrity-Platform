import { useEffect, useState, type ReactNode } from 'react';
import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import {
  AppWindow,
  BellRing,
  Bot,
  CheckCircle2,
  Cloud,
  Database,
  FileText,
  Globe,
  KeyRound,
  Layers,
  Network,
  Server,
  Terminal,
  Webcam,
  Zap
} from 'lucide-react';
import { useAnimationController } from './AnimationController';
import { AlertBubble } from './AlertBubble';
import { DashboardPreview, MiniBars, Pill } from './DashboardPreview';
import { FeatureSlide, type FeatureSlideData } from './FeatureSlide';
import { FloatingBubble } from './FloatingBubble';
import { IntegrityScoreBubble } from './IntegrityScoreBubble';
import { TelemetryPreview } from './TelemetryPreview';
import { TimelinePreview } from './TimelinePreview';

const cornerTopRight = { position: 'absolute' as const, top: 0, right: 0 };
const cornerBottomLeft = { position: 'absolute' as const, bottom: 0, left: 0 };

function RadarScan() {
  const { enabled } = useAnimationController();
  return (
    <Box
      sx={{
        position: 'relative',
        width: 78,
        height: 78,
        borderRadius: '50%',
        border: '1px solid rgba(34, 211, 238, 0.3)',
        overflow: 'hidden',
        flexShrink: 0
      }}
    >
      {[0.7, 0.4, 0.12].map((inset) => (
        <Box
          key={inset}
          sx={{
            position: 'absolute',
            inset: `${((1 - inset) / 2) * 100}%`,
            borderRadius: '50%',
            border: '1px solid rgba(34, 211, 238, 0.22)'
          }}
        />
      ))}
      <motion.div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'conic-gradient(from 0deg, rgba(34,211,238,0.5), transparent 75deg)'
        }}
        animate={enabled ? { rotate: 360 } : undefined}
        transition={{ duration: 3.2, repeat: Infinity, ease: 'linear' }}
      />
      <Box
        sx={{
          position: 'absolute',
          top: '28%',
          left: '60%',
          width: 9,
          height: 9,
          borderRadius: '50%',
          bgcolor: '#ef4444',
          boxShadow: '0 0 12px 3px rgba(239, 68, 68, 0.6)'
        }}
      />
    </Box>
  );
}

function CopilotDetectionVisual() {
  const { enabled } = useAnimationController();
  return (
    <DashboardPreview url="interview.integrity.pro/live/candidate-042">
      <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1.5 }}>
        <Box
          sx={{
            borderRadius: 1.5,
            border: '1px solid rgba(148, 163, 184, 0.15)',
            bgcolor: 'rgba(255, 255, 255, 0.03)',
            overflow: 'hidden'
          }}
        >
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 0.75,
              px: 1.25,
              py: 0.75,
              borderBottom: '1px solid rgba(148, 163, 184, 0.1)'
            }}
          >
            <Box
              component={motion.span}
              sx={{ width: 7, height: 7, borderRadius: '50%', bgcolor: '#22c55e' }}
              animate={enabled ? { opacity: [1, 0.35, 1] } : undefined}
              transition={{ duration: 1.6, repeat: Infinity, ease: 'easeInOut' }}
            />
            <Typography sx={{ fontSize: 10.5, fontWeight: 700 }}>Interview running</Typography>
            <Box sx={{ flex: 1 }} />
            <Pill color="#22c55e" bg="rgba(34, 197, 94, 0.14)">
              Screen share
            </Pill>
          </Box>
          <Box sx={{ position: 'relative', p: 1.5, minHeight: 108 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Webcam size={18} color="#22d3ee" />
              <Box>
                <Typography sx={{ fontSize: 12, fontWeight: 700 }}>Senior Frontend · Round 2</Typography>
                <Typography sx={{ fontSize: 10, color: 'text.secondary' }}>Candidate A</Typography>
              </Box>
            </Box>
            <motion.div
              style={{
                position: 'absolute',
                left: 0,
                right: 0,
                height: 26,
                background: 'linear-gradient(180deg, transparent, rgba(34,211,238,0.16), transparent)'
              }}
              animate={enabled ? { top: ['12%', '78%', '12%'] } : undefined}
              transition={{ duration: 3.4, repeat: Infinity, ease: 'easeInOut' }}
            />
            <Box sx={{ position: 'absolute', top: 10, right: 10, textAlign: 'center' }}>
              <Box sx={{ position: 'relative', width: 36, height: 36, mx: 'auto', display: 'grid', placeItems: 'center', borderRadius: 10, bgcolor: 'rgba(239, 68, 68, 0.12)', color: '#ef4444', border: '1px dashed rgba(239, 68, 68, 0.5)' }}>
                <Bot size={17} />
                <Box
                  component={motion.span}
                  sx={{ position: 'absolute', inset: -4, borderRadius: 12, border: '1px solid rgba(239, 68, 68, 0.5)' }}
                  animate={enabled ? { scale: [1, 1.35], opacity: [0.7, 0] } : undefined}
                  transition={{ duration: 1.8, repeat: Infinity, ease: 'easeOut' }}
                />
              </Box>
              <Typography sx={{ fontSize: 8.5, color: '#f87171', mt: 0.5 }}>invisible copilot</Typography>
            </Box>
          </Box>
        </Box>

        <Box
          sx={{
            borderRadius: 1.5,
            border: '1px solid rgba(148, 163, 184, 0.15)',
            bgcolor: 'rgba(255, 255, 255, 0.03)',
            p: 1.5
          }}
        >
          <Typography sx={{ fontSize: 11, fontWeight: 700, mb: 1 }}>Integrity Pro detection</Typography>
          <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>
            <RadarScan />
            <Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <Box sx={{ width: 7, height: 7, borderRadius: '50%', bgcolor: '#ef4444', boxShadow: '0 0 8px #ef4444' }} />
                <Typography sx={{ fontSize: 11, fontWeight: 700 }}>Hidden process</Typography>
              </Box>
              <Typography sx={{ fontSize: 10.5, fontFamily: 'monospace', color: '#f87171', mt: 0.5 }}>
                copilot_agent.exe
              </Typography>
              <Typography sx={{ fontSize: 9, color: 'text.secondary' }}>PID 4812 · overlay hidden</Typography>
            </Box>
          </Box>
        </Box>
      </Box>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          px: 1.5,
          py: 1,
          borderRadius: 1.5,
          border: '1px solid rgba(239, 68, 68, 0.35)',
          bgcolor: 'rgba(239, 68, 68, 0.08)'
        }}
      >
        <BellRing size={13} color="#f87171" />
        <Typography sx={{ fontSize: 11, fontWeight: 700, flex: 1 }}>Real-time alert sent to recruiter</Typography>
        <Typography sx={{ fontSize: 9.5, color: 'text.secondary' }}>2s ago</Typography>
      </Box>
    </DashboardPreview>
  );
}

function OverlayDetectionVisual() {
  const overlays = [
    { name: 'copilot_assistant · overlay', state: 'Detected', color: '#ef4444', icon: <AppWindow size={12} /> },
    { name: 'taskbar ghost window', state: 'Flagged', color: '#f59e0b', icon: <AppWindow size={12} /> },
    { name: 'screen.grab hook', state: 'Monitored', color: '#22d3ee', icon: <Network size={12} /> }
  ];
  return (
    <DashboardPreview url="detect.integrity.pro/overlays">
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
        {overlays.map((overlay, index) => (
          <motion.div
            key={overlay.name}
            initial={{ opacity: 0, x: 12 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 + index * 0.14, duration: 0.4, ease: 'easeOut' }}
          >
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                px: 1.5,
                py: 1,
                borderRadius: 1.5,
                bgcolor: 'rgba(255, 255, 255, 0.04)',
                border: `1px solid ${overlay.color}33`
              }}
            >
              <Box sx={{ color: overlay.color, flexShrink: 0 }}>{overlay.icon}</Box>
              <Typography sx={{ fontSize: 11.5, fontFamily: 'monospace', flex: 1, color: 'text.primary' }}>
                {overlay.name}
              </Typography>
              <Box sx={{ fontSize: 10, fontWeight: 700, color: overlay.color, px: 1, py: 0.25, borderRadius: 6, bgcolor: `${overlay.color}22` }}>
                {overlay.state}
              </Box>
            </Box>
          </motion.div>
        ))}
      </Box>
    </DashboardPreview>
  );
}

function ProcessVisual() {
  const processes = [
    { name: 'vscode.exe', pid: 2041, state: 'Allowed', color: '#22c55e' },
    { name: 'copilot_agent.exe', pid: 4812, state: 'Prohibited', color: '#ef4444' },
    { name: 'overlay_helper.exe', pid: 4888, state: 'Flagged', color: '#f59e0b' }
  ];
  return (
    <DashboardPreview url="detect.integrity.pro/processes">
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
        {processes.map((process, index) => (
          <motion.div
            key={process.name}
            initial={{ opacity: 0, x: 12 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 + index * 0.14, duration: 0.4, ease: 'easeOut' }}
          >
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                px: 1.5,
                py: 1,
                borderRadius: 1.5,
                bgcolor: 'rgba(255, 255, 255, 0.04)',
                border: `1px solid ${process.color}33`
              }}
            >
              <Terminal size={12} color={process.color} />
              <Typography sx={{ fontSize: 11.5, fontFamily: 'monospace', flex: 1, color: 'text.primary' }}>
                {process.name}
              </Typography>
              <Typography sx={{ fontSize: 10, color: 'text.secondary', fontFamily: 'monospace' }}>PID {process.pid}</Typography>
              <Box sx={{ fontSize: 10, fontWeight: 700, color: process.color, px: 1, py: 0.25, borderRadius: 6, bgcolor: `${process.color}22` }}>
                {process.state}
              </Box>
            </Box>
          </motion.div>
        ))}
      </Box>
      <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap' }}>
        <Pill color="#22c55e" bg="rgba(34, 197, 94, 0.14)">
          Process baseline synced
        </Pill>
        <Pill color="#f59e0b" bg="rgba(245, 158, 11, 0.14)">
          2 flagged
        </Pill>
      </Box>
    </DashboardPreview>
  );
}

function ApiVisual() {
  const requests = [
    { method: 'POST', url: 'api.openai.com/v1/chat', app: 'copilot_agent', state: 'Blocked', color: '#ef4444' },
    { method: 'POST', url: 'api.anthropic.com/v1/messages', app: 'overlay_helper', state: 'Blocked', color: '#ef4444' },
    { method: 'GET', url: 'app.integrity.pro/v1/timeline', app: 'integrity-agent', state: 'OK', color: '#22c55e' }
  ];
  return (
    <DashboardPreview url="detect.integrity.pro/api">
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
        {requests.map((request, index) => (
          <motion.div
            key={request.url}
            initial={{ opacity: 0, x: 12 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 + index * 0.14, duration: 0.4, ease: 'easeOut' }}
          >
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                px: 1.5,
                py: 0.9,
                borderRadius: 1.5,
                bgcolor: 'rgba(255, 255, 255, 0.04)',
                border: `1px solid ${request.color}33`
              }}
            >
              <Box
                sx={{
                  fontSize: 9,
                  fontWeight: 800,
                  px: 1,
                  py: 0.3,
                  borderRadius: 5,
                  color: request.color,
                  bgcolor: `${request.color}22`,
                  flexShrink: 0
                }}
              >
                {request.method}
              </Box>
              <Typography sx={{ fontSize: 11, fontFamily: 'monospace', flex: 1, color: 'text.primary' }}>
                {request.url}
              </Typography>
              <Box sx={{ fontSize: 10, fontWeight: 700, color: request.color, flexShrink: 0 }}>{request.state}</Box>
            </Box>
          </motion.div>
        ))}
      </Box>
      <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap' }}>
        <Pill color="#ef4444" bg="rgba(239, 68, 68, 0.14)">
          Correlated with interview timeline
        </Pill>
        <Pill color="#22c55e" bg="rgba(34, 197, 94, 0.14)">
          Integrity alert raised
        </Pill>
      </Box>
    </DashboardPreview>
  );
}

function PolicyVisual() {
  const policies = [
    { title: 'No external AI tools', severity: 'Critical', color: '#ef4444', delay: 0.3 },
    { title: 'Camera always on', severity: 'High', color: '#fb923c', delay: 0.5 },
    { title: 'Single screen only', severity: 'Medium', color: '#f59e0b', delay: 0.7 }
  ];
  return (
    <DashboardPreview url="app.integrity.pro/policies">
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
          transition={{ delay: policy.delay, duration: 0.4, ease: 'easeOut' }}
        >
          <Box sx={{ px: 1.5, py: 1.15, borderRadius: 1.5, bgcolor: 'rgba(255, 255, 255, 0.04)' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.75 }}>
              <Typography sx={{ fontSize: 12, fontWeight: 600 }}>{policy.title}</Typography>
              <Box sx={{ fontSize: 10, fontWeight: 700, color: policy.color, px: 1, py: 0.25, borderRadius: 6, bgcolor: `${policy.color}22` }}>
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
    <DashboardPreview url="app.integrity.pro/analytics">
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 1 }}>
        {stats.map((stat, index) => (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 + index * 0.1, duration: 0.4, ease: 'easeOut' }}
          >
            <Box sx={{ px: 1, py: 1, borderRadius: 1.5, bgcolor: 'rgba(255, 255, 255, 0.04)', textAlign: 'center' }}>
              <Typography sx={{ fontSize: 15, fontWeight: 800, color: '#60a5fa' }}>{stat.value}</Typography>
              <Typography sx={{ fontSize: 10, color: 'text.secondary' }}>{stat.label}</Typography>
            </Box>
          </motion.div>
        ))}
      </Box>
      <MiniBars />
      <Typography sx={{ fontSize: 10, color: 'text.secondary' }}>Integrity trend · last 8 weeks</Typography>
    </DashboardPreview>
  );
}

function ReportsVisual() {
  const reports = [
    { name: 'q4_engineering_audit.pdf', progress: '92%', width: '92%', delay: 0.3 },
    { name: 'q4_finance_interviews.pdf', progress: 'Complete', width: '100%', delay: 0.55 }
  ];
  return (
    <DashboardPreview url="app.integrity.pro/reports">
      {reports.map((report) => (
        <motion.div
          key={report.name}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: report.delay, duration: 0.4, ease: 'easeOut' }}
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
            <Box sx={{ width: 26, height: 32, borderRadius: 1, bgcolor: '#ef4444', display: 'grid', placeItems: 'center', flexShrink: 0 }}>
              <FileText size={14} color="#fff" />
            </Box>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography sx={{ fontSize: 12, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
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
          Interview evidence · 214 events
        </Pill>
        <Pill>PDF export ready</Pill>
      </Box>
    </DashboardPreview>
  );
}

const SERVER_ICONS: Record<string, ReactNode> = {
  gateway: <Server size={14} />,
  identity: <KeyRound size={14} />,
  discovery: <Globe size={14} />,
  interviews: <Webcam size={14} />,
  reports: <FileText size={14} />,
  database: <Database size={14} />
};

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
    <DashboardPreview url="status.integrity.pro">
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 1 }}>
        {nodes.map((node) => (
          <motion.div
            key={node.label}
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: node.delay, duration: 0.4, ease: 'easeOut' }}
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
        <Pill>
          <Cloud size={10} /> AWS
        </Pill>
        <Pill>
          <Layers size={10} /> 3 availability zones
        </Pill>
        <Pill>
          <Zap size={10} /> auto-scaling
        </Pill>
      </Box>
    </DashboardPreview>
  );
}

const SLIDES: FeatureSlideData[] = [
  {
    id: 'copilot-detection',
    kicker: 'Flagship Capability',
    accent: '#22d3ee',
    title: 'Detect Invisible AI Interview Copilots',
    description:
      'Integrity Pro detects AI interview assistants and hidden desktop copilots that remain invisible during screen sharing.',
    visual: <CopilotDetectionVisual />,
    bubbles: [
      <AlertBubble key="b1" title="AI Copilot Detected" message="Hidden process flagged" time="2s" style={cornerTopRight} />,
      <IntegrityScoreBubble key="b2" score={82} accent="#f59e0b" style={cornerBottomLeft} />
    ]
  },
  {
    id: 'invisible-copilot',
    kicker: 'Detection',
    accent: '#a78bfa',
    title: 'Invisible AI Copilot Detection',
    description:
      'Detect hidden interview assistants, overlay applications, and invisible desktop copilots — even when they never appear on screen.',
    visual: <OverlayDetectionVisual />,
    bubbles: [
      <AlertBubble key="b1" title="Invisible Overlay Detected" message="Not visible in screen share" time="now" style={cornerTopRight} />,
      <FloatingBubble key="b2" icon={<Bot size={13} />} label="Background Analysis" sub="continuous process scan" accent="#a78bfa" style={cornerBottomLeft} />
    ]
  },
  {
    id: 'process-intelligence',
    kicker: 'Process Intelligence',
    accent: '#3b82f6',
    title: 'Process Intelligence',
    description:
      'Monitor running processes, identify prohibited applications, and detect unauthorized software execution in real time.',
    visual: <ProcessVisual />,
    bubbles: [
      <AlertBubble key="b1" title="Unauthorized Process" message="copilot_agent.exe" time="now" style={cornerTopRight} />,
      <FloatingBubble key="b2" icon={<Layers size={13} />} label="Real-Time Monitoring" sub="process baseline synced" accent="#3b82f6" style={cornerBottomLeft} />
    ]
  },
  {
    id: 'api-activity',
    kicker: 'API Activity Monitoring',
    accent: '#22c55e',
    title: 'API Activity Monitoring',
    description:
      'Monitor outbound API calls from unauthorized applications and correlate suspicious activity with the interview timeline.',
    visual: <ApiVisual />,
    bubbles: [
      <AlertBubble key="b1" title="API Request Intercepted" message="blocked · openai.com" time="now" style={cornerTopRight} />,
      <FloatingBubble key="b2" icon={<Network size={13} />} label="Correlated" sub="matched to timeline" accent="#22c55e" style={cornerBottomLeft} />
    ]
  },
  {
    id: 'timeline',
    kicker: 'Timeline',
    accent: '#60a5fa',
    title: 'Real-Time Integrity Timeline',
    description: 'Every integrity event in chronological order — from identity verification to session evidence.',
    visual: <TimelinePreview />,
    bubbles: [
      <IntegrityScoreBubble key="b1" score={92} style={cornerTopRight} />,
      <FloatingBubble key="b2" icon={<Zap size={13} />} label="Telemetry Events" sub="streamed live" accent="#60a5fa" style={cornerBottomLeft} />
    ]
  },
  {
    id: 'policy-engine',
    kicker: 'Policies',
    accent: '#f59e0b',
    title: 'Enterprise Policy Engine',
    description: 'Organization-specific interview policies with violation scoring and automated risk evaluation.',
    visual: <PolicyVisual />,
    bubbles: [
      <AlertBubble key="b1" title="Policy Violation" message="no external AI tools" time="now" style={cornerTopRight} />,
      <FloatingBubble key="b2" icon={<Layers size={13} />} label="Risk Level" sub="elevated · 78" accent="#f59e0b" style={cornerBottomLeft} />
    ]
  },
  {
    id: 'telemetry',
    kicker: 'Telemetry',
    accent: '#22d3ee',
    title: 'Interview Telemetry',
    description:
      'System events, audio devices, display changes, screen sharing state, and network events — captured continuously.',
    visual: <TelemetryPreview />,
    bubbles: [
      <FloatingBubble key="b1" icon={<Webcam size={13} />} label="Screen Share Active" sub="1 stream" accent="#22d3ee" pulse style={cornerTopRight} />,
      <FloatingBubble key="b2" icon={<KeyRound size={13} />} label="Microphone Status" sub="audio monitored" accent="#3b82f6" style={cornerBottomLeft} />
    ]
  },
  {
    id: 'analytics',
    kicker: 'Analytics',
    accent: '#fb7185',
    title: 'Recruiter Analytics',
    description: 'Interview statistics, integrity trends, and organization-wide dashboards.',
    visual: <AnalyticsVisual />,
    bubbles: [
      <IntegrityScoreBubble key="b1" score={96} style={cornerTopRight} />,
      <FloatingBubble key="b2" icon={<CheckCircle2 size={13} />} label="Interview Protected" sub="zero violations" accent="#fb7185" style={cornerBottomLeft} />
    ]
  },
  {
    id: 'audit-reports',
    kicker: 'Reports',
    accent: '#fb923c',
    title: 'Audit Reports',
    description: 'Download enterprise audit reports, generate interview evidence, and export PDF reports.',
    visual: <ReportsVisual />,
    bubbles: [
      <FloatingBubble key="b1" icon={<FileText size={13} />} label="Compliance Enabled" sub="SOC 2 evidence" accent="#fb923c" style={cornerTopRight} />,
      <FloatingBubble key="b2" icon={<KeyRound size={13} />} label="Encrypted Session" sub="AES-256" accent="#22c55e" pulse style={cornerBottomLeft} />
    ]
  },
  {
    id: 'cloud-native',
    kicker: 'Cloud',
    accent: '#34d399',
    title: 'Cloud Native Platform',
    description: 'Highly available and scalable microservices deployed on AWS.',
    visual: <CloudVisual />,
    bubbles: [
      <FloatingBubble key="b1" icon={<Cloud size={13} />} label="99.99% Uptime" sub="multi-region" accent="#34d399" style={cornerTopRight} />,
      <FloatingBubble key="b2" icon={<Server size={13} />} label="AWS Deployed" sub="us-east-1 · eu-west-1" accent="#3b82f6" style={cornerBottomLeft} />
    ]
  }
];

const SLIDE_INTERVAL = 6000;

/**
 * Auto-rotating product showcase carousel for the auth screens. The first
 * slide is always the platform's primary differentiator: invisible AI
 * copilot detection.
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
      <Box sx={{ position: 'relative', minHeight: { md: 430, lg: 470 } }}>
        <AnimatePresence mode="wait">
          <motion.div
            key={slide.id}
            initial={{ opacity: 0, y: 28 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.55, ease: 'easeOut' }}
          >
            <FeatureSlide slide={slide} />
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
