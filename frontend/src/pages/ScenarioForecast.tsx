import React, {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import api from '../api/client';
import {useThemeClasses} from '../lib/theme';

type Point={year:number;month:number;income:number;expenses:number;profit:number;students:number};
type Result={assumptions:string;basedOnMonths:number;baselineIncome:number;baselineExpenses:number;baselineStudents:number;scenarios:{pessimist:Point[];baza:Point[];optimist:Point[]}};
const names={pessimist:'Pessimist',baza:'Baza',optimist:'Optimist'};
export const ScenarioForecast:React.FC=()=>{
 const t=useThemeClasses(),[months,setMonths]=useState(6);
 const {data,isLoading}=useQuery<Result>({queryKey:['scenario-forecast',months],queryFn:async()=>(await api.get('/forecast/scenarios',{params:{months}})).data});
 return <section className="space-y-5">
   <div className="flex justify-between gap-3 items-center"><h1 className={`text-2xl font-bold ${t.title}`}>Ssenari proqnozları</h1>
   <select className="rounded-lg border p-2 bg-transparent" value={months} onChange={e=>setMonths(+e.target.value)}><option value={3}>3 ay</option><option value={6}>6 ay</option></select></div>
   <p className={t.muted}>{data?.assumptions||'Yüklənir...'} </p>
   {isLoading?<p>Yüklənir...</p>:data&&Object.entries(data.scenarios).map(([key,values])=>
   <div key={key} className={`overflow-x-auto rounded-xl border p-4 ${t.cardBg}`}>
     <h2 className={`text-lg font-bold mb-3 ${t.title}`}>{names[key as keyof typeof names]}</h2>
     <table className="w-full text-sm text-left"><thead><tr><th className="p-2">Ay</th><th className="p-2">Tələbə sayı</th><th className="p-2">Gəlir</th><th className="p-2">Xərc</th><th className="p-2">Mənfəət</th></tr></thead>
     <tbody>{values.map(v=><tr key={v.year+'-'+v.month} className="border-t border-slate-700/20">
     <td className="p-2">{v.month}/{v.year}</td><td className="p-2">{v.students}</td><td className="p-2">{v.income.toLocaleString()} AZN</td><td className="p-2">{v.expenses.toLocaleString()} AZN</td><td className="p-2">{v.profit.toLocaleString()} AZN</td>
     </tr>)}</tbody></table>
   </div>)}
 </section>;
};
