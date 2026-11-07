package vn.edu.davs.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "userworkgroups")
public class UserWorkgroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getCreatedAt() { return createdAt; }
}
