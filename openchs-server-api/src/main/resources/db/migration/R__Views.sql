DROP VIEW IF EXISTS mother_program_enrolments;
CREATE OR REPLACE VIEW mother_program_enrolments AS
  SELECT
    individual.id                                                                     individual,
    min(catchment.id)                                                                 catchment,
    program_enrolment.id                                                              enrolment,
    program_enrolment.enrolment_date_time                                             internal_enrolment_date,
    to_char(program_enrolment.enrolment_date_time, 'DD-Mon-YYYY')                     enrolment_date,
    program_enrolment.program_exit_date_time                                          internal_exit_date,
    to_char(program_enrolment.program_exit_date_time, 'DD-Mon-YYYY')                  exit_date,
    individual.date_of_birth                                                          internal_date_of_birth,
    to_char(individual.date_of_birth, 'DD-Mon-YYYY')                                  date_of_birth,
    min(address_level.id)                                                             address,
    to_char(date_obs(program_enrolment, 'Last Menstrual Period'), 'DD-Mon-YYYY')      LMP,
    date_obs(program_enrolment, 'Estimated Date of Delivery')                         internal_edd,
    to_char(date_obs(program_enrolment, 'Estimated Date of Delivery'), 'DD-Mon-YYYY') EDD,
    coded_obs_exists(program_enrolment, 'High Risk Conditions')                       is_high_risk,
    coded_obs(program_enrolment, 'High Risk Conditions')                              high_risk_conditions,
    count(program_encounter)                                                          number_of_visits,
    sum(is_overdue_visit(program_encounter))                                          number_of_overdue_visits
  FROM program_enrolment
    INNER JOIN individual ON program_enrolment.individual_id = individual.id
    INNER JOIN address_level ON address_level.id = individual.address_id
    INNER JOIN catchment_address_mapping ON catchment_address_mapping.addresslevel_id = address_level.id
    INNER JOIN catchment ON catchment_address_mapping.catchment_id = catchment.id
    LEFT OUTER JOIN program_encounter ON program_enrolment.id = program_encounter.program_enrolment_id
  GROUP BY individual.id, program_enrolment.id;

DROP VIEW IF EXISTS mother_program_encounters;
CREATE OR REPLACE VIEW mother_program_encounters AS
  SELECT
    individual.id                                                                     individual,
    to_char(individual.date_of_birth, 'DD-Mon-YYYY')                                  date_of_birth,
    individual.date_of_birth                                                          internal_date_of_birth,
    address_level.id                                                                  address,
    program_enrolment.id                                                              enrolment_id,
    program_enrolment.enrolment_date_time                                             internal_enrolment_date,
    to_char(program_enrolment.enrolment_date_time, 'DD-Mon-YYYY')                     enrolment_date,
    to_char(date_obs(program_enrolment, 'Last Menstrual Period'), 'DD-Mon-YYYY')      LMP,
    to_char(date_obs(program_enrolment, 'Estimated Date of Delivery'), 'DD-Mon-YYYY') EDD,
    program_encounter.id                                                              visit,
    program_encounter.scheduled_date_time                                             internal_scheduled_visit_date,
    to_char(program_encounter.scheduled_date_time, 'DD-Mon-YYYY')                     scheduled_visit_date,
    encounter_type.id                                                                 visit_type,
    program_encounter.name                                                            visit_name,
    program_encounter.encounter_date_time                                             internal_visit_date,
    to_char(program_encounter.encounter_date_time, 'DD-Mon-YYYY')                     visit_date,
    coded_obs_exists(program_enrolment, 'High Risk Conditions')                       is_high_risk,
    coded_obs(program_enrolment, 'High Risk Conditions')                              high_risk_conditions,
    catchment.id                                                                      catchment
  FROM program_encounter
    INNER JOIN program_enrolment ON program_encounter.program_enrolment_id = program_enrolment.id
    INNER JOIN encounter_type ON program_encounter.encounter_type_id = encounter_type.id
    INNER JOIN individual ON program_enrolment.individual_id = individual.id
    INNER JOIN address_level ON address_level.id = individual.address_id
    INNER JOIN catchment_address_mapping ON catchment_address_mapping.addresslevel_id = address_level.id
    INNER JOIN catchment ON catchment_address_mapping.catchment_id = catchment.id;

-- <Common>
DROP VIEW IF EXISTS checklist_items;
CREATE OR REPLACE VIEW checklist_items AS
  SELECT
    individual.id                                                    individual,
    catchment.id                                                     catchment,
    program_enrolment.id                                             enrolment,
    program_enrolment.enrolment_date_time                            internal_enrolment_date,
    to_char(program_enrolment.enrolment_date_time, 'DD-Mon-YYYY')    enrolment_date,
    program_enrolment.program_exit_date_time                         internal_exit_date,
    to_char(program_enrolment.program_exit_date_time, 'DD-Mon-YYYY') exit_date,
    individual.date_of_birth                                         internal_date_of_birth,
    to_char(individual.date_of_birth, 'DD-Mon-YYYY')                 date_of_birth,
    address_level.id                                                 address,
    checklist_item.id                                                checklist_item_id,
    to_char(checklist_item.completion_date, 'DD-Mon-YYYY')           completion_date,
    to_char(checklist_item.due_date, 'DD-Mon-YYYY')                  due_date,
    to_char(checklist_item.max_date, 'DD-Mon-YYYY')                  max_date,
    concept.id                                                       checklist_item,
    checklist.id                                                     checklist,
    checklist_item.completion_date                                   internal_completion_date,
    checklist_item.due_date                                          internal_due_date,
    checklist_item.max_date                                          internal_max_date
  FROM checklist_item
    INNER JOIN concept ON checklist_item.concept_id = concept.id
    INNER JOIN checklist ON checklist.id = checklist_item.checklist_id
    INNER JOIN program_enrolment ON program_enrolment.id = checklist.program_enrolment_id
    INNER JOIN individual ON program_enrolment.individual_id = individual.id
    INNER JOIN address_level ON address_level.id = individual.address_id
    INNER JOIN catchment_address_mapping ON catchment_address_mapping.addresslevel_id = address_level.id
    INNER JOIN catchment ON catchment_address_mapping.catchment_id = catchment.id;
-- </Common>