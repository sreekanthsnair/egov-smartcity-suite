-----------------Role action mappings to search Activities for Revision Estimate---------------
insert into EG_ACTION (ID,NAME,URL,QUERYPARAMS,PARENTMODULE,ORDERNUMBER,DISPLAYNAME,ENABLED,CONTEXTROOT,VERSION,CREATEDBY,CREATEDDATE,LASTMODIFIEDBY,LASTMODIFIEDDATE,APPLICATION) values (NEXTVAL('SEQ_EG_ACTION'),'WorksRevisionEstimateSearchActivities','/revisionestimate/ajax-searchactivities',null,(select id from EG_MODULE where name = 'WorksRevisionEstimate'),1,'Search Activities for Revision Estimate','false','egworks',0,1,now(),1,now(),(select id from eg_module  where name = 'Works Management'));
insert into eg_roleaction (roleid, actionid) values ((select id from eg_role where name = 'Super User'),(select id from eg_action where name ='WorksRevisionEstimateSearchActivities' and contextroot = 'egworks'));
insert into eg_roleaction (roleid, actionid) values ((select id from eg_role where name = 'Works Creator'),(select id from eg_action where name ='WorksRevisionEstimateSearchActivities' and contextroot = 'egworks'));

-----------------Role action mappings to get search activities form for Revision Estimate----------------
insert into EG_ACTION (ID,NAME,URL,QUERYPARAMS,PARENTMODULE,ORDERNUMBER,DISPLAYNAME,ENABLED,CONTEXTROOT,VERSION,CREATEDBY,CREATEDDATE,LASTMODIFIEDBY,LASTMODIFIEDDATE,APPLICATION) values (NEXTVAL('SEQ_EG_ACTION'),'WorksRevisionEstimateSearchActivitiesForm','/revisionestimate/searchactivityform',null,(select id from EG_MODULE where name = 'WorksRevisionEstimate'),1,'Search Activities for Revision Estimate Form','false','egworks',0,1,now(),1,now(),(select id from eg_module  where name = 'Works Management'));
insert into eg_roleaction (roleid, actionid) values ((select id from eg_role where name = 'Super User'),(select id from eg_action where name ='WorksRevisionEstimateSearchActivitiesForm' and contextroot = 'egworks'));
insert into eg_roleaction (roleid, actionid) values ((select id from eg_role where name = 'Works Creator'),(select id from eg_action where name ='WorksRevisionEstimateSearchActivitiesForm' and contextroot = 'egworks'));

--rollback delete FROM EG_ROLEACTION WHERE roleid = (SELECT id FROM eg_role WHERE name = 'Works Creator') and actionid = (SELECT id FROM eg_action WHERE name ='WorksRevisionEstimateSearchActivitiesForm' and contextroot = 'egworks');
--rollback delete FROM EG_ROLEACTION WHERE roleid = (SELECT id FROM eg_role WHERE name = 'Super User') and actionid = (SELECT id FROM eg_action WHERE name ='WorksRevisionEstimateSearchActivitiesForm' and contextroot = 'egworks');
--rollback delete FROM EG_ACTION WHERE name = 'WorksRevisionEstimateSearchActivitiesForm' and contextroot = 'egworks';

--rollback delete FROM EG_ROLEACTION WHERE roleid = (SELECT id FROM eg_role WHERE name = 'Works Creator') and actionid = (SELECT id FROM eg_action WHERE name ='WorksRevisionEstimateSearchActivities' and contextroot = 'egworks');
--rollback delete FROM EG_ROLEACTION WHERE roleid = (SELECT id FROM eg_role WHERE name = 'Super User') and actionid = (SELECT id FROM eg_action WHERE name ='WorksRevisionEstimateSearchActivities' and contextroot = 'egworks');
--rollback delete FROM EG_ACTION WHERE name = 'WorksRevisionEstimateSearchActivities' and contextroot = 'egworks';