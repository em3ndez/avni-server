package org.openchs.web;

import org.hibernate.Hibernate;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openchs.application.Form;
import org.openchs.application.FormElement;
import org.openchs.application.FormElementGroup;
import org.openchs.application.FormMapping;
import org.openchs.common.AbstractControllerIntegrationTest;
import org.openchs.dao.ConceptRepository;
import org.openchs.dao.application.FormElementGroupRepository;
import org.openchs.dao.application.FormElementRepository;
import org.openchs.dao.application.FormMappingRepository;
import org.openchs.dao.application.FormRepository;
import org.openchs.domain.Concept;
import org.openchs.domain.ConceptAnswer;
import org.openchs.framework.security.AuthenticationFilter;
import org.openchs.web.request.ConceptContract;
import org.openchs.web.request.application.FormContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Sql({"/test-data.sql"})
public class FormControllerIntegrationTest extends AbstractControllerIntegrationTest {
    @Autowired
    private FormMappingRepository formMappingRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private FormElementRepository formElementRepository;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private FormElementGroupRepository formElementGroupRepository;

    private Object getJson(String path) throws IOException {
        return mapper.readValue(this.getClass().getResource(path), Object.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        post("/programs", getJson("/ref/program.json"));
        post("/concepts", getJson("/ref/concepts.json"));
        post("/forms", getJson("/ref/forms/originalForm.json"));
    }

    @After
    public void tearDown() throws Exception {
        template.getRestTemplate().setInterceptors(
                Collections.singletonList((request, body, execution) -> {
                    request.getHeaders()
                            .add(AuthenticationFilter.ORGANISATION_NAME_HEADER, "OpenCHS");
                    return execution.execute(request, body);
                }));
    }

    @Test
    public void findByEntityId() {
        Page<FormMapping> fmPage = formMappingRepository.findByEntityId(1L, new PageRequest(0, 1));
        assertEquals(1, fmPage.getContent().size());
    }

    @Test
    public void getForms() {
        ResponseEntity<String> response = template.getForEntity("/forms/program/1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).startsWith("[{\"name\":\"encounter_form\",\"uuid\":\"2c32a184-6d27-4c51-841d-551ca94594a5\",\"formType\":\"Encounter\",\"programName\":\"Diabetes\"");
        assertThat(response.getBody()).contains("\"links\":[{\"rel\":\"self\"");
        assertThat(response.getBody()).contains("{\"rel\":\"form\"");
        assertThat(response.getBody()).contains("{\"rel\":\"formElementGroups\"");
        assertThat(response.getBody()).contains("{\"rel\":\"createdBy\"");
        assertThat(response.getBody()).contains("{\"rel\":\"lastModifiedBy\"");
    }

    @Test
    @Ignore("Not Applicable as coded-concepts are created/updated by concept API")
    public void renameOfAnswersViaFormElements() throws IOException {
        ResponseEntity<FormContract> formResponse = template
                .getForEntity(String.format("/forms/export?formUUID=%s", "0c444bf3-54c3-41e4-8ca9-f0deb8760831"),
                        FormContract.class);
        assertEquals(HttpStatus.OK, formResponse.getStatusCode());
        FormContract form = formResponse.getBody();
        List<ConceptContract> answers = form.getFormElementGroups().get(0).getFormElements().get(0).getConcept().getAnswers();
        for (ConceptContract answer : answers) {
            if (answer.getUuid().equals("28e76608-dddd-4914-bd44-3689eccfa5ca")) {
                assertEquals("Yes, started", answer.getName());
            }
        }
        post("/forms", getJson("/ref/forms/formWithRenamedAnswer.json"));
        formResponse = template
                .getForEntity(String.format("/forms/export?formUUID=%s", "0c444bf3-54c3-41e4-8ca9-f0deb8760831"),
                        FormContract.class);
        assertEquals(HttpStatus.OK, formResponse.getStatusCode());
        form = formResponse.getBody();
        answers = form.getFormElementGroups().get(0).getFormElements().get(0).getConcept().getAnswers();
        for (ConceptContract answer : answers) {
            if (answer.getUuid().equals("28e76608-dddd-4914-bd44-3689eccfa5ca")) {
                assertEquals("Yes Started", answer.getName());
            }
        }
    }

    @Test
    @Ignore("Not Applicable as coded-concepts are created/updated by concept API")
    public void deleteExistingAnswerInFormElement() throws IOException {
        post("/forms", getJson("/ref/forms/originalForm.json"));

        Concept conceptWithAnswerToBeDeleted = conceptRepository.findByName("Have you started going to school once again");
        assertThat(conceptWithAnswerToBeDeleted.getConceptAnswers().size()).isEqualTo(3);
        ConceptAnswer answerToBeDeleted = conceptWithAnswerToBeDeleted.findConceptAnswerByName("Yes, started");
        assertThat(answerToBeDeleted).isNotNull();
        assertThat(answerToBeDeleted.isVoided()).isFalse();


        post("/forms", getJson("/ref/forms/formWithDeletedAnswer.json"));

        Concept conceptWithDeletedAnswer = conceptRepository.findByName("Have you started going to school once again");
        assertThat(conceptWithDeletedAnswer.getConceptAnswers().size()).isEqualTo(3);
        ConceptAnswer answerDeleted = conceptWithDeletedAnswer.findConceptAnswerByName("Yes, started");
        assertThat(answerDeleted).isNotNull();
        assertThat(answerDeleted.isVoided()).isTrue();
    }

    @Test
    public void deleteFormElement() throws IOException {
        post("/forms", getJson("/ref/forms/originalForm.json"));

        assertThat(formElementRepository.findByUuid("45a1595c-c324-4e76-b8cd-0873e465b5ae")).isNotNull();

        post("/forms", getJson("/ref/forms/formWithDeletedFormElement.json"));

        assertThat(formElementRepository.findByUuid("45a1595c-c324-4e76-b8cd-0873e465b5ae").isVoided()).isTrue();
    }


    @Test
    public void deleteFormElementGroup() throws IOException {
        post("/forms", getJson("/ref/forms/originalForm.json"));
        assertThat(formElementGroupRepository.findByUuid("e47c7604-6cb6-45bb-a36a-05a03f958cdf")).isNotNull();


        post("/forms", getJson("/ref/forms/formWithDeletedFormElementGroup.json"));
        assertThat(formElementGroupRepository.findByUuid("e47c7604-6cb6-45bb-a36a-05a03f958cdf").isVoided()).isTrue();
    }

    @Test
    @Ignore("Not Applicable as coded-concepts are created/updated by concept API")
    public void changingOrderOfAllAnswers() throws IOException {
        Concept codedConcept = conceptRepository.findByUuid("dcfc771a-0785-43be-bcb1-0d2755793e0e");
        Set<ConceptAnswer> conceptAnswers = codedConcept.getConceptAnswers();
        conceptAnswers.forEach(conceptAnswer -> {
            switch (conceptAnswer.getOrder()) {
                case 1:
                    assertEquals("28e76608-dddd-4914-bd44-3689eccfa5ca", conceptAnswer.getAnswerConcept().getUuid());
                    break;
                case 2:
                    assertEquals("9715936e-03f2-44da-900f-33588fe95250", conceptAnswer.getAnswerConcept().getUuid());
                    break;
                case 3:
                    assertEquals("e7b50c78-3d90-484d-a224-9887887780dc", conceptAnswer.getAnswerConcept().getUuid());
                    break;
            }

        });
        post("/forms", getJson("/ref/forms/formWithChangedAnswerOrder.json"));
        codedConcept = conceptRepository.findByUuid("dcfc771a-0785-43be-bcb1-0d2755793e0e");
        conceptAnswers = codedConcept.getConceptAnswers();
        conceptAnswers.forEach(conceptAnswer -> {
            switch (conceptAnswer.getOrder()) {
                case 1:
                    assertEquals("9715936e-03f2-44da-900f-33588fe95250", conceptAnswer.getAnswerConcept().getUuid());
                    break;
                case 2:
                    assertEquals("28e76608-dddd-4914-bd44-3689eccfa5ca", conceptAnswer.getAnswerConcept().getUuid());
                    break;
                case 3:
                    assertEquals("e7b50c78-3d90-484d-a224-9887887780dc", conceptAnswer.getAnswerConcept().getUuid());
                    break;
            }

        });
    }

    @Test
    public void abilityToAddFormElementsForOnlyAnOrganisationToAnExistingFormElementGroup() throws IOException {
        post("/forms", getJson("/ref/demo/originalForm.json"));
        FormElementGroup formElementGroup = formElementGroupRepository.findByUuid("dd37cacf-c628-457e-b474-01c4966a473c");
        Hibernate.initialize(formElementGroup.getFormElements());
        assertEquals(2, formElementGroup.getFormElements().size());
        template.getRestTemplate().setInterceptors(
                Collections.singletonList((request, body, execution) -> {
                    request.getHeaders()
                            .add(AuthenticationFilter.ORGANISATION_NAME_HEADER, "demo");
                    return execution.execute(request, body);
                }));
        template.patchForObject("/forms", getJson("/ref/demo/originalFormChanges.json"), Void.class, new Object());
        formElementGroup = formElementGroupRepository.findByUuid("dd37cacf-c628-457e-b474-01c4966a473c");
        assertEquals(3, formElementGroup.getFormElements().size());
        List<FormElement> formElements = new ArrayList<>(formElementGroup.getFormElements());
        formElements.sort(Comparator.comparingDouble(FormElement::getDisplayOrder));
        FormElement addedFormElement = formElements.get(1);
        assertEquals("Additional Form Element", addedFormElement.getName());
        assertEquals("836ceda5-3d09-49f6-aa0b-28512bc2028a", addedFormElement.getUuid());
        assertEquals(1.1, addedFormElement.getDisplayOrder().doubleValue(), 0);
        assertEquals("43124c33-898d-42fa-a53d-b4c6fa36c581", addedFormElement.getConcept().getUuid());
        assertEquals(2, addedFormElement.getOrganisationId().intValue());
    }

    @Test
    public void abilityToAddFormElementGroupForOnlyAnOrganisationToAnExistingFormElementGroup() throws IOException {
        post("/forms", getJson("/ref/demo/originalForm.json"));
        Form form = formRepository.findByUuid("0c444bf3-54c3-41e4-8ca9-f0deb8760831");
        Hibernate.initialize(form.getFormElementGroups());
        assertEquals(2, form.getFormElementGroups().size());
        template.getRestTemplate().setInterceptors(
                Collections.singletonList((request, body, execution) -> {
                    request.getHeaders()
                            .add(AuthenticationFilter.ORGANISATION_NAME_HEADER, "demo");
                    return execution.execute(request, body);
                }));
        template.patchForObject("/forms", getJson("/ref/demo/originalNewFormElementGroup.json"), Void.class, new Object());
        form = formRepository.findByUuid("0c444bf3-54c3-41e4-8ca9-f0deb8760831");
        assertEquals(3, form.getFormElementGroups().size());
        ArrayList<FormElementGroup> formElementGroups = new ArrayList<>(form.getFormElementGroups());
        formElementGroups.sort(Comparator.comparingDouble(FormElementGroup::getDisplayOrder));
        FormElementGroup formElementGroup = formElementGroups.get(1);
        assertEquals("Additional Form Element Group", formElementGroup.getName());
        assertEquals("118a1605-c7df-4942-9666-4c17af585d24", formElementGroup.getUuid());
        assertEquals(1.1, formElementGroup.getDisplayOrder().doubleValue(), 0);
        assertEquals(2, formElementGroup.getOrganisationId().intValue());
    }

    @Test
    @Ignore
    public void getAllApplicableFormElementGroupsAndFormElementsForAnOrganisation() throws IOException {
        post("/forms", getJson("/ref/demo/originalForm.json"));
        Form form = formRepository.findByUuid("0c444bf3-54c3-41e4-8ca9-f0deb8760831");
        Hibernate.initialize(form.getFormElementGroups());
        assertEquals(2, form.getFormElementGroups().size());
        template.getRestTemplate().setInterceptors(
                Collections.singletonList((request, body, execution) -> {
                    request.getHeaders()
                            .add(AuthenticationFilter.ORGANISATION_NAME_HEADER, "demo");
                    return execution.execute(request, body);
                }));
        template.patchForObject("/forms", getJson("/ref/demo/originalNewFormElementGroup.json"), Void.class, new Object());
        form = formRepository.findByUuid("0c444bf3-54c3-41e4-8ca9-f0deb8760831");
        assertEquals(3, form.getFormElementGroups().size());
        ArrayList<FormElementGroup> formElementGroups = new ArrayList<>(form.getFormElementGroups());
        formElementGroups.sort(Comparator.comparingDouble(FormElementGroup::getDisplayOrder));
        FormElementGroup formElementGroup = formElementGroups.get(1);
        assertEquals("Additional Form Element Group", formElementGroup.getName());
        assertEquals("118a1605-c7df-4942-9666-4c17af585d24", formElementGroup.getUuid());
        assertEquals(1.1, formElementGroup.getDisplayOrder().doubleValue(), 0);
        assertEquals(2, formElementGroup.getOrganisationId().intValue());
        MultiValueMap<String, String> uriParams = new LinkedMultiValueMap<>();
        uriParams.put("catchmentId", Arrays.asList("1"));
        uriParams.put("lastModifiedDateTime", Arrays.asList("1900-01-01T00:00:00.001Z"));
        uriParams.put("size", Arrays.asList("100"));
        uriParams.put("page", Arrays.asList("0"));
        String path = UriComponentsBuilder.fromPath("/form/search/lastModified").queryParams(uriParams).toUriString();
        ResponseEntity<LinkedHashMap> formResponse = template.getForEntity(path, LinkedHashMap.class, uriParams);
        assertEquals(HttpStatus.OK, formResponse.getStatusCode());
        LinkedHashMap<String, LinkedHashMap<String, List<LinkedHashMap<String, List>>>> formBody = formResponse.getBody();
        assertEquals(4, formBody.get("_embedded").get("form").get(0).get("allFormElements").size());
        template.getRestTemplate().setInterceptors(
                Collections.singletonList((request, body, execution) -> {
                    request.getHeaders()
                            .add(AuthenticationFilter.ORGANISATION_NAME_HEADER, "a-demo");
                    return execution.execute(request, body);
                }));
        formResponse = template.getForEntity(path, LinkedHashMap.class, uriParams);
        assertEquals(HttpStatus.OK, formResponse.getStatusCode());
        assertEquals(3, formBody.get("_embedded").get("form").get(0).get("allFormElements").size());
    }

    @Test
    public void addConceptAsPartOfFormElement() throws IOException {
        Object json = getJson("/ref/forms/formElementWithConceptDefinedAlong.json");
        post("/forms", json);
        assertThat(conceptRepository.findByUuid("79604c31-4fa5-4300-a460-9328d8b6217e").getUnit()).isEqualTo("mg/ml");
    }
}