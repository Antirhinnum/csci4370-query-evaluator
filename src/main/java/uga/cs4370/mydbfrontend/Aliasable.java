package uga.cs4370.mydbfrontend;

import java.util.Optional;

/**
 * Reprents an object that can be both named and aliased.
 */
public interface Aliasable<T> extends Nameable<T> {

    /**
     * @return The alias of the wrapped object. May be null.
     */
    Optional<String> getAlias();

    /**
     * Sets the alias of the wrapped object. May be null.
     */
    void setAlias(String value);

    /**
     * @return The alias of the wrapped object, or the name if there is no alias.
     */
    String getNameOrAlias();
}
