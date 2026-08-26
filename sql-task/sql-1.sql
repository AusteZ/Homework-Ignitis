SELECT bi.item_name,
       SUM(bi.quantity * bi.amount) AS revenue
FROM bill_item bi
GROUP BY bi.item_name
ORDER BY revenue DESC LIMIT 5;