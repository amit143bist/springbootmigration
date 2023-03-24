INSERT INTO public.corescheduledbatchlog(
	batchid, batchtype, batchstartdatetime, batchenddatetime, batchstartparameters, createddatetime, updateddatetime, createdby, updatedby, totalrecords)
	VALUES ('e781ca58-dec7-44b7-a312-5c21fded402d', 'migrationbatch', '2019-08-27 00:00:00', null, 'Start Params', '2019-08-27 00:00:00', null, 'TestScript', null, 20);

INSERT INTO public.corescheduledbatchlog(
	batchid, batchtype, batchstartdatetime, batchenddatetime, batchstartparameters, createddatetime, updateddatetime, createdby, updatedby, totalrecords)
	VALUES ('e781ca58-dec7-44b7-a312-5c21fded402e', 'migrationbatch', '2019-09-01 00:00:00', '2019-09-01 01:00:00', 'Start Params', '2019-08-27 00:00:00', null, 'TestScript', null, 20);

INSERT INTO public.corescheduledbatchlog(
	batchid, batchtype, batchstartdatetime, batchenddatetime, batchstartparameters, createddatetime, updateddatetime, createdby, updatedby, totalrecords)
	VALUES ('e781ca58-dec7-44b7-a312-5c21fded402f', 'migrationbatch', '2019-09-05 23:59:59', '2019-09-01 01:00:00', 'Start Params', '2019-08-27 00:00:00', null, 'TestScript', null, 20);

INSERT INTO public.corescheduledbatchlog(
	batchid, batchtype, batchstartdatetime, batchenddatetime, batchstartparameters, createddatetime, updateddatetime, createdby, updatedby, totalrecords)
	VALUES ('d43ac5ef-ebc0-40e9-8122-9574d8641731', 'otherbatchtype', '2019-09-01 23:00:00', '2019-09-01 01:00:00', 'Start Params', '2019-08-27 00:00:00', null, 'TestScript', null, 20);

INSERT INTO public.coreconcurrentprocesslog(
	processid, batchid, processstartdatetime, processenddatetime, processstatus, totalrecordsinprocess, createddatetime, updateddatetime, createdby, updatedby)
	VALUES ('84a3a1d3-02e0-4ca5-a5bc-590f37e0834e', 'e781ca58-dec7-44b7-a312-5c21fded402d', '2019-08-27 00:00:00', null, 'Completed', 500, '2019-08-27 00:00:00', null, 'TestScript', null);
	
INSERT INTO public.coreprocessfailurelog(
	processfailureid, processid, failurecode, failurereason, failuredatetime, failurerecordid, failurestep, createddatetime, updateddatetime, createdby, updatedby, retrystatus, retrycount)
	VALUES ('f228254a-6f82-4ec1-9c8a-44a1a20577f1', '84a3a1d3-02e0-4ca5-a5bc-590f37e0834e', 'ERROR_10', 'DB Error', '2019-09-05 00:00:00', '1234', 'FETCH_PRONTO_DOC', '2019-09-05 00:00:00', null, 'TestScript', null, null, null);
	
INSERT INTO public.coreprocessfailurelog(
	processfailureid, processid, failurecode, failurereason, failuredatetime, failurerecordid, failurestep, createddatetime, updateddatetime, createdby, updatedby, retrystatus, retrycount)
	VALUES ('f228254a-6f82-4ec1-9c8a-44a1a20577f2', '84a3a1d3-02e0-4ca5-a5bc-590f37e0834e', 'ERROR_11', 'API Error', '2019-09-05 01:00:00', '1234', 'FETCH_PRONTO_DOC', '2019-09-05 00:00:00', null, 'TestScript', null, 'F', null);	

INSERT INTO public.coreprocessfailurelog(
	processfailureid, processid, failurecode, failurereason, failuredatetime, failurerecordid, failurestep, createddatetime, updateddatetime, createdby, updatedby, retrystatus, retrycount)
	VALUES ('f228254a-6f82-4ec1-9c8a-44a1a20577f3', '84a3a1d3-02e0-4ca5-a5bc-590f37e0834e', 'ERROR_12', 'Service Error', '2019-09-05 02:00:00', '1234', 'FETCH_PRONTO_DOC', '2019-09-05 00:00:00', null, 'TestScript', null, 'T', null);	