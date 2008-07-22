ALTER TABLE MAILARCHIVE_MESSAGE ADD (
       SUBJECT           VARCHAR2 (255) default null,
       BODY              CLOB default null,
       HTMLBODY          CLOB default null
);

CREATE INDEX MAILARCHIVE_SUBJECT_INDEX ON MAILARCHIVE_MESSAGE
(
        SUBJECT
);

