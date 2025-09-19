package dev.langchain4j.quarkus.workshop;

import java.util.Optional;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Customer extends PanacheEntity {

    String firstName;
    String lastName;

    public static Optional<Customer> findByFirstAndLastName(String firstName, String lastName) {
        return find("upper(firstName) = ?1 and upper(lastName) = ?2", firstName.toUpperCase(), lastName.toUpperCase()).firstResultOptional();
    }
}
