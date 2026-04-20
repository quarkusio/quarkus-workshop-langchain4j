package dev.langchain4j.quarkus.workshop;

import java.util.Optional;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Customer extends PanacheEntity {

    String firstName;
    String lastName;

    public static Optional<Customer> findByFirstAndLastName(String firstName, String lastName) {
        return find("LOWER(firstName) = LOWER(?1) and LOWER(lastName) = LOWER(?2)", firstName, lastName).firstResultOptional();
    }
}
