CREATE OR REPLACE FUNCTION insert_into_migrationauditentries(_auditid uuid, _recordid text, _processid uuid, _auditentrydatetime timestamp without time zone, _recordphasestatus text, _recordphase text,  _hashedentry text)
RETURNS bigint AS
$BODY$
DECLARE
    _createddatetime timestamp without time zone := now();
    _createdby text := 'migrationuserInsert';
BEGIN
 INSERT INTO public.migrationauditentries(
	auditid, recordid, processid, auditentrydatetime, recordphasestatus, recordphase, createddatetime, createdby, hashedentry)
  VALUES(_auditid, _recordid, _processid, _auditentrydatetime, _recordphasestatus, _recordphase, _createddatetime, _createdby, _hashedentry);
  RETURN 1;
END;
$BODY$
LANGUAGE 'plpgsql' VOLATILE
 COST 100;
 
 
CREATE OR REPLACE FUNCTION insert_into_migrationauditentriesv2(_auditid uuid[], _recordid text[], _processid uuid[], _auditentrydatetime timestamp without time zone[], _recordphasestatus text[], _recordphase text[], _hashedentry text[])
RETURNS bigint AS
$BODY$
DECLARE
    _createddatetime timestamp without time zone := now();
    _createdby text := 'migrationuserInsert';
BEGIN
 INSERT INTO public.migrationauditentries(
	auditid, recordid, processid, auditentrydatetime, recordphasestatus, recordphase, createddatetime, createdby, hashedentry)
  SELECT unnest(_auditid), unnest(_recordid), unnest(_processid), unnest(_auditentrydatetime), unnest(_recordphasestatus), unnest(_recordphase), _createddatetime, _createdby, unnest(_hashedentry);
  return 1;
END;
$BODY$
LANGUAGE plpgsql VOLATILE
COST 100; 