export enum Role {
  ADMIN = 'ADMIN',
  MANAGER = 'MANAGER',
}

export enum GroupStatus {
  AKTIV = 'AKTIV',
  BASLAYIB = 'BASLAYIB',
  TOPLANIR = 'TOPLANIR',
  TAMAMLANMIS = 'TAMAMLANMIS',
  DAYANDIRILMIS = 'DAYANDIRILMIS',
}

export enum PaymentStatus {
  ODENILIB = 'ODENILIB',
  GOZLENILIR = 'GOZLENILIR',
  GECIKIB = 'GECIKIB',
  BAGISLANIB = 'BAGISLANIB',
  DONDURULUB = 'DONDURULUB',
}

export enum PaymentMethod {
  NAGD = 'NAGD',
  KART = 'KART',
  KOCURME = 'KOCURME',
}

export enum TeacherStatus {
  AKTIV = 'AKTIV',
  ARXIV = 'ARXIV',
}

export enum ExpenseType {
  YER = 'YER',
  ISCHI_MAASI = 'ISCHI_MAASI',
  INTERNET = 'INTERNET',
  ELEKTRIK = 'ELEKTRIK',
  SU = 'SU',
  QAZ = 'QAZ',
  DIGER = 'DIGER',
  REKLAM = 'REKLAM',
  KOMMUNAL = 'KOMMUNAL',
  AVADANLIQ = 'AVADANLIQ',
}

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: Role;
}

export type LeadSource = 'INSTAGRAM'|'WHATSAPP'|'TIKTOK'|'ZENG'|'TOVSIYE'|'SAYT'|'BIRBASA_GELIS';
export type LeadStage = 'YENI'|'ELAQE_SAXLANILDI'|'SINAQ_KONSULTASIYA'|'DUSUNUR'|'QEYDIYYAT'|'ITIRILDI';
export type StudentStatus = 'AKTIV'|'DONDURULUB'|'BITIRIB'|'AYRILIB';

export interface Lead {
  id: number;
  fullName: string;
  phone: string;
  source: LeadSource;
  stage: LeadStage;
  assignedManagerId?: number | null;
  convertedStudentId?: number | null;
  assignedManagerName?: string;
  consultationAt?: string;
  lostReason?: string;
  interestedCourse?: string;
  note?: string;
  createdAt?: string;
}

export interface Student {
  id: number;
  firstName: string;
  lastName: string;
  fin: string;
  email: string;
  phone?: string;
  birthDate?: string;
  source?: LeadSource;
  parentName?: string;
  parentPhone?: string;
  address?: string;
  note?: string;
  status?: StudentStatus;
  exitReason?: string;
  groupId?: number;
  registrationDate: string;
  archived: boolean;
}

export interface Teacher {
  id: number;
  firstName: string;
  lastName: string;
  fin: string;
  email: string;
  phone?: string;
  status: TeacherStatus;
}

export interface CourseGroup {
  id: number;
  groupNumber: string;
  startDate: string;
  durationMonths: number;
  endDate: string;
  status: GroupStatus;
  courseId?: number;
  courseName?: string;
  lessonDays?: string;
  lessonTime?: string;
  trainingMode?: 'EYANI'|'ONLAYN';
  room?: string;
  maxStudents?: number;
}

export interface GroupStudent {
  id: number;
  studentId: number;
  monthlyAmount: number;
  joinDate: string;
  note?: string;
  studentFirstName: string;
  studentLastName: string;
  studentFin: string;
}

export interface GroupTeacher {
  id: number;
  teacherId: number;
  salaryPercentage: number;
  calculatedSalary?: number;
  salaryStatus?: string;
  effectiveMonth: number;
  effectiveYear: number;
  teacherFirstName: string;
  teacherLastName: string;
}

export interface Payment {
  id: number;
  studentId: number;
  groupId: number;
  paymentYear: number;
  paymentMonth: number;
  amount: number;
  paymentDate?: string;
  dueDate?: string;
  paymentMethod?: PaymentMethod;
  paidAmount?: number;
  remainingAmount?: number;
  discountAmount?: number;
  discountReason?: string;
  frozen?: boolean;
  studentPhone?: string;
  status: PaymentStatus;
  note?: string;
  studentFirstName: string;
  studentLastName: string;
  groupNumber: string;
}

export interface Expense {
  id: number;
  type: ExpenseType;
  amount: number;
  expenseYear: number;
  expenseMonth: number;
  recurring: boolean;
  description?: string;
  adChannel?: string;
}

export interface DashboardData {
  totalStudents: number;
  activeStudents: number;
  totalTeachers: number;
  activeGroups: number;
  currentMonthCollected: number;
  currentMonthExpected: number;
  collectionRate: number;
  overdueCount: number;
  recentPayments: Payment[];
  groupSummaries: GroupSummary[];
}

export interface GroupSummary {
  groupId: number;
  groupNumber: string;
  studentCount: number;
  collected: number;
  teacherShare: number;
  netProfit: number;
}

export interface ReportData {
  year: number;
  month: number;
  pendingPayments: Payment[];
  paidPayments: Payment[];
  overduePayments: Payment[];
  totalCollected: number;
  totalExpected: number;
  collectionRate: number;
  totalIncome: number;
  totalTeacherSalaries: number;
  totalFixedExpenses: number;
  totalUtilityExpenses: number;
  totalExpenses: number;
  netProfit: number;
  monthlyComparison: MonthlyComparison[];
}

export interface MonthlyComparison {
  year: number;
  month: number;
  income: number;
  expenses: number;
  profit: number;
}

export interface ForecastPoint {
  year: number;
  month: number;
  income: number;
  expenses: number;
  netProfit: number;
  isProjected: boolean;
}

export interface TrendSummary {
  monthlyProfitTrend: number;
  projectedAnnualProfit: number;
  trendDirection: 'GELIŞEN' | 'STABIL' | 'AZALAN';
  averageMonthlyIncome: number;
  averageMonthlyExpenses: number;
  averageMonthlyProfit: number;
}

export interface ForecastData {
  history: ForecastPoint[];
  forecast: ForecastPoint[];
  trend: TrendSummary;
}
