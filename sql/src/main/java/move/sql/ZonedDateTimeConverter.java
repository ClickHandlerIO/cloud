package move.sql;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.jooq.Converter;

public class ZonedDateTimeConverter implements Converter<Timestamp, ZonedDateTime>{

    private static final long serialVersionUID = 1L;

    @Override
    public ZonedDateTime from(final Timestamp timestamp) {
        return ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
    }

    @Override
    public Timestamp to(final ZonedDateTime zonedDateTime) {
        return Timestamp.from(zonedDateTime.toInstant());
    }

    @Override
    public Class<Timestamp> fromType() {
        return Timestamp.class;
    }

    @Override
    public Class<ZonedDateTime> toType() {
        return ZonedDateTime.class;
    }
}
