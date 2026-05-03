package com.digitalsignage.admin.entity;

import com.digitalsignage.admin.entity.base.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "playlist_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_playlist_item_order",
                columnNames = {"playlist_id", "order_index"}
        )
)
public class PlaylistItem extends BaseAuditableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}
