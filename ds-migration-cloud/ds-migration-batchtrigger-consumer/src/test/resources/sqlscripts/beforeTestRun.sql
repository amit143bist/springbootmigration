DELETE FROM public.coreprocessfailurelog;

DELETE FROM public.coreconcurrentprocesslog;

DELETE FROM public.corescheduledbatchlog;

INSERT INTO public.corescheduledbatchlog(
	batchid, batchtype, batchstartdatetime, batchenddatetime, batchstartparameters, createddatetime, updateddatetime, createdby, updatedby, totalrecords)
	VALUES ('e781ca58-dec7-44b7-a312-5c21fded402d', 'migrationbatch', '2019-08-27 00:00:00', null, 'Start Params', '2019-08-27 00:00:00', null, 'TestScript', null, 20);

INSERT INTO public.coreconcurrentprocesslog(
	processid, batchid, processstartdatetime, processenddatetime, processstatus, totalrecordsinprocess, createddatetime, updateddatetime, createdby, updatedby)
	VALUES ('84a3a1d3-02e0-4ca5-a5bc-590f37e0834e', 'e781ca58-dec7-44b7-a312-5c21fded402d', '2019-08-27 00:00:00', null, 'In-Progress', 500, '2019-08-27 00:00:00', null, 'TestScript', null);