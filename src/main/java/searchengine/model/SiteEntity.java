package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "site")
public class SiteEntity {

    public SiteEntity(String url, String name) {
        this.url = url;
        this.name = name;
        this.status = IndexingStatus.INDEXING;
        this.statusTime = LocalDateTime.now();
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "status", columnDefinition = "ENUM('INDEXING','INDEXED','FAILED')", nullable = false)
    @Enumerated(EnumType.STRING)
    private IndexingStatus status;
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(name="url", columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(name="name", columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

}
