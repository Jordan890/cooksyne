package com.cooksyne.adapters.persistencejpa;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "external_identities",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"issuer", "subject"})})
@Getter
@Setter
@NoArgsConstructor
public class ExternalIdentityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ext_identity_seq")
    @SequenceGenerator(
            name = "ext_identity_seq",
            sequenceName = "ext_identity_sequence",
            allocationSize = 50
    )
    private Long id;

    @Column(nullable = false)
    private Long userId; // FK to UserEntity.id

    @Column(nullable = false)
    private String issuer;

    @Column(nullable = false)
    private String subject;
}
