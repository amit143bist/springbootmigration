INSERT INTO public.corescheduledbatchlog(
	batchid, batchtype, batchstartdatetime, batchenddatetime, batchstartparameters, createddatetime, updateddatetime, createdby, updatedby, totalrecords)
	VALUES ('e781ca58-dec7-44b7-a312-5c21fded402d', 'migrationbatch', '2019-08-27 00:00:00', null, 'Start Params', '2019-08-27 00:00:00', null, 'TestScript', null, 20);
	
INSERT INTO public.coreconcurrentprocesslog(
	processid, batchid, processstartdatetime, processenddatetime, processstatus, totalrecordsinprocess, createddatetime, updateddatetime, createdby, updatedby)
	VALUES ('84a3a1d3-02e0-4ca5-a5bc-590f37e0834e', 'e781ca58-dec7-44b7-a312-5c21fded402d', '2019-08-27 00:00:00', null, 'Completed', 500, '2019-08-27 00:00:00', null, 'TestScript', null);
	
INSERT INTO public.migrationrecordidentries(
	recordid, docusignid, createddatetime, updateddatetime, createdby, updatedby)
	VALUES ('9876', 'e781ca58-dec7-44b7-a312-5c21fded402d', '2019-08-27 00:00:00', null, 'TestScrip', null);
	
INSERT INTO public.migrationauditentries(
	auditid, recordid, processid, auditentrydatetime, recordphasestatus, recordphase, createddatetime, updateddatetime, createdby, updatedby, hashedentry)
	VALUES ('05fb0a45-484a-43d4-9694-33edcddbfc69', '1234', '84a3a1d3-02e0-4ca5-a5bc-590f37e0834e', '2019-09-04T15:17:49.887', 'S', 'FETCH_PRONTO_DOC', '2019-09-04T15:17:50.404', null, 'TestScript', null, 'eb53c0347b998c7b4f22726487d1efbd');
	
INSERT INTO public.migrationauditentries(
	auditid, recordid, processid, auditentrydatetime, recordphasestatus, recordphase, createddatetime, updateddatetime, createdby, updatedby, hashedentry)
	VALUES ('68973656-bef4-483e-90d5-1e275b1131ef', '1234', '84a3a1d3-02e0-4ca5-a5bc-590f37e0834e', '2019-09-04T17:17:49.887', 'S', 'FETCH_SIGNATURE_DETAILS', '2019-09-04T15:17:50.404', null, 'TestScript', null, '23f685f7a79442f7fa305a7d3969af79');
	
INSERT INTO public.migrationauditentries(
	auditid, recordid, processid, auditentrydatetime, recordphasestatus, recordphase, createddatetime, updateddatetime, createdby, updatedby, hashedentry)
	VALUES ('c461abe4-01be-4f95-a543-17a57a459bec', '95875351', 'acb4e224-c435-4992-80fb-aff9801e9ad7', '2019-10-02T18:27:58.060', 'S', 'OST_DS_ID_ORACLE', '2019-09-04T15:17:50.404', null, 'TestScript', null, '5725266c53f5a55c1cb9563976f0cf60');