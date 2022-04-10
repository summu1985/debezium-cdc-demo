package com.redhat.demo.debeziumcdc.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "expenses")
public class Expenses {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotBlank(message="spender name is mandatory")
    private String spenderName;

    @NotBlank(message="expense description is mandatory")
    private String expenseDescription;

    @NotEmpty(message = "Expense amount cannot be empty")
    private double expenseAmount;

    @NotBlank(message = "Expense date cannot be blank")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date expenseDate;

    public Expenses() {}

    public Expenses(String spenderName, String expenseDescription, double expenseAmount, Date expenseDate) {
      this.spenderName = spenderName;
      this.expenseDescription = expenseDescription;
      this.expenseAmount = expenseAmount;
      this.expenseDate = expenseDate;
    }

    // Getters
    public String getSpenderName() {
      return this.spenderName;
    }

    public long getId() {
      return this.id;
    }

    public String getExpenseDescription() {
      return this.expenseDescription;
    }

    public double getExpenseAmount() {
      return this.expenseAmount;
    }

    public Date getExpenseDate() {
      return this.expenseDate;
    }

    // Setters

    public void setId(long id) {
      this.id = id;
    }

    public void setSpenderName(String spenderName) {
      this.spenderName = spenderName;
    }

    public void setExpenseDescription(String expenseDescription){
      this.expenseDescription = expenseDescription;
    }

    public void setExpenseAmount (double expenseAmount) {
      this.expenseAmount = expenseAmount;
    }

    public void setExpenseDate( Date expenseDate) {
      this.expenseDate = expenseDate;
    }

    @Override
    public String toString() {
        return "Expense{" + "id=" + id + ", Spender name=" + spenderName + ", Expense Amount =" + expenseAmount 
        + "Expense Description = " + expenseDescription + "Expense Date = " + expenseDate + '}';
    }
}
