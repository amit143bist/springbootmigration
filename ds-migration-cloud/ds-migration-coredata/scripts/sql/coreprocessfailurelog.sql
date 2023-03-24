-- Table: public.coreprocessfailurelog

-- DROP TABLE public.coreprocessfailurelog;

CREATE TABLE public.coreprocessfailurelog
(
    processfailureid uuid NOT NULL,
    processid uuid NOT NULL,
    failurecode text COLLATE pg_catalog."default" NOT NULL,
    failurereason text COLLATE pg_catalog."default" NOT NULL,
    failuredatetime timestamp without time zone NOT NULL,
    failurerecordid text COLLATE pg_catalog."default" NOT NULL,
    failurestep text COLLATE pg_catalog."default" NOT NULL,
    createddatetime timestamp without time zone NOT NULL,
    updateddatetime timestamp without time zone,
    createdby text COLLATE pg_catalog."default" NOT NULL,
    updatedby text COLLATE pg_catalog."default",
    retrystatus text COLLATE pg_catalog."default",
    retrycount bigint,
    CONSTRAINT coreprocessfailurelog_pkey PRIMARY KEY (processfailureid),
    CONSTRAINT coreprocessfailurelog_processid_fkey FOREIGN KEY (processid)
        REFERENCES public.coreconcurrentprocesslog (processid) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.coreprocessfailurelog
    OWNER to postgres;