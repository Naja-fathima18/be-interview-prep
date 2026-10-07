CREATE UNIQUE INDEX ux_loan_open_per_book ON loan (book_id) WHERE returned_at IS NULL;
