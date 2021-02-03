package org.acme.servlet.extension;

import com.vaadin.flow.server.VaadinSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * This Servlet Filter kicks in at the end of a request to "rewrite" the VaadinSession instance into the HttpSession in order to persist the updated
 * VaadinSession instance in Redis.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class QuarkusRedisFilter implements Filter {

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

        chain.doFilter(request, response);

        if (request instanceof HttpServletRequest) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) request;

            if (!httpServletRequest.getRequestURI().contains("/VAADIN")) {
                final HttpSession session = httpServletRequest.getSession(false);

                if (session == null) {
                    return;
                }
/*
                final DummyBean dummyBean = (DummyBean) session.getAttribute("dummyBean");
                if (dummyBean != null) {
                    System.out.println("_____________________________________________________________________________");
                    System.out.println("DummyBean is restored from session-attribute in session: " + session.getId());
                    dummyBean.sayHello();
                }

                if (dummyBean == null) {
                    System.out.println("_____________________________________________________________________________");
                    System.out.println("DummyBean was null, created new bean in attribute of session: " + session.getId());
                    final DummyBean bean = new DummyBean("test", "lastname", Arrays.asList("Hey", "Ciao", "Buongiorno"));
                    bean.setHelloEventHandler(() -> System.out.println("HEY!! You triggered the hello function"));
                    session.setAttribute("dummyBean", bean);
                }
*/
                for (final Enumeration<String> attributeNames = session.getAttributeNames(); attributeNames.hasMoreElements();) {
                    final String name = attributeNames.nextElement();
                    final Object value = session.getAttribute(name);
                    if (value instanceof VaadinSession) {
                        session.setAttribute(name, value);
                    }
                }


            }
        }
    }

}