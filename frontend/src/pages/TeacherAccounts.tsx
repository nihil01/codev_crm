import React, {useState} from 'react';
import {useQuery,useMutation,useQueryClient} from '@tanstack/react-query';
import {teachersApi} from '../api/teachers';
import api from '../api/client';
import {useThemeClasses} from '../lib/theme';
import toast from 'react-hot-toast';

type Account={id:number;teacherId:number;email:string;fullName:string;active:boolean};

export const TeacherAccounts:React.FC=()=>{
 const t=useThemeClasses(),cache=useQueryClient();
 const [teacherId,setTeacherId]=useState(0),[password,setPassword]=useState('');
 const {data:teachers=[]}=useQuery({queryKey:['teachers'],queryFn:teachersApi.getAll});
 const {data:accounts=[]}=useQuery<Account[]>({queryKey:['teacher-accounts'],
   queryFn:async()=>(await api.get('/admin/teacher-accounts')).data});
 const create=useMutation({
  mutationFn:async()=> (await api.post('/admin/teacher-accounts',{teacherId,password})).data,
  onSuccess:()=>{cache.invalidateQueries({queryKey:['teacher-accounts']});setPassword('');setTeacherId(0);toast.success('Hesab yaradıldı');},
  onError:(e:any)=>toast.error(e.response?.data?.message||'Hesab yaradılmadı')
 });
 const toggle=useMutation({
  mutationFn:async(account:Account)=>(await api.put(`/admin/teacher-accounts/${account.id}/active`,null,{params:{active:!account.active}})).data,
  onSuccess:()=>{cache.invalidateQueries({queryKey:['teacher-accounts']});toast.success('Status yeniləndi');},
  onError:(e:any)=>toast.error(e.response?.data?.message||'Status dəyişdirilmədi')
 });
 return <section className="space-y-5">
  <h1 className={`text-2xl font-semibold ${t.title}`}>Müəllim hesabları</h1>
  <p className={t.muted}>Hesablar avtomatik yaradılmır. Admin müəllimə fərdi giriş verir və lazım gəldikdə deaktiv edir.</p>
  <form onSubmit={e=>{e.preventDefault();create.mutate();}} className={`p-4 rounded-xl border space-y-3 ${t.cardBg}`}>
   <label className="block">Müəllim<select className="w-full rounded-lg border p-2 bg-transparent" required value={teacherId} onChange={e=>setTeacherId(+e.target.value)}>
    <option value={0}>Seçin</option>{teachers.filter(t=>t.status==='AKTIV').map(te=><option key={te.id} value={te.id}>{te.firstName} {te.lastName} ({te.email})</option>)}
   </select></label>
   <label className="block">Müvəqqəti şifrə (ən az 12 simvol)<input className="w-full rounded-lg border p-2 bg-transparent" type="password" autoComplete="new-password" minLength={12} required value={password} onChange={e=>setPassword(e.target.value)}/></label>
   <button disabled={!teacherId||create.isPending} className="px-4 py-2 bg-indigo-600 text-white rounded-lg disabled:opacity-50">Hesab yarat</button>
  </form>
  <div className={`p-4 rounded-xl border space-y-3 ${t.cardBg}`}>
   {accounts.length===0?'Hesab yoxdur':accounts.map(a=><div key={a.id} className="flex justify-between gap-3 items-center border-b pb-3">
    <div><p className="font-semibold">{a.fullName}</p><p className={t.muted}>{a.email} · {a.active?'Aktiv':'Deaktiv'}</p></div>
    <button className="rounded-lg border px-3 py-2" disabled={toggle.isPending} onClick={()=>toggle.mutate(a)}>{a.active?'Deaktiv et':'Aktiv et'}</button>
   </div>)}
  </div>
 </section>;
};
