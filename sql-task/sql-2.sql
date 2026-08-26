SELECT
    b.client_id
    YEAR(b.bill_datetime)  AS bill_year,
    MONTH(b.bill_datetime) AS bill_month,
    SUM(b.amount_due)      AS total_amount
FROM bills b
GROUP BY
    b.client_id,
    YEAR(b.bill_datetime),
    MONTH(b.bill_datetime)
HAVING SUM(b.amount_due) > 10000;