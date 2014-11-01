package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.FilterFeature.HttpResponseValidator;
import org.jooby.Session.Store.SaveReason;
import org.junit.Test;

import com.google.common.collect.Lists;

public class SaveSessionFeature extends ServerFeature {

  private static int index = 0;

  private static final List<SaveReason> reasons = Lists.newArrayList(SaveReason.DIRTY,
      SaveReason.TIME, SaveReason.DIRTY, SaveReason.PRESERVE_ON_STOP);
  {

    use(new Session.Store() {
      @Override
      public void save(final Session session, final SaveReason reason) {
        assertEquals(reasons.get(index++), reason);
      }

      @Override
      public Session get(final String id) {
        return null;
      }

      @Override
      public void delete(final String id) {
      }

      @Override
      public String generateID(final long seed) {
        return "1234";
      }
    }).preserveOnStop(true)
        .saveInterval(2);

    get("/session", (req, res) -> {
      req.session().set("xx", "XX");
      res.send(req.session().id());
    });

    get("/session/1", (req, res) -> {
      String id = req.session().id();
      res.send(id);
    });

  }

  @Test
  public void saveSession() throws Exception {
    String sessionId = "1234|anN8BeWjnfVFT4P/FGkN7YbYAPhfXvTCx7P9CBrPa/s";
    String cookieId = "jooby.sid=" + sessionId + ";Path=/;Secure;HttpOnly";

    assertEquals(
        sessionId,
        execute(
            GET(uri("session")),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
              assertEquals(cookieId, response.getFirstHeader("Set-Cookie").getValue());
            }));

    assertEquals(
        sessionId,
        execute(
            GET(uri("session/1")).addHeader("Cookie", cookieId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    Thread.sleep(2000L);
    assertEquals(
        sessionId,
        execute(
            GET(uri("session/1")).addHeader("Cookie", cookieId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    assertEquals(
        sessionId,
        execute(
            GET(uri("session/1")).addHeader("Cookie", cookieId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    assertEquals(
        sessionId,
        execute(
            GET(uri("session")).addHeader("Cookie", cookieId),
            (response) -> {
              assertEquals(200, response.getStatusLine().getStatusCode());
            }));

    assertEquals(index, reasons.size() - 1);
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }

}