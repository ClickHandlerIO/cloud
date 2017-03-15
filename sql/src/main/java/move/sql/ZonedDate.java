package move.sql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.*;

@JsonSerialize(using = ZonedDateSerializer.class)
@JsonDeserialize(using = ZonedDateDeserializer.class)
public class ZonedDate {
   @NoColumn
   @JsonIgnore
   private boolean inited = false;

   @Column(embeddedName = true)
   @JsonIgnore
   private LocalDateTime utc;

   @NoColumn
   @JsonIgnore
   private String zone;

   @NoColumn
   @JsonIgnore
   private ZonedDateTime local;

   /**
    * New ZonedDate in UTC
    */
   public ZonedDate() {
      this.utc = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
      this.zone = "UTC";
      update();
   }

   /**
    * New ZonedDate in the zone specified.
    * @param zone
    */
   public ZonedDate(String zone) {
      this.utc = ZonedDateTime.now(ZoneId.of(zone)).toLocalDateTime();
      this.zone = zone;
      update();
   }

   /**
    * New ZonedDate from a java LocalDateTime.  Value passed in is assumed to be in UTC.
    * @param utc
    */
   public ZonedDate(LocalDateTime utc) {
      this.utc = utc;
      this.zone = "UTC";
      update();
   }

   /**
    * New ZonedDate with supplied date, time and zone details.
    * @param year
    * @param monthOfYear
    * @param dayOfMonth
    * @param hourOfDay
    * @param minuteOfHour
    * @param zone
    */
   public ZonedDate(int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, String zone) {
      this.utc = LocalDateTime.of(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, 0, 0);
      this.zone = zone;
      update();
   }


   // Getters & Setters
   public LocalDateTime getUtc() {
      return utc;
   }

   public void setUtc(LocalDateTime utc) throws Exception {
      if (this.utc != null && !zone.equalsIgnoreCase("UTC")) {
         throw new Exception("Cannot set date after it has been set as something other than UTC");
      }

      this.utc = utc;
      update();
   }

   public String getZone() {
      return zone;
   }

   public ZonedDate setZone(String zone) {
      this.zone = zone;
      update();

      return this;
   }


   // Converters to the timezone
   public ZonedDateTime asZonedDateTime() {
      return local;
   }

   public LocalDateTime asLocalDateTime() {
      return local.toLocalDateTime();
   }

   public LocalDate asLocalDate() {
      return local.toLocalDate();
   }

   public LocalTime asLocalTime() {
      return local.toLocalTime();
   }


   // Helper Functions
   public ZonedDate startOfDay() {
      return this.withHour(0).withMinute(0).withSecond(0).withHour(0);
   }

   public ZonedDate endOfDay() {
      return this.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
   }

   public ZonedDate max() {
      this.utc = (LocalDateTime.MAX.atZone(ZoneId.of(zone)).toLocalDateTime());
      update();

      return this;
   }

   public ZonedDate min() {
      this.utc = (LocalDateTime.MIN.atZone(ZoneId.of(zone)).toLocalDateTime());
      update();

      return this;
   }

   public ZonedDate plusMonths(int months) {
      this.utc = this.utc.plusMonths(months);
      update();

      return this;
   }

   public ZonedDate minusMonths(int months) {
      this.utc = this.utc.minusMonths(months);
      update();

      return this;
   }

   public ZonedDate plusDays(int days) {
      this.utc = this.utc.plusDays(days);
      update();

      return this;
   }

   public ZonedDate minusDays(int days) {
      this.utc = this.utc.minusDays(days);
      update();

      return this;
   }

   public ZonedDate plusHours(long hours) {
      this.utc = this.utc.plusHours(hours);
      update();

      return this;
   }

   public ZonedDate minusHours(int hours) {
      this.utc = this.utc.minusHours(hours);
      update();

      return this;
   }

   public ZonedDate plusMinutes(long minutes) {
      this.utc = this.utc.plusMinutes(minutes);
      update();

      return this;
   }

   public ZonedDate minusMinutes(long minutes) {
      this.utc = this.utc.minusMinutes(minutes);
      update();

      return this;
   }

   public ZonedDate plusSeconds(long seconds) {
      this.utc = this.utc.plusSeconds(seconds);
      update();

      return this;
   }

   public ZonedDate minusSeconds(long seconds) {
      this.utc = this.utc.minusSeconds(seconds);
      update();

      return this;
   }

   public ZonedDate plusMillis(long millis) {
      long nanos = millis * 1000000;
      this.utc = this.utc.plusNanos(nanos);
      update();

      return this;
   }

   public ZonedDate minusMillis(long millis) {
      long nanos = millis * 1000000;
      this.utc = this.utc.minusNanos(nanos);
      update();

      return this;
   }

   public ZonedDate withTime(int hour, int min, int second) {
      return this.withHour(hour).withMinute(min).withSecond(second);
   }

   public ZonedDate withHour(int hour) {
      LocalDateTime atHour = this.local.toLocalDateTime().withHour(hour).withMinute(0).withSecond(0).withNano(0);
      local = atHour.atZone(ZoneId.of(this.zone));
      this.utc = local.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();

      return this;
   }

   public ZonedDate withMinute(int minute) {
      LocalDateTime atHour = local.toLocalDateTime().withMinute(minute).withSecond(0).withNano(0);
      local = atHour.atZone(ZoneId.of(this.zone));
      this.utc = local.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();

      return this;
   }

   public ZonedDate withSecond(int second) {
      LocalDateTime atHour = local.toLocalDateTime().withSecond(second).withNano(0);
      local = atHour.atZone(ZoneId.of(this.zone));
      this.utc = local.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();

      return this;
   }

   public ZonedDate withNano(int nano) {
      LocalDateTime atHour = local.toLocalDateTime().withNano(nano);
      local = atHour.atZone(ZoneId.of(this.zone));
      this.utc = local.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();

      return this;
   }

   public boolean isBefore(ZonedDate checkDate) {
      return this.getUtc().isBefore(checkDate.getUtc());
   }

   public boolean isAfter(ZonedDate checkDate) {
      return this.getUtc().isAfter(checkDate.getUtc());
   }

   public boolean isSameDay(ZonedDate checkDate) {
      return this.getUtc().toLocalDate().isEqual(checkDate.getUtc().toLocalDate());
   }

   public boolean isNull(){
      return this.getUtc() == null;
   }

   public ZonedDate copy() {
      ZonedDate newDate = new ZonedDate(this.getUtc());
      newDate.setZone(this.zone);
      return newDate;
   }

   private synchronized void update() {
      if (utc == null) {
         local = null;
         return;
      }

      if (zone == null) {
         zone = "UTC";
      }

      if (!inited) {
         // This is first time.
         local = ZonedDateTime.of(utc, ZoneId.of(zone));
         utc = local.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
         inited = true;
      } else {
         ZonedDateTime from = ZonedDateTime.of(utc, ZoneId.of("UTC"));
         local = from.withZoneSameInstant(ZoneId.of(zone));
         utc = local.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
      }
   }
}
