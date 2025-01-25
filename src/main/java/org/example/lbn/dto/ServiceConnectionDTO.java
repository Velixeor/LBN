package org.example.lbn.dto;


import lombok.Builder;
import org.example.lbn.entity.ServiceConnection;


@Builder
public record ServiceConnectionDTO(String address,
                                   String typeService,
                                   Integer port,
                                   String path) {

    public static ServiceConnection toEntity(ServiceConnectionDTO serviceConnectionDTO) {
        ServiceConnection serviceConnection = new ServiceConnection();
        serviceConnection.setTypeService(serviceConnectionDTO.typeService());
        serviceConnection.setPort(serviceConnectionDTO.port());
        serviceConnection.setPath(serviceConnectionDTO.path());
        return serviceConnection;
    }
}
