package br.com.sicape.api.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sicape.api.domain.entity.GroupParticipantMembership;

public interface GroupParticipantMembershipRepository extends BaseRepository<GroupParticipantMembership> {
    Optional<GroupParticipantMembership> findByGroupIdAndConvictedIdAndEndDateIsNull(
        Long groupId,
        Long convictedId
    );

    Optional<GroupParticipantMembership> findTopByGroupIdAndConvictedIdOrderByStartDateDesc(
        Long groupId,
        Long convictedId
    );

    @Query("""
        select m from GroupParticipantMembership m
        where m.group.uuid = :groupUuid
          and m.convicted.uuid = :convictedUuid
          and m.startDate <= :date
          and (m.endDate is null or m.endDate >= :date)
        order by m.startDate desc
        """)
    List<GroupParticipantMembership> findMembershipAtDate(
        @Param("groupUuid") UUID groupUuid,
        @Param("convictedUuid") UUID convictedUuid,
        @Param("date") java.time.LocalDate date
    );

    @EntityGraph(attributePaths = "convicted")
    @Query("""
        select m from GroupParticipantMembership m
        where m.group.uuid = :groupUuid
        order by m.startDate, m.convicted.name
        """)
    List<GroupParticipantMembership> findAllByGroupUuid(@Param("groupUuid") UUID groupUuid);
}
