package org.acme.servlet.main;

import java.io.Serializable;

public class GreetService implements Serializable {

    private static final long serialVersionUID = 1293914851943829076L;

    public String greet(final String name) {

        if (name == null || name.isEmpty()) {
            return "Hello anonymous user";
        } else {
            return "Hello " + name;
        }
    }
}
