package org.example.lbn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service_connection", schema = "lb")
public class ServiceConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "address", nullable = false, unique = true)
    private String address;

    @Column(name = "type_service", nullable = false)
    private String typeService;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "last_checked")
    private LocalDateTime lastChecked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
