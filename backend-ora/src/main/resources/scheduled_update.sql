CREATE TABLE TSK_SCHEDULED
(
  ID                    NUMBER                    NOT NULL,
  NAME                  VARCHAR2(256 char)        NOT NULL,
  CRON                  VARCHAR2(256 char)        NOT NULL,
  STATUS                NUMBER                    NOT NULL,
  JSON                  VARCHAR2(2000 char)       NOT NULL,
  CREATED               TIMESTAMP,
  QUEUE_LIMIT           NUMBER,
  ERR_COUNT             NUMBER,
  LAST_ERR_MESSAGE      VARCHAR2(2000 char),
  PRIMARY KEY (ID)
);

CREATE SEQUENCE SEQ_TSK_SCHEDULED MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;

CREATE OR REPLACE TRIGGER TSK_SCHEDULED_BI
BEFORE INSERT
ON TSK_SCHEDULED
FOR EACH ROW
  BEGIN
    IF :NEW.id IS NULL
    THEN
      SELECT
          SEQ_TSK_SCHEDULED.NEXTVAL
      INTO :NEW.id
      FROM DUAL;
    END IF;
  END;
/

