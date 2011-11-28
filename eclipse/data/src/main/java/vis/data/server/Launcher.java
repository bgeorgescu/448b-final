package vis.data.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import vis.data.util.SQL.SQLCloseFilter;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class Launcher {
	
//	@Path("/$")
//	public static class Root {
//		@GET
//		public void redirect() {
//			throw new WebApplicationException(Response.seeOther(URI.create("/index.html")).build());
//		}
//	}

	public static void main(String[] args) {
		ServletHolder sh = new ServletHolder(ServletContainer.class);
		sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
		sh.setInitParameter("com.sun.jersey.config.property.packages", "vis.data.server");
		sh.setInitParameter("com.sun.jersey.spi.container.ResourceFilters", "com.sun.jersey.api.container.filter.RolesAllowedResourceFilterFactory");
		sh.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
		
		FilterHolder fh = new FilterHolder(SQLCloseFilter.class);
		EnumSet<DispatcherType> fd = EnumSet.of(DispatcherType.REQUEST);

		ServletHolder ssh = new ServletHolder(DefaultServlet.class);
		
		final int PORT = 8080;
		try {
			//make sure the port is available,jetty won't
			ServerSocket s = new ServerSocket(PORT);
			s.close();
		} catch (IOException e) {
			throw new RuntimeException("port in use " + PORT);
		}

        Server server = new Server(8080);
		ServletContextHandler root = new ServletContextHandler(ServletContextHandler.SESSIONS);
		root.setContextPath("/");
		root.setResourceBase(new File(".").getAbsolutePath());
		//make the default page go to the top level thing
		root.setWelcomeFiles(new String[] { "index.html" } );

		//clean up sql automatically
		root.addFilter(fh, "/", fd);
		root.addFilter(fh, "/*", fd);
		//map api
		root.addServlet(sh, "/");
		root.addServlet(sh, "/*");
		//add static content path
		root.addServlet(ssh, "/index.html");
		root.addServlet(ssh, "/images/*");
		root.addServlet(ssh, "/html/*");
		root.addServlet(ssh, "/css/*");
		root.addServlet(ssh, "/js/*");

		
		//add redirects for the root
		RewriteHandler rh = new RewriteHandler(); 
		rh.setRewriteRequestURI(true); 
		rh.setRewritePathInfo(false); 
		rh.setOriginalPathAttribute("requestedPath"); 
		RewriteRegexRule rule = new RewriteRegexRule(); 
		rule.setRegex("/"); 
		rule.setReplacement("/index.html"); 
		rh.addRule(rule); 
		rh.setHandler(root); 
		server.setHandler(rh);         

		try {
			server.start();
		} catch (Exception e) {
			throw new RuntimeException("fail!", e);
		}
	}
}
