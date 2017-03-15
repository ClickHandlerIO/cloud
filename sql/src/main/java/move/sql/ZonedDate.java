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
   private LocalDateTime date;

   @NoColumn
   @JsonIgnore
   private String zone;

   @NoColumn
   @JsonIgnore
   private ZonedDateTime value;

   // Constructors
   public ZonedDate() {
      this.date = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
      this.zone = "UTC";
      update();
   }

   public ZonedDate(String zone) {
      this.date = ZonedDateTime.now(ZoneId.of(zone)).toLocalDateTime();
      this.zone = zone;
      update();
   }

   public ZonedDate(ZonedDateTime date) {
      this.zone = date.getZone().getId();
      this.date = date.toLocalDateTime();
      update();
   }

   public ZonedDate(LocalDateTime date) {
      this.date = date;
      this.zone = "UTC";
      update();
   }

   public ZonedDate(int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, String zone) {
      this.date = LocalDateTime.of(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, 0, 0);
      this.zone = zone;
      update();
   }

   // Getters & Setters
   public LocalDateTime getDate() {
      return date;
   }

   public void setDate(LocalDateTime date) throws Exception {
      if (this.date != null && !zone.equalsIgnoreCase("UTC")) {
         throw new Exception("Cannot set date after it has been set as something other than UTC");
      }

      this.date = date;
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

   public ZonedDateTime getValue() {
      return value;
   }

   public void setValue(ZonedDateTime value) throws Exception {
      if (this.value != null) {
         throw new Exception("Value Cannot Be Changed Once Set!");
      }

      this.value = value;
      update();
   }


   // Converters to the timezone
   public ZonedDateTime asZonedDateTime() {
      return value;
   }

   public LocalDateTime asLocalDateTime() {
      return value.toLocalDateTime();
   }

   public LocalDate asLocalDate() {
      return value.toLocalDate();
   }

   public LocalTime asLocalTime() {
      return value.toLocalTime();
   }


   // Helper Functions
   public ZonedDate startOfDay() {
      return this.withHour(0).withMinute(0).withSecond(0).withHour(0);
   }

   public ZonedDate endOfDay() {
      return this.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
   }

   public ZonedDate max() {
      this.date = (LocalDateTime.MAX.atZone(ZoneId.of(zone)).toLocalDateTime());
      update();

      return this;
   }

   public ZonedDate min() {
      this.date = (LocalDateTime.MIN.atZone(ZoneId.of(zone)).toLocalDateTime());
      update();

      return this;
   }

   public ZonedDate plusMonths(int months) {
      this.date = this.date.plusMonths(months);
      update();

      return this;
   }

   public ZonedDate minusMonths(int months) {
      this.date = this.date.minusMonths(months);
      update();

      return this;
   }

   public ZonedDate plusDays(int days) {
      this.date = this.date.plusDays(days);
      update();

      return this;
   }

   public ZonedDate minusDays(int days) {
      this.date = this.date.minusDays(days);
      update();

      return this;
   }

   public ZonedDate plusHours(long hours) {
      this.date = this.date.plusHours(hours);
      update();

      return this;
   }

   public ZonedDate minusHours(int hours) {
      this.date = this.date.minusHours(hours);
      update();

      return this;
   }

   public ZonedDate plusMinutes(long minutes) {
      this.date = this.date.plusMinutes(minutes);
      update();

      return this;
   }

   public ZonedDate minusMinutes(long minutes) {
      this.date = this.date.minusMinutes(minutes);
      update();

      return this;
   }

   public ZonedDate plusSeconds(long seconds) {
      this.date = this.date.plusSeconds(seconds);
      update();

      return this;
   }

   public ZonedDate minusSeconds(long seconds) {
      this.date = this.date.minusSeconds(seconds);
      update();

      return this;
   }

   public ZonedDate plusMillis(long millis) {
      long nanos = millis * 1000000;
      this.date = this.date.plusNanos(nanos);
      update();

      return this;
   }

   public ZonedDate minusMillis(long millis) {
      long nanos = millis * 1000000;
      this.date = this.date.minusNanos(nanos);
      update();

      return this;
   }

   public ZonedDate withTime(int hour, int min, int second) {
      return this.withHour(hour).withMinute(min).withSecond(second);
   }

   public ZonedDate withHour(int hour) {
      LocalDateTime atHour = this.value.toLocalDateTime().withHour(hour).withMinute(0).withSecond(0).withNano(0);
      value = atHour.atZone(ZoneId.of(this.zone));
      this.date = value.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();

      return this;
   }

   public ZonedDate withMinute(int minute) {
      LocalDateTime atHour = value.toLocalDateTime().withMinute(minute).withSecond(0).withNano(0);
      value = atHour.atZone(ZoneId.of(this.zone));
      this.date = value.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();

      return this;
   }

   public ZonedDate withSecond(int second) {
      LocalDateTime atHour = value.toLocalDateTime().withSecond(second).withNano(0);
      value = atHour.atZone(ZoneId.of(this.zone));
      this.date = value.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();

      return this;
   }

   public ZonedDate withNano(int nano) {
      LocalDateTime atHour = value.toLocalDateTime().withNano(nano);
      value = atHour.atZone(ZoneId.of(this.zone));
      this.date = value.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();

      return this;
   }

   public boolean isBefore(ZonedDate checkDate) {
      return this.getDate().isBefore(checkDate.getDate());
   }

   public boolean isAfter(ZonedDate checkDate) {
      return this.getDate().isAfter(checkDate.getDate());
   }

   public boolean isSameDay(ZonedDate checkDate) {
      return this.getDate().toLocalDate().isEqual(checkDate.getDate().toLocalDate());
   }

   public ZonedDate copy() {
      return new ZonedDate(this.asZonedDateTime());
   }

   private synchronized void update() {
      if (date == null) {
         value = null;
         return;
      }

      if (zone == null) {
         zone = "UTC";
      }

      if (!inited) {
         // This is first time.
         value = ZonedDateTime.of(date, ZoneId.of(zone));
         date = value.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
         inited = true;
      } else {
         ZonedDateTime from = ZonedDateTime.of(date, ZoneId.of("UTC"));
         value = from.withZoneSameInstant(ZoneId.of(zone));
         date = value.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
      }
   }
}
