package vis.data.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class CrossDomainRequestFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		chain.doFilter(req, resp);
		if(HttpServletResponse.class.isInstance(resp)) {
			HttpServletResponse hsr = (HttpServletResponse)resp;
			hsr.setHeader("Access-Control-Allow-Origin", "*");
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

}
