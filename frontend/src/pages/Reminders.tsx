import React from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/client';
import { useThemeClasses } from '../lib/theme';
type Reminder={paymentId:number;student:string;phone?:string;dueDate:string;remaining:number;message:string};
export const Reminders:React.FC=()=>{
  const t=useThemeClasses();
  const {data=[],isLoading}=useQuery<Reminder[]>({queryKey:['reminders'],queryFn:async()=>(await api.get('/payments/reminders')).data});
  return <section className="space-y-4">
    <h1 className={`text-2xl font-bold ${t.title}`}>Ödəniş xatırlatmaları</h1>
    <p className={t.muted}>Mesajlar avtomatik göndərilmir. Siyahıdan mətnləri köçürüb əlaqə saxlaya bilərsiniz.</p>
    {isLoading?<p>Yüklənir...</p>:data.length===0?<p>Bu gün üçün xatırlatma yoxdur</p>:
    data.map(r=><div key={r.paymentId} className={`rounded-xl border p-4 ${t.cardBg}`}>
      <div className="font-semibold">{r.student} · {r.remaining} AZN</div>
      <div className={t.muted}>Son tarix: {r.dueDate} · Telefon: {r.phone||'—'}</div>
      <p className="my-3">{r.message}</p>
      <button className="rounded-lg bg-indigo-600 px-3 py-2 text-white" onClick={()=>navigator.clipboard.writeText(r.message)}>Mətni köçür</button>
    </div>)}
  </section>;
};
