-- Table: public.migrationauditentries

-- DROP TABLE public.migrationauditentries;

CREATE TABLE public.migrationauditentries
(
    auditid uuid NOT NULL,
    recordid text COLLATE pg_catalog."default" NOT NULL,
    processid uuid NOT NULL,
    auditentrydatetime timestamp without time zone NOT NULL,
    recordphasestatus text COLLATE pg_catalog."default" NOT NULL,
    recordphase text COLLATE pg_catalog."default" NOT NULL,
    createddatetime timestamp without time zone NOT NULL,
    updateddatetime timestamp without time zone,
    createdby text COLLATE pg_catalog."default" NOT NULL,
    updatedby text COLLATE pg_catalog."default",
    hashedentry text COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT migrationauditentries_pkey PRIMARY KEY (auditid)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.migrationauditentries
    OWNER to postgres;