package com.redhat.demo.debeziumcdc.controller;

import javax.validation.Valid;

import com.redhat.demo.debeziumcdc.entities.Expenses;
import com.redhat.demo.debeziumcdc.repository.ExpenseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ExpensesController {
    
    private final ExpenseRepository expenseRepository;

    @Autowired
    public ExpensesController(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }
    
    @GetMapping("/index")
    public String showExpenseList(Model model) {
        model.addAttribute("expenses", expenseRepository.findAll());
        return "index";
    }
    
    @GetMapping("/add")
    public String showExpenseForm(Model model) {
        model.addAttribute("expense", new Expenses());
        return "add-expense";
    }

    @PostMapping("/addexpense")
    public String addExpense(@Valid Expenses expense, BindingResult result, Model model) {
        
        System.out.println("Inside /addexpense : " + expense + result.toString());
        if (result.hasErrors()) {
            return "add-expense";
        }
    
        System.out.println(expense);
        // expense.setCreatedAt(new Date());
        // expense.setLastUpdated(new Date());
        expenseRepository.save(expense);
        return "redirect:/index";
    }

    @GetMapping("/edit/{id}")
    public String showUpdateForm(@PathVariable("id") long id, Model model) {
        Expenses expense = expenseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid expense Id:" + id));
        model.addAttribute("expense", expense);
        System.out.println("Opened for edit " + expense);
        
        return "edit-expense";
    }

    @PostMapping("/expense/{id}")
    public String updateExpense(@PathVariable("id") long id, @Valid Expenses expense, BindingResult result, Model model) {
        
        if (result.hasErrors()) {
            expense.setId(id);
            return "edit-expense";
        }
        Expenses toUpdateExpense = expenseRepository.findById(id).get();
        //toUpdateExpense.setId(expense.getId());
        toUpdateExpense.setExpenseDescription(expense.getExpenseDescription());
        toUpdateExpense.setExpenseAmount(expense.getExpenseAmount());
        toUpdateExpense.setSpenderName(expense.getSpenderName());
        toUpdateExpense.setExpenseDate(expense.getExpenseDate());
        //toUpdateExpense.setCreatedAt(expense.getCreatedAt());
        // toUpdateExpense.setLastUpdated(new Date());
        System.out.println(toUpdateExpense);

        expenseRepository.save(toUpdateExpense);

        return "redirect:/index";
    }
    // additional CRUD methods

    @GetMapping("/delete/{id}")
    public String deleteExpense(@PathVariable("id") long id, Model model) {
        Expenses expense = expenseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        expenseRepository.delete(expense);
        
        return "redirect:/index";
    }
}