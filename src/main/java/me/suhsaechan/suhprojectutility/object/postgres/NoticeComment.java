package me.suhsaechan.suhprojectutility.object.postgres;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notice_comment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeComment extends BasePostgresEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID noticeCommentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @JsonIgnore
    private SuhProjectUtilityNotice notice;

    @Column(nullable = false)
    private String author;
    
    private String authorIp;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * IP 주소 익명화 처리
     * @return 익명화된 IP 주소 (마지막 자리 'x'로 처리)
     */
    public String getAnonymizedIp() {
        if (authorIp == null || authorIp.isEmpty()) {
            return "알 수 없음";
        }
        
        String[] parts = authorIp.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".x";
        }
        
        return authorIp.replaceAll(".$", "x");
    }
} 