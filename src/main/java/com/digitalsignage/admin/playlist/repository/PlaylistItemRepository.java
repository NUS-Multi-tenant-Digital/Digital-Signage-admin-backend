package com.digitalsignage.admin.playlist.repository;

import com.digitalsignage.admin.entity.PlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {

    boolean existsByMedia_Id(Long mediaId);

    void deleteByPlaylist_Id(Long playlistId);

    List<PlaylistItem> findByPlaylist_IdOrderByOrderIndexAsc(Long playlistId);

    @Query("SELECT pi FROM PlaylistItem pi JOIN FETCH pi.media WHERE pi.playlist.id = :playlistId ORDER BY pi.orderIndex ASC")
    List<PlaylistItem> findWithMediaByPlaylist_Id(@Param("playlistId") Long playlistId);
}
