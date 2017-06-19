package move.sql;

import java.sql.Date;
import java.time.LocalDate;

public class DateLocalDateConverter implements org.jooq.Converter<Date, LocalDate> {

  @Override
  public LocalDate from(Date date) {
    return date == null ? null : date.toLocalDate();
  }

  @Override
  public Date to(LocalDate localDate) {
    return localDate == null ? null : java.sql.Date.valueOf(localDate);
  }

  @Override
  public Class<Date> fromType() {
    return java.sql.Date.class;
  }

  @Override
  public Class<LocalDate> toType() {
    return LocalDate.class;
  }
}
