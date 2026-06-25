export enum Role {
  ADMIN = 'ADMIN',
}

export enum GroupStatus {
  AKTIV = 'AKTIV',
  TAMAMLANMIS = 'TAMAMLANMIS',
  DAYANDIRILMIS = 'DAYANDIRILMIS',
}

export enum PaymentStatus {
  ODENILIB = 'ODENILIB',
  GOZLENILIR = 'GOZLENILIR',
  GECIKIB = 'GECIKIB',
  BAGISLANIB = 'BAGISLANIB',
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

export interface Student {
  id: number;
  firstName: string;
  lastName: string;
  fin: string;
  email: string;
  phone?: string;
  birthDate?: string;
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
  paymentMethod?: PaymentMethod;
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
  isRecurring: boolean;
  description?: string;
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
