package vis.data.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.jetty.server.Response;

public class CrossDomainRequestFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		//this has to come in advance 
		if(Response.class.isInstance(resp)) {
			Response hsr = (Response)resp;
			hsr.setHeader("Access-Control-Allow-Origin", "*");
			hsr.setHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS");
			hsr.setHeader("Access-Control-Allow-Headers", "Content-Type,X-Requested-With");
		}
		chain.doFilter(req, resp);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

}
