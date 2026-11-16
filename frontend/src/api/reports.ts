import api from './client';
import { ReportData } from '../types';

export const reportsApi = {
  exportExcel: async(year:number,month:number):Promise<void>=>{
    const response=await api.get(`/reports/export/year/${year}/month/${month}`,{responseType:'blob'});
    const url=URL.createObjectURL(new Blob([response.data],{type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}));
    const a=document.createElement('a');a.href=url;a.download=`codev-hesabat-${year}-${month}.xlsx`;a.click();
    setTimeout(()=>URL.revokeObjectURL(url),1000);
  },
  getMonthly: async (year: number, month: number): Promise<ReportData> => {
    const { data } = await api.get(`/reports/monthly/year/${year}/month/${month}`);
    return data;
  },
};
