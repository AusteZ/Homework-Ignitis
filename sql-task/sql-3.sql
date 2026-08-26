WITH bill_history AS (SELECT b.bill_id,
                             b.client_id,
                             b.bill_number,
                             b.bill_datetime,
                             b.amount_due,
                             LAG(b.bill_datetime) OVER (
                                PARTITION BY b.client_id
                                ORDER BY b.bill_datetime, b.bill_id
                            ) AS previous_bill_datetime
                      FROM bills b),

     client_stats AS (SELECT client_id,
                             AVG(
                                     DATEDIFF(
                                             'DAY',
                                             previous_bill_datetime,
                                             bill_datetime
                                     )
                             ) AS avg_client_bill_interval
                      FROM bill_history
                      WHERE previous_bill_datetime IS NOT NULL
                      GROUP BY client_id),

     latest_bill_date AS (SELECT client_id,
                                 MAX(bill_datetime) AS latest_bill_datetime
                          FROM bills
                          GROUP BY client_id),

     last_bill AS (SELECT b.bill_id,
                          b.client_id,
                          b.bill_number,
                          b.amount_due
                   FROM bills b
                   JOIN latest_bill_date lbd
                   ON lbd.client_id = b.client_id AND lbd.latest_bill_datetime = b.bill_datetime),

     last_bill_items AS (SELECT bi.bill_id,
                                LISTAGG(bi.item_name, ', ')
                                    WITHIN GROUP (ORDER BY bi.bill_item_id) AS item_list
                        FROM bill_item bi
                        JOIN last_bill lb ON lb.bill_id = bi.bill_id
                        GROUP BY bi.bill_id)

SELECT c.client_name,
       c.client_surname,
       cs.avg_client_bill_interval,
       lb.bill_number AS last_bill_number,
       lb.amount_due  AS last_bill_due_amount,
       lbi.item_list  AS last_bill_item_list

FROM client c
LEFT JOIN client_stats cs ON cs.client_id = c.client_id
LEFT JOIN last_bill lb ON lb.client_id = c.client_id
LEFT JOIN last_bill_items lbi ON lbi.bill_id = lb.bill_id;