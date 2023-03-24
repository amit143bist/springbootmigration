-- Table: public.migrationrecordidentries

-- DROP TABLE public.migrationrecordidentries;

CREATE TABLE public.migrationrecordidentries
(
    recordid text COLLATE pg_catalog."default" NOT NULL,
    docusignid uuid NOT NULL,
    createddatetime timestamp without time zone NOT NULL,
    updateddatetime timestamp without time zone,
    createdby text COLLATE pg_catalog."default" NOT NULL,
    updatedby text COLLATE pg_catalog."default",
    CONSTRAINT migrationrecordidentries_pkey PRIMARY KEY (recordid)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.migrationrecordidentries
    OWNER to postgres;