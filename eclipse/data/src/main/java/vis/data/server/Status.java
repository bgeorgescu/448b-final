package vis.data.server;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlRootElement;

public class Status {
    @Path("/")
    public static class Yo {
    	@XmlRootElement
    	static class SomeData {
    		public String member = "foo";
    		public int[] array = new int[5];
    		String hidden = "secret";
    	}
        @GET
        @Produces("application/json")
        public SomeData get() {
            return new SomeData();
        }
    }
    @Path("/heartbeat")
    public static class HeartBeat {
        @GET
        public String get() {
            return "Ker-thump!";
        }
    }
    @RolesAllowed("admin")
    @Path("/protectedbeat")
    public static class ProtectedHeartBeat {
        @GET
        public String get() {
            return "Silent Ker-thump!";
        }
    }
}
