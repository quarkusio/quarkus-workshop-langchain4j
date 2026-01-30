CREATE SEQUENCE IF NOT EXISTS car_info_id_seq;

CREATE TABLE IF NOT EXISTS car_info (
    id INT PRIMARY KEY DEFAULT nextval('car_info_id_seq'),
    make VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    year INT NOT NULL,
    condition VARCHAR(255),
    status VARCHAR(20) NOT NULL
);

INSERT INTO car_info (id, make, model, year, condition, status) VALUES
    (nextval('car_info_id_seq'), 'Mercedes-Benz', 'C-Class', EXTRACT(YEAR FROM CURRENT_DATE) - 2, 'Minor dent on passenger door', 'RENTED'),
    (nextval('car_info_id_seq'), 'BMW', 'X5', EXTRACT(YEAR FROM CURRENT_DATE) - 1, 'Recently serviced, excellent condition', 'IN_MAINTENANCE'),
    (nextval('car_info_id_seq'), 'Audi', 'Q4', EXTRACT(YEAR FROM CURRENT_DATE) - 1, 'Brake pads recently replaced', 'RENTED'),
    (nextval('car_info_id_seq'), 'Nissan', 'Altima', EXTRACT(YEAR FROM CURRENT_DATE) - 8, 'Interior needs cleaning', 'AT_CLEANING'),
    (nextval('car_info_id_seq'), 'Ford', 'Focus', EXTRACT(YEAR FROM CURRENT_DATE) - 12, 'High mileage, engine issues', 'RENTED'),
    (nextval('car_info_id_seq'), 'Toyota', 'Corolla', EXTRACT(YEAR FROM CURRENT_DATE) - 3, 'Like new, no issues', 'RENTED'),
    (nextval('car_info_id_seq'), 'Honda', 'Civic', EXTRACT(YEAR FROM CURRENT_DATE) - 4, 'Good condition, minor wear and tear', 'RENTED'),
    (nextval('car_info_id_seq'), 'Ford', 'F-150', EXTRACT(YEAR FROM CURRENT_DATE) - 2, 'Small scratch on rear bumper', 'IN_MAINTENANCE');

