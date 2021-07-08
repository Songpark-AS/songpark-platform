CREATE SEQUENCE IF NOT EXISTS sid_sequence;
--;;
CREATE TABLE IF NOT EXISTS usr (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    mail varchar(255),
    descr varchar(255),
    pass varchar(255)
);
--;;
CREATE TABLE IF NOT EXISTS band (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    descr varchar(255)
);
--;;
CREATE TABLE IF NOT EXISTS bandmbr (
    userid INT NOT NULL,
    bandid INT NOT NULL,
    FOREIGN KEY(userid) 
        REFERENCES usr(sid),
    FOREIGN KEY(bandid)
        REFERENCES band(sid)
);
--;;
CREATE TABLE IF NOT EXISTS rec (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    title varchar(63),
    descr varchar(255),
    bandid INT NOT NULL,
    FOREIGN KEY(bandid)
        REFERENCES band(sid)
);
--;;
CREATE TABLE IF NOT EXISTS rating (
    userid INT NOT NULL,
    recid INT NOT NULL,
    stars INT,
    FOREIGN KEY(userid)
        REFERENCES usr(sid),
    FOREIGN KEY(recid)
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
CREATE TABLE IF NOT EXISTS tpownr (
    userid INT NOT NULL,
    tpid INT NOT NULL,
    as_of TIMESTAMP NOT NULL,
    FOREIGN KEY(userid)
        REFERENCES usr(sid),
    FOREIGN KEY(tpid)
        REFERENCES tp(sid)
);
--;;
CREATE TABLE IF NOT EXISTS chat (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    title varchar(63)
);
--;;
CREATE TABLE IF NOT EXISTS chatmbr (
    userid INT NOT NULL,
    chatid INT NOT NULL,
    FOREIGN KEY(userid)
        REFERENCES usr(sid),
    FOREIGN KEY(chatid)
        REFERENCES chat(sid)
);
--;;
CREATE TABLE IF NOT EXISTS msg (
    sid bigint NOT NULL UNIQUE DEFAULT nextval('sid_sequence'::regclass),
    content varchar(255),
    chatid INT NOT NULL,
    FOREIGN KEY(chatid)
        REFERENCES chat(sid)
);






















