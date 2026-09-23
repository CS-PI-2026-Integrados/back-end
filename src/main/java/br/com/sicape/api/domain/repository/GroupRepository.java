package br.com.sicape.api.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sicape.api.domain.entity.Group;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.enums.GroupStatus;

public interface GroupRepository extends BaseRepository<Group> {
	@EntityGraph(attributePaths = "presenters")
	@Query(value = """
		select distinct g from Group g
		where g.district = :district
		  and (:name is null or lower(g.name) like lower(concat('%', :name, '%')))
		  and (:subject is null or lower(g.subject) like lower(concat('%', :subject, '%')))
		  and (:status is null or g.status = :status)
		""",
		countQuery = """
		select count(g) from Group g
		where g.district = :district
		  and (:name is null or lower(g.name) like lower(concat('%', :name, '%')))
		  and (:subject is null or lower(g.subject) like lower(concat('%', :subject, '%')))
		  and (:status is null or g.status = :status)
		""")
	Page<Group> search(
		@Param("district") JudicialDistrict district,
		@Param("name") String name,
		@Param("subject") String subject,
		@Param("status") GroupStatus status,
		Pageable pageable
	);

	@Query("""
		select g.uuid as groupUuid, count(c) as participantCount
		from Group g left join g.convicteds c
		where g.uuid in :groupUuids
		group by g.uuid
		""")
	List<GroupParticipantCount> countParticipantsByGroupUuids(@Param("groupUuids") List<UUID> groupUuids);

	@EntityGraph(attributePaths = {"presenters", "convicteds"})
	@Query("select g from Group g where g.uuid = :uuid and g.district = :district")
	Optional<Group> findByUuidAndDistrict(
		@Param("uuid") UUID uuid,
		@Param("district") JudicialDistrict district
	);
}
