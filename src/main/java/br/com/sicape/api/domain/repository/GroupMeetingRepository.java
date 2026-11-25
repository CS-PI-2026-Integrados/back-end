package br.com.sicape.api.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.entity.GroupMeeting;

public interface GroupMeetingRepository extends BaseRepository<GroupMeeting> {
    @EntityGraph(attributePaths = {"presences", "presences.convicted", "presences.membership"})
    List<GroupMeeting> findAllByGroupOrderByDateAscIdAsc(Group group);

    boolean existsByUuidAndGroupNot(UUID uuid, Group group);
}
