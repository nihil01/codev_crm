package com.crm.service;

import com.crm.dto.PaymentDto;
import com.crm.entity.*;
import com.crm.enums.ExpenseType;
import com.crm.enums.GroupStatus;
import com.crm.enums.LeadStage;
import com.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BusinessAnalyticsService {
    private final LeadRepository leads;
    private final StudentRepository students;
    private final ExpenseRepository expenses;
    private final CourseGroupRepository groups;
    private final PaymentService payments;
    private final CourseGroupService groupService;
    private final TeacherShareAllocationRepository allocations;

    public Map<String,Object> summary(int year,int month) {
        YearMonth period=YearMonth.of(year,month);
        List<Lead> allLeads=leads.findAll().stream()
            .filter(l->l.getCreatedAt()!=null && YearMonth.from(l.getCreatedAt()).equals(period))
            .toList();
        List<Expense> monthlyExpenses=expenses.findByExpenseYearAndExpenseMonth(year,month)
            .stream().filter(e->!e.isCancelled()).toList();
        List<Student> enrolled=students.findAll().stream()
            .filter(s->s.getRegistrationDate()!=null && YearMonth.from(s.getRegistrationDate()).equals(period)).toList();

        List<Map<String,Object>> sources=new ArrayList<>();
        for(var source:com.crm.enums.LeadSource.values()){
            long total=allLeads.stream().filter(l->l.getSource()==source).count();
            long won=allLeads.stream().filter(l->l.getSource()==source&&l.getStage()==LeadStage.QEYDIYYAT).count();
            long registered=enrolled.stream().filter(s->s.getSource()==source).count();
            BigDecimal spent=monthlyExpenses.stream()
                .filter(e->e.getType()==ExpenseType.REKLAM && e.getAdChannel()!=null &&
                   e.getAdChannel().equalsIgnoreCase(source.name()))
                .map(Expense::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal cac=registered==0?BigDecimal.ZERO:spent.divide(BigDecimal.valueOf(registered),2,RoundingMode.HALF_UP);
            sources.add(row("source",source.name(),"leads",total,"won",won,
                "registered",registered,"advertising",spent,"cac",cac));
        }

        Map<String,long[]> managerCounts=new LinkedHashMap<>();
        for(Lead l:allLeads){
            String name=l.getAssignedManager()==null?"Təyin edilməyib":l.getAssignedManager().getFullName();
            long[] counters=managerCounts.computeIfAbsent(name,k->new long[2]);
            counters[0]++;
            if(l.getStage()==LeadStage.QEYDIYYAT)counters[1]++;
        }
        List<Map<String,Object>> managers=new ArrayList<>();
        managerCounts.forEach((name,n)->managers.add(row(
          "manager",name,"leads",n[0],"registered",n[1],"conversionPercent",
          n[0]==0?BigDecimal.ZERO:BigDecimal.valueOf(n[1]*100).divide(BigDecimal.valueOf(n[0]),2,RoundingMode.HALF_UP))));

        List<Map<String,Object>> debtors=payments.getAll().stream()
            .filter(p->p.getRemainingAmount()!=null && p.getRemainingAmount().signum()>0 &&
                !p.isFrozen() && p.getStatus()!=com.crm.enums.PaymentStatus.BAGISLANIB)
            .map(p->row("student",p.getStudentFirstName()+" "+p.getStudentLastName(),
                "group",p.getGroupNumber(),"period",p.getPaymentMonth()+"/"+p.getPaymentYear(),
                "dueDate",p.getDueDate(),"debt",p.getRemainingAmount(),"status",p.getStatus().name())).toList();

        List<Map<String,Object>> groupProfits=new ArrayList<>();
        // Cash history remains visible after group completion or suspension.
        for(CourseGroup group:groups.findAll()){
            BigDecimal collected=payments.getCollectedForGroupMonth(group.getId(),year,month);
            BigDecimal teacher=groupService.calculateTeacherSalary(group.getId(),year,month);
            if(collected.signum()>0 || teacher.signum()>0 ||
               group.getStatus()==GroupStatus.AKTIV || group.getStatus()==GroupStatus.BASLAYIB)
                groupProfits.add(row("group",group.getGroupNumber(),"collected",collected,
                    "teacherShare",teacher,"afterTeacherShare",collected.subtract(teacher)));
        }

        List<Map<String,Object>> teacherShares=new ArrayList<>();
        for(Object[] data:allocations.totalsByTeacherForPeriod(
            period.atDay(1).atStartOfDay(),period.plusMonths(1).atDay(1).atStartOfDay())) {
            teacherShares.add(row("teacherId",data[0],"teacher",data[1]+" "+data[2],
                "group",data[3],"earned",data[4]));
        }
        return row("sources",sources,"managers",managers,"debtors",debtors,
            "groups",groupProfits,"teachers",teacherShares);
    }

    private Map<String,Object> row(Object...pairs) {
        Map<String,Object> r=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2)r.put((String)pairs[i],pairs[i+1]);
        return r;
    }
}
