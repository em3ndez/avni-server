ALTER TABLE individual ADD UNIQUE (uuid);
ALTER TABLE program_enrolment ADD UNIQUE (uuid);
ALTER TABLE program_encounter ADD UNIQUE (uuid);
ALTER TABLE encounter ADD UNIQUE (uuid);
ALTER TABLE checklist ADD UNIQUE (uuid);
ALTER TABLE checklist_item ADD UNIQUE (uuid);
ALTER TABLE address_level ADD UNIQUE (uuid);
ALTER TABLE catchment ADD UNIQUE (uuid);
ALTER TABLE encounter_type ADD UNIQUE (uuid);
ALTER TABLE program ADD UNIQUE (uuid);
ALTER TABLE program_outcome ADD UNIQUE (uuid);
ALTER TABLE users ADD UNIQUE (uuid);