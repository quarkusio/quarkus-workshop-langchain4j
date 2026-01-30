CREATE SEQUENCE IF NOT EXISTS car_info_id_seq;

CREATE TABLE IF NOT EXISTS car_info (
    id INT PRIMARY KEY DEFAULT nextval('car_info_id_seq'),
    make VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    year INT NOT NULL,
    status VARCHAR(20) NOT NULL
);

INSERT INTO car_info (id, make, model, year, status) VALUES
    (nextval('car_info_id_seq'), 'Mercedes-Benz', 'C-Class', EXTRACT(YEAR FROM CURRENT_DATE) - 2, 'RENTED'),
    (nextval('car_info_id_seq'), 'BMW', 'X5', EXTRACT(YEAR FROM CURRENT_DATE) - 1, 'AT_CLEANING'),
    (nextval('car_info_id_seq'), 'Audi', 'Q4', EXTRACT(YEAR FROM CURRENT_DATE) - 1, 'RENTED'),
    (nextval('car_info_id_seq'), 'Nissan', 'Altima', EXTRACT(YEAR FROM CURRENT_DATE) - 8, 'AT_CLEANING'),
    (nextval('car_info_id_seq'), 'Ford', 'Focus', EXTRACT(YEAR FROM CURRENT_DATE) - 12, 'RENTED'),
    (nextval('car_info_id_seq'), 'Toyota', 'Corolla', EXTRACT(YEAR FROM CURRENT_DATE) - 3, 'RENTED'),
    (nextval('car_info_id_seq'), 'Honda', 'Civic', EXTRACT(YEAR FROM CURRENT_DATE) - 4, 'RENTED'),
    (nextval('car_info_id_seq'), 'Ford', 'F-150', EXTRACT(YEAR FROM CURRENT_DATE) - 2, 'AT_CLEANING');