import React, { useState } from 'react';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {studentsApi} from '../api/students';
import api from '../api/client';
import {useThemeClasses} from '../lib/theme';
import toast from 'react-hot-toast';

type FrozenMonth={id:number;year:number;month:number;createdBy:string};
export const StudentFreezes:React.FC=()=>{
  const t=useThemeClasses();
  const now=new Date();
  const [studentId,setStudentId]=useState(0),[year,setYear]=useState(now.getFullYear()),[month,setMonth]=useState(now.getMonth()+1);
  const cache=useQueryClient();
  const {data:students=[]}=useQuery({queryKey:['students'],queryFn:studentsApi.getAll});
  const {data:periods=[]}=useQuery<FrozenMonth[]>({queryKey:['freezes',studentId],enabled:studentId!==0,
    queryFn:async()=>(await api.get(`/students/${studentId}/freezes`)).data});
  const change=useMutation({
    mutationFn:async (freeze:boolean)=>{
      const url=`/students/${studentId}/freezes`;
      const params={year,month};
      if(freeze) await api.post(url,null,{params});else await api.delete(url,{params});
    },
    onSuccess:()=>{cache.invalidateQueries({queryKey:['freezes',studentId]});cache.invalidateQueries({queryKey:['payments']});toast.success('Yadda saxlanıldı');},
    onError:(e:any)=>toast.error(e.response?.data?.message||'Əməliyyat uğursuz oldu')
  });
  const found=periods.some(p=>p.year===year&&p.month===month);
  return <section className="space-y-5">
    <h1 className={`text-2xl font-bold ${t.title}`}>Dondurulmuş aylar</h1>
    <p className={t.muted}>Seçilmiş ayda tələbənin bütün qruplar üzrə ödənişləri dondurulur. Əvvəl ödəniş alınmış ay dondurulmur.</p>
    <div className={`rounded-xl border p-4 space-y-4 ${t.cardBg}`}>
      <label className="block">Tələbə<select className="w-full rounded-lg border p-2 bg-transparent" value={studentId} onChange={e=>setStudentId(+e.target.value)}>
        <option value={0}>Seçin</option>{students.map(s=><option value={s.id} key={s.id}>{s.firstName} {s.lastName}</option>)}
      </select></label>
      <div className="grid grid-cols-2 gap-3">
        <label>İl<input type="number" value={year} onChange={e=>setYear(+e.target.value)} className="w-full rounded-lg border p-2 bg-transparent"/></label>
        <label>Ay<select value={month} onChange={e=>setMonth(+e.target.value)} className="w-full rounded-lg border p-2 bg-transparent">
          {Array.from({length:12},(_,i)=><option key={i} value={i+1}>{i+1}</option>)}</select></label>
      </div>
      <button disabled={!studentId||change.isPending} className="bg-indigo-600 text-white px-4 py-2 rounded-lg disabled:opacity-50" onClick={()=>change.mutate(!found)}>
        {found?'Dondurmanı ləğv et':'Bu ayı dondur'}
      </button>
      {studentId!==0&&<div className="space-y-2">
        <h2 className="font-semibold">Dondurulmuş tarixlər</h2>
        {periods.length===0?'Qeyd yoxdur':periods.map(p=><p key={p.id} className={t.muted}>{p.month}/{p.year} · {p.createdBy}</p>)}
      </div>}
    </div>
  </section>;
};
