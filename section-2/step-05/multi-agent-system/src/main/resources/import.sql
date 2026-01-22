CREATE SEQUENCE IF NOT EXISTS car_info_id_seq;

CREATE TABLE IF NOT EXISTS car_info (
    id INT PRIMARY KEY DEFAULT nextval('car_info_id_seq'),
    make VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    year INT NOT NULL,
    condition VARCHAR(255),
    dispositionDate DATE,
    status VARCHAR(20) NOT NULL
);

INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status)
VALUES
    (nextval('car_info_id_seq'), 'Toyota', 'Corolla', 2020, 'Like new, no issues', CURRENT_DATE + INTERVAL '18 months', 'AVAILABLE'),
    (nextval('car_info_id_seq'), 'Honda', 'Civic', 2019, 'Good condition, minor wear and tear', NULL, 'RENTED'),
    (nextval('car_info_id_seq'), 'Ford', 'F-150', 2021, 'Small scratch on rear bumper', CURRENT_DATE + INTERVAL '24 months', 'IN_MAINTENANCE'),
    (nextval('car_info_id_seq'), 'Chevrolet', 'Malibu', 2018, 'Front seat has small stain', NULL, 'AT_CLEANING'),
    (nextval('car_info_id_seq'), 'BMW', 'X5', 2022, 'Recently serviced, excellent condition', NULL, 'AT_CLEANING'),
    (nextval('car_info_id_seq'), 'Mercedes-Benz', 'C-Class', 2020, 'Minor dent on passenger door', CURRENT_DATE + INTERVAL '12 months', 'RENTED'),
    (nextval('car_info_id_seq'), 'Audi', 'A4', 2021, 'Windshield has small chip', NULL, 'RENTED'),
    (nextval('car_info_id_seq'), 'Nissan', 'Altima', 2017, 'SCRAP: completely wrecked, ready for the junk yard', CURRENT_DATE + INTERVAL '6 months', 'PENDING_DISPOSITION'),
    (nextval('car_info_id_seq'), 'Audi', 'Q4', 2019, 'Brake pads recently replaced', NULL, 'RENTED');