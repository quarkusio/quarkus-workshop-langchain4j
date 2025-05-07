INSERT INTO customer (id, firstName, lastName)
VALUES (1, 'Speedy', 'McWheels');
INSERT INTO customer (id, firstName, lastName)
VALUES (2, 'Zoom', 'Thunderfoot');
INSERT INTO customer (id, firstName, lastName)
VALUES (3, 'Vroom', 'Lightyear');
INSERT INTO customer (id, firstName, lastName)
VALUES (4, 'Turbo', 'Gearshift');
INSERT INTO customer (id, firstName, lastName)
VALUES (5, 'Drifty', 'Skiddy');

ALTER SEQUENCE customer_seq RESTART WITH 5;

INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (1, 1, '2024-07-10', '2024-07-15');
INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (2, 1, '2024-08-05', '2024-08-12');
INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (3, 1, '2024-10-01', '2024-10-07');

INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (4, 2, '2024-07-20', '2024-07-25');
INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (5, 2, '2024-11-10', '2024-11-15');

INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (7, 3, '2024-06-15', '2024-06-20');
INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (8, 3, '2024-10-12', '2024-10-18');
INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (9, 3, '2024-12-03', '2024-12-09');

INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (10, 4, '2024-07-01', '2024-07-06');
INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (11, 4, '2024-07-25', '2024-07-30');
INSERT INTO booking (id, customer_id, dateFrom, dateTo)
VALUES (12, 4, '2024-10-15', '2024-10-22');

ALTER SEQUENCE booking_seq RESTART WITH 12;

