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
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['groups'] }); toast.success('Qrup yaradıldı'); closeModal(); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<CourseGroup> }) => groupsApi.update(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['groups'] }); toast.success('Qrup yeniləndi'); closeModal(); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const deleteMutation = useMutation({
    mutationFn: groupsApi.delete,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['groups'] }); toast.success('Qrup silindi'); setSelectedGroup(null); },
    onError: (error: any) => toast.error(error?.response?.data?.message || 'Qrup silinmədi'),
  });
  const completeMutation = useMutation({ mutationFn: groupsApi.complete, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['groups'] }); toast.success('Qrup tamamlandı'); } });
  const suspendMutation = useMutation({ mutationFn: groupsApi.suspend, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['groups'] }); toast.success('Qrup dayandırıldı'); } });
  const resumeMutation = useMutation({ mutationFn: groupsApi.resume, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['groups'] }); toast.success('Qrup bərpa edildi'); } });

  const assignTeacherMutation = useMutation({
    mutationFn: ({ groupId, data }: any) => groupsApi.assignTeacher(groupId, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['group-teachers'] }); toast.success('Müəllim təyin edildi'); setTeacherForm({ teacherId: 0, salaryPercentage: 30, effectiveMonth: new Date().getMonth() + 1, effectiveYear: new Date().getFullYear() }); },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Xəta'),
  });
  const removeTeacherMutation = useMutation({
    mutationFn: ({ groupId, teacherId }: { groupId: number; teacherId: number }) => groupsApi.removeTeacher(groupId, teacherId),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['group-teachers'] }); toast.success('Müəllim çıxarıldı'); },
  });

  const addStudentMutation = useMutation({
    mutationFn: ({ groupId, data }: any) => groupsApi.addStudent(groupId, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['group-students'] }); toast.success('Tələbə əlavə edildi'); setShowAddStudent(false); setAddStudentForm({ studentId: 0, monthlyAmount: 0, joinDate: '' }); },
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

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className={`text-2xl font-bold transition-colors duration-300 ${t.title}`}>Qruplar</h1>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg"><Plus size={18} /> Yeni Qrup</button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-8"><div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" /></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {groups.map((group, i) => (
            <motion.div key={group.id} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.05 }}
              onClick={() => setSelectedGroup(group.id)}
              className={`${t.cardBg} border rounded-xl p-5 cursor-pointer transition-all hover:border-indigo-500/50 ${selectedGroup === group.id ? 'border-indigo-500 ring-1 ring-indigo-500/30' : ''} duration-300`}>
              <div className="flex items-center justify-between mb-3">
                <h3 className={`text-lg font-semibold transition-colors duration-300 ${t.title}`}>{group.groupNumber}</h3>
                <span className={`px-2 py-1 rounded text-xs ${statusColors[group.status]}`}>{statusLabels[group.status]}</span>
              </div>
              <div className={`text-sm space-y-1 transition-colors duration-300 ${t.muted}`}>
                <p>Başlama: {new Date(group.startDate).toLocaleDateString('az-AZ')}</p>
                <p>Müddət: {group.durationMonths} ay</p>
                <p>Kurs: {group.courseName || 'Təyin edilməyib'}</p>
                <p>{group.lessonDays || '—'} {group.lessonTime || ''} · {group.trainingMode === 'ONLAYN' ? 'Onlayn' : 'Əyani'}</p>
                {group.endDate && <p>Bitmə: {new Date(group.endDate).toLocaleDateString('az-AZ')}</p>}
              </div>
              <div className="flex gap-2 mt-3">
                <button onClick={(e) => { e.stopPropagation(); openEdit(group); }} className={`p-1 rounded transition-colors ${t.iconBtn}`} title="Redaktə"><Edit2 size={14} /></button>
                {(group.status === 'AKTIV' || group.status === 'BASLAYIB') && (
                  <>
                    <button onClick={(e) => { e.stopPropagation(); completeMutation.mutate(group.id); }} className="text-xs text-emerald-400 hover:text-emerald-300 flex items-center gap-1"><CheckCircle size={14} /> Tamamla</button>
                    <button onClick={(e) => { e.stopPropagation(); suspendMutation.mutate(group.id); }} className="text-xs text-yellow-400 hover:text-yellow-300 flex items-center gap-1"><PauseCircle size={14} /> Dayandır</button>
                  </>
                )}
                {group.status === 'DAYANDIRILMIS' && (
                  <button onClick={(e) => { e.stopPropagation(); resumeMutation.mutate(group.id); }} className="text-xs text-emerald-400 hover:text-emerald-300 flex items-center gap-1"><PlayCircle size={14} /> Bərpa Et</button>
                )}
                <button onClick={(e) => { e.stopPropagation(); if (confirm('Silmək istəyirsiniz?')) deleteMutation.mutate(group.id); }} className={`p-1 rounded transition-colors ${t.iconBtnDanger}`} title="Sil"><Trash2 size={14} /></button>
              </div>
            </motion.div>
          ))}
        </div>
      )}

      {/* Selected Group Details */}
      {selectedGroup && (
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className={`${t.cardBg} border rounded-xl p-5 transition-colors duration-300`}>
          <div className="flex items-center justify-between mb-4">
            <h2 className={`text-lg font-semibold transition-colors duration-300 ${t.title}`}>Qrup Detalları - {groups.find(g => g.id === selectedGroup)?.groupNumber}</h2>
            <button onClick={() => setShowAddStudent(true)} className="flex items-center gap-2 px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-sm rounded-lg"><Plus size={16} /> Tələbə Əlavə Et</button>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Students */}
            <div>
              <h3 className={`flex items-center gap-2 text-sm font-medium mb-3 transition-colors duration-300 ${t.muted}`}><Users size={16} /> Tələbələr ({activeMembers.length} aktiv / {groupStudents.length} ümumi)</h3>
              <div className="space-y-2">
                {groupStudents.map((gs) => (
                  <div key={gs.id} className={`flex items-center justify-between p-3 rounded-lg transition-colors duration-300 ${t.itemBg}`}>
                    <div>
                      <p className={`text-sm font-medium transition-colors duration-300 ${t.title}`}>{gs.studentFirstName} {gs.studentLastName}</p>
                      <p className={`text-xs font-mono transition-colors duration-300 ${t.muted}`}>{gs.studentFin || 'FİN yoxdur'}</p>
                      {(gs.leftAt || gs.studentArchived || gs.studentStatus !== 'AKTIV') &&
                        <p className="text-xs text-amber-500">
                          {gs.leftAt ? `Ayrılıb: ${gs.leftAt}` : gs.studentArchived ? 'Arxiv' : gs.studentStatus}
                        </p>}
                    </div>
                    <div className="flex items-center gap-2">
                      <p className="text-sm text-emerald-400 font-medium">{gs.monthlyAmount} AZN/ay</p>
                      {!gs.leftAt &&
                        <button onClick={() => removeStudentFromGroup(gs.studentId)}
                          disabled={removeStudentMutation.isPending}
                          className={`p-1 rounded transition-colors ${t.iconBtnDanger}`}
                          title="Qrupdan çıxar"><Trash2 size={12} /></button>}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Teachers */}
            <div>
              <h3 className={`flex items-center gap-2 text-sm font-medium mb-3 transition-colors duration-300 ${t.muted}`}><GraduationCap size={16} /> Müəllimlər</h3>
              <div className="space-y-2">
                {groupTeachers.map((gt) => (
                  <div key={gt.id} className={`flex items-center justify-between p-3 rounded-lg transition-colors duration-300 ${t.itemBg}`}>
                    <div>
                      <p className={`text-sm font-medium transition-colors duration-300 ${t.title}`}>{gt.teacherFirstName} {gt.teacherLastName}</p>
                      <p className={`text-xs transition-colors duration-300 ${t.muted}`}>Effektiv: {gt.effectiveMonth}/{gt.effectiveYear}{gt.effectiveUntilMonth ? ' — '+gt.effectiveUntilMonth+'/'+gt.effectiveUntilYear : ''}</p>
                      <p className="text-xs text-emerald-500">Bu ay hesablanan: {gt.calculatedSalary||0} AZN</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <p className="text-sm text-orange-400 font-medium">%{gt.salaryPercentage}</p>
                      {!gt.effectiveUntilYear&&<button onClick={() => removeTeacherMutation.mutate({ groupId: selectedGroup, teacherId: gt.teacherId })} className={`p-1 rounded transition-colors ${t.iconBtnDanger}`} title="Çıxar"><Trash2 size={12} /></button>}
                    </div>
                  </div>
                ))}
                {/* Assign teacher form */}
                <div className={`p-3 rounded-lg border border-dashed transition-colors duration-300 ${t.border}`}>
                  <div className="grid grid-cols-2 gap-2 mb-2">
                    <select value={teacherForm.teacherId} onChange={(e) => setTeacherForm({ ...teacherForm, teacherId: +e.target.value })} className={`${inputCls} text-xs`}>
                      <option value={0}>Müəllim seç...</option>
                      {teachers.map((te) => <option key={te.id} value={te.id}>{te.firstName} {te.lastName}</option>)}
                    </select>
                    <input type="number" value={teacherForm.salaryPercentage} onChange={(e) => setTeacherForm({ ...teacherForm, salaryPercentage: +e.target.value })} placeholder="Faiz %" className={`${inputCls} text-xs`} min={1} max={100} />
                  </div>
                  <button onClick={() => { if (teacherForm.teacherId) assignTeacherMutation.mutate({ groupId: selectedGroup, data: teacherForm }); }} disabled={!teacherForm.teacherId} className="w-full py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs rounded-lg disabled:opacity-50">Müəllim Təyin Et</button>
                </div>
              </div>
            </div>
          </div>
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
