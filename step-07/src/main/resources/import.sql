INSERT INTO customer (id, firstName, lastName) VALUES (1, 'Speedy', 'McWheels');
INSERT INTO customer (id, firstName, lastName) VALUES (2, 'Zoom', 'Thunderfoot');
INSERT INTO customer (id, firstName, lastName) VALUES (3, 'Vroom', 'Lightyear');
INSERT INTO customer (id, firstName, lastName) VALUES (4, 'Turbo', 'Gearshift');
INSERT INTO customer (id, firstName, lastName) VALUES (5, 'Drifty', 'Skidmark');

ALTER SEQUENCE customer_seq RESTART WITH 5;

INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (1, 1, '2025-07-10', '2025-07-12');
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (2, 1, '2025-08-05', '2025-08-12');
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (3, 1, '2025-10-01', '2025-10-07');

INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (4, 2, '2025-07-20', '2025-07-25');
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (5, 2, '2025-11-10', '2025-11-15');

INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (7, 3, '2025-06-15', '2025-06-20');
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (8, 3, '2025-10-12', '2025-10-18');
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (9, 3, '2025-12-03', '2025-12-09');

INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (10, 4, '2025-07-01', '2025-07-06');
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (11, 4, '2025-07-25', '2025-07-30');
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (12, 4, '2025-10-15', '2025-10-22');

ALTER SEQUENCE booking_seq RESTART WITH 12;
