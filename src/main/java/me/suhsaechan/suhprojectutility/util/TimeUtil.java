package me.suhsaechan.suhprojectutility.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  public static final String DATE_TIME_FORMAT_FOR_VALIDATION_ID = "yyyyMMdd_HHmmss";
  public static final String DATE_TIME_MILLIS_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  public static final String TIME_FORMAT = "HH:mm:ss";

  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
  public static final DateTimeFormatter DATE_TIME_FORMATTER_FOR_VALIDATION_ID = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_FOR_VALIDATION_ID);
  public static final DateTimeFormatter DATE_TIME_MILLIS_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_MILLIS_FORMAT);
  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT);

  public static Long getQueryDurationInMillis(LocalDateTime startQueryTime, LocalDateTime endQueryTime) {
    return Duration.between(startQueryTime, endQueryTime).toMillis();
  }

  public static Long getInstantDurationInMillis(Instant startTime, Instant endTime) {
    return Duration.between(startTime, endTime).toMillis();
  }

  public static String formatLocalDateTimeNow() {
    return DATE_TIME_FORMATTER.format(LocalDateTime.now());
  }

  public static String formatLocalDateTimeNowForValidationId() {
    return DATE_TIME_FORMATTER_FOR_VALIDATION_ID.format(LocalDateTime.now());
  }

  public static String formatLocalDateTimeMillisNow() {
    return DATE_TIME_MILLIS_FORMATTER.format(LocalDateTime.now());
  }

  public static String formatLocalDateTimeMillis(LocalDateTime localDateTime) {
    return DATE_TIME_MILLIS_FORMATTER.format(localDateTime);
  }

  public static String formatInstant(Instant instant) {
    return DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
  }

  public static String formatInstantMillis(Instant instant) {
    return DATE_TIME_MILLIS_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
  }

  public static String formatLocalTime(LocalTime localTime) {
    return TIME_FORMATTER.format(localTime);
  }
}
