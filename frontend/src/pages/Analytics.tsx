import React, {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import api from '../api/client';
import {useThemeClasses} from '../lib/theme';

type Row=Record<string,string|number|null>;
type Data={sources:Row[];managers:Row[];debtors:Row[];groups:Row[];teachers:Row[]};
const sections=[
 {key:'sources',name:'Lead mənbələri və tələbə əldə etmə',cols:['source','leads','won','registered','advertising','cac']},
 {key:'managers',name:'Menecer nəticələri',cols:['manager','leads','registered','conversionPercent']},
 {key:'debtors',name:'Borclular',cols:['student','group','period','dueDate','debt','status']},
 {key:'groups',name:'Qrup qazancı (müəllim payından sonra)',cols:['group','collected','teacherShare','afterTeacherShare']},
 {key:'teachers',name:'Müəllimlərə hesablanmış paylar',cols:['teacher','group','earned']}
] as const;
export const Analytics:React.FC=()=>{
 const t=useThemeClasses();
 const now=new Date(),[year,setYear]=useState(now.getFullYear()),[month,setMonth]=useState(now.getMonth()+1);
 const {data,isLoading}=useQuery<Data>({queryKey:['analytics',year,month],queryFn:async()=>(await api.get(`/reports/analytics/year/${year}/month/${month}`)).data});
 return <section className="space-y-4">
  <h1 className={`text-2xl font-bold ${t.title}`}>CRM Analitikası</h1>
  <div className="flex gap-3"><input aria-label="İl" type="number" className="rounded-lg border p-2 bg-transparent" value={year} onChange={e=>setYear(+e.target.value)}/><select className="rounded-lg border p-2 bg-transparent" value={month} onChange={e=>setMonth(+e.target.value)}>{Array.from({length:12},(_,i)=><option value={i+1} key={i}>{i+1}</option>)}</select></div>
  {isLoading?<p>Yüklənir...</p>:sections.map(sec=><div key={sec.key} className={`rounded-xl border p-4 overflow-x-auto ${t.cardBg}`}>
    <h2 className={`font-semibold mb-3 ${t.title}`}>{sec.name}</h2><table className="w-full text-sm text-left">
    <thead><tr>{sec.cols.map(c=><th className="p-2" key={c}>{c}</th>)}</tr></thead>
    <tbody>{(data?.[sec.key]||[]).map((r,i)=><tr className="border-t border-slate-700/20" key={i}>{sec.cols.map(c=><td className="p-2" key={c}>{r[c]??'—'}</td>)}</tr>)}</tbody>
    </table></div>)}
 </section>;
};
