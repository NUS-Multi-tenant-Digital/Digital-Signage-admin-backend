package com.digitalsignage.admin.playlist.repository;

import com.digitalsignage.admin.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    List<Playlist> findByOrganization_IdOrderByUpdatedAtDesc(Long organizationId);

    Optional<Playlist> findByIdAndOrganization_Id(Long id, Long organizationId);
}
