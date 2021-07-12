CREATE SEQUENCE IF NOT EXISTS sid_sequence;
--;;
CREATE TABLE IF NOT EXISTS usr (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    e_mail text,
    descr text,
    passwd text
);
--;;
CREATE TABLE IF NOT EXISTS band (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    descr text
);
--;;
CREATE TABLE IF NOT EXISTS usr_band (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),    
    usr_sid BIGINT NOT NULL,
    band_sid BIGINT NOT NULL,
    FOREIGN KEY(usr_sid) 
        REFERENCES usr(sid),
    FOREIGN KEY(band_sid)
        REFERENCES band(sid)
);
--;;
CREATE TABLE IF NOT EXISTS rec (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    title text,
    descr text,
    band_sid BIGINT NOT NULL,
    FOREIGN KEY(band_sid)
        REFERENCES band(sid)
);
--;;
CREATE TABLE IF NOT EXISTS rec_rating (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    usr_sid BIGINT NOT NULL,
    rec_sid BIGINT NOT NULL,
    stars INT,
    FOREIGN KEY(usr_sid)
        REFERENCES usr(sid),
    FOREIGN KEY(rec_sid)
        REFERENCES rec(sid)
);
--;;
CREATE TABLE IF NOT EXISTS tp (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    voip inet,
    ip inet,
    active boolean
);
--;;
CREATE TABLE IF NOT EXISTS usr_tp (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    usr_sid BIGINT NOT NULL,
    tp_sid BIGINT NOT NULL,
    as_of TIMESTAMP NOT NULL,
    FOREIGN KEY(usr_sid)
        REFERENCES usr(sid),
    FOREIGN KEY(tp_sid)
        REFERENCES tp(sid)
);
--;;
CREATE TABLE IF NOT EXISTS chat (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    title text
);
--;;
CREATE TABLE IF NOT EXISTS usr_chat (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    usr_sid BIGINT NOT NULL,
    chat_sid BIGINT NOT NULL,
    FOREIGN KEY(usr_sid)
        REFERENCES usr(sid),
    FOREIGN KEY(chat_sid)
        REFERENCES chat(sid)
);
--;;
CREATE TABLE IF NOT EXISTS msg (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    content text,
    chat_sid BIGINT NOT NULL,
    FOREIGN KEY(chat_sid)
        REFERENCES chat(sid)
);






















