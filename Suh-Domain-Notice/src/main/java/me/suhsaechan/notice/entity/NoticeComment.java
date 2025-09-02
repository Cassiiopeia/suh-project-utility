package me.suhsaechan.notice.entity;

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
import lombok.ToString;
import me.suhsaechan.common.entity.BasePostgresEntity;
import me.suhsaechan.common.entity.SuhProjectUtilityNotice;
import me.suhsaechan.common.util.CommonUtil;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
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
    
    private String clientHash;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * IP 주소 익명화 처리
     * @return 익명화된 IP 주소 (마지막 자리 'x'로 처리)
     */
    public String getAnonymizedIp() {
        return CommonUtil.anonymizeIpAddress(authorIp);
    }
}
