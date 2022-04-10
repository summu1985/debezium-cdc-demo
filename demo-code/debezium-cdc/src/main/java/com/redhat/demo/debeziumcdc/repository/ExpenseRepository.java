package com.redhat.demo.debeziumcdc.repository;

import com.redhat.demo.debeziumcdc.entities.*;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends CrudRepository<Expenses, Long> {}