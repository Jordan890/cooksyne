package com.cartandcook.adapters.persistencejpa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "runtime_config")
@Getter
@Setter
@NoArgsConstructor
public class UserRuntimeConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "runtime_config_seq")
    @SequenceGenerator(name = "runtime_config_seq", sequenceName = "runtime_config_sequence", allocationSize = 50)
    private Long id;

    @Column
    private String dbUrl;

    @Column
    private String dbUsername;

    @Column
    private String dbPassword;

    @Column
    private String lastKnownGoodDbUrl;

    @Column
    private String lastKnownGoodDbUsername;

    @Column
    private String lastKnownGoodDbPassword;

    @Column
    private String oauth2IssuerUri;

    @Column
    private String port;

    @Column
    private Boolean autoRestartOnConfigSave;

    @Column
    private String aiProvider;

    @Column
    private String ollamaBaseUrl;

    @Column
    private String ollamaModel;

    @Column
    private String openAiApiKey;

    @Column
    private String openAiModel;

    @Column
    private String awsRegion;

    @Column
    private String bedrockModelId;

    @Column
    private String huggingFaceApiKey;

    @Column
    private String huggingFaceModel;
}
