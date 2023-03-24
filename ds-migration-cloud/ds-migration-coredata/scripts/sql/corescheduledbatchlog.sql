-- Table: public.corescheduledbatchlog

-- DROP TABLE public.corescheduledbatchlog;

CREATE TABLE public.corescheduledbatchlog
(
    batchid uuid NOT NULL,
    batchtype text COLLATE pg_catalog."default" NOT NULL,
    batchstartdatetime timestamp without time zone NOT NULL,
    batchenddatetime timestamp without time zone,
    batchstartparameters text COLLATE pg_catalog."default" NOT NULL,
    createddatetime timestamp without time zone NOT NULL,
    updateddatetime timestamp without time zone,
    createdby text COLLATE pg_catalog."default" NOT NULL,
    updatedby text COLLATE pg_catalog."default",
    CONSTRAINT corescheduledbatchlog_pkey PRIMARY KEY (batchid)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.corescheduledbatchlog
    OWNER to postgres;