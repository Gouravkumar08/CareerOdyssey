export interface UserRequest {
  name: string
  email: string
  phone?: string
  location?: string
  linkedinUrl?: string
  githubUrl?: string
  portfolioUrl?: string
  skills?: string
  education?: string
  noticePeriod?: string
  expectedSalary?: string
  currentCompany?: string
  currentDesignation?: string
  currentStartDate?: string // "yyyy-MM"
  currentEndDate?: string // "yyyy-MM"; absent = "Present"
  currentSummary?: string
  previousCompany?: string
  previousDesignation?: string
  previousStartDate?: string
  previousEndDate?: string
  previousSummary?: string
  resumeDriveLink?: string
  gmailUsername?: string
  gmailAppPassword?: string // blank on update = keep existing
}

export interface UserResponse {
  id: number
  name: string
  email: string
  phone: string | null
  location: string | null
  linkedinUrl: string | null
  githubUrl: string | null
  portfolioUrl: string | null
  skills: string | null
  education: string | null
  noticePeriod: string | null
  expectedSalary: string | null
  overallExperienceDisplay: string | null
  currentCompany: string | null
  currentDesignation: string | null
  currentStartDate: string | null
  currentEndDate: string | null
  currentDurationDisplay: string | null
  currentSummary: string | null
  previousCompany: string | null
  previousDesignation: string | null
  previousStartDate: string | null
  previousEndDate: string | null
  previousDurationDisplay: string | null
  previousSummary: string | null
  resumeDriveLink: string | null
  gmailUsername: string | null
  gmailAppPasswordSet: boolean
  lastUsedAt: string | null
  lastUsedDisplay: string | null
  createdAt: string
  updatedAt: string
}

export interface CompanyRequest {
  companyName: string
  location?: string
  website?: string
  careerSite?: string
  careerEmail?: string
  hrEmail?: string
  hrName?: string
  companyDescription?: string
  linkedinUrl?: string
  notes?: string
}

export interface CompanyResponse extends CompanyRequest {
  id: number
  createdAt: string
  updatedAt: string
}

export type OutreachStatus = 'DRAFT' | 'SENT' | 'REPLIED' | 'NO_REPLY' | 'FAILED'
export type RecipientType = 'HR' | 'CAREER'
export type ReplyStatus = 'SENT' | 'GOT_REPLY' | 'NO_REPLY' | 'DEAD'

export const REPLY_STATUS_LABELS: Record<ReplyStatus, string> = {
  SENT: 'Sent',
  GOT_REPLY: 'Replied',
  NO_REPLY: 'No reply',
  DEAD: 'Dead',
}

export interface OutreachSendRequest {
  userId: number
  companyIds: number[]
  templateId: number
  jobTitle?: string
  jobUrl?: string
}

export interface OutreachResponse {
  id: number
  userId: number
  userName: string
  companyId: number
  companyName: string
  recipientEmail: string
  recipientName: string
  recipientType: RecipientType
  templateId: number
  templateName: string | null
  subject: string | null
  jobTitle: string | null
  jobUrl: string | null
  status: OutreachStatus
  sentAt: string | null
  sentAtDisplay: string | null
  replyReceived: boolean
  repliedAt: string | null
  replyStatus: ReplyStatus
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface EmailTemplateResponse {
  id: number
  name: string
  subjectTemplate: string
  active: boolean
  followUp: boolean
}

export interface StreakResponse {
  currentStreak: number
  message: string
}

export interface ApiErrorBody {
  timestamp: string
  status: number
  error: string
  message: string
  fieldErrors: Record<string, string> | null
}

// ---- DSA Tracker ----

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

export interface SheetRequest {
  name: string
  description?: string
}

export interface SheetResponse {
  id: number
  name: string
  description: string | null
  createdAt: string
  questionCount: number
}

export interface QuestionRequest {
  topicName: string
  subTopic?: string
  title: string
  difficulty: Difficulty
  important: boolean
  practiceLink?: string
  youtubeLink?: string
  articleLink?: string
}

export interface QuestionResponse {
  id: number
  topicId: number
  topicName: string
  subTopic: string | null
  title: string
  difficulty: Difficulty
  important: boolean
  practiceLink: string | null
  youtubeLink: string | null
  articleLink: string | null
  solved: boolean
  revisionStarred: boolean
  note: string | null
  githubSolutionLink: string | null
  solvedAt: string | null
}

export interface ProgressUpdateRequest {
  solved?: boolean
  revisionStarred?: boolean
  note?: string
  githubSolutionLink?: string
}

export interface DifficultyStats {
  total: number
  solved: number
}

export interface SheetStatsResponse {
  totalQuestions: number
  solvedQuestions: number
  overallPercent: number
  topicsCount: number
  importantTotal: number
  importantSolved: number
  easy: DifficultyStats
  medium: DifficultyStats
  hard: DifficultyStats
}

export interface ImportResultResponse {
  importedCount: number
  skippedCount: number
  skippedReasons: string[]
}

export interface TargetPlanRequest {
  dailyTargetCount: number
  sheetId?: number | null
}

export interface TargetPlanResponse {
  dailyTargetCount: number
  sheetId: number | null
  startDate: string | null
  todaySolvedCount: number
}

export interface QuestionSummary {
  id: number
  title: string
}

export interface CalendarDayResponse {
  date: string
  solvedCount: number
  questions: QuestionSummary[]
}

export interface RecentSolvedResponse {
  questionId: number
  title: string | null
  topicName: string | null
  solvedAt: string
}

export interface DsaGoalTopicTargetRequest {
  topicId: number
  targetQuestionCount?: number | null // null/omitted = "complete every matching question"
  difficulties?: Difficulty[] // empty/omitted = no restriction
}
export interface DsaGoalItemRequest {
  topics: DsaGoalTopicTargetRequest[] // one or more
  sheetId?: number | null
  endDate: string // ISO yyyy-MM-dd
}
export interface DsaGoalTopicTargetResponse {
  topicId: number
  topicName: string
  targetQuestionCount: number | null
  difficulties: Difficulty[]
  totalTarget: number
  completedCount: number
  remainingCount: number
  todayTargetCount: number
  todayCompletedCount: number
  isWeekend: boolean
  overdue: boolean
  yesterdayMet: boolean | null
  done: boolean
}
export interface DsaGoalItemResponse {
  id: number
  sheetId: number | null
  sheetName: string | null
  startDate: string
  endDate: string
  topics: DsaGoalTopicTargetResponse[]
  done: boolean
  overallRemainingCount: number
  overallTodayTargetCount: number
}
export interface DsaGoalTopicDayBreakdown {
  topicId: number
  topicName: string
  plannedCount: number
  actualCount: number
}

export interface DsaGoalItemDayResponse {
  date: string
  plannedCount: number
  actualCount: number
  isWeekend: boolean
  topics: DsaGoalTopicDayBreakdown[]
}

export interface TopicResponse {
  id: number
  name: string
  questionCount: number
}

// ---- System Design Tracker ----

export type SdCategory = 'HLD' | 'LLD'

export interface SdSheetRequest {
  name: string
  description?: string
}

export interface SdSheetResponse {
  id: number
  name: string
  description: string | null
  createdAt: string
  itemCount: number
}

export interface SdItemRequest {
  topicName: string
  subTopic?: string
  title: string
  category: SdCategory
  important: boolean
  resourceLink?: string
  description?: string
  sheetId?: number
}

export interface SdItemResponse {
  id: number
  topicId: number
  topicName: string
  subTopic: string | null
  title: string
  category: SdCategory
  important: boolean
  resourceLink: string | null
  description: string | null
  completed: boolean
  revisionStarred: boolean
  note: string | null
  githubSolutionLink: string | null
  completedAt: string | null
}

export interface SdProgressUpdateRequest {
  completed?: boolean
  revisionStarred?: boolean
  note?: string
  githubSolutionLink?: string
}

export interface SdCategoryStats {
  total: number
  completed: number
}

export interface SdSheetStatsResponse {
  total: number
  completed: number
  overallPercent: number
  topicsCount: number
  importantTotal: number
  importantSolved: number
  hldTotal: number
  hldCompleted: number
  lldTotal: number
  lldCompleted: number
}

export interface SdTargetPlanRequest {
  dailyTargetCount: number
  sheetId?: number | null
}

export interface SdTargetPlanResponse {
  dailyTargetCount: number
  sheetId: number | null
  startDate: string | null
  todayCompletedCount: number
}

export interface SdImportResultResponse {
  importedCount: number
  skippedCount: number
  skippedReasons: string[]
}

export interface SdItemSummary {
  id: number
  title: string
}

export interface SdCalendarDayResponse {
  date: string
  completedCount: number
  topicsCount: number
  items: SdItemSummary[]
}

export interface SdRecentCompletedResponse {
  itemId: number
  title: string | null
  topicName: string | null
  completedAt: string
}

export interface SdGoalItemRequest {
  topicIds: number[] // no unit field — System Design goals are ALWAYS "complete these topics", no question-count mode
  sheetId?: number | null
  endDate: string
}
export interface SdGoalTopicStatus {
  topicId: number
  topicName: string
  totalTarget: number
  completedCount: number
  remainingCount: number
  todayTargetCount: number
  todayCompletedCount: number
  isWeekend: boolean
  overdue: boolean
  yesterdayMet: boolean | null
  done: boolean
}
export interface SdGoalItemResponse {
  id: number
  sheetId: number | null
  sheetName: string | null
  startDate: string
  endDate: string
  topics: SdGoalTopicStatus[]
  done: boolean
  overallRemainingCount: number
  overallTodayTargetCount: number
}
export interface SdGoalTopicDayBreakdown {
  topicId: number
  topicName: string
  plannedCount: number
  actualCount: number
}

export interface SdGoalItemDayResponse {
  date: string
  plannedCount: number
  actualCount: number
  isWeekend: boolean
  topics: SdGoalTopicDayBreakdown[]
}

export interface SdTopicRequest {
  name: string
}

export interface SdTopicResponse {
  id: number
  name: string
  orderIndex: number
  itemCount: number
}

export interface SdSubTopicResponse {
  name: string
  itemCount: number
}

export interface SdSubTopicRenameRequest {
  oldName: string
  newName: string
}

// ---- Free Resources ----

export interface ResourceCategoryRequest {
  name: string
}

export interface ResourceCategoryResponse {
  id: number
  name: string
  orderIndex: number
  itemCount: number
}

export interface ResourceSubCategoryResponse {
  name: string
  itemCount: number
}

export interface ResourceSubCategoryRenameRequest {
  oldName: string
  newName: string
}

export interface ResourceItemRequest {
  categoryName: string
  subCategory?: string
  name: string
  link: string
  important: boolean
}

export interface ResourceItemResponse {
  id: number
  categoryId: number
  categoryName: string | null
  subCategory: string | null
  name: string
  link: string
  important: boolean
}

export type TaskRecurrence = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface PlannerTaskRequest {
  title: string
  description?: string
  priority: TaskPriority
  recurrence: TaskRecurrence
  weekdays?: Weekday[]
  startDate: string
  endDate?: string
  progressUnit?: string
  progressCurrent?: number
  progressTarget?: number
}

export interface PlannerTaskResponse {
  id: number
  title: string
  description: string | null
  priority: TaskPriority
  recurrence: TaskRecurrence
  weekdays: Weekday[]
  startDate: string
  endDate: string | null
  progressUnit: string | null
  progressCurrent: number | null
  progressTarget: number | null
  updatedAt: string
}

export interface PlannerOccurrence {
  taskId: number
  title: string
  date: string
  priority: TaskPriority
  recurrence: TaskRecurrence
  completed: boolean
}

export interface PlannerCalendarDay {
  date: string
  tasks: PlannerOccurrence[]
}

export interface PlannerNoteResponse {
  id: number
  content: string
  updatedAt: string
}

export interface PlannerOverview {
  date: string
  today: PlannerOccurrence[]
  overdue: PlannerOccurrence[]
  upcoming: PlannerOccurrence[]
  progress: PlannerTaskResponse[]
  notes: PlannerNoteResponse[]
}

export interface QuickLinkRequest {
  title?: string
  url: string
}

export interface QuickLinkResponse {
  id: number
  title: string
  url: string
}
