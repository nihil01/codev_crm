import React from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/client';
import { useThemeClasses } from '../lib/theme';

interface AuditEvent {
  id:number; actorEmail:string; httpMethod:string; path:string;
  statusCode:number; createdAt:string;
}
export const Audit:React.FC=()=>{
  const t=useThemeClasses();
  const {data=[],isLoading}=useQuery<AuditEvent[]>({
    queryKey:['audit'],queryFn:async()=>(await api.get('/audit')).data
  });
  return <section className="space-y-4">
    <h1 className={`text-2xl font-bold ${t.title}`}>Dəyişiklik tarixçəsi</h1>
    <p className={t.muted}>Bu mərhələdə əməliyyat və vaxt qeyd edilir. Əvvəlki və yeni sahə dəyərləri növbəti mərhələdə əlavə olunacaq.</p>
    <div className={`rounded-xl border overflow-x-auto ${t.cardBg}`}>
      <table className="w-full text-sm">
        <thead><tr className={t.thClass}><th className="p-3 text-left">Vaxt</th><th className="p-3 text-left">İstifadəçi</th><th className="p-3 text-left">Əməliyyat</th><th className="p-3 text-left">Resurs</th></tr></thead>
        <tbody>{isLoading?<tr><td colSpan={4} className="p-4">Yüklənir...</td></tr>:data.map(e=><tr key={e.id} className="border-t border-slate-700/20">
          <td className="p-3">{new Date(e.createdAt).toLocaleString('az-AZ')}</td>
          <td className="p-3">{e.actorEmail}</td>
          <td className="p-3">{e.httpMethod}</td>
          <td className="p-3">{e.path}</td>
        </tr>)}</tbody>
      </table>
    </div>
  </section>;
};
