-- Table: public.coreconcurrentprocesslog

-- DROP TABLE public.coreconcurrentprocesslog;

CREATE TABLE public.coreconcurrentprocesslog
(
    processid uuid NOT NULL,
    batchid uuid NOT NULL,
    processstartdatetime timestamp without time zone NOT NULL,
    processenddatetime timestamp without time zone,
    processstatus text COLLATE pg_catalog."default" NOT NULL,
    totalrecordsinprocess bigint NOT NULL,
    createddatetime timestamp without time zone NOT NULL,
    updateddatetime timestamp without time zone,
    createdby text COLLATE pg_catalog."default" NOT NULL,
    updatedby text COLLATE pg_catalog."default",
    CONSTRAINT coreconcurrentprocesslog_pkey PRIMARY KEY (processid),
    CONSTRAINT coreconcurrentprocesslog_batchid_fk FOREIGN KEY (batchid)
        REFERENCES public.corescheduledbatchlog (batchid) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.coreconcurrentprocesslog
    OWNER to postgres;