package dev.langchain4j.quarkus.workshop;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;

@Entity
public class Booking extends PanacheEntity {

    @ManyToOne
    Customer customer;
    LocalDate dateFrom;
    LocalDate dateTo;
    String location;
}
