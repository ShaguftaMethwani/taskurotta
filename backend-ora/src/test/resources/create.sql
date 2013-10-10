CREATE TABLE ERRORS (
  ID          INT,
  PROCESS_ID  VARCHAR2(256) NOT NULL,
  ACTOR_ID    VARCHAR2(256) NOT NULL,
  MESSAGE     VARCHAR2(256) NOT NULL,
  STACKTRACE  CLOB,
  PRIMARY KEY (ID)
);

CREATE INDEX ERRORS_PROCESS_ID_IDX ON ERRORS(PROCESS_ID);

CREATE INDEX ERRORS_ACTOR_ID_IDX ON ERRORS(ACTOR_ID);

CREATE INDEX ERRORS_MESSAGE_IDX ON ERRORS(MESSAGE);

CREATE SEQUENCE errors_seq
  MINVALUE 1
  MAXVALUE 999999999999999999999999999
  START WITH 1
  INCREMENT BY 1
  CACHE 20;