CREATE TABLE IF NOT EXISTS car_info (
    id INT PRIMARY KEY,
    make VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    year INT NOT NULL,
    status VARCHAR(20) NOT NULL
);

INSERT INTO car_info (id, make, model, year, status) VALUES (1, 'Toyota', 'Corolla', 2020, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, status) VALUES (2, 'Honda', 'Civic', 2019, 'RENTED');
INSERT INTO car_info (id, make, model, year, status) VALUES (3, 'Ford', 'F-150', 2021, 'AT_CAR_WASH');
INSERT INTO car_info (id, make, model, year, status) VALUES (4, 'Chevrolet', 'Malibu', 2018, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, status) VALUES (5, 'BMW', 'X5', 2022, 'AT_CAR_WASH');
INSERT INTO car_info (id, make, model, year, status) VALUES (6, 'Mercedes-Benz', 'C-Class', 2020, 'RENTED');
INSERT INTO car_info (id, make, model, year, status) VALUES (7, 'Audi', 'A4', 2021, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, status) VALUES (8, 'Nissan', 'Altima', 2017, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, status) VALUES (9, 'Toyota', 'Camry', 2019, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, status) VALUES (10, 'Honda', 'Accord', 2020, 'RENTED');
INSERT INTO car_info (id, make, model, year, status) VALUES (11, 'Ford', 'Mustang', 2022, 'AT_CAR_WASH');
INSERT INTO car_info (id, make, model, year, status) VALUES (12, 'Chevrolet', 'Silverado', 2021, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, status) VALUES (13, 'BMW', '3 Series', 2020, 'AT_CAR_WASH');
INSERT INTO car_info (id, make, model, year, status) VALUES (14, 'Mercedes-Benz', 'E-Class', 2023, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, status) VALUES (15, 'Audi', 'Q5', 2022, 'RENTED');

