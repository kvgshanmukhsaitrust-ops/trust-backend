package com.trustplatform.applicant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trustplatform.common.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "case_documents")
public class CaseDocument extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    @JsonIgnore
    private AssistanceCase assistanceCase;

    @Column(nullable = false, length = 255)
    private String documentName;

    @Column(nullable = false, length = 1000)
    private String documentUrl;

    @Column(length = 255)
    private String publicId; // Cloudinary publicId for easy deletion

    @Column(length = 100)
    private String fileType;
}
