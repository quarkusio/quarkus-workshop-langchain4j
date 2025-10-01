CREATE TABLE IF NOT EXISTS car_info (
    id INT PRIMARY KEY,
    make VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    year INT NOT NULL,
    condition VARCHAR(255),
    dispositionDate DATE,
    status VARCHAR(20) NOT NULL
);

INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (1, 'Toyota', 'Corolla', 2020, 'Like new, no issues', CURRENT_DATE + INTERVAL '18 months', 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (2, 'Honda', 'Civic', 2019, 'Good condition, minor wear and tear', NULL, 'RENTED');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (3, 'Ford', 'F-150', 2021, 'Small scratch on rear bumper', CURRENT_DATE + INTERVAL '24 months', 'IN_MAINTENANCE');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (4, 'Chevrolet', 'Malibu', 2018, 'Front seat has small stain', NULL, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (5, 'BMW', 'X5', 2022, 'Recently serviced, excellent condition', NULL, 'AT_CAR_WASH');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (6, 'Mercedes-Benz', 'C-Class', 2020, 'Minor dent on passenger door', CURRENT_DATE + INTERVAL '12 months', 'RENTED');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (7, 'Audi', 'A4', 2021, 'Windshield has small chip', NULL, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (8, 'Nissan', 'Altima', 2017, 'Interior needs cleaning', CURRENT_DATE + INTERVAL '6 months', 'PENDING_DISPOSITION');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (9, 'Toyota', 'Camry', 2019, 'Brake pads recently replaced', NULL, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (10, 'Honda', 'Accord', 2020, 'Small scratch on driver''s side', NULL, 'RENTED');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (11, 'Ford', 'Mustang', 2022, 'Needs oil change soon', NULL, 'IN_MAINTENANCE');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (12, 'Chevrolet', 'Silverado', 2021, 'Excellent condition after recent detailing', CURRENT_DATE + INTERVAL '15 months', 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (13, 'BMW', '3 Series', 2020, 'Needs tire rotation', NULL, 'AT_CAR_WASH');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (14, 'Mercedes-Benz', 'E-Class', 2023, 'Like new, no issues', NULL, 'AVAILABLE');
INSERT INTO car_info (id, make, model, year, condition, dispositionDate, status) VALUES (15, 'Audi', 'Q5', 2022, 'Good condition, minor wear and tear', CURRENT_DATE + INTERVAL '20 months', 'RENTED');

