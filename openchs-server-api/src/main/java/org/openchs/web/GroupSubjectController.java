package org.openchs.web;

import org.joda.time.DateTime;
import org.openchs.dao.*;
import org.openchs.domain.GroupRole;
import org.openchs.domain.GroupSubject;
import org.openchs.domain.Individual;
import org.openchs.domain.SubjectType;
import org.openchs.service.IndividualService;
import org.openchs.service.UserService;
import org.openchs.util.BadRequestError;
import org.openchs.web.request.GroupRoleContract;
import org.openchs.web.request.GroupSubjectContract;
import org.openchs.web.request.GroupSubjectMemberContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class GroupSubjectController extends AbstractController<GroupSubject> implements RestControllerResourceProcessor<GroupSubject>, OperatingIndividualScopeAwareFilterController<GroupSubject> {

    private final GroupSubjectRepository groupSubjectRepository;
    private final UserService userService;
    private final IndividualRepository individualRepository;
    private final GroupRoleRepository groupRoleRepository;
    private final SubjectTypeRepository subjectTypeRepository;
    private final IndividualService individualService;
    private final Logger logger;

    @Autowired
    public GroupSubjectController(GroupSubjectRepository groupSubjectRepository, UserService userService, IndividualRepository individualRepository, GroupRoleRepository groupRoleRepository, SubjectTypeRepository subjectTypeRepository, IndividualService individualService) {
        this.groupSubjectRepository = groupSubjectRepository;
        this.userService = userService;
        this.individualRepository = individualRepository;
        this.groupRoleRepository = groupRoleRepository;
        this.subjectTypeRepository = subjectTypeRepository;
        this.individualService = individualService;
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @RequestMapping(value = "/groupSubject", method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('user', 'organisation_admin')")
    public PagedResources<Resource<GroupSubject>> getGroupSubjectsByOperatingIndividualScope(
            @RequestParam("lastModifiedDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime lastModifiedDateTime,
            @RequestParam("now") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime now,
            @RequestParam(value = "subjectTypeUuid", required = false) String groupSubjectTypeUuid,
            Pageable pageable) {
        if (groupSubjectTypeUuid == null || groupSubjectTypeUuid.isEmpty())
            return wrap(new PageImpl<>(Collections.emptyList()));
        SubjectType subjectType = subjectTypeRepository.findByUuid(groupSubjectTypeUuid);
        if(subjectType == null) return wrap(new PageImpl<>(Collections.emptyList()));
        return wrap(getCHSEntitiesForUserByLastModifiedDateTimeAndFilterByType(userService.getCurrentUser(), lastModifiedDateTime, now, subjectType.getId(), pageable));
    }

    @RequestMapping(value = "/groupSubjects", method = RequestMethod.POST)
    @Transactional
    @PreAuthorize(value = "hasAnyAuthority('user', 'organisation_admin')")
    public void save(@RequestBody GroupSubjectContract request) {
        Individual groupSubject = individualRepository.findByUuid(request.getGroupSubjectUUID());
        Individual memberSubject = individualRepository.findByUuid(request.getMemberSubjectUUID());
        GroupRole groupRole = groupRoleRepository.findByUuid(request.getGroupRoleUUID());

        GroupSubject existingOrNewGroupSubject = newOrExistingEntity(groupSubjectRepository, request, new GroupSubject());
        existingOrNewGroupSubject.setGroupSubject(groupSubject);
        existingOrNewGroupSubject.setMemberSubject(memberSubject);
        existingOrNewGroupSubject.setGroupRole(groupRole);
        existingOrNewGroupSubject.setMembershipStartDate(request.getMembershipStartDate());
        existingOrNewGroupSubject.setMembershipEndDate(request.getMembershipEndDate());
        existingOrNewGroupSubject.setVoided(request.isVoided());

        groupSubjectRepository.save(existingOrNewGroupSubject);
    }

    @RequestMapping(value = "/web/groupSubjects/{groupId}/members", method = RequestMethod.GET)
    @Transactional
    @PreAuthorize(value = "hasAnyAuthority('user', 'organisation_admin', 'admin')")
    public List<GroupSubjectMemberContract> getGroupMembers(@PathVariable Long groupId) {
        Optional<Individual> optionalGroup = individualRepository.findById(groupId);
        if (optionalGroup.isPresent()) {
            Individual group =  optionalGroup.get();
            List<GroupSubject> groupSubjects = groupSubjectRepository.findAllByGroupSubject(group);
            return groupSubjects.stream().map(groupSubject -> {
                Individual individual = individualRepository.findByUuid(groupSubject.getMemberSubjectUUID());
                GroupRole groupRole = groupRoleRepository.findByUuid(groupSubject.getGroupRole().getUuid());
                return individualService.createGroupSubjectMemberContract(individual, groupRole);
            }).collect(Collectors.toList());
        } else {
            throw new BadRequestError("Invalid Group Id");
        }
    }

    @RequestMapping(value = "/web/groupSubjects/{groupId}/roles", method = RequestMethod.GET)
    @Transactional
    @PreAuthorize(value = "hasAnyAuthority('user', 'organisation_admin', 'admin')")
    public List<GroupRoleContract> getGroupRoles(@PathVariable Long groupId) {
        Optional<Individual> optionalGroup = individualRepository.findById(groupId);
        if (optionalGroup.isPresent()) {
            Individual group =  optionalGroup.get();
            return groupRoleRepository.findByGroupSubjectType_IdAndIsVoidedFalse(group.getSubjectType().getId())
                    .stream()
                    .map(GroupRoleContract::fromEntity).collect(Collectors.toList());
        } else {
            throw new BadRequestError("Invalid Group Id");
        }
    }

    @Override
    public Resource<GroupSubject> process(Resource<GroupSubject> resource) {
        GroupSubject groupSubject = resource.getContent();
        resource.removeLinks();
        resource.add(new Link(groupSubject.getGroupSubject().getUuid(), "groupSubjectUUID"));
        resource.add(new Link(groupSubject.getMemberSubject().getUuid(), "memberSubjectUUID"));
        resource.add(new Link(groupSubject.getGroupRole().getUuid(), "groupRoleUUID"));
        return resource;
    }

    @Override
    public OperatingIndividualScopeAwareRepositoryWithTypeFilter<GroupSubject> repository() {
        return groupSubjectRepository;
    }
}
