package move.remoting;

import move.common.WireFormat;

/**
 *
 */
public class WsEncoding {

  /**
   *
   * @param message
   * @return
   */
  public static WsMessage decode(String message) {
    if (message == null || message.isEmpty()) {
      return null;
    }

    final String headerJson;
    final String body;
    if (message.charAt(0) == '{') {
      headerJson = message;
      body = null;
    } else {
      int headerIndex = message.indexOf('{');
      if (headerIndex < 0 || headerIndex > 6) {
        return null;
      }

      String headerLengthStr = message.substring(0, headerIndex);
      final int headerLength;
      try {
        headerLength = Integer.parseInt(headerLengthStr);
      } catch (Throwable e) {
        return null;
      }

      try {
        headerJson = message.substring(headerIndex, headerIndex + headerLength);
      } catch (Throwable e) {
        return null;
      }

      body = message.substring(headerIndex + headerLength);
    }

    return new WsMessage(WireFormat.parse(WsHeader.class, headerJson), body);
  }

  /**
   *
   * @param message
   * @return
   */
  public static String encode(WsMessage message) {
    if (message == null || message.header == null) {
      return "";
    }

    final String headerJson = WireFormat.stringify(message.header);

    if (message.body == null || message.body.isEmpty() || message.body.equals("{}")) {
      return headerJson;
    }

    return headerJson.length() + headerJson + message.body;
  }
}
