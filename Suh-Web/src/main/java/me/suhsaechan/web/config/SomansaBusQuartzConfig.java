package me.suhsaechan.web.config;

import me.suhsaechan.common.constant.ServerOptionKey;
import me.suhsaechan.common.service.ServerOptionService;
import me.suhsaechan.somansabus.job.SomansaBusAutoReservationJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;

@Configuration
public class SomansaBusQuartzConfig {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private ServerOptionService serverOptionService;

  @Bean
  public JobDetail somansaBusJobDetail() {
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("applicationContext", applicationContext);

    return JobBuilder.newJob(SomansaBusAutoReservationJob.class)
        .withIdentity("somansaBusAutoReservationJob", "somansaBus")
        .storeDurably()
        .usingJobData(jobDataMap)
        .build();
  }

  @Bean
  public Trigger somansaBusJobTrigger(JobDetail somansaBusJobDetail) {
    int fromHour;
    int toHour;
    try {
      fromHour = Integer.parseInt(serverOptionService.getOption(ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_FROM).getOptionValue());
      toHour = Integer.parseInt(serverOptionService.getOption(ServerOptionKey.SOMANSA_BUS_SCHEDULER_TIME_TO).getOptionValue());
    } catch (Exception e) {
      fromHour = 22;
      toHour = 23;
    }

    if (fromHour > toHour) toHour = fromHour;
    int rangeMinutes = (toHour - fromHour) * 60 + 59;
    int randomMinutes = new Random().nextInt(rangeMinutes + 1);

    LocalTime nextTime = LocalTime.of(fromHour, 0).plusMinutes(randomMinutes);
    LocalDate tomorrow = LocalDate.now().plusDays(1);

    Date firstFireTime = Date.from(
        tomorrow.atTime(nextTime).atZone(ZoneId.of("Asia/Seoul")).toInstant()
    );

    return TriggerBuilder.newTrigger()
        .withIdentity("somansaBusAutoReservationTrigger", "somansaBus")
        .forJob(somansaBusJobDetail)
        .startAt(firstFireTime)
        .build();
  }
}
