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

INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (1, 1, CURRENT_DATE + 1, CURRENT_DATE + 3, 'Verbier, Switzerland');
INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (2, 1, CURRENT_DATE + 14, CURRENT_DATE + 16, 'Sao Paulo, Brazil');
INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (3, 1, CURRENT_DATE + 30, CURRENT_DATE + 34, 'Antwerp, Belgium');

INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (4, 2, CURRENT_DATE + 2, CURRENT_DATE + 7, 'Tokyo, Japan');
INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (5, 2, CURRENT_DATE + 60, CURRENT_DATE + 65, 'Brisbane, Australia');

INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (7, 3, CURRENT_DATE + 3, CURRENT_DATE + 8, 'Missoula, Montana');
INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (8, 3, CURRENT_DATE + 35, CURRENT_DATE + 41, 'Singapore');
INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (9, 3, CURRENT_DATE + 90, CURRENT_DATE + 96, 'Capetown, South Africa');

INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (10, 4, CURRENT_DATE + 1, CURRENT_DATE + 6, 'Nuuk, Greenland');
INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (11, 4, CURRENT_DATE + 75, CURRENT_DATE + 80, 'Santiago de Chile');
INSERT INTO booking (id, customer_id, dateFrom, dateTo, location)
VALUES (12, 4, CURRENT_DATE + 120, CURRENT_DATE + 127, 'Dubai');

ALTER SEQUENCE booking_seq RESTART WITH 12;
