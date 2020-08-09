DELETE FROM SYS_CONFIG;

INSERT INTO SYS_CONFIG (SERVICE_NAME, ENV, PROPERTY_KEY, PROPERTY_VALUE, PROPERTY_DEFAULT_VALUE, REMARK, LABEL) VALUES
('CONSUMER', 'dev', 'test', 'test1@baomidou.com','','test','master'),
('CONSUMER', 'dev', 'name', null ,'123','name','master');