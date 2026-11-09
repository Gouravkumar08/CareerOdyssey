import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { usersApi } from '../../api/users'
import { outreachApi } from '../../api/outreach'
import { dsaApi } from '../../api/dsa'
import { systemDesignApi } from '../../api/systemDesign'
import { activityApi } from '../../api/activity'
import type {
  DsaGoalItemResponse,
  OutreachResponse,
  RecentSolvedResponse,
  SdGoalItemResponse,
  SdRecentCompletedResponse,
  StreakResponse,
  UserResponse,
} from '../../api/types'
import { REPLY_STATUS_LABELS } from '../../api/types'
import { Spinner } from '../../components/Spinner'
import { CheckCircleIcon, AlertIcon } from '../../components/icons/NavIcons'
import { flattenDsaGoals, flattenSdGoals } from '../../utils/goalLabels'
import { DashboardPlannerCard } from '../planner/DashboardPlannerCard'
import { QuickLinks } from './QuickLinks'

type PriorityAction = {
  text: string
  to: string
}

const recentSolvedFormatter = new Intl.DateTimeFormat('en-US', {
  weekday: 'long',
  month: 'long',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
  hour12: true,
})

function formatSolvedAt(iso: string) {
  return recentSolvedFormatter.format(new Date(iso))
}

function formatCompletedAt(iso: string) {
  return recentSolvedFormatter.format(new Date(iso))
}

const REPLY_NUDGE_THRESHOLD_DAYS = 15

function daysSince(iso: string) {
  return Math.floor((Date.now() - new Date(iso).getTime()) / (1000 * 60 * 60 * 24))
}

/** Explains today's goal number instead of just showing two numbers side by side —
 * when you've already done more than today's target, say so plainly rather than
 * leaving "done: 21, target: 1" looking like a mismatch. */
function goalTodayLabel(hasGoal: boolean, completed: number, targetCount: number) {
  if (!hasGoal) return 'no goal set'
  if (targetCount > 0 && completed >= targetCount) {
    return `Target met ✓ (today's target was ${targetCount})`
  }
  return `of ${targetCount} goal target`
}

const FALLBACK_QUOTE = { text: 'Nothing happens to the wise man against his expectation.', author: 'Seneca' }

const dashboardDateFormatter = new Intl.DateTimeFormat('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })
const dashboardTimeFormatter = new Intl.DateTimeFormat('en-US', { hour: 'numeric', minute: '2-digit', second: '2-digit', hour12: true })

function DashboardQuoteClock() {
  const [quote, setQuote] = useState<{ text: string; author: string } | null>(null)
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    let cancelled = false
    fetch('https://www.stoic-quotes.com/api/quote')
      .then((r) => r.json())
      .then((data) => {
        if (!cancelled) setQuote({ text: data.text, author: data.author })
      })
      .catch(() => {
        if (!cancelled) setQuote(FALLBACK_QUOTE)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(id)
  }, [])

  return (
    <div className="card dashboard-quote-clock">
      <div className="dashboard-quote">
        {quote ? (
          <>
            <p className="dashboard-quote-text">“{quote.text}”</p>
            <p className="dashboard-quote-author">— {quote.author}</p>
          </>
        ) : (
          <p className="dashboard-quote-text dashboard-quote-loading">Loading quote…</p>
        )}
      </div>
      <div className="dashboard-clock">
        <div className="dashboard-clock-time">{dashboardTimeFormatter.format(now)}</div>
        <div className="dashboard-clock-date">{dashboardDateFormatter.format(now)}</div>
      </div>
    </div>
  )
}

export function DashboardPage() {
  const [users, setUsers] = useState<UserResponse[]>([])
  const [outreach, setOutreach] = useState<OutreachResponse[]>([])
  const [recentSolved, setRecentSolved] = useState<RecentSolvedResponse[]>([])
  const [recentCompletedSd, setRecentCompletedSd] = useState<SdRecentCompletedResponse[]>([])
  const [streak, setStreak] = useState<StreakResponse | null>(null)
  const [dsaGoalItems, setDsaGoalItems] = useState<DsaGoalItemResponse[]>([])
  const [sdGoalItems, setSdGoalItems] = useState<SdGoalItemResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      usersApi.list().catch(() => []),
      outreachApi.list().catch(() => []),
      dsaApi.planner.recent(2, 5).catch(() => []),
      systemDesignApi.planner.recent(2, 5).catch(() => []),
      activityApi.streak().catch(() => null),
      dsaApi.planner.listGoalItems().catch(() => []),
      systemDesignApi.planner.listGoalItems().catch(() => []),
    ]).then(([u, o, r, sdR, streakResult, dsaGoalItemsResult, sdGoalItemsResult]) => {
      setUsers(u)
      setOutreach(o)
      setRecentSolved(r)
      setRecentCompletedSd(sdR)
      setStreak(streakResult)
      setDsaGoalItems(dsaGoalItemsResult)
      setSdGoalItems(sdGoalItemsResult)
      setLoading(false)
    })
  }, [])

  const dsaRows = useMemo(() => flattenDsaGoals(dsaGoalItems), [dsaGoalItems])
  const sdRows = useMemo(() => flattenSdGoals(sdGoalItems), [sdGoalItems])
  const flatGoalRows = useMemo(() => [...dsaRows, ...sdRows], [dsaRows, sdRows])
  const activeGoalRows = useMemo(() => flatGoalRows.filter((r) => !r.done), [flatGoalRows])
  const todayStr = useMemo(() => new Date().toLocaleDateString('en-CA'), [])

  const dsaHasGoal = dsaRows.length > 0
  const dsaTodayTarget = dsaRows.reduce((sum, r) => sum + r.todayTargetCount, 0)
  const dsaTodayCompleted = dsaRows.reduce((sum, r) => sum + r.todayCompletedCount, 0)
  const sdHasGoal = sdRows.length > 0
  const sdTodayTarget = sdRows.reduce((sum, r) => sum + r.todayTargetCount, 0)
  const sdTodayCompleted = sdRows.reduce((sum, r) => sum + r.todayCompletedCount, 0)

  function daysLeft(endDate: string) {
    const end = new Date(`${endDate}T00:00:00`)
    const today = new Date(`${todayStr}T00:00:00`)
    return Math.ceil((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  }

  if (loading) {
    return (
      <div className="page">
        <Spinner />
      </div>
    )
  }

  const missingGmail = users.filter((u) => !u.gmailAppPasswordSet)
  const failedOutreach = outreach.filter((o) => o.status === 'FAILED')
  const pendingReplyOutreach = outreach.filter(
    (o) => o.status === 'SENT' && o.replyStatus === 'SENT' && o.sentAt && daysSince(o.sentAt) >= REPLY_NUDGE_THRESHOLD_DAYS,
  )

  const todayKey = new Date().toLocaleDateString('en-CA')
  const todaySolvedQuestions = recentSolved.filter((q) => new Date(q.solvedAt).toLocaleDateString('en-CA') === todayKey)
  const todayTopics = Array.from(new Set(todaySolvedQuestions.map((q) => q.topicName).filter((t): t is string => !!t)))

  const todayCompletedSdItems = recentCompletedSd.filter(
    (i) => new Date(i.completedAt).toLocaleDateString('en-CA') === todayKey,
  )
  const todaySdTopics = Array.from(new Set(todayCompletedSdItems.map((i) => i.topicName).filter((t): t is string => !!t)))

  const actions: PriorityAction[] = []
  if (dsaHasGoal && dsaTodayCompleted < dsaTodayTarget) {
    const remaining = dsaTodayTarget - dsaTodayCompleted
    const progress = dsaTodayCompleted > 0
      ? ` (${dsaTodayCompleted}/${dsaTodayTarget} done${todayTopics.length > 0 ? ` — covered ${todayTopics.join(', ')}` : ''})`
      : ` (0/${dsaTodayTarget} done yet)`
    actions.push({
      text: `${remaining} more DSA question${remaining > 1 ? 's' : ''} left to hit today's goal target${progress}`,
      to: '/dsa-tracker',
    })
  }
  if (sdHasGoal && sdTodayCompleted < sdTodayTarget) {
    const remaining = sdTodayTarget - sdTodayCompleted
    const progress = sdTodayCompleted > 0
      ? ` (${sdTodayCompleted}/${sdTodayTarget} done${todaySdTopics.length > 0 ? ` — covered ${todaySdTopics.join(', ')}` : ''})`
      : ` (0/${sdTodayTarget} done yet)`
    actions.push({
      text: `${remaining} more System Design item${remaining > 1 ? 's' : ''} left to hit today's goal target${progress}`,
      to: '/system-design',
    })
  }
  if (missingGmail.length > 0) {
    actions.push({
      text: `${missingGmail.length} candidate profile${missingGmail.length > 1 ? 's' : ''} missing Gmail credentials`,
      to: '/profiles',
    })
  }
  if (failedOutreach.length > 0) {
    actions.push({
      text: `${failedOutreach.length} outreach attempt${failedOutreach.length > 1 ? 's' : ''} failed to send`,
      to: '/outreach',
    })
  }
  if (pendingReplyOutreach.length > 0) {
    actions.push({
      text: `${pendingReplyOutreach.length} outreach${pendingReplyOutreach.length > 1 ? 's' : ''} sent ${REPLY_NUDGE_THRESHOLD_DAYS}+ days ago still need a reply status`,
      to: '/outreach',
    })
  }

  const recentOutreach = outreach.slice(0, 5)
  const sentCount = outreach.filter((o) => o.status === 'SENT').length

  return (
    <div className="page">
      <DashboardQuoteClock />

      <QuickLinks />

      <div className="page-stats">
        <div className={`stat-card stagger-in ${streak && streak.currentStreak > 0 ? 'accent-streak' : ''}`} style={{ '--stagger-index': 0 } as React.CSSProperties}>
          <div className="stat-label">Consistency Streak</div>
          <div className={`stat-value ${streak && streak.currentStreak > 0 ? 'streak-text' : ''}`}>
            {streak ? `${streak.currentStreak} day${streak.currentStreak === 1 ? '' : 's'}` : '—'}
            {streak && streak.currentStreak > 0 && ' 🔥'}
          </div>
          {streak && streak.currentStreak > 0 && <div className="stat-sub">{streak.message}</div>}
        </div>
        <div className="stat-card accent-easy stagger-in" style={{ '--stagger-index': 1 } as React.CSSProperties}>
          <div className="stat-label">Outreach Sent</div>
          <div className="stat-value easy-text">{sentCount}</div>
        </div>
        <div className="stat-card stagger-in" style={{ '--stagger-index': 2 } as React.CSSProperties}>
          <div className="stat-label">DSA Today</div>
          <div className="stat-value">{dsaHasGoal ? dsaTodayCompleted : '—'}</div>
          <div className="stat-sub">{goalTodayLabel(dsaHasGoal, dsaTodayCompleted, dsaTodayTarget)}</div>
        </div>
        <div className="stat-card stagger-in" style={{ '--stagger-index': 3 } as React.CSSProperties}>
          <div className="stat-label">System Design Today</div>
          <div className="stat-value">{sdHasGoal ? sdTodayCompleted : '—'}</div>
          <div className="stat-sub">{goalTodayLabel(sdHasGoal, sdTodayCompleted, sdTodayTarget)}</div>
        </div>
      </div>

      <h2 className="section-heading">Goals</h2>

      {activeGoalRows.length === 0 ? (
        <div className="card dashboard-goal-empty">
          <p className="empty-state">No active goals — set one up in the Planner.</p>
        </div>
      ) : (
        <div className="card dashboard-goal-list">
          {activeGoalRows.map((row) => {
            const remainingDays = daysLeft(row.endDate)
            return (
              <div className="dashboard-goal-row" key={row.key}>
                <span className={`badge badge-topic dashboard-goal-track dashboard-goal-track-${row.trackLabel === 'DSA' ? 'dsa' : 'sd'}`}>
                  {row.trackLabel}
                </span>
                <div className="dashboard-goal-main">
                  <div className="dashboard-goal-label">{row.label}</div>
                  <div className="dashboard-goal-sub">
                    Today: {row.todayCompletedCount}/{row.todayTargetCount} · Remaining: {row.remainingCount}
                    {row.sheetName ? ` · ${row.sheetName}` : ''}
                  </div>
                </div>
                <div className="dashboard-goal-days">
                  {remainingDays >= 0 ? `${remainingDays} day${remainingDays === 1 ? '' : 's'} left` : 'overdue'}
                </div>
              </div>
            )
          })}
        </div>
      )}

      <h2 className="section-heading">Today's Planned Things To Do</h2>

      <div className="dashboard-planner">
        <DashboardPlannerCard dsaGoalItems={dsaGoalItems} sdGoalItems={sdGoalItems} />
      </div>

      <h2 className="section-heading">Priority Actions</h2>

      {actions.length === 0 ? (
        <div className="card dashboard-caught-up">
          <span className="dashboard-caught-up-icon">
            <CheckCircleIcon />
          </span>
          <div>
            <div className="dashboard-caught-up-title">You're all caught up</div>
            <div className="dashboard-caught-up-sub">
              No actions currently need your attention — profiles, companies and outreach all look healthy.
            </div>
          </div>
        </div>
      ) : (
        <div className="card dashboard-actions">
          {actions.map((action, i) => (
            <Link className="dashboard-action-row" to={action.to} key={i}>
              <span className="dashboard-action-icon">
                <AlertIcon />
              </span>
              <span>{action.text}</span>
              <span className="dashboard-action-arrow">→</span>
            </Link>
          ))}
        </div>
      )}

      <div className="dashboard-columns">
        <div className="card">
          <div className="dashboard-card-header">
            <h3>Recent Outreach</h3>
            <Link to="/outreach">View all →</Link>
          </div>
          {recentOutreach.length === 0 ? (
            <p className="empty-state">Nothing sent yet.</p>
          ) : (
            <div className="dashboard-activity-list">
              {recentOutreach.map((o) => (
                <div className="dashboard-activity-row" key={o.id}>
                  <div className="dashboard-activity-main">
                    <div className="dashboard-activity-title">{o.companyName}</div>
                    <div className="dashboard-activity-sub">
                      {o.recipientEmail} · {o.sentAtDisplay ?? 'Not sent yet'}
                      {o.templateName ? ` · ${o.templateName}` : ''}
                    </div>
                  </div>
                  <div className="badge-row">
                    {o.status === 'SENT' ? (
                      <span className={`badge badge-reply-${o.replyStatus}`}>{REPLY_STATUS_LABELS[o.replyStatus]}</span>
                    ) : (
                      <span className={`badge badge-${o.status}`}>{o.status}</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card">
          <div className="dashboard-card-header">
            <h3>DSA Practice</h3>
            <Link to="/planner">Open planner →</Link>
          </div>
          {dsaHasGoal ? (
            <>
              <div className="dashboard-activity-row">
                <div>
                  <div className="dashboard-activity-title">Solved today</div>
                  <div className="dashboard-activity-sub">{goalTodayLabel(dsaHasGoal, dsaTodayCompleted, dsaTodayTarget)}</div>
                </div>
                <div className="stat-value" style={{ fontSize: '1.4rem' }}>
                  {dsaTodayCompleted}
                </div>
              </div>
              <div className="progress-bar-track">
                <div
                  className="progress-bar-fill"
                  style={{ width: `${Math.min(100, Math.round((dsaTodayCompleted / dsaTodayTarget) * 100))}%` }}
                />
              </div>
            </>
          ) : (
            <p className="empty-state">Set a goal in the Planner to track progress here.</p>
          )}

          <div className="dashboard-card-subheader">Recently Solved</div>
          {recentSolved.length === 0 ? (
            <p className="empty-state">No questions solved in the last 2 days.</p>
          ) : (
            <div className="dashboard-activity-list">
              {recentSolved.map((q) => (
                <div className="dashboard-activity-row" key={`${q.questionId}-${q.solvedAt}`}>
                  <div>
                    <div className="dashboard-activity-title">{q.title ?? 'Untitled question'}</div>
                    <div className="dashboard-activity-sub">{formatSolvedAt(q.solvedAt)}</div>
                  </div>
                  {q.topicName && <span className="badge badge-topic">{q.topicName}</span>}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card">
          <div className="dashboard-card-header">
            <h3>System Design Practice</h3>
            <Link to="/planner">Open planner →</Link>
          </div>
          {sdHasGoal ? (
            <>
              <div className="dashboard-activity-row">
                <div>
                  <div className="dashboard-activity-title">Completed today</div>
                  <div className="dashboard-activity-sub">{goalTodayLabel(sdHasGoal, sdTodayCompleted, sdTodayTarget)}</div>
                </div>
                <div className="stat-value" style={{ fontSize: '1.4rem' }}>
                  {sdTodayCompleted}
                </div>
              </div>
              <div className="progress-bar-track">
                <div
                  className="progress-bar-fill"
                  style={{ width: `${Math.min(100, Math.round((sdTodayCompleted / sdTodayTarget) * 100))}%` }}
                />
              </div>
            </>
          ) : (
            <p className="empty-state">Set a goal in the Planner to track progress here.</p>
          )}

          <div className="dashboard-card-subheader">Recently Completed</div>
          {recentCompletedSd.length === 0 ? (
            <p className="empty-state">No items completed in the last 2 days.</p>
          ) : (
            <div className="dashboard-activity-list">
              {recentCompletedSd.map((i) => (
                <div className="dashboard-activity-row" key={`${i.itemId}-${i.completedAt}`}>
                  <div>
                    <div className="dashboard-activity-title">{i.title ?? 'Untitled item'}</div>
                    <div className="dashboard-activity-sub">{formatCompletedAt(i.completedAt)}</div>
                  </div>
                  {i.topicName && <span className="badge badge-topic">{i.topicName}</span>}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
