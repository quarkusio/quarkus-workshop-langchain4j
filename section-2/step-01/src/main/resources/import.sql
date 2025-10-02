CREATE SEQUENCE IF NOT EXISTS car_info_id_seq;

CREATE TABLE IF NOT EXISTS car_info (
    id INT PRIMARY KEY DEFAULT nextval('car_info_id_seq'),
    make VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    year INT NOT NULL,
    status VARCHAR(20) NOT NULL
);

INSERT INTO car_info (id, make, model, year, status) VALUES
    (nextval('car_info_id_seq'), 'Toyota', 'Corolla', 2020, 'AVAILABLE'),
    (nextval('car_info_id_seq'), 'Honda', 'Civic', 2019, 'RENTED'),
    (nextval('car_info_id_seq'), 'Ford', 'F-150', 2021, 'AT_CAR_WASH'),
    (nextval('car_info_id_seq'), 'Chevrolet', 'Malibu', 2018, 'AVAILABLE'),
    (nextval('car_info_id_seq'), 'BMW', 'X5', 2022, 'AT_CAR_WASH'),
    (nextval('car_info_id_seq'), 'Mercedes-Benz', 'C-Class', 2020, 'RENTED'),
    (nextval('car_info_id_seq'), 'Audi', 'A4', 2021, 'AVAILABLE'),
    (nextval('car_info_id_seq'), 'Nissan', 'Altima', 2017, 'RENTED'),
    (nextval('car_info_id_seq'), 'Audi', 'Q4', 2022, 'RENTED');