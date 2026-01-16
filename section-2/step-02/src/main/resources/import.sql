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
    (nextval('car_info_id_seq'), 'Toyota', 'Corolla', 2020, 'Like new, no issues', 'AVAILABLE'),
    (nextval('car_info_id_seq'), 'Honda', 'Civic', 2019, 'Good condition, minor wear and tear', 'RENTED'),
    (nextval('car_info_id_seq'), 'Ford', 'F-150', 2021, 'Small scratch on rear bumper', 'AT_CLEANING'),
    (nextval('car_info_id_seq'), 'Chevrolet', 'Malibu', 2018, 'Front seat has small stain', 'AVAILABLE'),
    (nextval('car_info_id_seq'), 'BMW', 'X5', 2022, 'Recently serviced, excellent condition', 'AT_CLEANING'),
    (nextval('car_info_id_seq'), 'Mercedes-Benz', 'C-Class', 2020, 'Minor dent on passenger door', 'RENTED'),
    (nextval('car_info_id_seq'), 'Audi', 'A4', 2021, 'Windshield has small chip', 'RENTED'),
    (nextval('car_info_id_seq'), 'Nissan', 'Altima', 2017, 'Interior needs cleaning', 'AT_CLEANING'),
    (nextval('car_info_id_seq'), 'Audi', 'Q4', 2019, 'Brake pads recently replaced', 'AVAILABLE');