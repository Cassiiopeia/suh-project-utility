package me.suhsaechan.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatDto {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private Long count;
}
