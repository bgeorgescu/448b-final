package vis.data.server;

import java.io.File;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import vis.data.util.SQL.SQLCloseFilter;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class Launcher {

	public static void main(String[] args) {
		ServletHolder sh = new ServletHolder(ServletContainer.class);
		sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
		sh.setInitParameter("com.sun.jersey.config.property.packages", "vis.data.server");
		sh.setInitParameter("com.sun.jersey.spi.container.ResourceFilters", "com.sun.jersey.api.container.filter.RolesAllowedResourceFilterFactory");
		sh.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
		
		FilterHolder fh = new FilterHolder(SQLCloseFilter.class);
		EnumSet<DispatcherType> fd = EnumSet.of(DispatcherType.REQUEST);

		ServletHolder ssh = new ServletHolder(DefaultServlet.class);

		Server server = new Server(8080);
		ServletContextHandler root = new ServletContextHandler(ServletContextHandler.SESSIONS);
		root.setContextPath("/");
		root.setResourceBase(new File(".").getAbsolutePath());
		//make the default page go to the top level thing
		root.setWelcomeFiles(new String[] { "index.html" } );
		server.setHandler(root);
		//clean up sql automatically
		root.addFilter(fh, "/api/*", fd);
		root.addFilter(fh, "/heartbeat/*", fd);
		//map api
		root.addServlet(sh, "/api/*");
		//add static content path
		root.addServlet(ssh, "/");
		root.addServlet(ssh, "/index.html");
		root.addServlet(ssh, "/html/*");
		root.addServlet(ssh, "/css/*");
		root.addServlet(ssh, "/js/*");
		
		try {
			server.start();
		} catch (Exception e) {
			throw new RuntimeException("fail!", e);
		}
	}
}
