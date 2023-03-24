INSERT INTO public.corescheduledbatchlog(
	batchid, batchtype, batchstartdatetime, batchenddatetime, batchstartparameters, createddatetime, updateddatetime, createdby, updatedby, totalrecords)
	VALUES ('b4ad9898-dd2f-43d4-b685-dd08aebc5063', 'migrationbatch', '2019-09-10 00:00:00', null, 'Start Params', '2019-08-27 00:00:00', null, 'TestScript', null, 20);

INSERT INTO public.corescheduledbatchlog(
	batchid, batchtype, batchstartdatetime, batchenddatetime, batchstartparameters, createddatetime, updateddatetime, createdby, updatedby, totalrecords)
	VALUES ('b4ad9898-dd2f-43d4-b685-dd08aebc5064', 'migrationbatch', '2019-09-14 23:59:59', null, 'Start Params', '2019-08-27 00:00:00', null, 'TestScript', null, 20);

INSERT INTO public.corescheduledbatchlog(
	batchid, batchtype, batchstartdatetime, batchenddatetime, batchstartparameters, createddatetime, updateddatetime, createdby, updatedby, totalrecords)
	VALUES ('b4ad9898-dd2f-43d4-b685-dd08aebc5065', 'migrationbatch', '2019-09-01 00:00:00', null, 'Start Params', '2019-08-27 00:00:00', null, 'TestScript', null, 20);

INSERT INTO public.coreconcurrentprocesslog(
	processid, batchid, processstartdatetime, processenddatetime, processstatus, totalrecordsinprocess, createddatetime, updateddatetime, createdby, updatedby)
	VALUES ('84a3a1d3-02e0-4ca5-a5bc-590f37e0835e', 'b4ad9898-dd2f-43d4-b685-dd08aebc5065', '2019-08-27 00:00:00', null, 'INPROGRESS', 50, '2019-08-27 00:00:00', null, 'TestScript', null);
	
INSERT INTO public.coreconcurrentprocesslog(
	processid, batchid, processstartdatetime, processenddatetime, processstatus, totalrecordsinprocess, createddatetime, updateddatetime, createdby, updatedby)
	VALUES ('84a3a1d3-02e0-4ca5-a5bc-590f37e0836e', 'b4ad9898-dd2f-43d4-b685-dd08aebc5065', '2019-08-27 00:00:00', null, 'INPROGRESS', 50, '2019-08-27 00:00:00', null, 'TestScript', null);
	
INSERT INTO public.coreprocessfailurelog(
	processfailureid, processid, failurecode, failurereason, failuredatetime, failurerecordid, failurestep, createddatetime, updateddatetime, createdby, updatedby, retrystatus, retrycount)
	VALUES ('f228254a-6f82-4ec1-9c8a-44a1a20577f5', '84a3a1d3-02e0-4ca5-a5bc-590f37e0835e', 'ERROR_11', 'API Error', '2019-09-10 01:00:00', '1234', 'FETCH_PRONTO_DOC', '2019-09-05 00:00:00', null, 'TestScript', null, 'F', null);	

INSERT INTO public.coreprocessfailurelog(
	processfailureid, processid, failurecode, failurereason, failuredatetime, failurerecordid, failurestep, createddatetime, updateddatetime, createdby, updatedby, retrystatus, retrycount)
	VALUES ('f228254a-6f82-4ec1-9c8a-44a1a20577f6', '84a3a1d3-02e0-4ca5-a5bc-590f37e0836e', 'ERROR_12', 'Service Error', '2019-09-11 02:00:00', '1234', 'FETCH_PRONTO_DOC', '2019-09-05 00:00:00', null, 'TestScript', null, 'T', null);
	
INSERT INTO public.coreprocessfailurelog(
	processfailureid, processid, failurecode, failurereason, failuredatetime, failurerecordid, failurestep, createddatetime, updateddatetime, createdby, updatedby, retrystatus, retrycount)
	VALUES ('f228254a-6f82-4ec1-9c8a-44a1a20577f7', '84a3a1d3-02e0-4ca5-a5bc-590f37e0836e', 'ERROR_12', 'Service Error', '2019-10-11 02:00:00', '1234', 'FETCH_PRONTO_DOC', '2019-09-05 00:00:00', null, 'TestScript', null, null, null);
	
INSERT INTO public.corescheduledbatchlog(
	batchid, batchtype, batchstartdatetime, batchenddatetime, batchstartparameters, createddatetime, updateddatetime, createdby, updatedby, totalrecords)
	VALUES ('b4ad9898-dd2f-43d4-b685-dd08aebc5067', 'migrationbatch', '2019-09-01 00:00:00', null, 'Start Params', '2019-08-27 00:00:00', null, 'TestScript', null, 20);

INSERT INTO public.coreconcurrentprocesslog(
	processid, batchid, processstartdatetime, processenddatetime, processstatus, totalrecordsinprocess, createddatetime, updateddatetime, createdby, updatedby)
	VALUES ('d361a0aa-6b09-4fd8-861d-aafce14f7e14', 'b4ad9898-dd2f-43d4-b685-dd08aebc5067', '2019-08-27 00:00:00', '2019-10-10 00:00:00', 'COMPLETED', 50, '2019-08-27 00:00:00', null, 'TestScript', null);