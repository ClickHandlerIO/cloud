package move.sql;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class ZonedDateSerializer extends JsonSerializer<ZonedDate> {
   @Override
   public void serialize(ZonedDate zonedDate, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
      jsonGenerator.writeString(zonedDate.getDate() == null ? null : zonedDate.getDate().toString() + "Z");
   }
}
