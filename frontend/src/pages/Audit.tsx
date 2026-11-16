import React, {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import api from '../api/client';
import {useThemeClasses} from '../lib/theme';

interface AuditEvent {
  id:number; actorEmail:string; httpMethod:string; path:string; statusCode:number;
  createdAt:string; entityType?:string; entityId?:number; action?:string;
  beforeJson?:string; afterJson?:string;
}
function readJson(raw?:string):Record<string,unknown> {
  if(!raw)return {};
  try {const parsed:unknown=JSON.parse(raw);
    return parsed && typeof parsed==='object' && !Array.isArray(parsed) ?
      parsed as Record<string,unknown> : {};
  } catch {return {};}
}
function renderValue(v:unknown):string {
  if(v===null||v===undefined)return '—';
  if(typeof v==='object')return JSON.stringify(v);
  return String(v);
}
export const Audit:React.FC=()=>{
  const t=useThemeClasses();
  const [showHttp,setShowHttp]=useState(false);
  const {data=[],isLoading}=useQuery<AuditEvent[]>({
    queryKey:['audit'],queryFn:async()=>(await api.get('/audit')).data
  });
  const items=data.filter(event=>showHttp||event.action!=='HTTP_REQUEST');
  return <section className="space-y-5">
    <div className="flex flex-wrap justify-between gap-3 items-center">
      <h1 className={`text-2xl font-bold ${t.title}`}>Dəyişiklik tarixçəsi</h1>
      <label className="text-sm flex gap-2 items-center">
        <input type="checkbox" checked={showHttp} onChange={e=>setShowHttp(e.target.checked)}/>
        Texniki API əməliyyatlarını göstər
      </label>
    </div>
    <p className={t.muted}>Yalnız administrator görür. Şifrə və giriş tokenləri tarixçəyə yazılmır.</p>
    {isLoading?<p>Yüklənir...</p>:items.length===0?<p>Dəyişiklik tapılmadı</p>:
     items.map(event=>{
       const before=readJson(event.beforeJson),after=readJson(event.afterJson);
       const keys=Object.keys({...before,...after});
       return <article key={event.id} className={`rounded-xl border p-4 space-y-2 ${t.cardBg}`}>
         <div className="flex flex-wrap gap-3 justify-between items-start">
           <div><span className={`font-semibold ${t.title}`}>{event.action||event.httpMethod}</span>
           <span className={t.muted}> · {event.entityType||event.path}{event.entityId?' #'+event.entityId:''}</span></div>
           <span className={`text-sm ${t.muted}`}>{new Date(event.createdAt).toLocaleString('az-AZ')}</span>
         </div>
         <p className={`text-sm ${t.muted}`}>{event.actorEmail}</p>
         {keys.length>0&&<div className="overflow-x-auto">
           <table className="w-full text-sm text-left"><thead><tr><th className="p-2">Sahə</th><th className="p-2">Əvvəl</th><th className="p-2">Sonra</th></tr></thead>
           <tbody>{keys.map(key=><tr key={key} className="border-t border-slate-700/20">
             <td className="p-2 font-medium">{key}</td>
             <td className="p-2 break-all">{renderValue(before[key])}</td>
             <td className="p-2 break-all">{renderValue(after[key])}</td>
           </tr>)}</tbody></table>
         </div>}
       </article>;
     })}
  </section>;
};
