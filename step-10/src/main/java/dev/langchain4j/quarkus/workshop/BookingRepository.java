package dev.langchain4j.quarkus.workshop;

import dev.langchain4j.agent.tool.Tool;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;

import static dev.langchain4j.quarkus.workshop.Exceptions.BookingCannotBeCancelledException;
import static dev.langchain4j.quarkus.workshop.Exceptions.BookingNotFoundException;
import static dev.langchain4j.quarkus.workshop.Exceptions.CustomerNotFoundException;

@ApplicationScoped
public class BookingRepository implements PanacheRepository<Booking> {


    @Tool("Cancel a booking")
    @Transactional
    public void cancelBooking(long bookingId, String customerFirstName, String customerLastName) {
        Booking booking = getBookingDetails(bookingId, customerFirstName, customerLastName);
        // too late to cancel
        if (booking.dateFrom.minusDays(11).isBefore(LocalDate.now())) {
            throw new BookingCannotBeCancelledException(bookingId);
        }
        // too short to cancel
        if (booking.dateTo.minusDays(4).isBefore(booking.dateFrom)) {
            throw new BookingCannotBeCancelledException(bookingId);
        }
        delete(booking);
    }

    @Tool("List booking for a customer")
    @Transactional
    public List<Booking> listBookingsForCustomer(String customerName, String customerSurname) {
        var found = Customer.find("firstName = ?1 and lastName = ?2", customerName, customerSurname).singleResultOptional();
        if (found.isEmpty()) {
            throw new CustomerNotFoundException(customerName, customerSurname);
        }
        return list("customer", found.get());
    }


    @Tool("Get booking details")
    @Transactional
    public Booking getBookingDetails(long bookingId, String customerFirstName, String customerLastName) {
        Booking found = findById(bookingId);
        if (found == null) {
            throw new BookingNotFoundException(bookingId);
        }
        if (!found.customer.firstName.equals(customerFirstName) || !found.customer.lastName.equals(customerLastName)) {
            throw new BookingNotFoundException(bookingId);
        }
        return found;
    }
}
