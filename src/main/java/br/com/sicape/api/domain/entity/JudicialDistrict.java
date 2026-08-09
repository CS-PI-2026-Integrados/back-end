package br.com.sicape.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class JudicialDistrict extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;
}