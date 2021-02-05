package org.acme.servlet.serviceinterfaces;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.VisibleForTesting;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;

/**
 * @author Martin Vysny <mavi@vaadin.com>
 */
public class CustomErrorHandler implements ErrorHandler, Serializable {

    private static final AtomicInteger ERROR_ID = new AtomicInteger();
    private static transient final Logger log = LoggerFactory.getLogger(CustomErrorHandler.class);
    private static final long serialVersionUID = -261190722817965684L;

    @VisibleForTesting
    static void resetCounter(final int id) {

        ERROR_ID.set(id);
    }

    @Override
    public void error(final ErrorEvent event) {

        final int id = ERROR_ID.incrementAndGet();
        log.error("Application error #" + id, event.getThrowable());
        Notification
                .show("An application error #" + id + " occurred, please see application logs for details", 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR, NotificationVariant.LUMO_PRIMARY);
    }
}
