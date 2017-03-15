package move.sql;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;

public class ZonedDateDeserializer extends JsonDeserializer<ZonedDate> {
    @Override
    public ZonedDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        String dateText = jsonParser.getText();

        // Only accept dates in ISO format.  TODO: add better validation based on format here maybe?
        if (dateText.length() != 16
            && dateText.length() != 17
           && dateText.length() != 19
           & dateText.length() != 20
           && dateText.length() != 23
           && dateText.length() != 24) {
            throw new IOException("Dates must be properly formed to ISO standard yyyy-MM-dd'T'HH:mm:ss'Z'");
        }

        // If date includes a timezone it must be UTC
        if ((dateText.length() == 17 || dateText.length() == 20 || dateText.length() == 24) && !dateText.endsWith("Z")) {
            throw new IOException("Dates must be confirmed in UTC");
        }

        // Take off the Z
        if (dateText.length() == 17 || dateText.length() == 20) {
            dateText = dateText.substring(0, dateText.length() - 1);
        }

        // Take off the microseconds
        if (dateText.length() == 23) {
            dateText = dateText.substring(0, dateText.length() - 4);
        }

        // Take off the Z and the microseconds.
        if (dateText.length() == 24) {
            dateText = dateText.substring(0, dateText.length() - 5);
        }

        LocalDateTime dateTime = LocalDateTime.parse(dateText);

        return new ZonedDate(dateTime);
    }
}
