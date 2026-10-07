ALTER TABLE expense
    ADD CONSTRAINT chk_expense_category CHECK (category IN ('FOOD', 'TRAVEL', 'BILLS', 'OTHER'));
