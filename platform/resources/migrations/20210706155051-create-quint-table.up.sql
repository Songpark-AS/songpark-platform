CREATE SEQUENCE public.q_i_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;
--;;
ALTER SEQUENCE public.q_i_seq
    OWNER TO postgres;
--;;
CREATE TABLE public.q
(
    i bigint NOT NULL DEFAULT nextval('q_i_seq'::regclass),
    g bigint,
    s bigint[],
    p text,
    otype text,
    oint bigint,
    otx text,
    odt timestamp without time zone,
    CONSTRAINT q_pkey PRIMARY KEY (i)
);
--;;
ALTER TABLE public.q
    OWNER to postgres;
--;;
COMMENT ON TABLE public.q
    IS 'A Graph DB emulation';