package org.acme.servlet.redisextension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * @author inacta AG
 * @since 1.0
 */
public class DummyBean implements Serializable {

    private static final long serialVersionUID = -93432024994567178L;
    private String firstName;
    private String lastName;
    private List<String> greetings;
    private final Long id;

    public DummyBean(final String firstName, final String lastName, final List<String> greetings) {

        this.firstName = firstName;
        this.lastName = lastName;
        this.greetings = greetings;

        final Random rand = new Random();
        this.id = rand.nextLong();
    }

    public void sayHello() {

        this.greetings.forEach(greeting -> System.out.println(greeting + " " + this.firstName + " " + this.lastName));
        this.helloEventHandler.onHelloEvent();
    }

    @FunctionalInterface
    public interface HelloEventHandler extends Serializable {

        void onHelloEvent();
    }

    private HelloEventHandler helloEventHandler;

    /**
     * Gets the value of the id property.
     *
     * @return possible object is {@link Long}
     */
    public Long getId() {

        return this.id;
    }

    /**
     * Gets the value of the helloEventHandler property.
     *
     * @return possible object is {@link HelloEventHandler}
     */
    public HelloEventHandler getHelloEventHandler() {

        return this.helloEventHandler;
    }

    /**
     * Sets the value of the helloEventHandler property
     *
     * @param helloEventHandler
     *            allowed object is {@link HelloEventHandler}
     * @return the {@link DummyBean}
     */
    public DummyBean setHelloEventHandler(final HelloEventHandler helloEventHandler) {

        this.helloEventHandler = helloEventHandler;
        return this;
    }

    /**
     * Gets the value of the greetings property.
     *
     * @return possible object is {@link List< String>}
     */
    public List<String> getGreetings() {

        if (this.greetings == null) {
            this.greetings = new ArrayList<>();
        }
        return this.greetings;
    }

    /**
     * Sets the value of the greetings property
     *
     * @param greetings
     *            allowed object is {@link List< String>}
     * @return the {@link DummyBean}
     */
    public DummyBean setGreetings(final List<String> greetings) {

        this.greetings = greetings;
        return this;
    }

    /**
     * Gets the value of the firstName property.
     *
     * @return possible object is {@link String}
     */
    public String getFirstName() {

        return this.firstName;
    }

    /**
     * Sets the value of the firstName property
     *
     * @param firstName
     *            allowed object is {@link String}
     * @return the {@link DummyBean}
     */
    public DummyBean setFirstName(final String firstName) {

        this.firstName = firstName;
        return this;
    }

    /**
     * Gets the value of the lastName property.
     *
     * @return possible object is {@link String}
     */
    public String getLastName() {

        return this.lastName;
    }

    /**
     * Sets the value of the lastName property
     *
     * @param lastName
     *            allowed object is {@link String}
     * @return the {@link DummyBean}
     */
    public DummyBean setLastName(final String lastName) {

        this.lastName = lastName;
        return this;
    }

    @Override
    public String toString() {

        return "DummyBean{"
                + "firstName='"
                + this.firstName
                + '\''
                + ", lastName='"
                + this.lastName
                + '\''
                + ", greetings="
                + this.greetings
                + ", helloEventHandler="
                + this.helloEventHandler
                + '}';
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DummyBean dummyBean = (DummyBean) o;
        return Objects.equals(getFirstName(), dummyBean.getFirstName())
                && Objects.equals(getLastName(), dummyBean.getLastName())
                && Objects.equals(getGreetings(), dummyBean.getGreetings())
                && Objects.equals(getHelloEventHandler(), dummyBean.getHelloEventHandler());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getFirstName(), getLastName(), getGreetings(), getHelloEventHandler());
    }
}
