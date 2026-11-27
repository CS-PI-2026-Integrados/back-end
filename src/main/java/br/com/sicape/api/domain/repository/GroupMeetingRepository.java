package br.com.sicape.api.domain.repository;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.entity.GroupMeeting;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.JudicialDistrict;

public interface GroupMeetingRepository extends BaseRepository<GroupMeeting> {
    @Query("""
        select new br.com.sicape.api.domain.repository.ConvictedGroupOccurrence(
            m.uuid, m.date, g.name, m.status, p.status
        )
        from GroupMeeting m join m.group g
        left join GroupMeetingPresence p on p.meeting = m and p.convicted = :convicted
        where g.district = :district and m.date >= :from and m.date < :to
          and (g.deleted = false or m.status <> br.com.sicape.api.domain.enums.GroupMeetingStatus.PENDENTE)
          and (
            p.id is not null
            or exists (
                select membership.id from GroupParticipantMembership membership
                where membership.group = g and membership.convicted = :convicted
                  and membership.startDate <= m.date
                  and (membership.endDate is null or membership.endDate >= m.date)
            )
          )
        order by m.date, m.id
        """)
    List<ConvictedGroupOccurrence> findSchedule(
        @Param("convicted") Convicted convicted,
        @Param("district") JudicialDistrict district,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    @EntityGraph(attributePaths = {"presences", "presences.convicted", "presences.membership"})
    List<GroupMeeting> findAllByGroupOrderByDateAscIdAsc(Group group);

    boolean existsByUuidAndGroupNot(UUID uuid, Group group);
}
