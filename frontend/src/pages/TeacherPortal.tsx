import React, {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import api from '../api/client';
import {useThemeClasses} from '../lib/theme';

type MyGroup={
 groupId:number;groupNumber:string;courseName:string|null;
 status:string;lessonDays:string|null;lessonTime:string|null;
 trainingMode:string|null;room:string|null;sharePercent:number;
 earnedForMonth:number;effectiveMonth:number;effectiveYear:number;
 untilMonth:number|null;untilYear:number|null;
};
type Overview={year:number;month:number;totalEarned:number;groups:MyGroup[]};

export const TeacherPortal:React.FC=()=>{
 const t=useThemeClasses(),today=new Date();
 const [year,setYear]=useState(today.getFullYear());
 const [month,setMonth]=useState(today.getMonth()+1);
 const {data,isLoading,error}=useQuery<Overview>({
  queryKey:['teacher-portal',year,month],
  queryFn:async()=>(await api.get('/teacher-portal',{params:{year,month}})).data
 });
 return <section className="space-y-5">
  <div className="flex flex-wrap items-center justify-between gap-3">
   <h1 className={`text-2xl font-semibold ${t.title}`}>Mənim qruplarım və payım</h1>
   <div className="flex gap-2">
    <input aria-label="İl" type="number" min={2020} max={2100} className="w-24 rounded-lg border p-2 bg-transparent" value={year} onChange={e=>setYear(+e.target.value)}/>
    <select aria-label="Ay" className="rounded-lg border p-2 bg-transparent" value={month} onChange={e=>setMonth(+e.target.value)}>
     {Array.from({length:12},(_,i)=><option value={i+1} key={i}>{i+1}</option>)}
    </select>
   </div>
  </div>
  {isLoading?<p>Yüklənir...</p>:error?<p>Məlumatları yükləmək mümkün olmadı.</p>:
   <div className={`p-5 rounded-xl border ${t.cardBg}`}>
    <p className={t.muted}>Seçilmiş ay üzrə hesablanmış pay</p>
    <p className="text-2xl font-bold">{(data?.totalEarned||0).toLocaleString('az-AZ')} AZN</p>
   </div>}
  <p className={t.muted}>Yalnız sizə təyin edilmiş qruplar və faktiki mədaxillərdən hesablanmış paylar göstərilir.</p>
  <div className="grid gap-4 md:grid-cols-2">
   {data?.groups.map(g=><div key={g.groupId+'-'+g.effectiveYear+'-'+g.effectiveMonth} className={`p-4 border rounded-xl space-y-2 ${t.cardBg}`}>
    <div className={`font-semibold ${t.title}`}>{g.groupNumber} · {g.courseName||'Kurs'}</div>
    <p className={t.muted}>Status: {g.status}</p>
    <p className={t.muted}>{g.lessonDays||'—'} {g.lessonTime||''} · {g.trainingMode||'—'} · {g.room||'—'}</p>
    <p>Pay faizi: {g.sharePercent}%</p>
    <p className="font-semibold">Bu ay hesablanıb: {Number(g.earnedForMonth).toLocaleString('az-AZ')} AZN</p>
    <p className={t.muted}>Təyinat: {g.effectiveMonth}/{g.effectiveYear} — {g.untilMonth&&g.untilYear?`${g.untilMonth}/${g.untilYear}`:'davam edir'}</p>
   </div>)}
  </div>
 </section>;
};
