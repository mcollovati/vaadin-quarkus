package org.acme.servlet.polymer;

import java.util.ArrayList;
import java.util.List;

import org.acme.servlet.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

/**
 * Demoes that PolymerTemplate-based components and forms works correctly on top of Quarkus.
 * 
 * @author Martin Vysny <mavi@vaadin.com>
 */
@PageTitle("Polymer Template Demo | Vaadin Quarkus Demo")
@Route(value = "polymer",
        layout = MainLayout.class)
@PreserveOnRefresh
public class PolymerExampleRoute extends VerticalLayout {

    private static final long serialVersionUID = 1419808543819845043L;
    // visible for testing
    final UserForm.User user;

    public PolymerExampleRoute() {

        add(new Paragraph("A PolymerTemplate-based component demo"));
        add(new H2("Basic PolymerTemplate With Model"));
        add(new HelloWorld());

        // demo the @Id annotation working well with PolymerTemplate
        add(new H2(getTranslation("test.label")));
        this.user = new UserForm.User();
        this.user.setFirstName("Hello");
        this.user.setLastName("World");
        this.user.setEmail("hello@world.earth");
        this.user.setComment("Hi!");
        final UserForm userForm = new UserForm();
        add(userForm);
        userForm.getBinder().readBean(this.user);
        add(new Button("Save", e -> {
            if (userForm.getBinder().isValid() && userForm.getBinder().writeBeanIfValid(this.user)) {
                System.out.println("Saved bean: " + this.user);
                Notification.show("Saved bean: " + this.user);
            } else {
                Notification.show("Please correct validation errors");
            }
        }));

        // demo a dom-repeat
        add(new H2("TemplateModel With a List of Beans and 'dom-repeat'"));
        final List<UserForm.User> users = new ArrayList<>();
        users.add(new UserForm.User("jd@foo.bar", "John", "D"));
        users.add(new UserForm.User("janed@foo.bar", "Jane", "D"));
        users.add(new UserForm.User("miked@foo.bar", "Mike", "D"));
        final UserTable userTable = new UserTable();
        userTable.setUsers(users);
        add(userTable);
    }
}
