import React from 'react';
import {useQuery} from '@tanstack/react-query';
import api from '../api/client';
import {useThemeClasses} from '../lib/theme';

type Dispatch={id:number;invoiceId:number;kind:string;status:string;
  attempts:number;createdAt?:string;sentAt?:string;lastError?:string};
export const ReminderDispatches:React.FC=()=>{
 const t=useThemeClasses();
 const {data=[],isLoading,error,refetch}=useQuery<Dispatch[]>({
  queryKey:['admin-reminder-dispatches'],
  queryFn:async()=>(await api.get('/admin/reminder-dispatches')).data
 });
 return <section className="space-y-4">
  <div className="flex gap-3 justify-between">
   <h1 className={`text-2xl font-semibold ${t.title}`}>Avtomatik xatırlatma jurnalı</h1>
   <button className="border rounded-lg px-3 py-2" onClick={()=>refetch()}>Yenilə</button>
  </div>
  <p className={t.muted}>WEBHOOK rejimi yalnız provayder konfiqurasiya olunduqda işləyir. MANUAL rejimində avtomatik mesaj göndərilmir.</p>
  <div className={`overflow-x-auto rounded-xl border ${t.cardBg}`}>
   <table className="w-full text-sm text-left">
    <thead><tr>{['ID','Hesab','Status','Cəhd','Yaradılıb','Göndərilib','Xəta'].map(x=><th className="p-3" key={x}>{x}</th>)}</tr></thead>
    <tbody>{isLoading?<tr><td colSpan={7} className="p-3">Yüklənir...</td></tr>:error?
    <tr><td colSpan={7} className="p-3">Jurnal yüklənmədi</td></tr>:
    data.map(d=><tr key={d.id} className="border-t border-slate-700/20">
      <td className="p-3">{d.id}</td><td className="p-3">{d.invoiceId}</td><td className="p-3">{d.status}</td>
      <td className="p-3">{d.attempts}</td>
      <td className="p-3">{d.createdAt?.replace('T',' ').slice(0,16)||'—'}</td>
      <td className="p-3">{d.sentAt?.replace('T',' ').slice(0,16)||'—'}</td>
      <td className="p-3">{d.lastError||'—'}</td>
    </tr>)}</tbody>
   </table>
  </div>
 </section>;
};
