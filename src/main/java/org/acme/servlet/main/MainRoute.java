package org.acme.servlet.main;

import com.vaadin.flow.function.SerializableConsumer;
import org.acme.servlet.MainLayout;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableEventListener;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Port of the "Skeleton Starter" Vaadin app on top of Quarkus+Undertow.
 * 
 * @author Martin Vysny <mavi@vaadin.com>
 */
@PageTitle("Skeleton Starter Demo | Vaadin Quarkus Demo")
@Route(value = "",
        layout = MainLayout.class)
public class MainRoute extends VerticalLayout {

    private static final long serialVersionUID = 2028635200678251124L;

    public MainRoute() {

        add(new Span("Port of the \"Skeleton Starter\" Vaadin app on top of Quarkus+Undertow."));

        // Button click listeners can be defined as lambda expressions
        final Button button = new Button("Say hello");
        button.addClickListener(((SerializableEventListener & ComponentEventListener<ClickEvent<Button>>) e -> Notification.show("test")));

        // Theme variants give you predefined extra styles for components.
        // Example: Primary button is more prominent look.
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // You can specify keyboard shortcuts for buttons.
        // Example: Pressing enter in this view clicks the Button.
        // button.addClickShortcut(Key.ENTER);

        add(button);
    }
}
