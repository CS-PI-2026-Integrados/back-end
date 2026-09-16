package br.com.sicape.api.application.group.usecase;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import br.com.sicape.api.application.group.dto.request.CreateGroupRequest;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.enums.GroupFrequency;
import br.com.sicape.api.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class CreateGroupUseCase {
    private final GroupRepository repo;
    
    public Group execute(
        CreateGroupRequest request,
        AuthContext auth
    ) {
        Group group = new Group();

        group.setName(request.name());
        group.setDescription(request.description());
        group.setMinimumMeetingsCount(6);
        group.setTotalMeetingsCounts(8);
        group.setFrequency(GroupFrequency.MONTHLY);
        group.setMeetingBaseTime(LocalTime.now());
        group.setStartDate(LocalDate.now());
        group.setPredictedEndDate(LocalDate.now());
        group.setRealEndDate(LocalDate.now());
        group.setDistrict(auth.district());

        repo.save(group);

        return group;
    }
}
