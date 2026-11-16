import React,{useState} from 'react';
import {useMutation,useQuery,useQueryClient} from '@tanstack/react-query';
import toast from 'react-hot-toast';
import {ExpenseType} from '../types';
import {expensesApi,RecurringRule} from '../api/expenses';
import {useThemeClasses} from '../lib/theme';

const names:Record<string,string>={YER:'İcarə',ISCHI_MAASI:'İşçi maaşı',REKLAM:'Reklam',
  KOMMUNAL:'Kommunal',AVADANLIQ:'Avadanlıq',DIGER:'Digər',
  INTERNET:'İnternet',ELEKTRIK:'Elektrik',SU:'Su',QAZ:'Qaz'};
const channels=['INSTAGRAM','WHATSAPP','TIKTOK','SAYT','ZENG','TOVSIYE','BIRBASA_GELIS'];
const today=new Date();
const defaults:RecurringRule={
  id:0,type:ExpenseType.YER,amount:0,description:'',adChannel:'',
  startYear:today.getFullYear(),startMonth:today.getMonth()+1,active:true
};
export const RecurringRules:React.FC=()=>{
  const t=useThemeClasses(),cache=useQueryClient();
  const [editing,setEditing]=useState<RecurringRule|null>(null);
  const [form,setForm]=useState<RecurringRule>({...defaults});
  const [show,setShow]=useState(false);
  const {data=[],isLoading}=useQuery({queryKey:['expense-rules'],queryFn:expensesApi.getRules});
  const save=useMutation({
    mutationFn:()=>editing?expensesApi.updateRule(form):expensesApi.createRule(form),
    onSuccess:()=>{
      cache.invalidateQueries({queryKey:['expense-rules']});
      cache.invalidateQueries({queryKey:['exp']});
      setShow(false);toast.success('Daimi xərc qaydası yadda saxlanıldı');
    },onError:(e:any)=>toast.error(e.response?.data?.message||'Qayda saxlanılmadı')
  });
  const active=useMutation({
    mutationFn:({id,enabled}:{id:number;enabled:boolean})=>expensesApi.activateRule(id,enabled),
    onSuccess:()=>{cache.invalidateQueries({queryKey:['expense-rules']});cache.invalidateQueries({queryKey:['exp']});},
    onError:(e:any)=>toast.error(e.response?.data?.message||'Status dəyişdirilmədi')
  });
  const open=(r?:RecurringRule)=>{setEditing(r||null);setForm(r?{...r}:{...defaults});setShow(true);};
  const input='w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900';
  return <section className="space-y-5">
    <div className="flex items-center justify-between gap-3">
      <h1 className={`text-2xl font-bold ${t.title}`}>Daimi xərc qaydaları</h1>
      <button className="bg-indigo-600 text-white rounded-lg px-4 py-2" onClick={()=>open()}>Yeni qayda</button>
    </div>
    <p className={t.muted}>Köhnə sistemdən köçürülən daimi xərclər təhlükəsizlik üçün əvvəlcə deaktiv edilir; düzgünlüyünü yoxlayıb aktivləşdirin.</p>
    <p className={t.muted}>Hər qaydanın ayrıca ID-si var. Aylıq xərc avtomatik bir dəfə yaranır;
      qaydanı dəyişmək artıq yaranmış ayların məbləğini dəyişmir. Qaydanı söndürmək keçmiş xərcləri silmir.</p>
    {isLoading?<p>Yüklənir...</p>:<div className="grid gap-3 md:grid-cols-2">
      {data.map(r=><div key={r.id} className={`rounded-xl border p-4 space-y-2 ${t.cardBg}`}>
        <div className="flex justify-between"><strong>{names[r.type]||r.type}</strong><span className={r.active?'text-emerald-500':'text-slate-500'}>{r.active?'Aktiv':'Dayandırılıb'}</span></div>
        <p className="font-bold">{r.amount} AZN / ay</p>
        <p className={t.muted}>{r.description||'—'} {r.adChannel?'· '+r.adChannel:''}</p>
        <p className={`text-xs ${t.muted}`}>Başlanğıc: {r.startMonth}/{r.startYear} · Qayda #{r.id}</p>
        <div className="flex gap-3"><button className="text-indigo-500" onClick={()=>open(r)}>Redaktə</button>
          <button className="text-amber-500" disabled={active.isPending} onClick={()=>active.mutate({id:r.id,enabled:!r.active})}>
          {r.active?'Dayandır':'Aktivləşdir'}</button></div>
      </div>)}
    </div>}
    {show&&<div role="dialog" aria-modal="true" className="fixed inset-0 z-50 bg-black/60 grid place-items-center p-4">
      <form className="w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-xl bg-white p-6 text-slate-900 space-y-3"
       onSubmit={e=>{e.preventDefault();save.mutate();}}>
        <h2 className="text-xl font-bold">{editing?'Daimi xərci redaktə et':'Yeni daimi xərc'}</h2>
        <label className="block">Növ<select className={input} value={form.type} onChange={e=>setForm({...form,type:e.target.value as ExpenseType})}>
          {Object.entries(names).map(([key,name])=><option key={key} value={key}>{name}</option>)}</select></label>
        <label className="block">Məbləğ (AZN)<input className={input} type="number" step="0.01" min="0.01" required value={form.amount||''} onChange={e=>setForm({...form,amount:Number(e.target.value)})}/></label>
        <label className="block">Təsvir<input className={input} value={form.description||''} onChange={e=>setForm({...form,description:e.target.value})}/></label>
        {form.type===ExpenseType.REKLAM&&<label className="block">Reklam kanalı<select className={input} value={form.adChannel||''} required onChange={e=>setForm({...form,adChannel:e.target.value})}>
          <option value="">Seçin</option>{channels.map(c=><option key={c} value={c}>{c}</option>)}</select></label>}
        <div className="grid grid-cols-2 gap-3">
          <label>Başlanğıc il<input type="number" min={2020} disabled={!!editing} className={input} value={form.startYear} onChange={e=>setForm({...form,startYear:Number(e.target.value)})}/></label>
          <label>Başlanğıc ay<select className={input} disabled={!!editing} value={form.startMonth} onChange={e=>setForm({...form,startMonth:Number(e.target.value)})}>
            {Array.from({length:12},(_,i)=><option key={i} value={i+1}>{i+1}</option>)}</select></label>
        </div>
        <label className="flex gap-2 items-center"><input type="checkbox" checked={form.active} onChange={e=>setForm({...form,active:e.target.checked})}/>Aktiv</label>
        <div className="flex gap-3 pt-3">
          <button type="button" className="flex-1 border rounded-lg p-2" onClick={()=>setShow(false)}>Ləğv</button>
          <button type="submit" className="flex-1 bg-indigo-600 text-white rounded-lg p-2" disabled={save.isPending}>Yadda saxla</button>
        </div>
      </form>
    </div>}
  </section>;
};
