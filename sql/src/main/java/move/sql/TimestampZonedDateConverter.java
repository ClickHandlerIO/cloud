package move.sql;

import java.sql.Timestamp;

public class TimestampZonedDateConverter implements org.jooq.Converter<Timestamp, ZonedDate> {

   @Override
   public ZonedDate from(Timestamp timestamp) {
      return timestamp == null ? null : new ZonedDate(timestamp.toLocalDateTime());
   }

   @Override
   public Timestamp to(ZonedDate zonedDate) {
      return zonedDate == null || zonedDate.getUtc() == null ? null : Timestamp.valueOf(zonedDate.getUtc());
   }

   @Override
   public Class<Timestamp> fromType() {
      return Timestamp.class;
   }

   @Override
   public Class<ZonedDate> toType() {
      return ZonedDate.class;
   }
}
