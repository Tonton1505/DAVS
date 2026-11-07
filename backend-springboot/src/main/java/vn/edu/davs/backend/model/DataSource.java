package vn.edu.davs.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "datasources")
public class DataSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getCreatedAt() { return createdAt; }
}
