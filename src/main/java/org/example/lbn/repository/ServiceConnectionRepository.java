package org.example.lbn.repository;


import org.example.lbn.entity.ServiceConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ServiceConnectionRepository extends JpaRepository<ServiceConnection, Integer> {
}
