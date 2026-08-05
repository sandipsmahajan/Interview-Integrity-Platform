import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { api } from '../lib/api';
import { useAppStore } from '../stores/appStore';
import { CONSENT_CATEGORIES } from '../lib/types';

export function ConsentScreen() {
  const {
    launchContext,
    remoteConfig,
    interview,
    setConsentGranted,
    setInterview,
    setScreen,
    setInterviewActive,
    setInterviewStartTime,
    setError,
  } = useAppStore();

  const [selectedCategories, setSelectedCategories] = useState<Set<string>>(
    new Set(CONSENT_CATEGORIES.map((c) => c.id)),
  );
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [expandedWhy, setExpandedWhy] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      try {
        const config = await api.getRemoteConfig();
        useAppStore.getState().setRemoteConfig(config);
      } catch {
        // use existing config
      }
      try {
        const ctx = await api.getInterview();
        setInterview(ctx);
      } catch {
        // continue
      }
      setLoading(false);
    }
    load();
  }, [setInterview]);

  function toggleCategory(id: string) {
    setSelectedCategories((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  const toggleExpand = (id: string) => {
    setExpandedWhy((prev) => (prev === id ? null : id));
  };

  async function handleAccept() {
    setSubmitting(true);
    try {
      const categories = Array.from(selectedCategories);
      await api.acceptConsent(categories);
      setConsentGranted(true);

      try {
        const ctx = await api.startInterview();
        setInterview(ctx);
      } catch {
        // continue with existing interview context
      }

      setInterviewActive(true);
      setInterviewStartTime(Date.now());
      setScreen('interview');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Consent failed');
      setSubmitting(false);
    }
  }

  async function handleDecline() {
    try {
      await api.declineConsent();
    } catch {
      // ignore
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <motion.div
          className="w-8 h-8 border-2 border-indigo-400 border-t-transparent rounded-full"
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
        />
      </div>
    );
  }

  const mandatoryCategories = new Set(['telemetry']);

  return (
    <motion.div
      className="min-h-screen overflow-auto p-6"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      <div className="max-w-[1100px] mx-auto flex flex-col gap-[18px]">
        {/* Header */}
        <div className="bg-[var(--bg-card)] border border-[var(--border)] rounded-xl p-4 px-5 flex items-center justify-between">
          <div className="flex items-center gap-3.5">
            <svg width="28" height="28" viewBox="0 0 32 32" fill="none">
              <path
                d="M16 2L4 8v8c0 8.4 5.1 16.2 12 18 6.9-1.8 12-9.6 12-18V8L16 2z"
                stroke="#6366f1"
                strokeWidth="2"
                fill="none"
              />
              <path
                d="M11 16l4 4 6-8"
                stroke="#22c55e"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
            <div>
              <h2 className="text-base font-semibold text-white">
                Consent &amp; Permissions
              </h2>
              <p className="text-xs text-slate-400">
                {remoteConfig?.orgName ?? interview?.companyName ?? 'Organization'}
              </p>
            </div>
          </div>
          {interview && (
            <div className="flex gap-6 text-right">
              <div>
                <p className="text-[11px] font-bold uppercase tracking-widest text-slate-500">
                  Interview
                </p>
                <p className="text-sm font-semibold text-white">
                  {interview.jobTitle}
                </p>
              </div>
              <div>
                <p className="text-[11px] font-bold uppercase tracking-widest text-slate-500">
                  Date
                </p>
                <p className="text-sm font-semibold text-white">
                  {new Date(interview.startsAt).toLocaleDateString(undefined, {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                  })}
                </p>
              </div>
              <div>
                <p className="text-[11px] font-bold uppercase tracking-widest text-slate-500">
                  Candidate
                </p>
                <p className="text-sm font-semibold text-white">
                  {interview.candidateName}
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Interview Details Card */}
        {interview && (
          <div className="bg-[var(--bg-card)] border border-[var(--border)] rounded-xl p-5">
            <h3 className="text-sm font-semibold text-white mb-3">
              Interview Details
            </h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {[
                ['Company', interview.companyName],
                ['Role', interview.jobTitle],
                [
                  'Date',
                  new Date(interview.startsAt).toLocaleDateString(undefined, {
                    weekday: 'long',
                    month: 'long',
                    day: 'numeric',
                  }),
                ],
                [
                  'Time',
                  `${new Date(interview.startsAt).toLocaleTimeString(undefined, {
                    hour: '2-digit',
                    minute: '2-digit',
                  })} - ${new Date(interview.endsAt).toLocaleTimeString(undefined, {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}`,
                ],
                ['Recruiter', interview.recruiterName],
                ['Candidate Email', interview.candidateEmail],
                [
                  'Meeting URL',
                  <span
                    key="url"
                    className="text-indigo-400 text-sm break-all"
                  >
                    {interview.meetingUrl}
                  </span>,
                ],
                [
                  'Retention',
                  `${remoteConfig?.dataRetentionDays ?? 90} days`,
                ],
              ].map(([label, value]) => (
                <div key={label as string}>
                  <p className="text-[11px] font-bold uppercase tracking-widest text-slate-500 mb-1">
                    {label}
                  </p>
                  <p className="text-sm font-semibold text-white">{value}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Main Content */}
        <div className="grid gap-4 grid-cols-[2fr_1fr] max-[900px]:grid-cols-1">
          {/* Monitoring Categories */}
          <div className="bg-[var(--bg-card)] border border-[var(--border)] rounded-xl p-5">
            <h3 className="text-sm font-semibold text-white mb-1">
              Monitoring Categories
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Select the categories you consent to monitor during your
              interview. Mandatory items cannot be deselected.
            </p>

            <div className="flex flex-col">
              {CONSENT_CATEGORIES.map((category) => {
                const isMandatory = mandatoryCategories.has(category.id);
                const isChecked = selectedCategories.has(category.id);
                return (
                  <label
                    key={category.id}
                    className={`flex items-start gap-3 py-3.5 border-b border-[var(--border)] last:border-b-0 ${
                      isMandatory ? 'cursor-not-allowed opacity-70' : 'cursor-pointer'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={isChecked}
                      onChange={() =>
                        !isMandatory && toggleCategory(category.id)
                      }
                      disabled={isMandatory}
                      className="mt-0.5 accent-indigo-500"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <strong className="text-sm text-white">
                          {category.label}
                        </strong>
                        {isMandatory && (
                          <span className="text-[10px] font-bold uppercase px-1.5 py-0.5 rounded bg-indigo-500/20 text-indigo-400">
                            Required
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-slate-400 mt-1 leading-relaxed">
                        {category.description}
                      </p>
                    </div>
                  </label>
                );
              })}
            </div>
          </div>

          {/* Sidebar */}
          <div className="flex flex-col gap-4">
            {/* Session Info */}
            <div className="bg-[var(--bg-card)] border border-[var(--border)] rounded-xl p-5">
              <h3 className="text-sm font-semibold text-white mb-3">
                Session Info
              </h3>
              <div className="flex flex-col gap-2">
                {[
                  ['Organization', remoteConfig?.orgName ?? '—'],
                  ['Interview', interview?.jobTitle ?? '—'],
                  ['Candidate', interview?.candidateName ?? '—'],
                  [
                    'Support',
                    remoteConfig?.supportEmail ?? '—',
                  ],
                  [
                    'Retention',
                    `${remoteConfig?.dataRetentionDays ?? 90} days`,
                  ],
                ].map(([label, value]) => (
                  <div key={label}>
                    <dt className="text-[11px] font-bold uppercase tracking-widest text-slate-500 mb-1">
                      {label}
                    </dt>
                    <dd className="text-[15px] font-semibold text-white m-0">
                      {value}
                    </dd>
                  </div>
                ))}
              </div>

              {(remoteConfig?.privacyPolicyUrl ||
                remoteConfig?.termsOfServiceUrl) && (
                <div className="flex flex-col gap-2 mt-4">
                  {remoteConfig.privacyPolicyUrl && (
                    <a
                      href={remoteConfig.privacyPolicyUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-indigo-400 text-sm no-underline hover:underline"
                    >
                      Privacy Policy
                    </a>
                  )}
                  {remoteConfig.termsOfServiceUrl && (
                    <a
                      href={remoteConfig.termsOfServiceUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-indigo-400 text-sm no-underline hover:underline"
                    >
                      Terms of Service
                    </a>
                  )}
                </div>
              )}
            </div>

            {/* Selected Count */}
            <div className="bg-[var(--bg-card)] border border-[var(--border)] rounded-xl p-5">
              <p className="text-xs text-slate-400">
                <span className="text-white font-semibold text-lg">
                  {selectedCategories.size}
                </span>{' '}
                / {CONSENT_CATEGORIES.length} categories selected
              </p>
            </div>
          </div>
        </div>

        {/* Why We Collect */}
        <div className="bg-[var(--bg-card)] border border-[var(--border)] rounded-xl p-5">
          <h3 className="text-sm font-semibold text-white mb-3">
            Why we collect this data
          </h3>
          <div className="flex flex-col gap-1">
            {[
              {
                id: 'integrity',
                title: 'Ensuring assessment integrity',
                body: 'Monitoring helps ensure all candidates have a fair and equal interview experience by detecting unauthorized assistance, screen sharing, or use of prohibited applications during the assessment.',
              },
              {
                id: 'compliance',
                title: 'Compliance with hiring policies',
                body: 'Your organization requires certain monitoring to meet internal hiring standards and regulatory requirements. Collected data is stored securely and retained per the organization\'s data retention policy.',
              },
              {
                id: 'transparency',
                title: 'Transparency and your rights',
                body: 'You have the right to know exactly what data is being collected. All monitoring categories are listed above with descriptions. Data is only collected during your interview session and is accessible only to authorized personnel.',
              },
            ].map((item) => (
              <div
                key={item.id}
                className="border-b border-[var(--border)] last:border-b-0"
              >
                <button
                  onClick={() => toggleExpand(item.id)}
                  className="w-full flex items-center justify-between py-3 text-left hover:bg-white/[0.02] rounded transition-colors"
                >
                  <span className="text-sm text-white">{item.title}</span>
                  <motion.svg
                    width="16"
                    height="16"
                    viewBox="0 0 16 16"
                    fill="none"
                    animate={{
                      rotate: expandedWhy === item.id ? 180 : 0,
                    }}
                    transition={{ duration: 0.2 }}
                  >
                    <path
                      d="M4 6l4 4 4-4"
                      stroke="#94a3b8"
                      strokeWidth="1.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </motion.svg>
                </button>
                <AnimatePresence initial={false}>
                  {expandedWhy === item.id && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.2 }}
                      className="overflow-hidden"
                    >
                      <p className="text-xs text-slate-400 leading-relaxed pb-3 px-0">
                        {item.body}
                      </p>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            ))}
          </div>
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3">
          <button
            onClick={handleDecline}
            disabled={submitting}
            className="bg-transparent border border-[var(--border)] rounded-lg text-slate-300 font-medium text-sm py-2.5 px-4 cursor-pointer transition-all hover:border-red-500/50 hover:text-red-400 disabled:opacity-45 disabled:cursor-not-allowed"
          >
            Decline
          </button>
          <button
            onClick={handleAccept}
            disabled={submitting}
            className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 disabled:bg-indigo-800 disabled:cursor-not-allowed text-white font-medium rounded-lg py-2.5 px-5 text-sm transition-colors"
          >
            {submitting && (
              <motion.div
                className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full"
                animate={{ rotate: 360 }}
                transition={{ duration: 0.8, repeat: Infinity, ease: 'linear' }}
              />
            )}
            {submitting ? 'Starting...' : 'Accept & Start Interview'}
          </button>
        </div>

        {launchContext && (
          <p className="text-center text-xs text-slate-600 font-mono">
            v{launchContext.clientVersion} &middot; {launchContext.deviceId}
          </p>
        )}
      </div>
    </motion.div>
  );
}
