package vis.data.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;

import vis.data.model.meta.RequestLogAccessor;

public class RequestLogFilter implements Filter {
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException 
	{
		RequestLogAccessor rla;
		try {
			rla = new RequestLogAccessor();
		} catch (SQLException e) {
			throw new RuntimeException("request logger creation failed", e);
		}
		HttpServletRequest hr = (HttpServletRequest)req;
		String uri = hr.getPathInfo(), json = null;
		
		Date start = new Date();
		
		if(req.getContentType().equals("application/json")) {
			ServletInputStream s = req.getInputStream();
			json = IOUtils.toString(s);
			final String body = json;
			req = new HttpServletRequestWrapper((HttpServletRequest)req) {
				public ServletInputStream getInputStream() throws IOException {
					return new ServletInputStream() {
						InputStream in_ = IOUtils.toInputStream(body);
						@Override
						public int read() throws IOException {
							return in_.read();
						}
					};
				}
			};
		}
		if(json != null)
			chain.doFilter(req, resp);
		Date end = new Date();
		try {
			rla.logRequest(end.getTime() - start.getTime(), uri, json);
		} catch (SQLException e) {
			e.printStackTrace(System.err);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

}
