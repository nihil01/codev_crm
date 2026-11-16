import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { groupsApi } from '../api/groups';
import { studentsApi } from '../api/students';
import { teachersApi } from '../api/teachers';
import { CourseGroup } from '../types';
import { useThemeClasses } from '../lib/theme';
import { Plus, Edit2, Trash2, Users, GraduationCap, X, CheckCircle, PauseCircle, PlayCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../api/client';

const statusColors: Record<string, string> = {
  AKTIV: 'bg-emerald-500/10 text-emerald-400',
  BASLAYIB: 'bg-emerald-500/10 text-emerald-400',
  TOPLANIR: 'bg-sky-500/10 text-sky-400',
  TAMAMLANMIS: 'bg-blue-500/10 text-blue-400',
  DAYANDIRILMIS: 'bg-yellow-500/10 text-yellow-400',
};
const statusLabels: Record<string, string> = { AKTIV: 'Başlayıb', BASLAYIB: 'Başlayıb', TOPLANIR: 'Toplanır', TAMAMLANMIS: 'Tamamlanmış', DAYANDIRILMIS: 'Dayandırılmış' };

export const Groups: React.FC = () => {
  const t = useThemeClasses();
  const [showModal, setShowModal] = useState(false);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [editing, setEditing] = useState<CourseGroup | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const emptyForm = { groupNumber: '', startDate: '', durationMonths: 6, courseId: 0, lessonDays: '', lessonTime: '', trainingMode: 'EYANI' as 'EYANI'|'ONLAYN', room:'', maxStudents: 20, status: 'TOPLANIR' as CourseGroup['status'] };
  const [form, setForm] = useState(emptyForm);
  const [teacherForm, setTeacherForm] = useState({ teacherId: 0, salaryPercentage: 30, effectiveMonth: new Date().getMonth() + 1, effectiveYear: new Date().getFullYear() });
  const queryClient = useQueryClient();

  const { data: groups = [], isLoading } = useQuery({ queryKey: ['groups'], queryFn: groupsApi.getAll });
  const {data:courses=[]} = useQuery<{id:number;name:string}[]>({queryKey:['courses'],queryFn:async()=>(await api.get('/courses')).data});
  const [newCourse,setNewCourse] = useState('');
  const courseMutation = useMutation({
    mutationFn:async(name:string)=> (await api.post('/courses',{name})).data,
    onSuccess:()=>{queryClient.invalidateQueries({queryKey:['courses']});setNewCourse('');toast.success('Kurs əlavə edildi');},
    onError:()=>toast.error('Kurs əlavə olunmadı')
  });
  const { data: students = [] } = useQuery({ queryKey: ['students-active'], queryFn: studentsApi.getActive });
  const { data: teachers = [] } = useQuery({ queryKey: ['teachers-active'], queryFn: teachersApi.getActive });
  const { data: groupStudents = [] } = useQuery({ queryKey: ['group-students', selectedGroup], queryFn: () => groupsApi.getStudents(selectedGroup!), enabled: !!selectedGroup });
  const { data: groupTeachers = [] } = useQuery({ queryKey: ['group-teachers', selectedGroup], queryFn: () => groupsApi.getTeachers(selectedGroup!), enabled: !!selectedGroup });

  const createMutation = useMutation({
    mutationFn: groupsApi.create,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['groups'] }); queryClient.invalidateQueries({ queryKey: ['dashboard'] }); toast.success('Qrup yaradıldı'); closeModal(); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<CourseGroup> }) => groupsApi.update(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['groups'] }); queryClient.invalidateQueries({ queryKey: ['dashboard'] }); toast.success('Qrup yeniləndi'); closeModal(); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const deleteMutation = useMutation({
    mutationFn: groupsApi.delete,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['groups'] }); queryClient.invalidateQueries({ queryKey: ['dashboard'] }); toast.success('Qrup silindi'); setSelectedGroup(null); },
    onError: (error: any) => toast.error(error?.response?.data?.message || 'Qrup silinmədi'),
  });
  const completeMutation = useMutation({
    mutationFn: groupsApi.complete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast.success('Qrup tamamlandı');
    },
    onError: (error: any) => toast.error(error?.response?.data?.message || 'Qrup tamamlanmadı'),
  });
  const suspendMutation = useMutation({
    mutationFn: groupsApi.suspend,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast.success('Qrup dayandırıldı');
    },
    onError: (error: any) => toast.error(error?.response?.data?.message || 'Qrup dayandırılmadı'),
  });
  const resumeMutation = useMutation({
    mutationFn: groupsApi.resume,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast.success('Qrup bərpa edildi');
    },
    onError: (error: any) => toast.error(error?.response?.data?.message || 'Qrup bərpa edilmədi'),
  });

  const assignTeacherMutation = useMutation({
    mutationFn: ({ groupId, data }: any) => groupsApi.assignTeacher(groupId, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['group-teachers'] }); queryClient.invalidateQueries({ queryKey: ['groups'] }); toast.success('Müəllim təyin edildi'); setTeacherForm({ teacherId: 0, salaryPercentage: 30, effectiveMonth: new Date().getMonth() + 1, effectiveYear: new Date().getFullYear() }); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const removeTeacherMutation = useMutation({
    mutationFn: ({ groupId, teacherId }: { groupId: number; teacherId: number }) => groupsApi.removeTeacher(groupId, teacherId),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['group-teachers'] }); queryClient.invalidateQueries({ queryKey: ['groups'] }); toast.success('Müəllim çıxarıldı'); },
    onError: (error: any) => toast.error(error?.response?.data?.message || 'Müəllim çıxarılmadı'),
  });

  const addStudentMutation = useMutation({
    mutationFn: ({ groupId, data }: any) => groupsApi.addStudent(groupId, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['group-students'] }); queryClient.invalidateQueries({ queryKey: ['groups'] }); queryClient.invalidateQueries({ queryKey: ['dashboard'] }); toast.success('Tələbə əlavə edildi'); setShowAddStudent(false); setAddStudentForm({ studentId: 0, monthlyAmount: 0, joinDate: '' }); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const errorMessage = (error: any) =>
    error?.response?.data?.message ||
    Object.values(error?.response?.data?.errors || {}).join('; ') ||
    'Əməliyyat yerinə yetirilmədi';

  const removeStudentMutation = useMutation({
    mutationFn: ({ groupId, studentId, reason }: { groupId: number; studentId: number; reason: string }) =>
      groupsApi.removeStudent(groupId, studentId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['group-students'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
      queryClient.invalidateQueries({ queryKey: ['students'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      toast.success('Tələbə qrupdan çıxarıldı; maliyyə tarixçəsi saxlanıldı');
    },
    onError: (error: any) => toast.error(errorMessage(error), { duration: 6500 }),
  });

  const removeStudentFromGroup = (studentId: number) => {
    if(!selectedGroup) return;
    const reason = window.prompt('Tələbənin qrupdan ayrılma səbəbi:');
    if(!reason?.trim()) return;
    removeStudentMutation.mutate({groupId:selectedGroup,studentId,reason:reason.trim()});
  };
  const activeMembers = groupStudents.filter(gs =>
    !gs.leftAt && !gs.studentArchived && gs.studentStatus === 'AKTIV');
  const [showAddStudent, setShowAddStudent] = useState(false);
  const [addStudentForm, setAddStudentForm] = useState({ studentId: 0, monthlyAmount: 0, joinDate: '' });

  const handleAddStudent = () => {
    if (!selectedGroup || !addStudentForm.studentId) return;
    addStudentMutation.mutate({ groupId: selectedGroup, data: addStudentForm });
  };

  const openCreate = () => { setEditing(null); setForm({...emptyForm}); setShowModal(true); };
  const openEdit = (g: CourseGroup) => { setEditing(g); setForm({groupNumber:g.groupNumber,startDate:g.startDate,durationMonths:g.durationMonths,courseId:g.courseId||0,lessonDays:g.lessonDays||'',lessonTime:g.lessonTime||'',trainingMode:g.trainingMode||'EYANI',room:g.room||'',maxStudents:g.maxStudents||20,status:g.status}); setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); };
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.courseId) {toast.error('Kurs seçilməlidir');return;}
    editing?.id ? updateMutation.mutate({ id: editing.id, data: form }) : createMutation.mutate(form);
  };

  const inputCls = `w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors duration-300 ${t.inputBgModal}`;
  const matchingGroups = groups.filter(group => {
    const matchesStatus = statusFilter === 'ALL' || group.status === statusFilter;
    const needle = search.trim().toLocaleLowerCase('az-AZ');
    const searchable = [
      group.groupNumber, group.courseName, group.lessonDays,
      group.lessonTime, group.room, ...(group.teacherNames || [])
    ].filter(Boolean).join(' ').toLocaleLowerCase('az-AZ');
    return matchesStatus && (!needle || searchable.includes(needle));
  });
  const dateLabel = (value?: string) =>
    value ? new Date(value + 'T00:00:00').toLocaleDateString('az-AZ') : '—';

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className={`text-2xl font-bold transition-colors duration-300 ${t.title}`}>Qruplar</h1>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg"><Plus size={18} /> Yeni Qrup</button>
      </div>

      {/* Group list: one sortable/filterable tabular overview, not cards. */}
      <div className={`rounded-xl border p-4 space-y-4 ${t.cardBg}`}>
        <div className="flex flex-wrap items-center gap-3">
          <label className="flex-1 min-w-[220px]">
            <span className={`block text-xs mb-1 ${t.muted}`}>Qrup, kurs, müəllim və ya otaq axtar</span>
            <input type="search" value={search} onChange={e => setSearch(e.target.value)}
              placeholder="Axtar..." className={`w-full px-3 py-2 rounded-lg border ${t.inputBg}`} />
          </label>
          <label className="min-w-[165px]">
            <span className={`block text-xs mb-1 ${t.muted}`}>Status</span>
            <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}
              className={`w-full px-3 py-2 rounded-lg border ${t.inputBg}`}>
              <option value="ALL">Bütün qruplar</option>
              {Object.entries(statusLabels).map(([key, label]) =>
                <option key={key} value={key}>{label}</option>)}
            </select>
          </label>
          <div className={`text-xs self-end pb-2 ${t.muted}`}>
            {matchingGroups.length} / {groups.length} qrup
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1380px] text-sm text-left">
            <thead className={t.thClass}>
              <tr>
                {['Qrup', 'Kurs', 'Başlama', 'Bitmə', 'Müddət', 'Dərs günləri / saat',
                  'Format', 'Otaq', 'Tələbələr / limit', 'Müəllimlər', 'Status', 'Əməliyyatlar']
                  .map(label => <th key={label} scope="col"
                    className="px-3 py-3 font-semibold whitespace-nowrap">{label}</th>)}
              </tr>
            </thead>
            <tbody>
              {isLoading && <tr><td colSpan={12} className={`px-3 py-8 text-center ${t.muted}`}>Yüklənir...</td></tr>}
              {!isLoading && matchingGroups.length === 0 &&
                <tr><td colSpan={12} className={`px-3 py-8 text-center ${t.muted}`}>Qrup tapılmadı</td></tr>}
              {!isLoading && matchingGroups.map(group => (
                <tr key={group.id}
                  onClick={() => setSelectedGroup(id => id === group.id ? null : group.id)}
                  tabIndex={0}
                  onKeyDown={event => {
                    if (event.target === event.currentTarget &&
                        (event.key === 'Enter' || event.key === ' ')) {
                      event.preventDefault();
                      setSelectedGroup(id => id === group.id ? null : group.id);
                    }
                  }}
                  aria-selected={selectedGroup === group.id}
                  className={`border-b ${t.rowBorder} ${t.rowHover} cursor-pointer transition-colors
                    ${selectedGroup === group.id ? 'bg-indigo-500/10 ring-1 ring-inset ring-indigo-500/40' : ''}`}>
                  <td className={`px-3 py-3 font-semibold whitespace-nowrap ${t.title}`}>
                    {group.groupNumber}
                  </td>
                  <td className="px-3 py-3 max-w-[200px]">{group.courseName || '—'}</td>
                  <td className="px-3 py-3 whitespace-nowrap">{dateLabel(group.startDate)}</td>
                  <td className="px-3 py-3 whitespace-nowrap">{dateLabel(group.endDate)}</td>
                  <td className="px-3 py-3 whitespace-nowrap">{group.durationMonths} ay</td>
                  <td className="px-3 py-3 whitespace-nowrap">
                    {group.lessonDays || '—'} {group.lessonTime || ''}
                  </td>
                  <td className="px-3 py-3">{group.trainingMode === 'ONLAYN' ? 'Onlayn'
                    : group.trainingMode === 'EYANI' ? 'Əyani' : '—'}</td>
                  <td className="px-3 py-3">{group.room || '—'}</td>
                  <td className="px-3 py-3 whitespace-nowrap">
                    <span className="font-semibold">{group.activeStudentCount ?? '—'}</span>
                    <span className={t.muted}> / {group.maxStudents ?? '∞'}</span>
                  </td>
                  <td className="px-3 py-3 min-w-[170px]">
                    {group.teacherNames?.length ? group.teacherNames.join(', ') : '—'}
                  </td>
                  <td className="px-3 py-3">
                    <span className={`inline-block rounded px-2 py-1 text-xs whitespace-nowrap ${statusColors[group.status]}`}>
                      {statusLabels[group.status] || group.status}
                    </span>
                  </td>
                  <td className="px-3 py-3">
                    <div className="flex items-center gap-2 whitespace-nowrap">
                      <button onClick={e => { e.stopPropagation(); openEdit(group); }}
                        className={`p-1.5 rounded ${t.iconBtn}`} title="Redaktə"
                        aria-label={`${group.groupNumber} redaktə et`}><Edit2 size={16} /></button>
                      {(group.status === 'AKTIV' || group.status === 'BASLAYIB') && <>
                        <button onClick={e => { e.stopPropagation(); completeMutation.mutate(group.id); }}
                          className="p-1.5 rounded text-emerald-400" title="Tamamla"
                          aria-label={`${group.groupNumber} tamamla`}><CheckCircle size={16} /></button>
                        <button onClick={e => { e.stopPropagation(); suspendMutation.mutate(group.id); }}
                          className="p-1.5 rounded text-yellow-400" title="Dayandır"
                          aria-label={`${group.groupNumber} dayandır`}><PauseCircle size={16} /></button>
                      </>}
                      {group.status === 'DAYANDIRILMIS' &&
                        <button onClick={e => { e.stopPropagation(); resumeMutation.mutate(group.id); }}
                          className="p-1.5 rounded text-emerald-400" title="Bərpa et"
                          aria-label={`${group.groupNumber} bərpa et`}><PlayCircle size={16} /></button>}
                      <button onClick={e => { e.stopPropagation();
                          if (window.confirm(`${group.groupNumber} qrupunu silmək istəyirsiniz?`))
                            deleteMutation.mutate(group.id);
                        }} className={`p-1.5 rounded ${t.iconBtnDanger}`} title="Sil"
                        aria-label={`${group.groupNumber} sil`}><Trash2 size={16} /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className={`text-xs ${t.muted}`}>
          Qrupa toxunaraq tələbə və müəllim cədvəllərini açın. Tarixi qeydiyyatlar saxlanılır,
          lakin aktiv tələbə sayına daxil edilmir.
        </p>
      </div>

      {/* Preserve the full management workflow in table-based details. */}
      {selectedGroup && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
          className={`${t.cardBg} border rounded-xl p-5 space-y-5`}>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className={`text-lg font-semibold ${t.title}`}>
              Qrup Detalları — {groups.find(g => g.id === selectedGroup)?.groupNumber}
            </h2>
            <div className="flex gap-2">
              <button onClick={() => setShowAddStudent(true)}
                className="flex items-center gap-2 px-3 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm rounded-lg">
                <Plus size={16} /> Tələbə əlavə et
              </button>
              <button onClick={() => setSelectedGroup(null)} className={`p-2 rounded ${t.iconBtn}`}
                title="Bağla" aria-label="Qrup detallarını bağla"><X size={18} /></button>
            </div>
          </div>

          <section className="space-y-3">
            <h3 className={`flex items-center gap-2 text-sm font-semibold ${t.title}`}>
              <Users size={17} /> Tələbələr ({activeMembers.length} aktiv / {groupStudents.length} ümumi)
            </h3>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[800px] text-sm text-left">
                <thead className={t.thClass}><tr>
                  {['Ad və soyad', 'FİN', 'Qeydiyyat tarixi', 'Aylıq ödəniş', 'Status', 'Çıxma səbəbi', 'Əməliyyat']
                    .map(label => <th key={label} className="px-3 py-2">{label}</th>)}
                </tr></thead>
                <tbody>
                  {groupStudents.length === 0 &&
                    <tr><td colSpan={7} className={`p-4 text-center ${t.muted}`}>Tələbə yoxdur</td></tr>}
                  {groupStudents.map(gs => (
                    <tr key={gs.id} className={`border-b ${t.rowBorder}`}>
                      <td className={`px-3 py-3 font-medium ${t.title}`}>
                        {gs.studentFirstName} {gs.studentLastName}
                      </td>
                      <td className="px-3 py-3 font-mono">{gs.studentFin || '—'}</td>
                      <td className="px-3 py-3 whitespace-nowrap">{dateLabel(gs.joinDate)}</td>
                      <td className="px-3 py-3 whitespace-nowrap text-emerald-400">{gs.monthlyAmount} AZN/ay</td>
                      <td className="px-3 py-3">
                        {gs.leftAt ? `Ayrılıb: ${dateLabel(gs.leftAt)}`
                          : gs.studentArchived ? 'Arxiv' : gs.studentStatus || '—'}
                      </td>
                      <td className="px-3 py-3">{gs.exitReason || '—'}</td>
                      <td className="px-3 py-3">
                        {!gs.leftAt && <button onClick={() => removeStudentFromGroup(gs.studentId)}
                          disabled={removeStudentMutation.isPending}
                          className={`p-1.5 rounded ${t.iconBtnDanger}`} title="Qrupdan çıxar">
                          <Trash2 size={15} />
                        </button>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="space-y-3">
            <h3 className={`flex items-center gap-2 text-sm font-semibold ${t.title}`}>
              <GraduationCap size={17} /> Müəllimlər ({groupTeachers.length})
            </h3>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px] text-sm text-left">
                <thead className={t.thClass}><tr>
                  {['Ad və soyad', 'Müəllim payı', 'Başlanğıc', 'Bitmə', 'Bu ay hesablanan', 'Əməliyyat']
                    .map(label => <th key={label} className="px-3 py-2">{label}</th>)}
                </tr></thead>
                <tbody>
                  {groupTeachers.length === 0 &&
                    <tr><td colSpan={6} className={`p-4 text-center ${t.muted}`}>Müəllim yoxdur</td></tr>}
                  {groupTeachers.map(gt => (
                    <tr key={gt.id} className={`border-b ${t.rowBorder}`}>
                      <td className={`px-3 py-3 font-medium ${t.title}`}>
                        {gt.teacherFirstName} {gt.teacherLastName}
                      </td>
                      <td className="px-3 py-3 text-orange-400">%{gt.salaryPercentage}</td>
                      <td className="px-3 py-3">{gt.effectiveMonth}/{gt.effectiveYear}</td>
                      <td className="px-3 py-3">{gt.effectiveUntilYear
                        ? `${gt.effectiveUntilMonth}/${gt.effectiveUntilYear}` : 'Davam edir'}</td>
                      <td className="px-3 py-3 text-emerald-400">
                        {(gt.calculatedSalary || 0).toLocaleString('az-AZ')} AZN
                      </td>
                      <td className="px-3 py-3">
                        {!gt.effectiveUntilYear && <button
                          onClick={() => removeTeacherMutation.mutate({
                            groupId: selectedGroup, teacherId: gt.teacherId
                          })}
                          disabled={removeTeacherMutation.isPending}
                          className={`p-1.5 rounded ${t.iconBtnDanger}`} title="Müəllimi çıxar">
                          <Trash2 size={15} />
                        </button>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className={`p-3 rounded-lg border border-dashed ${t.border}`}>
              <h4 className={`text-sm font-medium mb-3 ${t.title}`}>Müəllim təyin et</h4>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <select value={teacherForm.teacherId}
                  onChange={e => setTeacherForm({ ...teacherForm, teacherId: +e.target.value })}
                  className={`${inputCls} text-sm`}>
                  <option value={0}>Müəllim seç...</option>
                  {teachers.map(te => <option key={te.id} value={te.id}>
                    {te.firstName} {te.lastName}</option>)}
                </select>
                <input type="number" min={1} max={100} step={0.01}
                  value={teacherForm.salaryPercentage}
                  onChange={e => setTeacherForm({ ...teacherForm, salaryPercentage: +e.target.value })}
                  placeholder="Faiz %" className={`${inputCls} text-sm`} />
                <button onClick={() => {
                    if (teacherForm.teacherId)
                      assignTeacherMutation.mutate({ groupId: selectedGroup, data: teacherForm });
                  }}
                  disabled={!teacherForm.teacherId || assignTeacherMutation.isPending}
                  className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm rounded-lg px-3 py-2 disabled:opacity-50">
                  Müəllim təyin et
                </button>
              </div>
            </div>
          </section>
        </motion.div>
      )}

      {/* Create/Edit Modal */}
      <AnimatePresence>
        {showModal && (
          <>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className={`fixed inset-0 z-40 ${t.overlay}`} onClick={closeModal} />
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <div className={`${t.modalBg} border rounded-xl w-full max-w-md p-6 transition-colors duration-300`}>
                <div className="flex items-center justify-between mb-6">
                  <h2 className={`text-lg font-semibold transition-colors duration-300 ${t.title}`}>{editing ? 'Qrupu Redaktə Et' : 'Yeni Qrup'}</h2>
                  <button onClick={closeModal} className={t.iconBtn}><X size={20} /></button>
                </div>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Qrup Nömrəsi</label><input type="text" value={form.groupNumber} onChange={(e) => setForm({ ...form, groupNumber: e.target.value })} placeholder="G-2024-01" className={inputCls} required /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Başlama Tarixi</label><input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} className={inputCls} required /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Davam Müddəti (ay)</label><input type="number" value={form.durationMonths} onChange={(e) => setForm({ ...form, durationMonths: +e.target.value })} min={1} className={inputCls} required /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Kurs</label><select className={inputCls} value={form.courseId} onChange={e=>setForm({...form,courseId:+e.target.value})}><option value={0}>Kurs seçin</option>{courses.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
                  <div className="flex gap-2"><input className={inputCls} placeholder="Yeni kurs adı" value={newCourse} onChange={e=>setNewCourse(e.target.value)}/><button type="button" className="px-3 rounded-lg border" disabled={!newCourse.trim()} onClick={()=>courseMutation.mutate(newCourse)}>Əlavə et</button></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Dərs günləri</label><input className={inputCls} placeholder="B.e., Ç.a., C." value={form.lessonDays} onChange={e=>setForm({...form,lessonDays:e.target.value})}/></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Dərs saatı</label><input type="time" className={inputCls} value={form.lessonTime} onChange={e=>setForm({...form,lessonTime:e.target.value})}/></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Format</label><select className={inputCls} value={form.trainingMode} onChange={e=>setForm({...form,trainingMode:e.target.value as 'EYANI'|'ONLAYN'})}><option value="EYANI">Əyani</option><option value="ONLAYN">Onlayn</option></select></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Otaq</label><input className={inputCls} value={form.room} onChange={e=>setForm({...form,room:e.target.value})}/></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Maksimum say</label><input type="number" min={1} className={inputCls} value={form.maxStudents} onChange={e=>setForm({...form,maxStudents:+e.target.value})}/></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Status</label><select className={inputCls} value={form.status} onChange={e=>setForm({...form,status:e.target.value as CourseGroup['status']})}><option value="TOPLANIR">Toplanır</option><option value="BASLAYIB">Başlayıb</option><option value="DAYANDIRILMIS">Dayandırılıb</option><option value="TAMAMLANMIS">Tamamlanıb</option></select></div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={closeModal} className={`flex-1 py-2 rounded-lg transition-colors ${t.cancelBtn}`}>Ləğv Et</button>
                    <button type="submit" className="flex-1 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors">{editing ? 'Yenilə' : 'Yarat'}</button>
                  </div>
                </form>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Add Student Modal */}
      <AnimatePresence>
        {showAddStudent && (
          <>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className={`fixed inset-0 z-40 ${t.overlay}`} onClick={() => setShowAddStudent(false)} />
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <div className={`${t.modalBg} border rounded-xl w-full max-w-md p-6 transition-colors duration-300`}>
                <div className="flex items-center justify-between mb-6">
                  <h2 className={`text-lg font-semibold transition-colors duration-300 ${t.title}`}>Tələbə Əlavə Et</h2>
                  <button onClick={() => setShowAddStudent(false)} className={t.iconBtn}><X size={20} /></button>
                </div>
                <div className="space-y-4">
                  <div>
                    <label className={`block text-sm mb-1 ${t.label}`}>Tələbə</label>
                    <select value={addStudentForm.studentId} onChange={(e) => setAddStudentForm({ ...addStudentForm, studentId: +e.target.value })} className={inputCls}>
                      <option value={0}>Seçin...</option>
                      {students.map((s) => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} ({s.fin})</option>)}
                    </select>
                  </div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Aylıq Məbləğ (AZN)</label><input type="number" value={addStudentForm.monthlyAmount || ''} onChange={(e) => setAddStudentForm({ ...addStudentForm, monthlyAmount: +e.target.value })} className={inputCls} min={0} step={0.01} required /></div>
                  <div><label className={`block text-sm mb-1 ${t.label}`}>Qoşulma Tarixi</label><input type="date" value={addStudentForm.joinDate} onChange={(e) => setAddStudentForm({ ...addStudentForm, joinDate: e.target.value })} className={inputCls} required /></div>
                  <button onClick={handleAddStudent} disabled={!addStudentForm.studentId} className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg disabled:opacity-50">Əlavə Et</button>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
};
