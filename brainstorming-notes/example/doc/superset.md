templates we will choose from to create dashboard for, with some metadata:

| template_uid 	 | template_code (the one used as   pivot fact_table_{template_code} 	 | template_name_ar                      	 | root_events_count 	 | repeat_events_count 	 |
|----------------|---------------------------------------------------------------------|-----------------------------------------|---------------------|-----------------------|
| LaGeMmmCEtH  	 | iccm_supporting_supervision_501                                   	 | الإشراف الداعم لمتطوعات صحة المجتمع 1 	 | 244               	 | 565                 	 |
| mpmjpJcMuas  	 | iccm_supervision_502                                              	 | الاشراف على المتطوعات 2               	 | 224               	 | 6394                	 |
| Eelt7ZePvz0  	 | lsm                                                               	 | استمارة المكافحة اليرقية              	 | 8960              	 | 5542                	 |
| IP2dMtoJkO4  	 | hf_supervision _physcian_401                                      	 | الاشراف على المرافق الصحية (طبيب)     	 | 103               	 | 320                 	 |
| M3fdtzBSpn8  	 | health_facility_supply_301                                        	 | استمارة الامداد للمرافق الصحية        	 | 169               	 | 640                 	 |
| ck2pHW93sk2  	 | emrgncy_inv_and_resp_phys_201                                     	 | التقصي والاستجابة الطارئة-الطبيب      	 | 3221              	 | 5266                	 |
| BoEmHvJUEpb  	 | outdoor_lsm_103v2                                                 	 | مكافحة المصادر اليرقية خارج المنازل   	 | 554               	 | 1590                	 |
| YLcsWJlB7uy  	 | emrgncy_inv_and_resp_lab_202                                      	 | التقصي والاستجابة الطارئة-المخبري     	 | 328               	 | 16611               	 |
| ONIaOpzoYAe  	 | indoor_ent_surveillance_101                                       	 | استمارة الترصد الحشري داخل المباني    	 | 258               	 | 271                 	 |

* **Primary audience / persona(s)** (operation room, or general for start) — drives KPI choices & layout.
* **Key metrics / KPIs** check the elements and suggest
* **Filters I want on the dashboard**: we mostly have global filters (assigned teams, assigned org_unit, visit day,
  user, user group, activity, date-range based on start_time (added new)).
* **Any lookup/dimension tables** most have been pivoted in same table (gov, district, assigned_org_unit_uid,
  assigned_org_unit_name, assigned_team_uid, assigned_team_code, activity_name, activity_uid, template_uid,
  template_name).
* **Branding / color preferences** (don't care).
* superset version 5.0.0.

* **Which template(s)** We will be working first with template: `Eelt7ZePvz0`:**
    * pivot fact: `fact_lsm`.
    * pivoted canonical elements (safe_name that were used as column
      name):
    ```text
    [breeding_habitat_description, breeding_habitat_type, breeding_habitat_depth_meter, larval_sample_collected,
      breeding_habitats_count, temphos_ml, other_parties_participated, householdname, indoor_lsm_yesno,
      breeding_habitat_width_meter, breedingsources, growth_regulator_grams, other_breeding_habitat, larval_sample_id,
      larval_stage_presence, breeding_habitat_length_meter, presence_of_breeding_sites, lsm_type, visitdate, workdays].
    ```

canonical elements metadata:

| canonical_element_id                 	 | template_uid 	 | repeat_ce_name (event_name) 	 | repeat_ce_id (event_ce_id)           	 | pivot_column_name             	 | name_en                       	 | name_ar                               	 | data_type 	    | semantic_type     	 |
|----------------------------------------|----------------|-------------------------------|----------------------------------------|---------------------------------|---------------------------------|-----------------------------------------|----------------|---------------------|
| 04de8028-5028-35c6-b22e-b714d55c3927 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | breeding_habitat_width_meter  	 | breeding_habitat_width_meter  	 | العرض                                 	 | TEXT      	    | Option            	 |
| 180444e6-4743-3af7-98bc-f756fddd4fe6 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | lsm_type                      	 | lsm_type                      	 | التدخل                                	 | ARRAY     	    | MultiSelectOption 	 |
| 1f15a264-f382-33fd-8dde-4811e9eb3262 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | indoor_lsm_yesno              	 | indoor_lsm_yesno              	 | المصدر داخل منزل                      	 | TEXT      	    | 	                   |
| 3fb227be-22fe-3910-8e6d-158a754898f0 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | larval_sample_collected       	 | larval_sample_collected       	 | تم جمع عينة يرقية                     	 | TEXT      	    | 	                   |
| 57530404-fc47-3c5e-9b59-870e399e2bc1 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | breeding_habitat_length_meter 	 | breeding_habitat_length_meter 	 | الطول                                 	 | TEXT      	    | Option            	 |
| 5a867ebc-9694-3e63-9803-8538d08ebcf5 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | larval_sample_id              	 | larval_sample_id              	 | رقم العينة                            	 | INTEGER   	    | 	                   |
| 5dbd22be-f455-395d-9065-6ed17aab21a1 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | breeding_habitat_type         	 | breeding_habitat_type         	 | نوع مصدر التوالد                      	 | TEXT      	    | Option            	 |
| 79ffacda-1828-3357-b8a5-955db38904a2 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | larval_stage_presence         	 | Larval_stage_presence         	 | الطوراليرقي                           	 | TEXT      	    | 	                   |
| 7e8dc105-0523-3392-9356-09be56d991f2 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | other_parties_participated    	 | other_parties_participated    	 | تم التخلص من المصدر بمشاركة جهات اخرى 	 | TEXT      	    | 	                   |
| 9f0214ed-1f3a-3e7d-9a98-e0eed3d8ffae 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | breeding_habitat_description  	 | breeding_habitat_description  	 | وصف المصدر                            	 | TEXT      	    | 	                   |
| affafd7e-779c-305f-994a-67c7b1b301a3 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | growth_regulator_grams        	 | growth_regulator_grams        	 | مثبط اليرقات (جم)                     	 | INTEGER   	    | 	                   |
| c5f64fef-f794-36f5-a63f-9b8c745ddd62 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | breeding_habitat_depth_meter  	 | breeding_habitat_depth_meter  	 | العمق                                 	 | TEXT      	    | Option            	 |
| c6704758-d0a1-3276-bc22-f68ce7c35d41 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | other_breeding_habitat        	 | other_breeding_habitat        	 | اخرى                                  	 | TEXT      	    | 	                   |
| c8e5c22a-4f97-33f1-a62b-9e1b83d5e899 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | householdname                 	 | Household Name                	 | اسم صاحب المصدر                       	 | TEXT      	    | Name              	 |
| d6119264-07d2-31ca-94c8-9af0b9d46c3a 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | temphos_ml                    	 | temphos_ml                    	 | تيمفوس (مل)                           	 | DECIMAL   	    | 	                   |
| df73b415-bfae-30cc-a962-219d66d56658 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | breeding_habitats_count       	 | breeding_habitats_count       	 | عدد مصادر التوالد                     	 | INTEGER   	    | 	                   |
| f3d1d1c3-8527-3509-9346-7240a679c94f 	 | Eelt7ZePvz0  	 | breedingsources             	 | f3d1d1c3-8527-3509-9346-7240a679c94f 	 | breedingsources               	 | breedingsources               	 | مصادر التوالد                         	 | ARRAY     	    | Repeat            	 |
| a9e8a2ed-37d7-3599-ac2c-d8ce657e5859 	 | Eelt7ZePvz0  	 | 	                             | 	                                      | workdays                      	 | Work Day                      	 | يوم العمل                             	 | TEXT      	    | Option            	 |
| dc8c3785-981e-3533-a02e-153486a74c2e 	 | Eelt7ZePvz0  	 | 	                             | 	                                      | presence_of_breeding_sites    	 | Presence of breeding sites    	 | هل يوجد مصادر للبعوض؟                 	 | Boolean      	 | 	                   |
| e9aa3309-545b-3cae-9027-7a5daac8158a 	 | Eelt7ZePvz0  	 | 	                             | 	                                      | visitdate                     	 | Visit Date                    	 | تاريخ الزيارة                         	 | TIMESTAMP 	    | 	                   |

Options available:

| canonical_element_id                 	 | pivot_column_name             	 | array_agg_set_of_available_options                                                                                                                                                        	 |
|----------------------------------------|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 04de8028-5028-35c6-b22e-b714d55c3927 	 | breeding_habitat_width_meter  	 | {greater_than_ten_meters,less_than_one_meter,one_to_ten_meters}                                                                                                                           	 |
| 57530404-fc47-3c5e-9b59-870e399e2bc1 	 | breeding_habitat_length_meter 	 | {greater_than_ten_meters,"less   than_one_meter",one_to_ten_meters}                                                                                                                       	 |
| 5dbd22be-f455-395d-9065-6ed17aab21a1 	 | breeding_habitat_type         	 | {agricultural_channels,exposed_water_tanks,Household_water_storage_containers,man_made_water_storages,other_habitats,pond,sewage_cesspool,swamp,tires,water_barriers,watercourses_valley} 	 |
| c5f64fef-f794-36f5-a63f-9b8c745ddd62 	 | breeding_habitat_depth_meter  	 | {greater_than_two_meter,half_to_two_meter,less_than_half_meter}                                                                                                                           	 |

Template's element count of non null values (not mentioned means has all values = null):

| canonical_element_id                 	 | safe_name                     	 | template_uid 	 | event_name 	 | non_null_values_count 	 |
|----------------------------------------|---------------------------------|----------------|--------------|-------------------------|
| e9aa3309-545b-3cae-9027-7a5daac8158a 	 | visitdate                     	 | Eelt7ZePvz0  	 | Submission 	 | 4499                  	 |
| 9f0214ed-1f3a-3e7d-9a98-e0eed3d8ffae 	 | breeding_habitat_description  	 | Eelt7ZePvz0  	 | Submission 	 | 2722                  	 |
| affafd7e-779c-305f-994a-67c7b1b301a3 	 | growth_regulator_grams        	 | Eelt7ZePvz0  	 | Submission 	 | 2681                  	 |
| f3d1d1c3-8527-3509-9346-7240a679c94f 	 | breedingsources               	 | Eelt7ZePvz0  	 | Submission 	 | 2733                  	 |
| 7e8dc105-0523-3392-9356-09be56d991f2 	 | other_parties_participated    	 | Eelt7ZePvz0  	 | Submission 	 | 2722                  	 |
| 3fb227be-22fe-3910-8e6d-158a754898f0 	 | larval_sample_collected       	 | Eelt7ZePvz0  	 | Submission 	 | 2482                  	 |
| 1f15a264-f382-33fd-8dde-4811e9eb3262 	 | indoor_lsm_yesno              	 | Eelt7ZePvz0  	 | Submission 	 | 2722                  	 |
| 5a867ebc-9694-3e63-9803-8538d08ebcf5 	 | larval_sample_id              	 | Eelt7ZePvz0  	 | Submission 	 | 294                   	 |
| c8e5c22a-4f97-33f1-a62b-9e1b83d5e899 	 | householdname                 	 | Eelt7ZePvz0  	 | Submission 	 | 403                   	 |
| 79ffacda-1828-3357-b8a5-955db38904a2 	 | larval_stage_presence         	 | Eelt7ZePvz0  	 | Submission 	 | 2722                  	 |
| d6119264-07d2-31ca-94c8-9af0b9d46c3a 	 | temphos_ml                    	 | Eelt7ZePvz0  	 | Submission 	 | 2486                  	 |
| 04de8028-5028-35c6-b22e-b714d55c3927 	 | breeding_habitat_width_meter  	 | Eelt7ZePvz0  	 | Submission 	 | 2431                  	 |
| 5dbd22be-f455-395d-9065-6ed17aab21a1 	 | breeding_habitat_type         	 | Eelt7ZePvz0  	 | Submission 	 | 2722                  	 |
| 180444e6-4743-3af7-98bc-f756fddd4fe6 	 | lsm_type                      	 | Eelt7ZePvz0  	 | Submission 	 | 2698                  	 |
| df73b415-bfae-30cc-a962-219d66d56658 	 | breeding_habitats_count       	 | Eelt7ZePvz0  	 | Submission 	 | 2424                  	 |
| 57530404-fc47-3c5e-9b59-870e399e2bc1 	 | breeding_habitat_length_meter 	 | Eelt7ZePvz0  	 | Submission 	 | 2431                  	 |
| c6704758-d0a1-3276-bc22-f68ce7c35d41 	 | other_breeding_habitat        	 | Eelt7ZePvz0  	 | Submission 	 | 1307                  	 |
| a9e8a2ed-37d7-3599-ac2c-d8ce657e5859 	 | workdays                      	 | Eelt7ZePvz0  	 | Submission 	 | 4499                  	 |
| dc8c3785-981e-3533-a02e-153486a74c2e 	 | presence_of_breeding_sites    	 | Eelt7ZePvz0  	 | Submission 	 | 4336                  	 |
| c5f64fef-f794-36f5-a63f-9b8c745ddd62 	 | breeding_habitat_depth_meter  	 | Eelt7ZePvz0  	 | Submission 	 | 2431                  	 |
