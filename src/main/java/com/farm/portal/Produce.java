package com.farm.portal;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class Produce {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String farm;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate harvestDate;
    @Enumerated(EnumType.STRING)
    private Status status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFarm() { return farm; }
    public void setFarm(String farm) { this.farm = farm; }
    public LocalDate getHarvestDate() { return harvestDate; }
    public void setHarvestDate(LocalDate d) { this.harvestDate = d; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
