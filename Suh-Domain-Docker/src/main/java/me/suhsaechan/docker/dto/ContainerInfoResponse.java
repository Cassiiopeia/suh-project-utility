package me.suhsaechan.docker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInfoResponse {
    private String name;
    private String status; // e.g., Up 3 days, Exited (1) 2 weeks ago
    private boolean running;
} 