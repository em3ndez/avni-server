package org.openchs.web.api;

import org.openchs.dao.*;
import org.openchs.domain.EncounterType;
import org.openchs.domain.ProgramEncounter;
import org.openchs.domain.ProgramEnrolment;
import org.openchs.service.ConceptService;
import org.openchs.web.request.api.ApiProgramEncounterRequest;
import org.openchs.web.request.api.RequestUtils;
import org.openchs.web.response.EncounterResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;

@RestController
public class ProgramEncounterApiController {
    private ProgramEncounterRepository programEncounterRepository;
    private ConceptRepository conceptRepository;
    private ConceptService conceptService;
    private ProgramEnrolmentRepository programEnrolmentRepository;
    private EncounterTypeRepository encounterTypeRepository;

    @Autowired
    public ProgramEncounterApiController(ProgramEncounterRepository programEncounterRepository, ConceptRepository conceptRepository, ConceptService conceptService, ProgramEnrolmentRepository programEnrolmentRepository, EncounterTypeRepository encounterTypeRepository) {
        this.programEncounterRepository = programEncounterRepository;
        this.conceptRepository = conceptRepository;
        this.conceptService = conceptService;
        this.programEnrolmentRepository = programEnrolmentRepository;
        this.encounterTypeRepository = encounterTypeRepository;
    }

    @GetMapping(value = "/api/programEncounter/{id}")
    @PreAuthorize(value = "hasAnyAuthority('user')")
    @ResponseBody
    public ResponseEntity<EncounterResponse> get(@PathVariable("id") String uuid) {
        ProgramEncounter programEncounter = programEncounterRepository.findByUuid(uuid);
        if (programEncounter == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(EncounterResponse.fromProgramEncounter(programEncounter, conceptRepository, conceptService), HttpStatus.OK);
    }

    @PostMapping(value = "/api/programEncounter")
    @PreAuthorize(value = "hasAnyAuthority('user')")
    @Transactional
    @ResponseBody
    public ResponseEntity<EncounterResponse> post(@RequestBody ApiProgramEncounterRequest request) {
        ProgramEncounter encounter = new ProgramEncounter();
        encounter.assignUUID();
        updateEncounter(encounter, request);
        return new ResponseEntity<>(EncounterResponse.fromProgramEncounter(encounter, conceptRepository, conceptService), HttpStatus.OK);
    }

    @PutMapping(value = "/api/programEncounter/{id}")
    @PreAuthorize(value = "hasAnyAuthority('user')")
    @Transactional
    @ResponseBody
    public ResponseEntity<EncounterResponse> put(@PathVariable String id, @RequestBody ApiProgramEncounterRequest request) {
        ProgramEncounter encounter = programEncounterRepository.findByUuid(id);
        if (encounter == null) {
            throw new IllegalArgumentException(String.format("Encounter not found with id '%s'", id));
        }
        updateEncounter(encounter, request);
        return new ResponseEntity<>(EncounterResponse.fromProgramEncounter(encounter, conceptRepository, conceptService), HttpStatus.OK);
    }

    private ProgramEncounter updateEncounter(ProgramEncounter encounter, ApiProgramEncounterRequest request) {
        ProgramEnrolment programEnrolment = programEnrolmentRepository.findByUuid(request.getEnrolmentId());
        if (programEnrolment == null) {
            throw new IllegalArgumentException(String.format("Program Enrolment not found with UUID '%s'", request.getEnrolmentId()));
        }

        EncounterType encounterType = encounterTypeRepository.findByName(request.getEncounterType());
        if (encounterType == null) {
            throw new IllegalArgumentException(String.format("Encounter type not found with name '%s'", request.getEncounterType()));
        }

        encounter.setEncounterType(encounterType);
        encounter.setEncounterLocation(request.getEncounterLocation());
        encounter.setCancelLocation(request.getCancelLocation());
        encounter.setEncounterDateTime(request.getEncounterDateTime());
        encounter.setEarliestVisitDateTime(request.getEarliestScheduledDate());
        encounter.setMaxVisitDateTime(request.getMaxScheduledDate());
        encounter.setCancelDateTime(request.getCancelDateTime());
        encounter.setProgramEnrolment(programEnrolment);
        encounter.setObservations(RequestUtils.createObservations(request.getObservations(), conceptRepository));
        encounter.setCancelObservations(RequestUtils.createObservations(request.getCancelObservations(), conceptRepository));

        return programEncounterRepository.save(encounter);
    }
}