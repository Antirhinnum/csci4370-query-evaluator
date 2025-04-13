package uga.cs4370.mydbfrontend;

/**
 * Represents an object that can be named.
 */
public interface Nameable<T> {

    /**
     * @return The object being named
     */
    T getValue();

    /**
     * Sets which object is being named
     */
    void setValue(T value);

    /**
     * @return The name of the wrapped object
     */
    String getName();

    /**
     * Sets the name of the wrapped object.
     *
     * @throws NullPointerException If value is null.
     */
    void setName(String value) throws NullPointerException;
}
