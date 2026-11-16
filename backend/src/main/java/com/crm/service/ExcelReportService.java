package com.crm.service;

import com.crm.dto.ReportDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Service @RequiredArgsConstructor
public class ExcelReportService {
    private final ReportService reports;
    private final BusinessAnalyticsService analytics;

    public byte[] export(int year,int month){
        ReportDto r=reports.getMonthlyReport(year,month);
        Map<String,Object> data=analytics.summary(year,month);
        try(XSSFWorkbook book=new XSSFWorkbook();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            Sheet summary=book.createSheet("İcmal");
            line(summary,0,"Göstərici","Məbləğ");
            line(summary,1,"İl",year);
            line(summary,2,"Ay",month);
            line(summary,3,"Ay üzrə hesablanıb (net)",r.getTotalExpected());
            line(summary,4,"Həmin hesablar üzrə ödənilib (bütün tarixlər)",r.getPaidAgainstExpected());
            line(summary,5,"Həmin hesabların qalıq borcu",r.getOutstandingForPeriod());
            line(summary,6,"Həmin hesablar üzrə ödəniş faizi (%)",r.getCollectionRate());
            line(summary,7,"Bu ay kassaya faktiki daxil olub",r.getTotalCollected());
            line(summary,8,"Bütün aylardan gecikmiş hesab sayı",r.getTotalOverdueCount());
            line(summary,9,"Bütün aylardan gecikmiş borc",r.getTotalOverdueDebt());
            line(summary,10,"Müəllim payları",r.getTotalTeacherSalaries());
            line(summary,11,"Daimi xərclər",r.getTotalFixedExpenses());
            line(summary,12,"Birdəfəlik xərclər",r.getTotalUtilityExpenses());
            line(summary,13,"Cəmi xərclər",r.getTotalExpenses());
            line(summary,14,"Real mədaxil - xərclər (təxmini mənfəət)",r.getNetProfit());
            summary.autoSizeColumn(0);summary.autoSizeColumn(1);
            section(book,data,"debtors","Borclular",new String[]{"student","group","period","dueDate","debt","status"});
            section(book,data,"groups","Qruplar",new String[]{"group","collected","teacherShare","afterTeacherShare"});
            section(book,data,"sources","Lead Mənbələri",new String[]{"source","leads","won","registered","advertising","cac"});
            section(book,data,"managers","Menecerlər",new String[]{"manager","leads","registered","conversionPercent"});
            section(book,data,"teachers","Müəllim payları",new String[]{"teacherId","teacher","group","earned"});
            book.write(out);
            return out.toByteArray();
        }catch(Exception ex){throw new IllegalStateException("Excel hesabatı yaradılmadı",ex);}
    }
    private void section(Workbook wb,Map<String,Object> data,String key,String title,String[] columns){
        Sheet sheet=wb.createSheet(title);
        line(sheet,0,(Object[])columns);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rows=(List<Map<String,Object>>)data.getOrDefault(key,List.of());
        int i=1;
        for(Map<String,Object> row:rows){
            Object[] values=Arrays.stream(columns).map(k->row.get(k)).toArray();
            line(sheet,i++,values);
        }
        for(int c=0;c<columns.length;c++)sheet.autoSizeColumn(c);
    }
    private void line(Sheet s,int index,Object... cells){
        Row row=s.createRow(index);
        for(int i=0;i<cells.length;i++){
            Cell c=row.createCell(i);
            Object v=cells[i];
            if(v instanceof Number number)c.setCellValue(number.doubleValue());
            else c.setCellValue(v==null?"":v.toString());
        }
    }
}
