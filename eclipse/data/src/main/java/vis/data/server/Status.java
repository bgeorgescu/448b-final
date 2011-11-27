package vis.data.server;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

public class Status {
    @Path("/heartbeat")
    public static class HeartBeat {
        @GET
        public String get() {
            return "Ker-thump!";
        }
    }
    @RolesAllowed("admin")
    @Path("/heartbeat/protected")
    public static class ProtectedHeartBeat {
        @GET
        public String get() {
            return "Silent Ker-thump!";
        }
    }
}
