package org.acme.servlet;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.acme.servlet.main.MainRoute;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.PWA;

/**
 * @author Martin Vysny <mavi@vaadin.com>
 */
@PWA(name = "Project Base for Vaadin",
        shortName = "Project Base",
        enableInstallPrompt = false)
@CssImport(value = "./styles/vaadin-text-field-styles.css",
        themeFor = "vaadin-text-field")
@CssImport("./styles/shared-styles.css")
public class MainLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private static final long serialVersionUID = -5345856138696216320L;
    private final H1 currentViewName = new H1();

    private static final Map<Class<? extends Component>, String> routes = new LinkedHashMap<>();
    static {
        routes.put(MainRoute.class, "Welcome");

    }

    private final Map<Class<? extends Component>, Tab> navigationTabMap = new HashMap<>();

    public MainLayout() {

    }

    @Override
    public void afterNavigation(final AfterNavigationEvent event) {

    }
}
